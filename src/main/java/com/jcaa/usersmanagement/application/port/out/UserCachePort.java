package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.Optional;

/**
 * Caché distribuida (Valkey/Redis) para lecturas frecuentes.
 *
 * <p>La caché solo guarda información que puede reconstruirse desde la base de datos o el bucket.
 * Por eso las implementaciones son tolerantes a fallos: si la caché no responde, las lecturas
 * retornan vacío y las escrituras se ignoran, sin interrumpir el caso de uso.
 */
public interface UserCachePort {

  Optional<UserModel> getUser(UserId userId);

  void putUser(UserModel user);

  void evictUser(UserId userId);

  Optional<ProfilePhotoModel> getProfilePhoto(ProfilePhotoKey key);

  void putProfilePhoto(ProfilePhotoKey key, ProfilePhotoModel photo);
}
