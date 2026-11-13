package com.jcaa.usersmanagement.domain.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.jcaa.usersmanagement.domain.enums.ProfilePhotoFormat;
import com.jcaa.usersmanagement.domain.exception.InvalidProfilePhotoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProfilePhotoKey")
class ProfilePhotoKeyTest {

  @Test
  @DisplayName("forUser() genera una key única bajo profile-photos/<id>/ con la extensión correcta")
  void shouldGenerateUniqueKeyForUser() {
    // Arrange
    final UserId userId = new UserId("u-001");

    // Act
    final ProfilePhotoKey first = ProfilePhotoKey.forUser(userId, ProfilePhotoFormat.PNG);
    final ProfilePhotoKey second = ProfilePhotoKey.forUser(userId, ProfilePhotoFormat.PNG);

    // Assert
    assertAll(
        "key generada",
        () -> assertTrue(first.value().startsWith("profile-photos/u-001/")),
        () -> assertTrue(first.value().endsWith(".png")),
        () -> assertNotEquals(first, second, "cada carga debe usar una key distinta"));
  }

  @Test
  @DisplayName("forUser() reemplaza caracteres no seguros del id")
  void shouldSanitizeUserId() {
    // Act
    final ProfilePhotoKey key =
        ProfilePhotoKey.forUser(new UserId("../a b"), ProfilePhotoFormat.JPEG);

    // Assert
    assertTrue(key.value().startsWith("profile-photos/___a_b/"), key.value());
  }

  @Test
  @DisplayName("el constructor rechaza keys fuera del prefijo de fotos de perfil")
  void shouldRejectKeyOutsidePrefix() {
    // Act & Assert
    assertThrows(
        InvalidProfilePhotoException.class, () -> new ProfilePhotoKey("otros/archivo.png"));
  }
}
