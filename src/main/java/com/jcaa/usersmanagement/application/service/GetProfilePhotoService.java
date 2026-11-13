package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.GetProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.application.service.dto.query.GetProfilePhotoQuery;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoNotFoundException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Entrega la foto de perfil de un usuario aplicando cache-aside en dos niveles: primero se resuelve
 * el usuario (caché → base de datos) para conocer la key de su foto, y luego la imagen (caché →
 * bucket). Cachear la imagen evita descargas repetidas desde Backblaze B2, cuyo plan gratuito
 * limita el ancho de banda y las transacciones diarias.
 */
@Service
@RequiredArgsConstructor
public class GetProfilePhotoService implements GetProfilePhotoUseCase {

  private final GetUserByIdPort getUserByIdPort;
  private final ProfilePhotoStoragePort profilePhotoStoragePort;
  private final UserCachePort userCachePort;
  private final Validator validator;

  @Override
  public ProfilePhotoModel execute(final GetProfilePhotoQuery query) {
    validateQuery(query);

    final UserId userId = new UserId(query.id());
    final UserModel user = findUserOrFail(userId);
    final ProfilePhotoKey key =
        user.getProfilePhotoKey()
            .orElseThrow(() -> ProfilePhotoNotFoundException.becauseUserHasNoPhoto(userId.value()));

    return userCachePort
        .getProfilePhoto(key)
        .orElseGet(() -> downloadAndCache(key, userId));
  }

  private void validateQuery(final GetProfilePhotoQuery query) {
    final Set<ConstraintViolation<GetProfilePhotoQuery>> violations = validator.validate(query);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }

  private UserModel findUserOrFail(final UserId userId) {
    return userCachePort
        .getUser(userId)
        .or(() -> loadUserAndCache(userId))
        .orElseThrow(() -> UserNotFoundException.becauseIdWasNotFound(userId.value()));
  }

  private Optional<UserModel> loadUserAndCache(final UserId userId) {
    final Optional<UserModel> user = getUserByIdPort.getById(userId);
    user.ifPresent(userCachePort::putUser);
    return user;
  }

  private ProfilePhotoModel downloadAndCache(final ProfilePhotoKey key, final UserId userId) {
    final ProfilePhotoModel photo =
        profilePhotoStoragePort
            .download(key)
            .orElseThrow(() -> ProfilePhotoNotFoundException.becauseUserHasNoPhoto(userId.value()));
    userCachePort.putProfilePhoto(key, photo);
    return photo;
  }
}
