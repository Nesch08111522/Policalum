package com.policalum.erp.application.dto.response;

import java.util.List;

public record PaginaPublicaDTO<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
