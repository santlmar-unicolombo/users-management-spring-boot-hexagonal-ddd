package com.jcaa.usersmanagement.infrastructure.adapter.storage;

import com.jcaa.usersmanagement.infrastructure.config.RequiredProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión al bucket privado de Backblaze B2 mediante su API compatible con S3 (Guía 9).
 *
 * <p>Equivalencias: {@code keyId} = accessKeyId y {@code applicationKey} = secretAccessKey. Las
 * credenciales se leen de variables de entorno y nunca se escriben en el código.
 */
@ConfigurationProperties(prefix = "storage.b2")
public record B2StorageProperties(
    boolean enabled,
    String endpoint,
    String region,
    String bucket,
    String keyId,
    String applicationKey) {

  public B2StorageProperties {
    if (enabled) {
      endpoint = RequiredProperties.requireText(endpoint, "storage.b2.endpoint (B2_ENDPOINT)");
      region = RequiredProperties.requireText(region, "storage.b2.region (B2_REGION)");
      bucket = RequiredProperties.requireText(bucket, "storage.b2.bucket (B2_BUCKET)");
      keyId = RequiredProperties.requireText(keyId, "storage.b2.key-id (B2_KEY_ID)");
      applicationKey =
          RequiredProperties.requireText(
              applicationKey, "storage.b2.application-key (B2_APPLICATION_KEY)");
    }
  }
}
