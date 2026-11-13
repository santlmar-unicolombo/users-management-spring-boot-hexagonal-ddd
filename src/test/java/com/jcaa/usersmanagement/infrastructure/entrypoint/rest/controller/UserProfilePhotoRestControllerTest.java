package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.DeleteProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.in.GetProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.in.UploadProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.service.dto.query.GetProfilePhotoQuery;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoStorageException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de la API de foto de perfil a través de Spring MVC y Spring Security: reglas de acceso
 * (dueño o ADMIN), respuesta binaria con su Content-Type y traducción de errores a HTTP.
 */
@WebMvcTest(
    controllers = UserProfilePhotoRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  RestAccessDeniedHandler.class
})
@DisplayName("UserProfilePhotoRestController")
class UserProfilePhotoRestControllerTest {

  private static final String OWNER_ID = "u-001";
  private static final String PATH = "/api/users/{id}/profile-photo";
  private static final byte[] PNG_BYTES = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
  };

  @Autowired private MockMvc mockMvc;

  @MockBean private UploadProfilePhotoUseCase uploadProfilePhotoUseCase;
  @MockBean private GetProfilePhotoUseCase getProfilePhotoUseCase;
  @MockBean private DeleteProfilePhotoUseCase deleteProfilePhotoUseCase;
  @MockBean private JwtTokenService jwtTokenService;

  private static MockMultipartFile pngFile() {
    return new MockMultipartFile("file", "avatar.png", "image/png", PNG_BYTES);
  }

  private static UserModel ownerWithPhoto() {
    return new UserModel(
            new UserId(OWNER_ID),
            new UserName("Ana Perez"),
            new UserEmail("ana@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.MEMBER,
            UserStatus.ACTIVE)
        .withProfilePhoto(new ProfilePhotoKey("profile-photos/u-001/a.png"));
  }

  @Test
  @WithMockUser(username = OWNER_ID, roles = "MEMBER")
  @DisplayName("POST permite al propio usuario subir su foto y retorna la URL de la foto")
  void shouldAllowOwnerToUpload() throws Exception {
    // Arrange
    when(uploadProfilePhotoUseCase.execute(any())).thenReturn(ownerWithPhoto());

    // Act & Assert
    mockMvc
        .perform(multipart(PATH, OWNER_ID).file(pngFile()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(OWNER_ID))
        .andExpect(jsonPath("$.profilePhotoUrl").value("/api/users/u-001/profile-photo"));
  }

  @Test
  @WithMockUser(username = "otro-usuario", roles = "MEMBER")
  @DisplayName("POST rechaza que un MEMBER suba la foto de otro usuario")
  void shouldRejectUploadForAnotherUser() throws Exception {
    // Act & Assert
    mockMvc
        .perform(multipart(PATH, OWNER_ID).file(pngFile()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(uploadProfilePhotoUseCase);
  }

  @Test
  @WithMockUser(username = "admin-1", roles = "ADMIN")
  @DisplayName("POST permite a un ADMIN subir la foto de cualquier usuario")
  void shouldAllowAdminToUploadForAnyUser() throws Exception {
    // Arrange
    when(uploadProfilePhotoUseCase.execute(any())).thenReturn(ownerWithPhoto());

    // Act & Assert
    mockMvc.perform(multipart(PATH, OWNER_ID).file(pngFile())).andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST requiere autenticación")
  void shouldRejectAnonymousUpload() throws Exception {
    // Act & Assert
    mockMvc
        .perform(multipart(PATH, OWNER_ID).file(pngFile()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = OWNER_ID, roles = "MEMBER")
  @DisplayName("POST responde 503 cuando el bucket no está disponible")
  void shouldReturnServiceUnavailableWhenBucketFails() throws Exception {
    // Arrange
    when(uploadProfilePhotoUseCase.execute(any()))
        .thenThrow(ProfilePhotoStorageException.becauseStorageIsNotConfigured());

    // Act & Assert
    mockMvc
        .perform(multipart(PATH, OWNER_ID).file(pngFile()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  @WithMockUser(username = "revisor-1", roles = "REVIEWER")
  @DisplayName("GET entrega los bytes de la imagen con su Content-Type")
  void shouldReturnImageBytes() throws Exception {
    // Arrange
    when(getProfilePhotoUseCase.execute(new GetProfilePhotoQuery(OWNER_ID)))
        .thenReturn(ProfilePhotoModel.fromUpload(PNG_BYTES));

    // Act & Assert
    mockMvc
        .perform(get(PATH, OWNER_ID))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(PNG_BYTES));
  }

  @Test
  @WithMockUser(username = "otro-usuario", roles = "MEMBER")
  @DisplayName("GET rechaza que un MEMBER vea la foto de otro usuario")
  void shouldRejectReadForAnotherMember() throws Exception {
    // Act & Assert
    mockMvc.perform(get(PATH, OWNER_ID)).andExpect(status().isForbidden());
    verifyNoInteractions(getProfilePhotoUseCase);
  }

  @Test
  @WithMockUser(username = OWNER_ID, roles = "MEMBER")
  @DisplayName("DELETE permite al propio usuario borrar su foto")
  void shouldAllowOwnerToDelete() throws Exception {
    // Act & Assert
    mockMvc.perform(delete(PATH, OWNER_ID)).andExpect(status().isNoContent());
  }
}
