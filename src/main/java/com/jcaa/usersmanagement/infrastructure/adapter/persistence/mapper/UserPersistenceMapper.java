package com.jcaa.usersmanagement.infrastructure.adapter.persistence.mapper;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.dto.UserPersistenceDto;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.entity.UserEntity;
import lombok.experimental.UtilityClass;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UtilityClass
public class UserPersistenceMapper {

  public UserPersistenceDto fromModelToDto(final UserModel user) {
    return new UserPersistenceDto(
        user.getId().value(),
        user.getName().value(),
        user.getEmail().value(),
        user.getPassword().value(),
        user.getRole().name(),
        user.getStatus().name(),
        null,
        null);
  }

  public UserEntity fromResultSetToEntity(final ResultSet resultSet) throws SQLException {
    return new UserEntity(
        resultSet.getString("id"),
        resultSet.getString("name"),
        resultSet.getString("email"),
        resultSet.getString("password"),
        resultSet.getString("role"),
        resultSet.getString("status"),
        resultSet.getString("profile_photo_key"),
        resultSet.getString("created_at"),
        resultSet.getString("updated_at"));
  }

  public UserModel fromEntityToModel(final UserEntity entity) {
    final UserModel user =
        new UserModel(
            new UserId(entity.id()),
            new UserName(entity.name()),
            new UserEmail(entity.email()),
            UserPassword.fromHash(entity.password()),
            UserRole.fromString(entity.role()),
            UserStatus.fromString(entity.status()));
    return hasProfilePhoto(entity)
        ? user.withProfilePhoto(new ProfilePhotoKey(entity.profilePhotoKey()))
        : user;
  }

  private boolean hasProfilePhoto(final UserEntity entity) {
    return Objects.nonNull(entity.profilePhotoKey()) && !entity.profilePhotoKey().isBlank();
  }

  public UserModel fromResultSetToModel(final ResultSet resultSet) throws SQLException {
    return fromEntityToModel(fromResultSetToEntity(resultSet));
  }

  public List<UserModel> fromResultSetToModelList(final ResultSet resultSet) throws SQLException {
    final List<UserModel> users = new ArrayList<>();
    while (resultSet.next()) {
      users.add(fromResultSetToModel(resultSet));
    }
    return users;
  }
}