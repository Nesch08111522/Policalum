package com.policalum.erp.infrastructure.security;

import com.policalum.erp.infrastructure.security.SesionUsuarioCache.Estado;
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
    private final SesionUsuarioCache sesionUsuarioCache;
    private final TokenDenylist tokenDenylist;

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
                if (tokenDenylist.revocado(token)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Usuario inactivo o sesión cerrada");
                    return;
                }
                String email = jwtUtil.getEmailFromToken(token);
                String rol = jwtUtil.getRolFromToken(token);

                Estado sesion = sesionUsuarioCache.consultar(email);

                int sesionToken = jwtUtil.getSesionVersion(token);
                if (!sesion.existe() || !sesion.activo() || sesionToken != sesion.version()) {
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
