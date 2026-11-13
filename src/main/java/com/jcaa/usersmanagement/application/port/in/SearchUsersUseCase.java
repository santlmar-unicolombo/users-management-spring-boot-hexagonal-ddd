package com.jcaa.usersmanagement.application.port.in;

import com.jcaa.usersmanagement.application.service.dto.query.SearchUsersQuery;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public interface SearchUsersUseCase {
  List<UserSearchResultModel> execute(@NotNull @Valid SearchUsersQuery query);
}
