package com.jcaa.usersmanagement.infrastructure.adapter.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.SearchUsersPort;
import com.jcaa.usersmanagement.application.port.out.UserSearchIndexPort;
import com.jcaa.usersmanagement.domain.exception.UserSearchUnavailableException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.model.UserSearchCriteria;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador de OpenSearch: mantiene el índice de usuarios y ejecuta las búsquedas (Guía 11).
 *
 * <p>Decisiones tomadas para el plan gratuito de Aiven (un nodo, máximo 20 shards):
 *
 * <ul>
 *   <li>El índice se crea con 1 shard primario y 0 réplicas.
 *   <li>El nombre usa un analizador con {@code asciifolding} para que "Jose" encuentre "José".
 *   <li>El correo usa el analizador {@code simple}, que separa "ana.perez@mail.com" en palabras.
 *   <li>El índice es una copia reconstruible: la base de datos sigue siendo la fuente de verdad.
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "search.opensearch", name = "enabled", havingValue = "true")
public class OpenSearchUserSearchAdapter implements UserSearchIndexPort, SearchUsersPort {

  private static final String INDEX_PATH = "/{index}";
  private static final String DOCUMENT_PATH = "/{index}/_doc/{id}";
  private static final String SEARCH_PATH = "/{index}/_search";
  private static final String BULK_PATH = "/_bulk?refresh=true";
  private static final MediaType NDJSON = MediaType.parseMediaType("application/x-ndjson");

  private static final String FIELD_NAME = "name";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_ROLE = "role";
  private static final String FIELD_STATUS = "status";
  private static final String FIELD_NAME_SORT = "name.keyword";
  private static final String TYPE = "type";
  private static final String TYPE_KEYWORD = "keyword";
  private static final String FOLDING_ANALYZER = "folding";

  private static final String LOG_INDEX_CREATED = "[OpenSearch] indice creado. index={}";
  private static final String LOG_INDEX_NOT_READY =
      "[OpenSearch] no se pudo verificar/crear el indice. Causa: {}";
  private static final String LOG_SYNC_FAILURE =
      "[OpenSearch] operacion {} omitida; use POST /api/users/search/reindex. Causa: {}";
  private static final String LOG_REINDEXED = "[OpenSearch] indice reconstruido. documentos={}";
  private static final String OPERATION_INDEX = "index";
  private static final String OPERATION_REMOVE = "remove";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String index;
  private volatile boolean indexReady;

  public OpenSearchUserSearchAdapter(
      final RestClient openSearchRestClient,
      final ObjectMapper objectMapper,
      final OpenSearchProperties properties) {
    this.restClient = openSearchRestClient;
    this.objectMapper = objectMapper;
    this.index = properties.index();
  }

  /** Crea el índice con su mapping al arrancar. Si OpenSearch no responde, se reintenta luego. */
  @EventListener(ApplicationReadyEvent.class)
  public void ensureIndexExists() {
    if (indexReady) {
      return;
    }
    try {
      if (!indexExists()) {
        createIndex();
        log.info(LOG_INDEX_CREATED, index);
      }
      indexReady = true;
    } catch (final RestClientException exception) {
      log.warn(LOG_INDEX_NOT_READY, exception.getClass().getSimpleName());
    }
  }

  @Override
  public void index(final UserModel user) {
    ensureIndexExists();
    try {
      restClient
          .put()
          .uri(DOCUMENT_PATH, index, user.getId().value())
          .contentType(MediaType.APPLICATION_JSON)
          .body(UserSearchDocumentMapper.fromModelToDocument(user))
          .retrieve()
          .toBodilessEntity();
    } catch (final RestClientException exception) {
      log.warn(LOG_SYNC_FAILURE, OPERATION_INDEX, exception.getClass().getSimpleName());
    }
  }

  @Override
  public void remove(final UserId userId) {
    try {
      restClient.delete().uri(DOCUMENT_PATH, index, userId.value()).retrieve().toBodilessEntity();
    } catch (final HttpClientErrorException.NotFound alreadyAbsent) {
      // El documento no estaba indexado: no hay nada que borrar.
    } catch (final RestClientException exception) {
      log.warn(LOG_SYNC_FAILURE, OPERATION_REMOVE, exception.getClass().getSimpleName());
    }
  }

  @Override
  public int reindexAll(final List<UserModel> users) {
    try {
      deleteIndexIfExists();
      createIndex();
      indexReady = true;
      final int indexed = users.isEmpty() ? 0 : bulkIndex(users);
      log.info(LOG_REINDEXED, indexed);
      return indexed;
    } catch (final RestClientException | JsonProcessingException exception) {
      throw UserSearchUnavailableException.becauseSearchFailed(exception);
    }
  }

  @Override
  public List<UserSearchResultModel> search(final UserSearchCriteria criteria) {
    try {
      final JsonNode response =
          restClient
              .post()
              .uri(SEARCH_PATH, index)
              .contentType(MediaType.APPLICATION_JSON)
              .body(buildSearchRequest(criteria))
              .retrieve()
              .body(JsonNode.class);
      return parseHits(response);
    } catch (final HttpClientErrorException.NotFound indexMissing) {
      return List.of();
    } catch (final RestClientException | JsonProcessingException exception) {
      throw UserSearchUnavailableException.becauseSearchFailed(exception);
    }
  }

  // ── índice

