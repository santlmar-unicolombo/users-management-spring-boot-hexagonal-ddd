package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.enums.ProfilePhotoFormat;
import com.jcaa.usersmanagement.domain.exception.InvalidProfilePhotoException;
import java.util.Arrays;
import java.util.Objects;

/**
 * Imagen de perfil validada: no vacía, de tamaño acotado y en un formato admitido (detectado por
 * su firma binaria). El arreglo de bytes se copia de forma defensiva para mantener inmutabilidad.
 */
public final class ProfilePhotoModel {

  public static final int MAXIMUM_SIZE_BYTES = 2 * 1024 * 1024;

  private final byte[] content;
  private final ProfilePhotoFormat format;

  private ProfilePhotoModel(final byte[] content, final ProfilePhotoFormat format) {
    this.content = content;
    this.format = format;
  }

  /** Valida el contenido recibido del cliente y detecta su formato real. */
  public static ProfilePhotoModel fromUpload(final byte[] content) {
    Objects.requireNonNull(content, "Profile photo content cannot be null");
    if (content.length == 0) {
      throw InvalidProfilePhotoException.becauseContentIsEmpty();
    }
    if (content.length > MAXIMUM_SIZE_BYTES) {
      throw InvalidProfilePhotoException.becauseSizeExceedsLimit(MAXIMUM_SIZE_BYTES);
    }
    final ProfilePhotoFormat format = ProfilePhotoFormat.detect(content);
    return new ProfilePhotoModel(Arrays.copyOf(content, content.length), format);
  }

  /**
   * Reconstruye una foto ya almacenada (bucket o caché). No vuelve a validar el tamaño, pero sí
   * detecta el formato a partir del contenido para servirla con el Content-Type correcto.
   */
  public static ProfilePhotoModel fromStorage(final byte[] content) {
    Objects.requireNonNull(content, "Profile photo content cannot be null");
    final ProfilePhotoFormat format = ProfilePhotoFormat.detect(content);
    return new ProfilePhotoModel(Arrays.copyOf(content, content.length), format);
  }

  public byte[] getContent() {
    return Arrays.copyOf(content, content.length);
  }

  public ProfilePhotoFormat getFormat() {
    return format;
  }

  public int getSizeInBytes() {
    return content.length;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) return true;
    if (!(other instanceof ProfilePhotoModel photo)) return false; // NOSONAR
    return format == photo.format && Arrays.equals(content, photo.content);
  }

  @Override
  public int hashCode() {
    return 31 * Objects.hash(format) + Arrays.hashCode(content);
  }

  @Override
  public String toString() {
    return "ProfilePhotoModel[format=" + format + ", sizeInBytes=" + content.length + "]";
  }
}
