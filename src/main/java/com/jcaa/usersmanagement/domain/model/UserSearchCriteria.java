package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import java.util.Optional;
import lombok.Value;

/** Criterios de búsqueda de usuarios: texto libre (nombre o correo) y filtros opcionales. */
@Value
public class UserSearchCriteria {

  String text;
  UserRole role;
  UserStatus status;
  int size;

  public Optional<String> getText() {
    return Optional.ofNullable(text).map(String::trim).filter(value -> !value.isEmpty());
  }

  public Optional<UserRole> getRole() {
    return Optional.ofNullable(role);
  }

  public Optional<UserStatus> getStatus() {
    return Optional.ofNullable(status);
  }
}
