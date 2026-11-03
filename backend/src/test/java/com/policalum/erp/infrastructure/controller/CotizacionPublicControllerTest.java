package com.policalum.erp.infrastructure.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.service.CotizacionService;
import com.policalum.erp.application.service.serviceImpl.CanalWebNotificador;
import com.policalum.erp.application.service.serviceImpl.CotizacionDocumentoService;
import com.policalum.erp.application.service.serviceImpl.LimitePublico;
import com.policalum.erp.application.service.serviceImpl.MensajeWebService;
import com.policalum.erp.infrastructure.security.JwtAuthenticationEntryPoint;
import com.policalum.erp.infrastructure.security.JwtUtil;
import com.policalum.erp.infrastructure.security.SesionUsuarioCache;
import com.policalum.erp.infrastructure.security.TokenDenylist;
import com.policalum.erp.domain.enums.CanalCotizacion;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortalRecepcionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CotizacionPublicControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean CotizacionService cotizaciones;
    @MockitoBean CotizacionDocumentoService documentos;
    @MockitoBean MensajeWebService mensajes;
    @MockitoBean CanalWebNotificador notificador;
    @MockitoBean LimitePublico limite;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean SesionUsuarioCache sesionUsuarioCache;
    @MockitoBean TokenDenylist tokenDenylist;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void postCotizacionValida_responde201() throws Exception {
        when(limite.permitir(any())).thenReturn(true);
        when(cotizaciones.crearDesdeWeb(any())).thenReturn(new CotizacionResponseDTO(
                UUID.randomUUID(), null, null, null, null, null, null,
                EstadoCotizacion.PENDIENTE, new BigDecimal("10.00"), null, null, null, null, List.of(),
                CanalCotizacion.WEB, "NUEVO (WEB)"));

        mvc.perform(post("/api/v1/public/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruc":"20987654321","razonSocial":"Cliente SAC","correo":"ventas@cliente.pe","telefono":"999888777","lineas":[{"productoId":"%s","cantidad":1}]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canal").value("WEB"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void postCotizacionConTelefonoMalformado_responde400() throws Exception {
        when(limite.permitir(any())).thenReturn(true);

        mvc.perform(post("/api/v1/public/cotizaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ruc":"20987654321","razonSocial":"Cliente SAC","correo":"ventas@cliente.pe","telefono":"abc","lineas":[{"productoId":"%s","cantidad":1}]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
