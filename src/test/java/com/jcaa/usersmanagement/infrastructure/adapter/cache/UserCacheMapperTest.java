package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserCacheMapper")
class UserCacheMapperTest {

  private static UserModel buildUser() {
    return new UserModel(
        new UserId("u-001"),
        new UserName("Ana Perez"),
        new UserEmail("ana@example.com"),
        UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuO"),
        UserRole.REVIEWER,
        UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("ida y vuelta conserva todos los campos, incluida la foto de perfil")
  void shouldRoundTripUserWithPhoto() {
    // Arrange
    final UserModel original =
        buildUser().withProfilePhoto(new ProfilePhotoKey("profile-photos/u-001/a.png"));

    // Act
    final UserModel restored =
        UserCacheMapper.fromEntryToModel(UserCacheMapper.fromModelToEntry(original));

    // Assert
    assertEquals(original, restored);
  }

  @Test
  @DisplayName("ida y vuelta de un usuario sin foto no inventa una foto")
  void shouldRoundTripUserWithoutPhoto() {
    // Arrange
    final UserModel original = buildUser();

    // Act
    final CachedUserEntry entry = UserCacheMapper.fromModelToEntry(original);
    final UserModel restored = UserCacheMapper.fromEntryToModel(entry);

    // Assert
    assertAll(
        "sin foto",
        () -> assertNull(entry.profilePhotoKey()),
        () -> assertFalse(restored.hasProfilePhoto()),
        () -> assertEquals(original, restored));
  }
}
