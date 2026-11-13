package com.jcaa.usersmanagement.infrastructure.adapter.search;

import com.jcaa.usersmanagement.application.port.out.SearchUsersPort;
import com.jcaa.usersmanagement.application.port.out.UserSearchIndexPort;
import com.jcaa.usersmanagement.domain.exception.UserSearchUnavailableException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.model.UserSearchCriteria;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Se activa cuando {@code OPENSEARCH_ENABLED} no es {@code true}. Las escrituras de usuarios
 * siguen funcionando (no hay índice que actualizar) y la búsqueda responde 503.
 */
@Component
@ConditionalOnProperty(
    prefix = "search.opensearch",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class DisabledUserSearchAdapter implements UserSearchIndexPort, SearchUsersPort {

  @Override
  public void index(final UserModel user) {
    // Sin motor de búsqueda configurado: no hay índice que actualizar.
  }

  @Override
  public void remove(final UserId userId) {
    // Sin motor de búsqueda configurado: no hay índice que actualizar.
  }

  @Override
  public int reindexAll(final List<UserModel> users) {
    throw UserSearchUnavailableException.becauseSearchIsNotConfigured();
  }

  @Override
  public List<UserSearchResultModel> search(final UserSearchCriteria criteria) {
    throw UserSearchUnavailableException.becauseSearchIsNotConfigured();
  }
}
