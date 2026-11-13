package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.util.Objects;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserCacheMapper {

  public CachedUserEntry fromModelToEntry(final UserModel user) {
    return new CachedUserEntry(
        user.getId().value(),
        user.getName().value(),
        user.getEmail().value(),
        user.getPassword().value(),
        user.getRole().name(),
        user.getStatus().name(),
        user.getProfilePhotoKey().map(ProfilePhotoKey::value).orElse(null));
  }

  public UserModel fromEntryToModel(final CachedUserEntry entry) {
    final UserModel user =
        new UserModel(
            new UserId(entry.id()),
            new UserName(entry.name()),
            new UserEmail(entry.email()),
            UserPassword.fromHash(entry.passwordHash()),
            UserRole.fromString(entry.role()),
            UserStatus.fromString(entry.status()));
    return Objects.isNull(entry.profilePhotoKey())
        ? user
        : user.withProfilePhoto(new ProfilePhotoKey(entry.profilePhotoKey()));
  }
}
