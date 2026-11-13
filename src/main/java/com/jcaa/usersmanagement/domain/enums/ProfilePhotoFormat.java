package com.jcaa.usersmanagement.domain.enums;

import com.jcaa.usersmanagement.domain.exception.InvalidProfilePhotoException;
import java.util.Objects;

/**
 * Formatos de imagen aceptados para la foto de perfil.
 *
 * <p>El formato se detecta a partir de los primeros bytes del archivo (firma o "magic bytes") y no
 * a partir del Content-Type enviado por el cliente, que puede ser falsificado.
 */
public enum ProfilePhotoFormat {
  JPEG("image/jpeg", "jpg"),
  PNG("image/png", "png"),
  WEBP("image/webp", "webp");

  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
  private static final byte[] PNG_SIGNATURE = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
  };
  private static final byte[] RIFF_SIGNATURE = {0x52, 0x49, 0x46, 0x46};
  private static final byte[] WEBP_SIGNATURE = {0x57, 0x45, 0x42, 0x50};
  private static final int WEBP_SIGNATURE_OFFSET = 8;

  private final String contentType;
  private final String extension;

  ProfilePhotoFormat(final String contentType, final String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public String contentType() {
    return contentType;
  }

  public String extension() {
    return extension;
  }

  public static ProfilePhotoFormat detect(final byte[] content) {
    Objects.requireNonNull(content, "Profile photo content cannot be null");
    if (startsWith(content, JPEG_SIGNATURE, 0)) {
      return JPEG;
    }
    if (startsWith(content, PNG_SIGNATURE, 0)) {
      return PNG;
    }
    if (startsWith(content, RIFF_SIGNATURE, 0)
        && startsWith(content, WEBP_SIGNATURE, WEBP_SIGNATURE_OFFSET)) {
      return WEBP;
    }
    throw InvalidProfilePhotoException.becauseFormatIsNotSupported();
  }

  private static boolean startsWith(
      final byte[] content, final byte[] signature, final int offset) {
    if (content.length < offset + signature.length) {
      return false;
    }
    for (int index = 0; index < signature.length; index++) {
      if (content[offset + index] != signature[index]) {
        return false;
      }
    }
    return true;
  }
}
