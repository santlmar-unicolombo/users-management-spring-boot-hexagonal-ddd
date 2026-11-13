package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.jcaa.usersmanagement.infrastructure.config.RequiredProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión a la caché distribuida Valkey de Aiven (Guía 10).
 *
 * <p>{@code uri} es la "Service URI" de Aiven: {@code rediss://default:PASSWORD@HOST:PORT}. El
 * esquema {@code rediss://} indica conexión cifrada con TLS. Los TTL son obligatorios porque la
 * instancia usa la política {@code allkeys-lru} y la caché solo guarda datos reconstruibles.
 */
@ConfigurationProperties(prefix = "cache.valkey")
public record ValkeyCacheProperties(
    boolean enabled,
    String uri,
    String keyPrefix,
    long userTtlSeconds,
    long photoTtlSeconds,
    int timeoutMillis,
    int maxConnections) {

  public ValkeyCacheProperties {
    if (enabled) {
      uri = RequiredProperties.requireText(uri, "cache.valkey.uri (VALKEY_URI)");
      keyPrefix = RequiredProperties.requireText(keyPrefix, "cache.valkey.key-prefix");
    }
  }
}
