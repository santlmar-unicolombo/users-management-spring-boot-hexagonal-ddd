package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.out.DeleteUserPort;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteUserService implements DeleteUserUseCase {

  private static final String LOG_PHOTO_NOT_DELETED =
      "[DeleteUserService] la foto de perfil no se borro del bucket. Causa: {}";

  private final DeleteUserPort deleteUserPort;
  private final GetUserByIdPort getUserByIdPort;
  private final ProfilePhotoStoragePort profilePhotoStoragePort;
  private final UserReadModelSyncService userReadModelSyncService;
  private final Validator validator;

  @Override
  public void execute(final DeleteUserCommand command) {
    validateCommand(command);

    final UserId userId = UserApplicationMapper.fromDeleteCommandToUserId(command);
    final UserModel existing = findExistingUserOrFail(userId);
    deleteUserPort.delete(userId);

    existing.getProfilePhotoKey().ifPresent(this::deletePhotoQuietly);
    userReadModelSyncService.onUserDeleted(userId);
  }

  private void validateCommand(final DeleteUserCommand command) {
    final Set<ConstraintViolation<DeleteUserCommand>> violations = validator.validate(command);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private UserModel findExistingUserOrFail(final UserId userId) {
    return getUserByIdPort
        .getById(userId)
        .orElseThrow(() -> UserNotFoundException.becauseIdWasNotFound(userId.value()));
  }

  private void deletePhotoQuietly(final ProfilePhotoKey key) {
    try {
      profilePhotoStoragePort.delete(key);
    } catch (final DomainException storageFailure) {
      log.warn(LOG_PHOTO_NOT_DELETED, storageFailure.getClass().getSimpleName());
    }
  }
}
