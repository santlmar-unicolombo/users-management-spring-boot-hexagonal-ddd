package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.UserRestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/** Contrato OpenAPI para {@link UserProfilePhotoRestController}. */
@Tag(
    name = "Profile photo",
    description =
        "Foto de perfil almacenada en un bucket privado de Backblaze B2. "
            + "Solo el propio usuario o un ADMIN pueden subirla o borrarla.")
public interface UserProfilePhotoRestControllerDocs {

  @Operation(
      summary = "Subir o reemplazar la foto de perfil",
      description =
          "multipart/form-data con el campo 'file'. Formatos JPEG, PNG o WEBP (se valida el "
              + "contenido real del archivo), máximo 2 MB. La foto anterior se borra del bucket.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Foto guardada. Retorna el usuario con su profilePhotoUrl.",
        content = @Content(schema = @Schema(implementation = UserRestResponse.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Archivo vacío, mayor a 2 MB o en un formato no admitido.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "Solo el propio usuario o un ADMIN.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "404",
        description = "El usuario no existe.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "503",
        description = "El bucket no está configurado o no respondió.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  UserRestResponse upload(
      @Parameter(description = "ID del usuario.", required = true) String id, MultipartFile file)
      throws IOException;

  @Operation(
      summary = "Obtener la foto de perfil",
      description =
          "Retorna los bytes de la imagen con su Content-Type. Se sirve desde la caché "
              + "(Valkey) cuando está disponible para no descargarla de nuevo del bucket.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Imagen JPEG, PNG o WEBP.",
        content = {
          @Content(mediaType = "image/jpeg"),
          @Content(mediaType = "image/png"),
          @Content(mediaType = "image/webp")
        }),
    @ApiResponse(
        responseCode = "404",
        description = "El usuario no existe o no tiene foto.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  ResponseEntity<byte[]> get(@Parameter(description = "ID del usuario.", required = true) String id);

  @Operation(
      summary = "Eliminar la foto de perfil",
      description = "Quita la referencia del usuario y borra el objeto del bucket. Es idempotente.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Foto eliminada (o el usuario no tenía)."),
    @ApiResponse(
        responseCode = "404",
        description = "El usuario no existe.",
        content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  void delete(@Parameter(description = "ID del usuario.", required = true) String id);
}
