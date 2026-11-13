package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.in.ReindexUsersUseCase;
import com.jcaa.usersmanagement.application.port.out.GetAllUsersPort;
import com.jcaa.usersmanagement.application.port.out.UserSearchIndexPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Reconstruye el índice de búsqueda desde la base de datos, que es la fuente de verdad. Útil para
 * cargar los usuarios existentes la primera vez o tras perder el índice (el plan gratuito de
 * OpenSearch no garantiza restauraciones).
 */
@Service
@RequiredArgsConstructor
public class ReindexUsersService implements ReindexUsersUseCase {

  private final GetAllUsersPort getAllUsersPort;
  private final UserSearchIndexPort userSearchIndexPort;

  @Override
  public int execute() {
    return userSearchIndexPort.reindexAll(getAllUsersPort.getAll());
  }
}
