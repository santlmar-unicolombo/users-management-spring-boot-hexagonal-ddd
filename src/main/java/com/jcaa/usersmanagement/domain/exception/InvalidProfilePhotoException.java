package com.jcaa.usersmanagement.domain.exception;

public final class InvalidProfilePhotoException extends DomainException {

  private static final String MESSAGE_EMPTY = "The profile photo must not be empty.";
  private static final String MESSAGE_TOO_LARGE =
      "The profile photo must not exceed %d bytes.";
  private static final String MESSAGE_UNSUPPORTED_FORMAT =
      "The profile photo must be a JPEG, PNG or WEBP image.";
  private static final String MESSAGE_INVALID_KEY = "The profile photo key is invalid.";

  private InvalidProfilePhotoException(final String message) {
    super(message);
  }

  public static InvalidProfilePhotoException becauseContentIsEmpty() {
    return new InvalidProfilePhotoException(MESSAGE_EMPTY);
  }

  public static InvalidProfilePhotoException becauseSizeExceedsLimit(final int maximumBytes) {
    return new InvalidProfilePhotoException(String.format(MESSAGE_TOO_LARGE, maximumBytes));
  }

  public static InvalidProfilePhotoException becauseFormatIsNotSupported() {
    return new InvalidProfilePhotoException(MESSAGE_UNSUPPORTED_FORMAT);
  }

  public static InvalidProfilePhotoException becauseKeyIsInvalid() {
    return new InvalidProfilePhotoException(MESSAGE_INVALID_KEY);
  }
}
