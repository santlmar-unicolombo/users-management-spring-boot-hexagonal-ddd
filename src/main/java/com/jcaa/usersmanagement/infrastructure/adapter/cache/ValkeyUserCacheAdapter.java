package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.UserCachePort;
import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Caché distribuida en Valkey (compatible con Redis).
 *
 * <ul>
 *   <li>{@code <prefijo>:user:<id>} → JSON del usuario, TTL corto (se invalida en cada escritura).
 *   <li>{@code <prefijo>:photo:<key del bucket>} → bytes de la imagen, TTL más largo. La key del
 *       bucket cambia con cada carga, así que esta entrada nunca queda desactualizada.
 * </ul>
 *
 * <p>Si Valkey falla, el adaptador registra una advertencia y el caso de uso continúa contra la
 * base de datos o el bucket (fail-open): la caché acelera, pero no es indispensable.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "cache.valkey", name = "enabled", havingValue = "true")
public class ValkeyUserCacheAdapter implements UserCachePort {

  private static final String USER_KEY_TEMPLATE = "%s:user:%s";
  private static final String PHOTO_KEY_TEMPLATE = "%s:photo:%s";
  private static final String LOG_CACHE_FAILURE = "[Valkey] operacion {} omitida. Causa: {}";
  private static final String OPERATION_GET_USER = "getUser";
  private static final String OPERATION_PUT_USER = "putUser";
  private static final String OPERATION_EVICT_USER = "evictUser";
  private static final String OPERATION_GET_PHOTO = "getProfilePhoto";
  private static final String OPERATION_PUT_PHOTO = "putProfilePhoto";

  private final JedisPooled valkeyClient;
  private final ObjectMapper objectMapper;
  private final String keyPrefix;
  private final long userTtlSeconds;
  private final long photoTtlSeconds;

  public ValkeyUserCacheAdapter(
      final JedisPooled valkeyClient,
      final ObjectMapper objectMapper,
      final ValkeyCacheProperties properties) {
    this.valkeyClient = valkeyClient;
    this.objectMapper = objectMapper;
    this.keyPrefix = properties.keyPrefix();
    this.userTtlSeconds = properties.userTtlSeconds();
    this.photoTtlSeconds = properties.photoTtlSeconds();
  }

  @Override
  public Optional<UserModel> getUser(final UserId userId) {
    try {
      final String json = valkeyClient.get(userKey(userId));
      if (Objects.isNull(json)) {
        return Optional.empty();
      }
      final CachedUserEntry entry = objectMapper.readValue(json, CachedUserEntry.class);
      return Optional.of(UserCacheMapper.fromEntryToModel(entry));
    } catch (final JedisException | JsonProcessingException | DomainException exception) {
      logFailure(OPERATION_GET_USER, exception);
      return Optional.empty();
    }
  }

  @Override
  public void putUser(final UserModel user) {
    try {
      final String json = objectMapper.writeValueAsString(UserCacheMapper.fromModelToEntry(user));
      valkeyClient.setex(userKey(user.getId()), userTtlSeconds, json);
    } catch (final JedisException | JsonProcessingException exception) {
      logFailure(OPERATION_PUT_USER, exception);
    }
  }

  @Override
  public void evictUser(final UserId userId) {
    try {
      valkeyClient.del(userKey(userId));
    } catch (final JedisException exception) {
      logFailure(OPERATION_EVICT_USER, exception);
    }
  }

  @Override
  public Optional<ProfilePhotoModel> getProfilePhoto(final ProfilePhotoKey key) {
    try {
      final byte[] content = valkeyClient.get(photoKey(key));
      if (Objects.isNull(content)) {
        return Optional.empty();
      }
      return Optional.of(ProfilePhotoModel.fromStorage(content));
    } catch (final JedisException | DomainException exception) {
      logFailure(OPERATION_GET_PHOTO, exception);
      return Optional.empty();
    }
  }

  @Override
  public void putProfilePhoto(final ProfilePhotoKey key, final ProfilePhotoModel photo) {
    try {
      valkeyClient.setex(photoKey(key), photoTtlSeconds, photo.getContent());
    } catch (final JedisException exception) {
      logFailure(OPERATION_PUT_PHOTO, exception);
    }
  }

  private String userKey(final UserId userId) {
    return String.format(USER_KEY_TEMPLATE, keyPrefix, userId.value());
  }

  private byte[] photoKey(final ProfilePhotoKey key) {
    return String.format(PHOTO_KEY_TEMPLATE, keyPrefix, key.value())
        .getBytes(StandardCharsets.UTF_8);
  }

  private static void logFailure(final String operation, final Exception exception) {
    log.warn(LOG_CACHE_FAILURE, operation, exception.getClass().getSimpleName());
  }
}
