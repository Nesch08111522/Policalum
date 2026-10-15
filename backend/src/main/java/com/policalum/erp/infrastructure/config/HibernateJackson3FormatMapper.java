package com.policalum.erp.infrastructure.config;

import java.lang.reflect.Type;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class HibernateJackson3FormatMapper extends AbstractJsonFormatMapper {

    // Instancia el ObjectMapper usando el constructor/builder de Jackson 3
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Override
    protected <T> T fromString(CharSequence charSequence, Type type) {
        try {
            return MAPPER.readValue(charSequence.toString(), MAPPER.constructType(type));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al deserializar JSON para el tipo: " + type, e);
        }
    }
    
    @Override
    protected <T> String toString(T value, Type type) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al serializar el valor a JSON para el tipo: " + type, e);
        }
    }
}