package com.policalum.erp.application.dto.request;
import  java.util.Map;

import com.policalum.erp.domain.enums.RolNombre;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
public record RolRequestDTO(
    @NotNull(message = "El nombre del rol no puede ser nulo")
    RolNombre nombre,
    @NotEmpty(message = "Los permisos no pueden estar vacios)")
    Map<String, Object> permisosJson

) {

   

}
