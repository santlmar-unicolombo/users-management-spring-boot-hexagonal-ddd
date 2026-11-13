package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ReindexRestResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserSearchRestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

/** Contrato OpenAPI para {@link UserSearchRestController}. */
@Tag(
    name = "User search",
    description = "Búsqueda de usuarios sobre el índice de OpenSearch (Guía 11).")
public interface UserSearchRestControllerDocs {

  @Operation(
      summary = "Buscar usuarios",
      description =
          "Texto libre sobre nombre y correo, tolerante a errores de escritura y a búsquedas "
              + "parciales, con filtros exactos opcionales por rol y estado. Los resultados se "
              + "ordenan por relevancia. Sin 'q' se listan por nombre.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Resultados (puede ser vacío).",
        content =
            @Content(
                array = @ArraySchema(schema = @Schema(implementation = UserSearchRestResponse.class)))),
    @ApiResponse(
        responseCode = "400",
        description = "Parámetros inválidos (rol/estado inexistente o size fuera de 1..50).",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description = "OpenSearch no está configurado o no respondió.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  List<UserSearchRestResponse> search(
      @Parameter(description = "Texto a buscar en nombre o correo.", example = "ana") String text,
      @Parameter(description = "ADMIN, MEMBER o REVIEWER.") String role,
      @Parameter(description = "ACTIVE, INACTIVE, PENDING o BLOCKED.") String status,
      @Parameter(description = "Cantidad máxima de resultados (1..50).", example = "20") int size);

  @Operation(
      summary = "Reconstruir el índice de búsqueda (solo ADMIN)",
      description =
          "Borra y vuelve a crear el índice a partir de la base de datos. Útil la primera vez o "
              + "si el índice se perdió.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Cantidad de usuarios indexados.",
        content = @Content(schema = @Schema(implementation = ReindexRestResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description = "OpenSearch no está configurado o no respondió.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  ReindexRestResponse reindex();
}
