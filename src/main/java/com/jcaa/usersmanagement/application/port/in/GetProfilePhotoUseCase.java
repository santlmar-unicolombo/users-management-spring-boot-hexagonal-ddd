package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.query.GetProfilePhotoQuery;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface GetProfilePhotoUseCase {
  ProfilePhotoModel execute(@NotNull @Valid GetProfilePhotoQuery query);
}
