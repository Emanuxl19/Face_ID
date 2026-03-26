package com.faceid.controller;

import com.faceid.model.AuditEvent;
import com.faceid.security.JwtService;
import com.faceid.security.LoginAttemptService;
import com.faceid.security.TokenBlacklistService;
import com.faceid.service.AuditLogService;
import com.faceid.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Autenticação via username/password → retorna JWT.
 *
 * <p>Proteções implementadas:
 * <ul>
 *   <li>Rate limiting por IP: {@value LoginAttemptService#MAX_ATTEMPTS} falhas
 *       em 15 minutos → HTTP 429 Too Many Requests.</li>
 *   <li>Audit log assíncrono de LOGIN_SUCCESS, LOGIN_FAILURE e LOGIN_BLOCKED.</li>
 *   <li>Tempo de resposta constante em falhas (não revela se o usuário existe).</li>
 * </ul>
 *
 * POST /api/auth/login
 *   Body:  { "username": "...", "password": "..." }
 *   Resp:  { "token": "...", "expiresAt": "..." }
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager  authManager;
    private final JwtService             jwtService;
    private final LoginAttemptService    loginAttemptService;
    private final TokenBlacklistService  tokenBlacklistService;
    private final AuditLogService        auditLogService;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          LoginAttemptService loginAttemptService,
                          TokenBlacklistService tokenBlacklistService,
                          AuditLogService auditLogService) {
        this.authManager          = authManager;
        this.jwtService           = jwtService;
        this.loginAttemptService  = loginAttemptService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.auditLogService      = auditLogService;
    }

    /**
     * Invalida o token JWT atual.
     * O token é adicionado à blacklist — tentativas de uso subsequentes retornam 401.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isValid(token)) {
                tokenBlacklistService.blacklist(token, jwtService.extractExpiration(token));
                auditLogService.log(AuditEvent.LOGOUT, null,
                        IpUtils.extractClientIp(httpRequest), "Token invalidado via logout");
            }
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extractClientIp(httpRequest);

        // ── Rate limiting ────────────────────────────────────────────────
        if (loginAttemptService.isBlocked(ip)) {
            auditLogService.log(AuditEvent.LOGIN_BLOCKED, null, ip,
                    "IP bloqueado — excesso de tentativas para usuario: " + request.username());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // ── Autenticação ─────────────────────────────────────────────────
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(ip);
            auditLogService.log(AuditEvent.LOGIN_FAILURE, null, ip,
                    "Credenciais invalidas para usuario: " + request.username()
                    + " — tentativas restantes: " + loginAttemptService.remainingAttempts(ip));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        loginAttemptService.loginSucceeded(ip);
        String token     = jwtService.generateToken(request.username());
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());
        auditLogService.log(AuditEvent.LOGIN_SUCCESS, null, ip,
                "Login bem-sucedido: " + request.username());

        return ResponseEntity.ok(new LoginResponse(token, expiresAt));
    }

    // ── Records internos ─────────────────────────────────────────────────────

    public record LoginRequest(
            @NotBlank(message = "Username obrigatorio") String username,
            @NotBlank(message = "Senha obrigatoria")   String password
    ) {}

    public record LoginResponse(String token, Instant expiresAt) {}
}
