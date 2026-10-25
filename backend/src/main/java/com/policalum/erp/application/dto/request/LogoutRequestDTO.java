package com.policalum.erp.application.dto.request;

/** El refresh es opcional: cerrar esta sesión no debe tumbar las otras del mismo usuario. */
public record LogoutRequestDTO(String refreshToken) {}
