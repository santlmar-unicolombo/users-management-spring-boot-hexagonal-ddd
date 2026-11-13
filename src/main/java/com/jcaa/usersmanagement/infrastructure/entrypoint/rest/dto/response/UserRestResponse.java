package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

/**
 * {@code profilePhotoUrl} es la ruta de la API que entrega la foto (requiere token). Es
 * {@code null} cuando el usuario no tiene foto. El bucket es privado, por eso no se expone su URL.
 */
public record UserRestResponse(
    String id,
    String name,
    String email,
    String role,
    String status,
    String profilePhotoUrl) {}
