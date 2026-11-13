package com.jcaa.usersmanagement.infrastructure.adapter.storage;

import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoStorageException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Se activa cuando {@code B2_ENABLED} no es {@code true}. Permite que el servicio arranque sin
 * bucket configurado; las operaciones de foto responden 503 con un mensaje claro.
 */
@Component
@ConditionalOnProperty(
    prefix = "storage.b2",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class DisabledProfilePhotoStorageAdapter implements ProfilePhotoStoragePort {

  @Override
  public void upload(final ProfilePhotoKey key, final ProfilePhotoModel photo) {
    throw ProfilePhotoStorageException.becauseStorageIsNotConfigured();
  }

  @Override
  public Optional<ProfilePhotoModel> download(final ProfilePhotoKey key) {
    throw ProfilePhotoStorageException.becauseStorageIsNotConfigured();
  }

  @Override
  public void delete(final ProfilePhotoKey key) {
    throw ProfilePhotoStorageException.becauseStorageIsNotConfigured();
  }
}
