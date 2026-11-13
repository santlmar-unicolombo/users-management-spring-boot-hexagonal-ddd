package com.jcaa.usersmanagement.infrastructure.adapter.persistence.entity;

public record UserEntity(
    String id,
    String name,
    String email,
    String password,
    String role,
    String status,
    String profilePhotoKey,
    String createdAt,
    String updatedAt) {}
