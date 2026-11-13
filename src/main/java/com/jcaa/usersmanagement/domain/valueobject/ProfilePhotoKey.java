package com.jcaa.usersmanagement.domain.valueobject;

import com.jcaa.usersmanagement.domain.enums.ProfilePhotoFormat;
import com.jcaa.usersmanagement.domain.exception.InvalidProfilePhotoException;
import java.util.Objects;
import java.util.UUID;

/**
 * Nombre (key) del objeto que contiene la foto de perfil dentro del bucket.
 *
 * <p>Cada carga genera una key nueva ({@code profile-photos/<userId>/<uuid>.<ext>}), de modo que
 * el contenido asociado a una key nunca cambia. Esto permite cachear la imagen por key sin riesgo
 * de servir una versión desactualizada.
 */
public record ProfilePhotoKey(String value) {

  private static final String PREFIX = "profile-photos/";
  private static final String KEY_TEMPLATE = PREFIX + "%s/%s.%s";
  private static final String UNSAFE_CHARACTERS = "[^A-Za-z0-9_-]";
  private static final String SAFE_REPLACEMENT = "_";
  private static final int MAXIMUM_LENGTH = 255;

  public ProfilePhotoKey {
    final String normalizedValue =
        Objects.requireNonNull(value, "ProfilePhotoKey cannot be null").trim();
    validate(normalizedValue);
    value = normalizedValue;
  }

  public static ProfilePhotoKey forUser(final UserId userId, final ProfilePhotoFormat format) {
    Objects.requireNonNull(userId, "UserId cannot be null");
    Objects.requireNonNull(format, "ProfilePhotoFormat cannot be null");
    final String safeUserId = userId.value().replaceAll(UNSAFE_CHARACTERS, SAFE_REPLACEMENT);
    return new ProfilePhotoKey(
        String.format(KEY_TEMPLATE, safeUserId, UUID.randomUUID(), format.extension()));
  }

  private static void validate(final String normalizedValue) {
    if (!normalizedValue.startsWith(PREFIX) || normalizedValue.length() > MAXIMUM_LENGTH) {
      throw InvalidProfilePhotoException.becauseKeyIsInvalid();
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
