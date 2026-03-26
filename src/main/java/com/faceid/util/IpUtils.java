package com.faceid.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extrai o IP real do cliente considerando proxies reversos.
 *
 * Prioridade: X-Forwarded-For → X-Real-IP → remoteAddr.
 *
 * Atenção: X-Forwarded-For pode ser forjado por clientes diretos.
 * Em produção, configure o proxy reverso para sobrescrever (não append) o header.
 */
public final class IpUtils {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
    };

    private IpUtils() {}

    public static String extractClientIp(HttpServletRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For pode ser "ip1, ip2, ip3" — o primeiro é o cliente real
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
