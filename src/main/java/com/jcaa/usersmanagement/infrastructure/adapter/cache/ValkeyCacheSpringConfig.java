package com.jcaa.usersmanagement.infrastructure.adapter.cache;

import com.jcaa.usersmanagement.infrastructure.config.ConfigurationException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

/**
 * Cliente Jedis ({@code JedisPooled}), la misma librería que propone el "Quick connect" de Aiven
 * para Java. Los datos de conexión se toman de la Service URI de Aiven:
 * {@code rediss://default:PASSWORD@HOST:PORT}, donde {@code rediss} activa TLS.
 *
 * <p>El pool valida las conexiones antes de usarlas porque Valkey cierra las conexiones inactivas
 * después de {@code valkey_timeout = 300} segundos.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "cache.valkey", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ValkeyCacheProperties.class)
public class ValkeyCacheSpringConfig {

  private static final String LOG_CLIENT_READY =
      "[ValkeyCacheSpringConfig] cliente Valkey inicializado. host={} tls={}";
  private static final String TLS_SCHEME = "rediss";
  private static final String DEFAULT_USER = "default";
  private static final String USER_INFO_SEPARATOR = ":";
  private static final String URI_PROPERTY = "cache.valkey.uri (VALKEY_URI) con host y puerto";

  @Bean(destroyMethod = "close")
  public JedisPooled valkeyClient(final ValkeyCacheProperties properties) {
    final URI uri = URI.create(properties.uri());
    if (Objects.isNull(uri.getHost()) || uri.getPort() < 0) {
      throw ConfigurationException.becauseRequiredPropertyIsMissing(URI_PROPERTY);
    }
    final boolean tls = TLS_SCHEME.equalsIgnoreCase(uri.getScheme());

    final ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(properties.maxConnections());
    poolConfig.setMaxIdle(properties.maxConnections());
    poolConfig.setTestOnBorrow(true);
    poolConfig.setTestWhileIdle(true);

    log.info(LOG_CLIENT_READY, uri.getHost(), tls);
    return new JedisPooled(
        new HostAndPort(uri.getHost(), uri.getPort()),
        buildClientConfig(uri, tls, properties.timeoutMillis()),
        poolConfig);
  }

  private static JedisClientConfig buildClientConfig(
      final URI uri, final boolean tls, final int timeoutMillis) {
    final DefaultJedisClientConfig.Builder builder =
        DefaultJedisClientConfig.builder()
            .ssl(tls)
            .connectionTimeoutMillis(timeoutMillis)
            .socketTimeoutMillis(timeoutMillis);

    final String userInfo = uri.getRawUserInfo();
    if (Objects.isNull(userInfo)) {
      return builder.build();
    }
    final int separator = userInfo.indexOf(USER_INFO_SEPARATOR);
    if (separator < 0) {
      // Solo contraseña (formato redis://:PASSWORD@host o redis://PASSWORD@host)
      return builder.user(DEFAULT_USER).password(decode(userInfo)).build();
    }
    final String user = decode(userInfo.substring(0, separator));
    return builder
        .user(user.isBlank() ? DEFAULT_USER : user)
        .password(decode(userInfo.substring(separator + 1)))
        .build();
  }

  private static String decode(final String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
