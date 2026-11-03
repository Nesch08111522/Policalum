package com.policalum.erp.infrastructure.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.policalum.erp.application.dto.response.CmsPublicoDTO;
import com.policalum.erp.application.service.serviceImpl.CmsPublicador;
import com.policalum.erp.application.service.serviceImpl.CmsService;
import com.policalum.erp.application.service.serviceImpl.PortafolioService;
import com.policalum.erp.infrastructure.security.JwtAuthenticationEntryPoint;
import com.policalum.erp.infrastructure.security.JwtUtil;
import com.policalum.erp.infrastructure.security.SesionUsuarioCache;
import com.policalum.erp.infrastructure.security.TokenDenylist;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CmsController.class)
@AutoConfigureMockMvc(addFilters = false)
class CmsControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean CmsService cms;
    @MockitoBean CmsPublicador publicador;
    @MockitoBean PortafolioService portafolio;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean SesionUsuarioCache sesionUsuarioCache;
    @MockitoBean TokenDenylist tokenDenylist;
    @MockitoBean JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void cmsPublico_responde200() throws Exception {
        when(cms.publico()).thenReturn(new CmsPublicoDTO("#111111", "#FFFFFF", 16, "claro", "", false,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        mvc.perform(get("/api/v1/public/cms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#111111"))
                .andExpect(jsonPath("$.themeMode").value("claro"));
    }

    @Test
    void guardarAjustesConColorInvalido_responde400() throws Exception {
        mvc.perform(put("/api/v1/cms/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"primaryColor\":\"azul\",\"secondaryColor\":\"#FFFFFF\",\"baseFontSize\":16,\"themeMode\":\"claro\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
