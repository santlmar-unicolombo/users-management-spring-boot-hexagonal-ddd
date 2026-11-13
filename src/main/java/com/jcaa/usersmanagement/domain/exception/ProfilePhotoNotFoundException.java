package com.jcaa.usersmanagement.domain.exception;

public final class ProfilePhotoNotFoundException extends DomainException {

  private static final String MESSAGE_BY_USER = "The user with id '%s' has no profile photo.";

  private ProfilePhotoNotFoundException(final String message) {
    super(message);
  }

  public static ProfilePhotoNotFoundException becauseUserHasNoPhoto(final String userId) {
    return new ProfilePhotoNotFoundException(String.format(MESSAGE_BY_USER, userId));
  }
}
