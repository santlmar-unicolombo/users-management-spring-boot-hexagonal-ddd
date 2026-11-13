package com.jcaa.usersmanagement.infrastructure.adapter.search;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UserSearchDocumentMapper {

  public UserSearchDocument fromModelToDocument(final UserModel user) {
    return new UserSearchDocument(
        user.getId().value(),
        user.getName().value(),
        user.getEmail().value(),
        user.getRole().name(),
        user.getStatus().name(),
        user.hasProfilePhoto());
  }

  public UserSearchResultModel fromDocumentToResult(
      final UserSearchDocument document, final double score) {
    return new UserSearchResultModel(
        document.id(),
        document.name(),
        document.email(),
        UserRole.fromString(document.role()),
        UserStatus.fromString(document.status()),
        document.hasProfilePhoto(),
        score);
  }
}
