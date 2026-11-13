package com.jcaa.usersmanagement.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import com.jcaa.usersmanagement.domain.enums.ProfilePhotoFormat;
import com.jcaa.usersmanagement.domain.exception.InvalidProfilePhotoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests para ProfilePhotoModel y la detección de formato de ProfilePhotoFormat.
 *
 * <p>Cubre: detección JPEG/PNG/WEBP por firma binaria, rechazo de formatos no admitidos, archivo
 * vacío, límite de tamaño y copia defensiva del contenido.
 */
@DisplayName("ProfilePhotoModel")
class ProfilePhotoModelTest {

  private static final byte[] PNG_BYTES = {
    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01
  };
  private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
  private static final byte[] WEBP_BYTES = {
    0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50
  };
  private static final byte[] GIF_BYTES = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

  @Test
  @DisplayName("fromUpload() detecta PNG, JPEG y WEBP por su contenido")
  void shouldDetectSupportedFormats() {
    // Act & Assert
    assertAll(
        "formatos admitidos",
        () -> assertEquals(ProfilePhotoFormat.PNG, formatOf(PNG_BYTES)),
        () -> assertEquals(ProfilePhotoFormat.JPEG, formatOf(JPEG_BYTES)),
        () -> assertEquals(ProfilePhotoFormat.WEBP, formatOf(WEBP_BYTES)));
  }

  private static ProfilePhotoFormat formatOf(final byte[] content) {
    return ProfilePhotoModel.fromUpload(content).getFormat();
  }

  @Test
  @DisplayName("fromUpload() rechaza formatos no admitidos aunque el cliente diga que es imagen")
  void shouldRejectUnsupportedFormat() {
    // Act & Assert
    assertThrows(InvalidProfilePhotoException.class, () -> ProfilePhotoModel.fromUpload(GIF_BYTES));
  }

  @Test
  @DisplayName("fromUpload() rechaza un archivo vacío")
  void shouldRejectEmptyContent() {
    // Act & Assert
    final byte[] empty = new byte[0];
    assertThrows(InvalidProfilePhotoException.class, () -> ProfilePhotoModel.fromUpload(empty));
  }

  @Test
  @DisplayName("fromUpload() rechaza imágenes mayores a 2 MB")
  void shouldRejectContentLargerThanLimit() {
    // Arrange
    final byte[] tooLarge = new byte[ProfilePhotoModel.MAXIMUM_SIZE_BYTES + 1];
    System.arraycopy(PNG_BYTES, 0, tooLarge, 0, PNG_BYTES.length);

    // Act & Assert
    assertThrows(InvalidProfilePhotoException.class, () -> ProfilePhotoModel.fromUpload(tooLarge));
  }

  @Test
  @DisplayName("getContent() retorna una copia: modificarla no altera la foto")
  void shouldReturnDefensiveCopy() {
    // Arrange
    final ProfilePhotoModel photo = ProfilePhotoModel.fromUpload(PNG_BYTES);

    // Act
    photo.getContent()[0] = 0;

    // Assert
    assertArrayEquals(PNG_BYTES, photo.getContent());
  }
}
