package com.jcaa.usersmanagement.application.service.dto.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Consulta de búsqueda. {@code text} se busca en nombre y correo (admite errores de escritura);
 * {@code role} y {@code status} son filtros exactos opcionales.
 */
public record SearchUsersQuery(
    @Size(max = 100, message = "q must have at most 100 characters") String text,
    String role,
    String status,
    @Min(value = 1, message = "size must be at least 1")
        @Max(value = 50, message = "size must be at most 50")
        int size) {}
