package com.jcaa.usersmanagement.infrastructure.adapter.storage;

import com.jcaa.usersmanagement.application.port.out.ProfilePhotoStoragePort;
import com.jcaa.usersmanagement.domain.exception.ProfilePhotoStorageException;
import com.jcaa.usersmanagement.domain.model.ProfilePhotoModel;
import com.jcaa.usersmanagement.domain.valueobject.ProfilePhotoKey;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Adaptador de salida hacia el bucket privado de Backblaze B2. Los objetos nunca se exponen
 * públicamente: la API descarga la imagen con sus credenciales y la entrega al cliente autorizado.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "storage.b2", name = "enabled", havingValue = "true")
public class BackblazeB2ProfilePhotoStorageAdapter implements ProfilePhotoStoragePort {

  private static final String LOG_UPLOADED = "[B2] objeto subido. bytes={}";
  private static final String LOG_FAILURE = "[B2] operacion {} fallida. Causa: {}";
  private static final String OPERATION_UPLOAD = "upload";
  private static final String OPERATION_DOWNLOAD = "download";
  private static final String OPERATION_DELETE = "delete";

  private final S3Client s3Client;
  private final String bucket;

  public BackblazeB2ProfilePhotoStorageAdapter(
      final S3Client s3Client, final B2StorageProperties properties) {
    this.s3Client = s3Client;
    this.bucket = properties.bucket();
  }

  @Override
  public void upload(final ProfilePhotoKey key, final ProfilePhotoModel photo) {
    final PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key.value())
            .contentType(photo.getFormat().contentType())
            .contentLength((long) photo.getSizeInBytes())
            .build();
    try {
      s3Client.putObject(request, RequestBody.fromBytes(photo.getContent()));
      log.info(LOG_UPLOADED, photo.getSizeInBytes());
    } catch (final SdkException exception) {
      log.warn(LOG_FAILURE, OPERATION_UPLOAD, exception.getClass().getSimpleName());
      throw ProfilePhotoStorageException.becauseUploadFailed(exception);
    }
  }

  @Override
  public Optional<ProfilePhotoModel> download(final ProfilePhotoKey key) {
    final GetObjectRequest request =
        GetObjectRequest.builder().bucket(bucket).key(key.value()).build();
    try {
      final ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(request);
      return Optional.of(ProfilePhotoModel.fromStorage(object.asByteArray()));
    } catch (final NoSuchKeyException notFound) {
      return Optional.empty();
    } catch (final SdkException exception) {
      log.warn(LOG_FAILURE, OPERATION_DOWNLOAD, exception.getClass().getSimpleName());
      throw ProfilePhotoStorageException.becauseDownloadFailed(exception);
    }
  }

  @Override
  public void delete(final ProfilePhotoKey key) {
    final DeleteObjectRequest request =
        DeleteObjectRequest.builder().bucket(bucket).key(key.value()).build();
    try {
      s3Client.deleteObject(request);
    } catch (final SdkException exception) {
      log.warn(LOG_FAILURE, OPERATION_DELETE, exception.getClass().getSimpleName());
      throw ProfilePhotoStorageException.becauseDeleteFailed(exception);
    }
  }
}
