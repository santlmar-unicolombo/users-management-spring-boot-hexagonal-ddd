package com.jcaa.usersmanagement.domain.exception;

/** El almacenamiento de archivos (bucket) no pudo completar la operación solicitada. */
public final class ProfilePhotoStorageException extends DomainException {

  private static final String MESSAGE_UPLOAD = "The profile photo could not be stored.";
  private static final String MESSAGE_DOWNLOAD = "The profile photo could not be retrieved.";
  private static final String MESSAGE_DELETE = "The profile photo could not be deleted.";
  private static final String MESSAGE_NOT_CONFIGURED =
      "The file storage service is not configured.";

  private ProfilePhotoStorageException(final String message) {
    super(message);
  }

  private ProfilePhotoStorageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public static ProfilePhotoStorageException becauseUploadFailed(final Throwable cause) {
    return new ProfilePhotoStorageException(MESSAGE_UPLOAD, cause);
  }

  public static ProfilePhotoStorageException becauseDownloadFailed(final Throwable cause) {
    return new ProfilePhotoStorageException(MESSAGE_DOWNLOAD, cause);
  }

  public static ProfilePhotoStorageException becauseDeleteFailed(final Throwable cause) {
    return new ProfilePhotoStorageException(MESSAGE_DELETE, cause);
  }

  public static ProfilePhotoStorageException becauseStorageIsNotConfigured() {
    return new ProfilePhotoStorageException(MESSAGE_NOT_CONFIGURED);
  }
}
