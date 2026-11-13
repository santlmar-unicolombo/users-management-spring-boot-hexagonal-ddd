package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.UploadProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.port.out.UpdateUserProfilePhotoPort;
import com.jcaa.usersmanagement.application.service.dto.command.UploadProfilePhotoCommand;
import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sube (o reemplaza) la foto de perfil de un usuario.
 *
 * <p>Orden: 1) validar la imagen, 2) subirla al bucket con una key nueva, 3) guardar la key en la
 * base de datos, 4) borrar la foto anterior del bucket. Si el paso 3 falla, se borra el objeto
 * recién subido para no dejar archivos huérfanos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UploadProfilePhotoService implements UploadProfilePhotoUseCase {

  private static final String LOG_ROLLBACK_FAILED =
      "[UploadProfilePhotoService] no se pudo borrar la foto huerfana. Causa: {}";
  private static final String LOG_OLD_PHOTO_NOT_DELETED =
      "[UploadProfilePhotoService] la foto anterior no se borro del bucket. Causa: {}";

  private final GetUserByIdPort getUserByIdPort;
  private final UpdateUserProfilePhotoPort updateUserProfilePhotoPort;
  private final ProfilePhotoStoragePort profilePhotoStoragePort;
  private final UserReadModelSyncService userReadModelSyncService;
  private final Validator validator;

  @Override
  public UserModel execute(final UploadProfilePhotoCommand command) {
    validateCommand(command);

    final UserId userId = new UserId(command.id());
    final UserModel current = findExistingUserOrFail(userId);
    final ProfilePhotoModel photo = ProfilePhotoModel.fromUpload(command.content());
    final ProfilePhotoKey newKey = ProfilePhotoKey.forUser(userId, photo.getFormat());

    profilePhotoStoragePort.upload(newKey, photo);
    final UserModel updated = saveReferenceOrRollback(current.withProfilePhoto(newKey), newKey);

    current.getProfilePhotoKey().ifPresent(this::deleteQuietly);
    userReadModelSyncService.onUserChanged(updated);
    return updated;
  }

  private void validateCommand(final UploadProfilePhotoCommand command) {
    final Set<ConstraintViolation<UploadProfilePhotoCommand>> violations =
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

  private UserModel saveReferenceOrRollback(
      final UserModel userWithPhoto, final ProfilePhotoKey uploadedKey) {
    try {
      return updateUserProfilePhotoPort.updateProfilePhoto(userWithPhoto);
    } catch (final RuntimeException persistenceFailure) {
      deleteQuietly(uploadedKey, LOG_ROLLBACK_FAILED);
      throw persistenceFailure;
    }
  }

  private void deleteQuietly(final ProfilePhotoKey key) {
    deleteQuietly(key, LOG_OLD_PHOTO_NOT_DELETED);
  }

  private void deleteQuietly(final ProfilePhotoKey key, final String logMessage) {
    try {
      profilePhotoStoragePort.delete(key);
    } catch (final DomainException storageFailure) {
      log.warn(logMessage, storageFailure.getClass().getSimpleName());
    }
  }
}
