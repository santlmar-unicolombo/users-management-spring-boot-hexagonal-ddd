package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.UserModel;

/**
 * Persiste únicamente la referencia (key) a la foto de perfil del usuario. Si el modelo no tiene
 * foto, la referencia se elimina. Retorna el usuario tal como quedó almacenado.
 */
public interface UpdateUserProfilePhotoPort {
  UserModel updateProfilePhoto(UserModel user);
}
