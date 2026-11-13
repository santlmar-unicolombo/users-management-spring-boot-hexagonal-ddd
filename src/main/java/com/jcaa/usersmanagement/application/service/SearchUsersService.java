package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.SearchUsersUseCase;
import com.jcaa.usersmanagement.application.port.out.SearchUsersPort;
import com.jcaa.usersmanagement.application.service.dto.query.SearchUsersQuery;
import com.jcaa.usersmanagement.application.service.mapper.UserApplicationMapper;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Búsqueda de usuarios sobre el índice de OpenSearch (no consulta la base de datos). */
@Service
@RequiredArgsConstructor
public class SearchUsersService implements SearchUsersUseCase {

  private final SearchUsersPort searchUsersPort;
  private final Validator validator;

  @Override
  public List<UserSearchResultModel> execute(final SearchUsersQuery query) {
    validateQuery(query);
    return searchUsersPort.search(UserApplicationMapper.fromSearchQueryToCriteria(query));
  }

  private void validateQuery(final SearchUsersQuery query) {
    final Set<ConstraintViolation<SearchUsersQuery>> violations = validator.validate(query);
    if (!violations.isEmpty()) {
      throw new ConstraintViolationException(violations);
    }
  }
}
