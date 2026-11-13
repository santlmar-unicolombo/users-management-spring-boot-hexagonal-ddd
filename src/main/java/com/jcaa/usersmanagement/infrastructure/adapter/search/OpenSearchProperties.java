package com.jcaa.usersmanagement.infrastructure.adapter.search;

import com.jcaa.usersmanagement.infrastructure.config.RequiredProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión al servicio OpenSearch de Aiven (Guía 11).
 *
 * <p>{@code url} es la "Service URI" de la pestaña OpenSearch (puerto del motor, no el 443 de
 * Dashboards). Puede incluir las credenciales ({@code https://avnadmin:PASSWORD@HOST:PORT}) o
 * enviarse sin ellas y definir {@code username}/{@code password} por separado.
 */
@ConfigurationProperties(prefix = "search.opensearch")
public record OpenSearchProperties(
    boolean enabled,
    String url,
    String username,
    String password,
    String index,
    int timeoutMillis) {

  public OpenSearchProperties {
    if (enabled) {
      url = RequiredProperties.requireText(url, "search.opensearch.url (OPENSEARCH_URL)");
      index = RequiredProperties.requireText(index, "search.opensearch.index (OPENSEARCH_INDEX)");
    }
  }
}
