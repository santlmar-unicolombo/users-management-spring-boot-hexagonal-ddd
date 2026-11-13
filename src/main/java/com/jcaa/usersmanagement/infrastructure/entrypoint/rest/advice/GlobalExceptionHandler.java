package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.advice;

import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoNotFoundException;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoStorageException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.domain.exception.UserSearchUnavailableException;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String MESSAGE_UNREADABLE_BODY =
      "El cuerpo de la solicitud es invalido o esta mal formado.";
  private static final String MESSAGE_NOT_FOUND = "El recurso solicitado no existe.";
  private static final String MESSAGE_METHOD_NOT_ALLOWED =
      "Metodo HTTP no permitido para este recurso.";
  private static final String MESSAGE_FILE_TOO_LARGE =
      "El archivo supera el tamano maximo permitido (2 MB).";
  private static final String MESSAGE_INVALID_MULTIPART =
      "Debe enviar la imagen como multipart/form-data en el campo 'file'.";
  private static final String MESSAGE_INVALID_PARAMETER =
      "El parametro '%s' tiene un valor invalido.";
  private static final String MESSAGE_UNSUPPORTED_MEDIA_TYPE =
      "Tipo de contenido no soportado para este recurso.";
  private static final String MESSAGE_FORBIDDEN = "No tiene permisos para realizar esta operacion.";

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleUserNotFound(final UserNotFoundException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler(ProfilePhotoNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleProfilePhotoNotFound(
      final ProfilePhotoNotFoundException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler({ProfilePhotoStorageException.class, UserSearchUnavailableException.class})
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ApiErrorResponse handleCloudServiceUnavailable(final DomainException exception) {
    return new ApiErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), exception.getMessage());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
  public ApiErrorResponse handleMaxUploadSize(final MaxUploadSizeExceededException ignored) {
    return new ApiErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE.value(), MESSAGE_FILE_TOO_LARGE);
  }

  @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleInvalidMultipart(final Exception ignored) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), MESSAGE_INVALID_MULTIPART);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleTypeMismatch(final MethodArgumentTypeMismatchException exception) {
    return new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        String.format(MESSAGE_INVALID_PARAMETER, exception.getName()));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
  public ApiErrorResponse handleUnsupportedMediaType(
      final HttpMediaTypeNotSupportedException ignored) {
    return new ApiErrorResponse(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), MESSAGE_UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ApiErrorResponse handleAccessDenied(final AccessDeniedException ignored) {
    return new ApiErrorResponse(HttpStatus.FORBIDDEN.value(), MESSAGE_FORBIDDEN);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleUserAlreadyExists(final UserAlreadyExistsException exception) {
    return new ApiErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiErrorResponse handleInvalidCredentials(final InvalidCredentialsException exception) {
    return new ApiErrorResponse(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleConstraintViolation(final ConstraintViolationException exception) {
    final String message =
        exception.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleMethodArgumentNotValid(
      final MethodArgumentNotValidException exception) {
    final String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(DomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleDomain(final DomainException exception) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
  }

  @ExceptionHandler(PersistenceException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handlePersistence(final PersistenceException ignored) {
    return new ApiErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error de persistencia.");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleUnreadableBody(final HttpMessageNotReadableException ignored) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), MESSAGE_UNREADABLE_BODY);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleNoResource(final NoResourceFoundException ignored) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), MESSAGE_NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
  public ApiErrorResponse handleMethodNotAllowed(
      final HttpRequestMethodNotSupportedException ignored) {
    return new ApiErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), MESSAGE_METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handleGeneral(final Exception ignored) {
    return new ApiErrorResponse(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error interno del servidor.");
  }
}

