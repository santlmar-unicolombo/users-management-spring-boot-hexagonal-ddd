package com.jcaa.usersmanagement.infrastructure.adapter.storage;

import java.net.URI;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Cliente del AWS SDK v2 apuntando al endpoint S3 de Backblaze B2 (no a AWS).
 *
 * <p>Desde la versión 2.30 el SDK calcula checksums CRC por defecto en cada carga; algunos
 * servicios compatibles con S3 no los aceptan, por eso se limitan a cuando la operación los exige.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "storage.b2", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(B2StorageProperties.class)
public class B2StorageSpringConfig {

  private static final Duration API_CALL_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration API_CALL_ATTEMPT_TIMEOUT = Duration.ofSeconds(15);
  private static final String LOG_CLIENT_READY =
      "[B2StorageSpringConfig] cliente S3 para Backblaze B2 inicializado. endpoint={} bucket={}";

  @Bean(destroyMethod = "close")
  public S3Client b2S3Client(final B2StorageProperties properties) {
    log.info(LOG_CLIENT_READY, properties.endpoint(), properties.bucket());
    return S3Client.builder()
        .endpointOverride(URI.create(properties.endpoint()))
        .region(Region.of(properties.region()))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.keyId(), properties.applicationKey())))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
        .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
        .overrideConfiguration(
            ClientOverrideConfiguration.builder()
                .apiCallTimeout(API_CALL_TIMEOUT)
                .apiCallAttemptTimeout(API_CALL_ATTEMPT_TIMEOUT)
                .build())
        .build();
  }
}
