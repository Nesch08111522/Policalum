package com.policalum.erp.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtUtil jwtUtil;
    @Mock SesionUsuarioCache sesionUsuarioCache;
    @Mock TokenDenylist tokenDenylist;
    @Mock FilterChain chain;
    @InjectMocks JwtAuthenticationFilter filter;

    @Test
    void tokenVencido_responde401JsonYNoContinua() throws Exception {
        when(jwtUtil.getClaims("vencido")).thenThrow(new ExpiredJwtException(null, new DefaultClaims(), "expirado"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/ventas");
        request.addHeader("Authorization", "Bearer vencido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"status\":401"));
        assertTrue(response.getContentAsString().contains("Sesión expirada o token no válido"));
        verify(chain, never()).doFilter(any(), any());
    }
}
