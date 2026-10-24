package com.policalum.erp.infrastructure.security;

import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;  // ← Inyectar UserRepository

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String email = jwtUtil.getEmailFromToken(token);
                String rol = jwtUtil.getRolFromToken(token);

                User user = userRepository.findByEmail(email).orElse(null);

                int sesionToken = jwtUtil.getSesionVersion(token);
                int sesionUsuario = user == null || user.getSesionVersion() == null ? 0 : user.getSesionVersion();
                if (user == null || !user.getActivo() || sesionToken != sesionUsuario) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Usuario inactivo o sesión cerrada");
                    return;
                }

                // Crear autenticación solo si el usuario está activo
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority(normalizarRol(rol)))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * El JWT guarda el enum RolNombre tal cual ("ROLE_ADMIN"). Antes se le anteponía
     * otro "ROLE_" y quedaba "ROLE_ROLE_ADMIN", con lo que hasRole('ADMIN') nunca coincidía.
     * Se normaliza para aceptar ambos formatos ("ROLE_ADMIN" o "ADMIN").
     */
    private static String normalizarRol(String rol) {
        if (rol == null) return "ROLE_ANONYMOUS";
        return rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;
    }
}
