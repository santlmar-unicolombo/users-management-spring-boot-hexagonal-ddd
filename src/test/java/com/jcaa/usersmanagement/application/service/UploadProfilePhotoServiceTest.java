package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.port.out.UpdateUserProfilePhotoPort;
import com.jcaa.usersmanagement.application.service.dto.command.UploadProfilePhotoCommand;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.InvalidProfilePhotoException;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoStorageException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests para UploadProfilePhotoService.
 *
 * <p>Cubre: flujo feliz (sube, guarda la key, borra la anterior y sincroniza), usuario inexistente,
 * imagen inválida, rollback del objeto subido si falla la base de datos, tolerancia a fallos al
 * borrar la foto anterior y validación del command.
 */
@DisplayName("UploadProfilePhotoService")
@ExtendWith(MockitoExtension.class)
class UploadProfilePhotoServiceTest {

  private static final String ID = "u-001";
  private static final byte[] PNG_BYTES = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
  };
  private static final ProfilePhotoKey OLD_KEY =
      new ProfilePhotoKey("profile-photos/u-001/old.png");

  @Mock private GetUserByIdPort getUserByIdPort;
  @Mock private UpdateUserProfilePhotoPort updateUserProfilePhotoPort;
  @Mock private ProfilePhotoStoragePort profilePhotoStoragePort;
  @Mock private UserReadModelSyncService userReadModelSyncService;

  private UploadProfilePhotoService service;
  private UserModel user;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new UploadProfilePhotoService(
              getUserByIdPort,
              updateUserProfilePhotoPort,
              profilePhotoStoragePort,
              userReadModelSyncService,
              validatorFactory.getValidator());
    }
    user =
        new UserModel(
            new UserId(ID),
            new UserName("Ana Perez"),
            new UserEmail("ana@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuO"),
            UserRole.MEMBER,
            UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("execute() sube la imagen, guarda su key, borra la anterior y sincroniza")
  void shouldUploadStoreKeyDeleteOldPhotoAndSync() {
    // Arrange
    final UserModel current = user.withProfilePhoto(OLD_KEY);
    when(getUserByIdPort.getById(new UserId(ID))).thenReturn(Optional.of(current));
    when(updateUserProfilePhotoPort.updateProfilePhoto(any()))
        .thenAnswer(call -> call.getArgument(0));

    // Act
    final UserModel result = service.execute(new UploadProfilePhotoCommand(ID, PNG_BYTES));

    // Assert
    final ArgumentCaptor<ProfilePhotoKey> uploadedKey =
        ArgumentCaptor.forClass(ProfilePhotoKey.class);
    verify(profilePhotoStoragePort).upload(uploadedKey.capture(), any(ProfilePhotoModel.class));
    assertAll(
        "resultado",
        () -> assertTrue(uploadedKey.getValue().value().endsWith(".png")),
        () -> assertEquals(Optional.of(uploadedKey.getValue()), result.getProfilePhotoKey()),
        () -> assertNotEquals(OLD_KEY, uploadedKey.getValue()));
    verify(profilePhotoStoragePort).delete(OLD_KEY);
    verify(userReadModelSyncService).onUserChanged(result);
  }

  @Test
  @DisplayName("execute() lanza UserNotFoundException y no sube nada si el usuario no existe")
  void shouldThrowWhenUserDoesNotExist() {
    // Arrange
    when(getUserByIdPort.getById(any())).thenReturn(Optional.empty());
    final UploadProfilePhotoCommand command = new UploadProfilePhotoCommand(ID, PNG_BYTES);

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> service.execute(command));
    verifyNoInteractions(profilePhotoStoragePort, updateUserProfilePhotoPort);
  }

  @Test
  @DisplayName("execute() rechaza archivos que no son imágenes admitidas sin tocar el bucket")
  void shouldRejectInvalidImage() {
    // Arrange
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(user));
    final UploadProfilePhotoCommand command =
        new UploadProfilePhotoCommand(ID, "no soy una imagen".getBytes());

    // Act & Assert
    assertThrows(InvalidProfilePhotoException.class, () -> service.execute(command));
    verifyNoInteractions(profilePhotoStoragePort, updateUserProfilePhotoPort);
  }

  @Test
  @DisplayName("execute() borra el objeto recién subido si falla la base de datos")
  void shouldRollbackUploadWhenDatabaseFails() {
    // Arrange
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(user));
    when(updateUserProfilePhotoPort.updateProfilePhoto(any()))
        .thenThrow(new IllegalStateException("db down"));
    final UploadProfilePhotoCommand command = new UploadProfilePhotoCommand(ID, PNG_BYTES);

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> service.execute(command));
    final ArgumentCaptor<ProfilePhotoKey> uploadedKey =
        ArgumentCaptor.forClass(ProfilePhotoKey.class);
    verify(profilePhotoStoragePort).upload(uploadedKey.capture(), any());
    verify(profilePhotoStoragePort).delete(uploadedKey.getValue());
    verifyNoInteractions(userReadModelSyncService);
  }

  @Test
  @DisplayName("execute() termina bien aunque no se pueda borrar la foto anterior")
  void shouldSucceedWhenOldPhotoCannotBeDeleted() {
    // Arrange
    when(getUserByIdPort.getById(any()))
        .thenReturn(Optional.of(user.withProfilePhoto(OLD_KEY)));
    when(updateUserProfilePhotoPort.updateProfilePhoto(any()))
        .thenAnswer(call -> call.getArgument(0));
    doThrow(ProfilePhotoStorageException.becauseDeleteFailed(new RuntimeException("down")))
        .when(profilePhotoStoragePort)
        .delete(OLD_KEY);

    // Act & Assert
    assertDoesNotThrow(() -> service.execute(new UploadProfilePhotoCommand(ID, PNG_BYTES)));
    verify(userReadModelSyncService).onUserChanged(any());
  }

  @Test
  @DisplayName("execute() lanza ConstraintViolationException cuando el command es inválido")
  void shouldThrowWhenCommandIsInvalid() {
    // Arrange
    final UploadProfilePhotoCommand command = new UploadProfilePhotoCommand(" ", null);

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.execute(command));
    verifyNoInteractions(getUserByIdPort, profilePhotoStoragePort);
  }
}
