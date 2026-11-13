package com.jcaa.usersmanagement.infrastructure.adapter.cache;

/**
 * Representación JSON del usuario guardada en Valkey. Incluye el hash BCrypt (nunca la contraseña
 * en texto plano) porque el modelo de dominio lo exige; la instancia exige autenticación y TLS.
 */
public record CachedUserEntry(
    String id,
    String name,
    String email,
    String passwordHash,
    String role,
    String status,
    String profilePhotoKey) {}
