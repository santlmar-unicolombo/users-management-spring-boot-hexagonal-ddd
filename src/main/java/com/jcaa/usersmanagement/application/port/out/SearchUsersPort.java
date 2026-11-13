package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserSearchCriteria;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import java.util.List;

public interface SearchUsersPort {
  List<UserSearchResultModel> search(UserSearchCriteria criteria);
}
