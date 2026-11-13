package com.jcaa.usersmanagement.infrastructure.adapter.search;

/** Documento JSON almacenado en el índice de usuarios de OpenSearch. Nunca incluye la contraseña. */
public record UserSearchDocument(
    String id,
    String name,
    String email,
    String role,
    String status,
    boolean hasProfilePhoto) {}
