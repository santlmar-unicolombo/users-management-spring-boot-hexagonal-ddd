package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.GetUserByIdUseCase;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Consulta un usuario por id aplicando cache-aside: primero se busca en la caché distribuida
 * (Valkey); si no está, se lee de la base de datos y se guarda en caché con un TTL.
 */
@Service
@RequiredArgsConstructor
public class GetUserByIdService implements GetUserByIdUseCase {

  private final GetUserByIdPort getUserByIdPort;
  private final UserCachePort userCachePort;
  private final Validator validator;

  @Override
  public UserModel execute(final GetUserByIdQuery query) {
    validateQuery(query);

    final UserId userId = UserApplicationMapper.fromGetUserByIdQueryToUserId(query);
    return userCachePort
        .getUser(userId)
        .or(() -> loadFromDatabaseAndCache(userId))
        .orElseThrow(() -> UserNotFoundException.becauseIdWasNotFound(userId.value()));
  }

  private Optional<UserModel> loadFromDatabaseAndCache(final UserId userId) {
    final Optional<UserModel> user = getUserByIdPort.getById(userId);
    user.ifPresent(userCachePort::putUser);
    return user;
  }

  private void validateQuery(final GetUserByIdQuery query) {
    final Set<ConstraintViolation<GetUserByIdQuery>> violations = validator.validate(query);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
