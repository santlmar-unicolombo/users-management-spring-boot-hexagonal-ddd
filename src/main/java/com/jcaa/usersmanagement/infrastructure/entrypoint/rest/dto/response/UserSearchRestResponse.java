package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response;

public record UserSearchRestResponse(
    String id,
    String name,
    String email,
    String role,
    String status,
    String profilePhotoUrl,
    double score) {}
