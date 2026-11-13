package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Se activa cuando {@code VALKEY_ENABLED} no es {@code true}: todas las lecturas van a la fuente. */
@Component
@ConditionalOnProperty(
    prefix = "cache.valkey",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoOpUserCacheAdapter implements UserCachePort {

  @Override
  public Optional<UserModel> getUser(final UserId userId) {
    return Optional.empty();
  }

  @Override
  public void putUser(final UserModel user) {
    // Sin caché configurada: no hay nada que guardar.
  }

  @Override
  public void evictUser(final UserId userId) {
    // Sin caché configurada: no hay nada que invalidar.
  }

  @Override
  public Optional<ProfilePhotoModel> getProfilePhoto(final ProfilePhotoKey key) {
    return Optional.empty();
  }

  @Override
  public void putProfilePhoto(final ProfilePhotoKey key, final ProfilePhotoModel photo) {
    // Sin caché configurada: no hay nada que guardar.
  }
}
