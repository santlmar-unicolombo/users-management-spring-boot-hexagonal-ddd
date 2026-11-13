package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.application.port.out.UserSearchIndexPort;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Mantiene las copias derivadas del usuario (caché en Valkey e índice en OpenSearch) alineadas
 * con la base de datos después de cada escritura.
 *
 * <p>La base de datos es la fuente de verdad: la entrada de caché se invalida (no se actualiza)
 * para que la siguiente lectura la reconstruya, y el documento del índice se reemplaza.
 */
@Service
@RequiredArgsConstructor
public class UserReadModelSyncService {

  private final UserCachePort userCachePort;
  private final UserSearchIndexPort userSearchIndexPort;

  public void onUserChanged(final UserModel user) {
    userCachePort.evictUser(user.getId());
    userSearchIndexPort.index(user);
  }

  public void onUserDeleted(final UserId userId) {
    userCachePort.evictUser(userId);
    userSearchIndexPort.remove(userId);
  }
}
