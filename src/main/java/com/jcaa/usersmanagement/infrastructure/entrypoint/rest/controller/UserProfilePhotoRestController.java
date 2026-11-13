package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.application.port.in.DeleteProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.in.GetProfilePhotoUseCase;
import com.jcaa.usersmanagement.application.port.in.UploadProfilePhotoUseCase;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.mapper.UserRestMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/{id}/profile-photo")
@RequiredArgsConstructor
public class UserProfilePhotoRestController implements UserProfilePhotoRestControllerDocs {

  private static final String FILE_PART = "file";

  private final UploadProfilePhotoUseCase uploadProfilePhotoUseCase;
  private final GetProfilePhotoUseCase getProfilePhotoUseCase;
  private final DeleteProfilePhotoUseCase deleteProfilePhotoUseCase;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UserRestResponse upload(
      @PathVariable final String id, @RequestPart(FILE_PART) final MultipartFile file)
      throws IOException {
    final UserModel user =
        uploadProfilePhotoUseCase.execute(
            UserRestMapper.toUploadProfilePhotoCommand(id, file.getBytes()));
    return UserRestMapper.toResponse(user);
  }

  @GetMapping
  public ResponseEntity<byte[]> get(@PathVariable final String id) {
    final ProfilePhotoModel photo =
        getProfilePhotoUseCase.execute(UserRestMapper.toGetProfilePhotoQuery(id));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(photo.getFormat().contentType()))
        .contentLength(photo.getSizeInBytes())
        .body(photo.getContent());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable final String id) {
    deleteProfilePhotoUseCase.execute(UserRestMapper.toDeleteProfilePhotoCommand(id));
  }
}
