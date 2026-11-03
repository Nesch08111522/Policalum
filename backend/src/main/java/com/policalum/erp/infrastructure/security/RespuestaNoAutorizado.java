package com.policalum.erp.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;

/** 401 JSON del filtro y del punto de entrada, el mismo contrato que el resto de la API. */
final class RespuestaNoAutorizado {

    private RespuestaNoAutorizado() {
    }

    static void escribir(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String ruta = request.getRequestURI() == null ? "" : escapar(request.getRequestURI());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
                + "\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + escapar(message)
                + "\",\"path\":\"" + ruta + "\"}");
    }

    private static String escapar(String valor) {
        return valor.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
