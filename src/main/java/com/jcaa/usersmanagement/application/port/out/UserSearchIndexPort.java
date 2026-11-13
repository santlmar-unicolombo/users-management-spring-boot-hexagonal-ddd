package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.List;

/**
 * Mantiene el índice de búsqueda (OpenSearch) sincronizado con la base de datos.
 *
 * <p>{@link #index} y {@link #remove} son de mejor esfuerzo: si el motor no responde se registra
 * el fallo y el índice puede reconstruirse luego con {@link #reindexAll}.
 */
public interface UserSearchIndexPort {

  void index(UserModel user);

  void remove(UserId userId);

  /** Reconstruye el índice completo a partir de los usuarios dados. Retorna cuántos indexó. */
  int reindexAll(List<UserModel> users);
}
