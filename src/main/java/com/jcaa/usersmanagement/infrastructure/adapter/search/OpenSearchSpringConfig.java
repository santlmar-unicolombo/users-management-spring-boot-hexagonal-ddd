package com.jcaa.usersmanagement.infrastructure.adapter.search;

import com.jcaa.usersmanagement.infrastructure.config.ConfigurationException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP para la API REST de OpenSearch (HTTPS + autenticación básica). Se usa el
 * {@link RestClient} de Spring para no agregar dependencias: las operaciones necesarias (crear
 * índice, indexar, borrar y buscar documentos JSON) son llamadas REST simples.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "search.opensearch", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(OpenSearchProperties.class)
public class OpenSearchSpringConfig {

  private static final String LOG_CLIENT_READY =
      "[OpenSearchSpringConfig] cliente OpenSearch inicializado. host={} index={}";
  private static final String CREDENTIALS_PROPERTY =
      "search.opensearch.username/password (OPENSEARCH_USERNAME/OPENSEARCH_PASSWORD)";
  private static final String USER_INFO_SEPARATOR = ":";

  @Bean
  public RestClient openSearchRestClient(final OpenSearchProperties properties) {
    final URI uri = URI.create(properties.url());
    final String[] credentials = resolveCredentials(uri, properties);
    final String baseUrl = uri.getScheme() + "://" + uri.getHost() + portSuffix(uri);

    final HttpClient httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(properties.timeoutMillis()))
            .build();
    final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofMillis(properties.timeoutMillis()));

    log.info(LOG_CLIENT_READY, uri.getHost(), properties.index());
    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .defaultHeaders(headers -> headers.setBasicAuth(credentials[0], credentials[1]))
        .build();
  }

  /** Usa username/password explícitos; si faltan, los toma de la URI de Aiven. */
  private static String[] resolveCredentials(final URI uri, final OpenSearchProperties properties) {
    if (hasText(properties.username()) && hasText(properties.password())) {
      return new String[] {properties.username(), properties.password()};
    }
    final String userInfo = uri.getRawUserInfo();
    if (Objects.isNull(userInfo) || !userInfo.contains(USER_INFO_SEPARATOR)) {
      throw ConfigurationException.becauseRequiredPropertyIsMissing(CREDENTIALS_PROPERTY);
    }
    final int separator = userInfo.indexOf(USER_INFO_SEPARATOR);
    return new String[] {
      URLDecoder.decode(userInfo.substring(0, separator), StandardCharsets.UTF_8),
      URLDecoder.decode(userInfo.substring(separator + 1), StandardCharsets.UTF_8)
    };
  }

  private static String portSuffix(final URI uri) {
    return uri.getPort() > 0 ? USER_INFO_SEPARATOR + uri.getPort() : "";
  }

  private static boolean hasText(final String value) {
    return Objects.nonNull(value) && !value.isBlank();
  }
}
