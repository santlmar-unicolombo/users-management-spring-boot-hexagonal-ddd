package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.application.port.out.UpdateUserProfilePhotoPort;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteProfilePhotoCommand;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("DeleteProfilePhotoService")
@ExtendWith(MockitoExtension.class)
class DeleteProfilePhotoServiceTest {

  private static final String ID = "u-001";
  private static final ProfilePhotoKey KEY = new ProfilePhotoKey("profile-photos/u-001/a.png");

  @Mock private GetUserByIdPort getUserByIdPort;
  @Mock private UpdateUserProfilePhotoPort updateUserProfilePhotoPort;
  @Mock private ProfilePhotoStoragePort profilePhotoStoragePort;
  @Mock private UserReadModelSyncService userReadModelSyncService;

  private DeleteProfilePhotoService service;
  private UserModel user;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service =
          new DeleteProfilePhotoService(
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
  @DisplayName("execute() quita la referencia, borra el objeto del bucket y sincroniza")
  void shouldRemoveReferenceDeleteObjectAndSync() {
    // Arrange
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(user.withProfilePhoto(KEY)));
    when(updateUserProfilePhotoPort.updateProfilePhoto(any())).thenReturn(user);

    // Act
    service.execute(new DeleteProfilePhotoCommand(ID));

    // Assert
    final ArgumentCaptor<UserModel> saved = ArgumentCaptor.forClass(UserModel.class);
    verify(updateUserProfilePhotoPort).updateProfilePhoto(saved.capture());
    assertFalse(saved.getValue().hasProfilePhoto(), "debe guardarse sin foto");
    verify(profilePhotoStoragePort).delete(KEY);
    verify(userReadModelSyncService).onUserChanged(user);
  }

  @Test
  @DisplayName("execute() no hace nada cuando el usuario no tiene foto (idempotente)")
  void shouldDoNothingWhenUserHasNoPhoto() {
    // Arrange
    when(getUserByIdPort.getById(any())).thenReturn(Optional.of(user));

    // Act
    service.execute(new DeleteProfilePhotoCommand(ID));

    // Assert
    verifyNoInteractions(
        updateUserProfilePhotoPort, profilePhotoStoragePort, userReadModelSyncService);
  }

  @Test
  @DisplayName("execute() lanza UserNotFoundException cuando el usuario no existe")
  void shouldThrowWhenUserDoesNotExist() {
    // Arrange
    when(getUserByIdPort.getById(any())).thenReturn(Optional.empty());
    final DeleteProfilePhotoCommand command = new DeleteProfilePhotoCommand(ID);

    // Act & Assert
    assertThrows(UserNotFoundException.class, () -> service.execute(command));
  }
}
