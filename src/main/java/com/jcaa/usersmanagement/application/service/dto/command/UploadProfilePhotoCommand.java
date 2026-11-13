package com.jcaa.usersmanagement.application.service.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UploadProfilePhotoCommand(
    @NotBlank(message = "id must not be blank") String id,
    @NotNull(message = "content must not be null") byte[] content) {}
