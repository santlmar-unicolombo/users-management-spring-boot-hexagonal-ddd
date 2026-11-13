package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;

/** Documento de usuario devuelto por el motor de búsqueda, con su puntaje de relevancia. */
public record UserSearchResultModel(
    String id,
    String name,
    String email,
    UserRole role,
    UserStatus status,
    boolean hasProfilePhoto,
    double score) {}
