package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.DeleteProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.port.out.UpdateUserProfilePhotoPort;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteProfilePhotoCommand;
import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Elimina la foto de perfil. Primero se quita la referencia en la base de datos (el usuario deja
 * de "tener" foto de inmediato) y después se borra el objeto del bucket. La operación es
 * idempotente: si el usuario no tiene foto, no hace nada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteProfilePhotoService implements DeleteProfilePhotoUseCase {

  private static final String LOG_DELETE_FAILED =
      "[DeleteProfilePhotoService] la foto no se borro del bucket. Causa: {}";

  private final GetUserByIdPort getUserByIdPort;
  private final UpdateUserProfilePhotoPort updateUserProfilePhotoPort;
  private final ProfilePhotoStoragePort profilePhotoStoragePort;
  private final UserReadModelSyncService userReadModelSyncService;
  private final Validator validator;

  @Override
  public void execute(final DeleteProfilePhotoCommand command) {
    validateCommand(command);

    final UserId userId = new UserId(command.id());
    final UserModel current = findExistingUserOrFail(userId);
    final Optional<ProfilePhotoKey> currentKey = current.getProfilePhotoKey();
    if (currentKey.isEmpty()) {
      return;
    }

    final UserModel updated =
        updateUserProfilePhotoPort.updateProfilePhoto(current.withoutProfilePhoto());
    deleteQuietly(currentKey.get());
    userReadModelSyncService.onUserChanged(updated);
  }

  private void validateCommand(final DeleteProfilePhotoCommand command) {
    final Set<ConstraintViolation<DeleteProfilePhotoCommand>> violations =
        validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private UserModel findExistingUserOrFail(final UserId userId) {
    return getUserByIdPort
        .getById(userId)
        .orElseThrow(() -> UserNotFoundException.becauseIdWasNotFound(userId.value()));
  }

  private void deleteQuietly(final ProfilePhotoKey key) {
    try {
      profilePhotoStoragePort.delete(key);
    } catch (final DomainException storageFailure) {
      log.warn(LOG_DELETE_FAILED, storageFailure.getClass().getSimpleName());
    }
  }
}