  private boolean indexExists() {
    try {
      restClient.head().uri(INDEX_PATH, index).retrieve().toBodilessEntity();
      return true;
    } catch (final HttpClientErrorException.NotFound notFound) {
      return false;
    }
  }

  private void createIndex() {
    restClient
        .put()
        .uri(INDEX_PATH, index)
        .contentType(MediaType.APPLICATION_JSON)
        .body(indexDefinition())
        .retrieve()
        .toBodilessEntity();
  }

  private void deleteIndexIfExists() {
    try {
      restClient.delete().uri(INDEX_PATH, index).retrieve().toBodilessEntity();
    } catch (final HttpClientErrorException.NotFound notFound) {
      // No existía: se creará desde cero.
    }
  }

  private static Map<String, Object> indexDefinition() {
    final Map<String, Object> foldingAnalyzer =
        Map.of(
            TYPE, "custom",
            "tokenizer", "standard",
            "filter", List.of("lowercase", "asciifolding"));
    final Map<String, Object> settings =
        Map.of(
            "number_of_shards", 1,
            "number_of_replicas", 0,
            "analysis", Map.of("analyzer", Map.of(FOLDING_ANALYZER, foldingAnalyzer)));
    final Map<String, Object> keywordSubField = Map.of(TYPE_KEYWORD, Map.of(TYPE, TYPE_KEYWORD));
    final Map<String, Object> properties =
        Map.of(
            "id", Map.of(TYPE, TYPE_KEYWORD),
            FIELD_NAME,
                Map.of(TYPE, "text", "analyzer", FOLDING_ANALYZER, "fields", keywordSubField),
            FIELD_EMAIL, Map.of(TYPE, "text", "analyzer", "simple", "fields", keywordSubField),
            FIELD_ROLE, Map.of(TYPE, TYPE_KEYWORD),
            FIELD_STATUS, Map.of(TYPE, TYPE_KEYWORD),
            "hasProfilePhoto", Map.of(TYPE, "boolean"));
    return Map.of("settings", settings, "mappings", Map.of("properties", properties));
  }

  private int bulkIndex(final List<UserModel> users) throws JsonProcessingException {
    final StringBuilder body = new StringBuilder();
    for (final UserModel user : users) {
      final Map<String, Object> action =
          Map.of("index", Map.of("_index", index, "_id", user.getId().value()));
      body.append(objectMapper.writeValueAsString(action)).append('\n');
      body.append(
              objectMapper.writeValueAsString(UserSearchDocumentMapper.fromModelToDocument(user)))
          .append('\n');
    }
    final JsonNode response =
        restClient
            .post()
            .uri(BULK_PATH)
            .contentType(NDJSON)
            .body(body.toString())
            .retrieve()
            .body(JsonNode.class);
    return countSuccessfulItems(response);
  }

  private static int countSuccessfulItems(final JsonNode bulkResponse) {
    int indexed = 0;
    for (final JsonNode item : bulkResponse.path("items")) {
      final int status = item.path("index").path("status").asInt();
      if (status >= 200 && status < 300) {
        indexed++;
      }
    }
    return indexed;
  }

  // ── búsqueda

  private static Map<String, Object> buildSearchRequest(final UserSearchCriteria criteria) {
    final List<Object> filters = new ArrayList<>();
    criteria.getRole().ifPresent(role -> filters.add(term(FIELD_ROLE, role.name())));
    criteria.getStatus().ifPresent(status -> filters.add(term(FIELD_STATUS, status.name())));

    final Object textQuery =
        criteria
            .getText()
            .<Object>map(OpenSearchUserSearchAdapter::fuzzyPrefixQuery)
            .orElse(Map.of("match_all", Map.of()));

    final Map<String, Object> request = new LinkedHashMap<>();
    request.put("size", criteria.getSize());
    request.put("query", Map.of("bool", Map.of("must", List.of(textQuery), "filter", filters)));
    if (criteria.getText().isEmpty()) {
      request.put("sort", List.of(Map.of(FIELD_NAME_SORT, Map.of("order", "asc"))));
    }
    return request;
  }

  /**
   * Búsqueda de texto completo en nombre (con mayor peso) y correo. {@code bool_prefix} trata la
   * última palabra como prefijo ("ana per" encuentra "Ana Pérez") y {@code fuzziness: AUTO}
   * tolera errores de escritura en las demás ("Lopes" encuentra "López").
   */
  private static Map<String, Object> fuzzyPrefixQuery(final String text) {
    return Map.of(
        "multi_match",
        Map.of(
            "query", text,
            TYPE, "bool_prefix",
            "fields", List.of(FIELD_NAME + "^2", FIELD_EMAIL),
            "fuzziness", "AUTO",
            "operator", "and"));
  }

  private static Map<String, Object> term(final String field, final String value) {
    return Map.of("term", Map.of(field, value));
  }

  private List<UserSearchResultModel> parseHits(final JsonNode response)
      throws JsonProcessingException {
    final List<UserSearchResultModel> results = new ArrayList<>();
    for (final JsonNode hit : response.path("hits").path("hits")) {
      final UserSearchDocument document =
          objectMapper.treeToValue(hit.path("_source"), UserSearchDocument.class);
      results.add(
          UserSearchDocumentMapper.fromDocumentToResult(document, hit.path("_score").asDouble()));
    }
    return results;
  }
}
