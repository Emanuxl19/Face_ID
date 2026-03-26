package com.faceid.util;

import com.faceid.exception.ForbiddenException;
import com.faceid.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitários de autorização reutilizáveis entre serviços.
 *
 * <p>Centraliza a lógica de IDOR para evitar duplicação em
 * {@code UserService}, {@code FaceRecognitionService} e {@code MultiAngleRegistrationService}.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Garante que o usuário autenticado só acessa seus próprios dados.
     * Administradores (ROLE_ADMIN) podem acessar qualquer recurso.
     *
     * @throws ForbiddenException HTTP 403 se o principal não bater com o dono do recurso.
     */
    public static void requireSelf(User targetUser) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        if (!auth.getName().equals(targetUser.getUsername())) {
            throw new ForbiddenException("Acesso negado: voce so pode acessar seus proprios dados.");
        }
    }
}
