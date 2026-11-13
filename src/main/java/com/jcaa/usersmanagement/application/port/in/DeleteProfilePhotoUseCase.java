package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.command.DeleteProfilePhotoCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface DeleteProfilePhotoUseCase {
  void execute(@NotNull @Valid DeleteProfilePhotoCommand command);
}
