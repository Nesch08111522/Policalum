package com.policalum.erp.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring no empareja /api/v1/perfil/ con @RequestMapping("/api/v1/perfil").
 * Esa petición caía en el recurso estático y el handler genérico respondía 500.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ApiTrailingSlashFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") && uri.length() > "/api/".length() && uri.endsWith("/")) {
            request.getRequestDispatcher(uri.substring(0, uri.length() - 1)).forward(request, response);
            return;
        }
        chain.doFilter(request, response);
    }
}
