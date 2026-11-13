package com.jcaa.usersmanagement.domain.model;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class UserModel {

  UserId id;
  UserName name;
  UserEmail email;
  UserPassword password;
  UserRole role;
  UserStatus status;

  /** Key del objeto en el bucket. Es opcional: un usuario puede no tener foto de perfil. */
  ProfilePhotoKey profilePhotoKey;

  /** Constructor para usuarios sin foto de perfil. */
  public UserModel(
      final UserId id,
      final UserName name,
      final UserEmail email,
      final UserPassword password,
      final UserRole role,
      final UserStatus status) {
    this(id, name, email, password, role, status, null);
  }

  public static UserModel create(
      final UserId id,
      final UserName name,
      final UserEmail email,
      final UserPassword password,
      final UserRole role) {
    return new UserModel(id, name, email, password, role, UserStatus.PENDING);
  }

  public Optional<ProfilePhotoKey> getProfilePhotoKey() {
    return Optional.ofNullable(profilePhotoKey);
  }

  public boolean hasProfilePhoto() {
    return Objects.nonNull(profilePhotoKey);
  }

  public UserModel activate() {
    return new UserModel(id, name, email, password, role, UserStatus.ACTIVE, profilePhotoKey);
  }

  public UserModel deactivate() {
    return new UserModel(id, name, email, password, role, UserStatus.INACTIVE, profilePhotoKey);
  }

  public UserModel withProfilePhoto(final ProfilePhotoKey newProfilePhotoKey) {
    Objects.requireNonNull(newProfilePhotoKey, "ProfilePhotoKey cannot be null");
    return new UserModel(id, name, email, password, role, status, newProfilePhotoKey);
  }

  public UserModel withoutProfilePhoto() {
    return new UserModel(id, name, email, password, role, status, null);
  }
}
