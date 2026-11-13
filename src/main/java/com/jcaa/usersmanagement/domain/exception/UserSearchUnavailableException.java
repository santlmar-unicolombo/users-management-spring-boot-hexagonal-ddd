package com.jcaa.usersmanagement.domain.exception;

/** El motor de búsqueda no está disponible o no pudo atender la consulta. */
public final class UserSearchUnavailableException extends DomainException {

  private static final String MESSAGE_FAILED = "The user search service is not available.";
  private static final String MESSAGE_NOT_CONFIGURED =
      "The user search service is not configured.";

  private UserSearchUnavailableException(final String message) {
    super(message);
  }

  private UserSearchUnavailableException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static UserSearchUnavailableException becauseSearchFailed(final Throwable cause) {
    return new UserSearchUnavailableException(MESSAGE_FAILED, cause);
  }

  public static UserSearchUnavailableException becauseSearchIsNotConfigured() {
    return new UserSearchUnavailableException(MESSAGE_NOT_CONFIGURED);
  }
}
