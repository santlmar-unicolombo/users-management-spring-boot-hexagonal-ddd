package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper;

import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteProfilePhotoCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UploadProfilePhotoCommand;
import com.jcaa.usersmanagement.application.service.dto.query.GetProfilePhotoQuery;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.application.service.dto.query.SearchUsersQuery;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.CreateUserRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request.UpdateUserRestRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserSearchRestResponse;
import lombok.experimental.UtilityClass;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@UtilityClass
public class UserRestMapper {

  private static final String PROFILE_PHOTO_PATH = "/api/users/{id}/profile-photo";

  public CreateUserCommand toCreateCommand(final CreateUserRestRequest request) {
    return new CreateUserCommand(
        request.id(),
        request.name(),
        request.email(),
        request.password(),
        request.role());
  }

  public UpdateUserCommand toUpdateCommand(final String id, final UpdateUserRestRequest request) {
    return new UpdateUserCommand(
        id,
        request.name(),
        request.email(),
        request.password(),
        request.role(),
        request.status());
  }

  public GetUserByIdQuery toGetByIdQuery(final String id) {
    return new GetUserByIdQuery(id);
  }

  public DeleteUserCommand toDeleteCommand(final String id) {
    return new DeleteUserCommand(id);
  }

  public UserRestResponse toResponse(final UserModel user) {
    return new UserRestResponse(
        user.getId().value(),
        user.getName().value(),
        user.getEmail().value(),
        user.getRole().name(),
        user.getStatus().name(),
        toProfilePhotoUrl(user.getId().value(), user.hasProfilePhoto()));
  }

  public List<UserRestResponse> toResponseList(final List<UserModel> users) {
    return users.stream().map(UserRestMapper::toResponse).toList();
  }

  public UploadProfilePhotoCommand toUploadProfilePhotoCommand(
      final String id, final byte[] content) {
    return new UploadProfilePhotoCommand(id, content);
  }

  public GetProfilePhotoQuery toGetProfilePhotoQuery(final String id) {
    return new GetProfilePhotoQuery(id);
  }

  public DeleteProfilePhotoCommand toDeleteProfilePhotoCommand(final String id) {
    return new DeleteProfilePhotoCommand(id);
  }

  public SearchUsersQuery toSearchQuery(
      final String text, final String role, final String status, final int size) {
    return new SearchUsersQuery(text, role, status, size);
  }

  public List<UserSearchRestResponse> toSearchResponseList(
      final List<UserSearchResultModel> results) {
    return results.stream().map(UserRestMapper::toSearchResponse).toList();
  }

  private UserSearchRestResponse toSearchResponse(final UserSearchResultModel result) {
    return new UserSearchRestResponse(
        result.id(),
        result.name(),
        result.email(),
        result.role().name(),
        result.status().name(),
        toProfilePhotoUrl(result.id(), result.hasProfilePhoto()),
        result.score());
  }

  /** Ruta de la API que entrega la foto, o {@code null} si el usuario no tiene foto. */
  private String toProfilePhotoUrl(final String userId, final boolean hasProfilePhoto) {
    if (!hasProfilePhoto) {
      return null; // NOSONAR: en el JSON se representa como "sin foto"
    }
    return UriComponentsBuilder.fromPath(PROFILE_PHOTO_PATH)
        .buildAndExpand(userId)
        .encode()
        .toUriString();
  }
}

