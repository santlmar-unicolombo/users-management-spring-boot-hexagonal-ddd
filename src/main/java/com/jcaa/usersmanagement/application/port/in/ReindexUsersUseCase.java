package com.jcaa.usersmanagement.application.port.in;

public interface ReindexUsersUseCase {
  /** Reconstruye el índice de búsqueda desde la base de datos. Retorna cuántos usuarios indexó. */
  int execute();
}
