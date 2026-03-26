package com.faceid.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT: intercepta cada requisição, valida o token Bearer e popula o SecurityContext.
 *
 * Fluxo:
 *   1. Extrai o header Authorization: Bearer <token>
 *   2. Valida a assinatura e expiração via JwtService
 *   3. Carrega o UserDetails e define a autenticação no SecurityContextHolder
 *   4. Passa para o próximo filtro (stateless — sem sessão HTTP)
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService             jwtService;
    private final UserDetailsService     userDetailsService;
    private final TokenBlacklistService  tokenBlacklistService;

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsService userDetailsService,
                         TokenBlacklistService tokenBlacklistService) {
        this.jwtService            = jwtService;
        this.userDetailsService    = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtService.isValid(token)
                && !tokenBlacklistService.isBlacklisted(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String username = jwtService.extractUsername(token);
            var userDetails = userDetailsService.loadUserByUsername(username);

            var auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
