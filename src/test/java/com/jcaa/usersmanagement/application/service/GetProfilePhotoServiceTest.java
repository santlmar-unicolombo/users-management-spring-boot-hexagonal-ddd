package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.application.service.dto.query.GetProfilePhotoQuery;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoNotFoundException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para GetProfilePhotoService.
 *
 * <p>Cubre: imagen servida desde la caché, descarga desde el bucket y guardado en caché, usuario
 * sin foto, objeto inexistente en el bucket y usuario inexistente.
 */
@DisplayName("GetProfilePhotoService")
@ExtendWith(MockitoExtension.class)
class GetProfilePhotoServiceTest {

  private static final String ID = "u-001";
  private static final ProfilePhotoKey KEY = new ProfilePhotoKey("profile-photos/u-001/a.png");
  private static final ProfilePhotoModel PHOTO =
      ProfilePhotoModel.fromUpload(
          new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00});

  @Mock private GetUserByIdPort getUserByIdPort;
  @Mock private ProfilePhotoStoragePort profilePhotoStoragePort;
  @Mock private UserCachePort userCachePort;

  private GetProfilePhotoService service;
  private UserModel userWithPhoto;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new GetProfilePhotoService(
              getUserByIdPort,
              profilePhotoStoragePort,
              userCachePort,
              validatorFactory.getValidator());
    }
    userWithPhoto =
        new UserModel(
                new UserId(ID),
                new UserName("Ana Perez"),
                new UserEmail("ana@example.com"),
                UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuO"),
                UserRole.MEMBER,
                UserStatus.ACTIVE)
            .withProfilePhoto(KEY);
  }

  @Test
  @DisplayName("execute() sirve la imagen desde la caché sin ir al bucket")
  void shouldServePhotoFromCache() {
    // Arrange
    when(userCachePort.getUser(any())).thenReturn(Optional.of(userWithPhoto));
    when(userCachePort.getProfilePhoto(KEY)).thenReturn(Optional.of(PHOTO));

    // Act
    final ProfilePhotoModel result = service.execute(new GetProfilePhotoQuery(ID));

    // Assert
    assertSame(PHOTO, result);
    verifyNoInteractions(profilePhotoStoragePort, getUserByIdPort);
  }

  @Test
  @DisplayName("execute() descarga del bucket y guarda en caché cuando no está cacheada")
  void shouldDownloadFromBucketAndCache() {
    // Arrange
    when(userCachePort.getUser(any())).thenReturn(Optional.empty());
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(userWithPhoto));
    when(userCachePort.getProfilePhoto(KEY)).thenReturn(Optional.empty());
    when(profilePhotoStoragePort.download(KEY)).thenReturn(Optional.of(PHOTO));

    // Act
    final ProfilePhotoModel result = service.execute(new GetProfilePhotoQuery(ID));

    // Assert
    assertSame(PHOTO, result);
    verify(userCachePort).putUser(userWithPhoto);
    verify(userCachePort).putProfilePhoto(KEY, PHOTO);
  }

  @Test
  @DisplayName("execute() lanza ProfilePhotoNotFoundException cuando el usuario no tiene foto")
  void shouldThrowWhenUserHasNoPhoto() {
    // Arrange
    when(userCachePort.getUser(any()))
        .thenReturn(Optional.of(userWithPhoto.withoutProfilePhoto()));
    final GetProfilePhotoQuery query = new GetProfilePhotoQuery(ID);

    // Act & Assert
    assertThrows(ProfilePhotoNotFoundException.class, () -> service.execute(query));
    verifyNoInteractions(profilePhotoStoragePort);
  }

  @Test
  @DisplayName("execute() lanza ProfilePhotoNotFoundException si el objeto no está en el bucket")
  void shouldThrowWhenObjectIsMissingInBucket() {
    // Arrange
    when(userCachePort.getUser(any())).thenReturn(Optional.of(userWithPhoto));
    when(userCachePort.getProfilePhoto(KEY)).thenReturn(Optional.empty());
    when(profilePhotoStoragePort.download(KEY)).thenReturn(Optional.empty());
    final GetProfilePhotoQuery query = new GetProfilePhotoQuery(ID);

    // Act & Assert
    assertThrows(ProfilePhotoNotFoundException.class, () -> service.execute(query));
    verify(userCachePort, never()).putProfilePhoto(any(), any());
  }

  @Test
  @DisplayName("execute() lanza UserNotFoundException cuando el usuario no existe")
  void shouldThrowWhenUserDoesNotExist() {
    // Arrange
    when(userCachePort.getUser(any())).thenReturn(Optional.empty());
    when(getUserByIdPort.getById(any())).thenReturn(Optional.empty());
    final GetProfilePhotoQuery query = new GetProfilePhotoQuery(ID);

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> service.execute(query));
  }
}
