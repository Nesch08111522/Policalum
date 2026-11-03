package com.policalum.erp.exception;

/** Regla de negocio rechazada antes de persistir. El cliente recibe 400. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
