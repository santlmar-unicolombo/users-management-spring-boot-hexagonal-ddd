package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import java.util.Optional;

/**
 * Almacenamiento de objetos (bucket privado) para las fotos de perfil.
 *
 * <p>Las implementaciones lanzan {@code ProfilePhotoStorageException} cuando el servicio de
 * almacenamiento falla o no está configurado.
 */
public interface ProfilePhotoStoragePort {

  void upload(ProfilePhotoKey key, ProfilePhotoModel photo);

  /** Retorna vacío si el objeto no existe en el bucket. */
  Optional<ProfilePhotoModel> download(ProfilePhotoKey key);

  void delete(ProfilePhotoKey key);
}
