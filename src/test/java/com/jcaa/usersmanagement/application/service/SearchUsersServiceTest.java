package com.jcaa.usersmanagement.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jcaa.usersmanagement.application.port.out.SearchUsersPort;
import com.jcaa.usersmanagement.application.service.dto.query.SearchUsersQuery;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.InvalidUserRoleException;
import com.jcaa.usersmanagement.domain.model.UserSearchCriteria;
import com.jcaa.usersmanagement.domain.model.UserSearchResultModel;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("SearchUsersService")
@ExtendWith(MockitoExtension.class)
class SearchUsersServiceTest {

  @Mock private SearchUsersPort searchUsersPort;

  private SearchUsersService service;

  @BeforeEach
  void setUp() {
    try (final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      service = new SearchUsersService(searchUsersPort, validatorFactory.getValidator());
    }
  }

  @Test
  @DisplayName("execute() convierte la consulta en criterios tipados y retorna los resultados")
  void shouldMapQueryToCriteria() {
    // Arrange
    final List<UserSearchResultModel> expected =
        List.of(
            new UserSearchResultModel(
                "u-1", "Ana", "ana@example.com", UserRole.ADMIN, UserStatus.ACTIVE, false, 1.5));
    final ArgumentCaptor<UserSearchCriteria> criteria =
        ArgumentCaptor.forClass(UserSearchCriteria.class);
    when(searchUsersPort.search(criteria.capture())).thenReturn(expected);

    // Act
    final List<UserSearchResultModel> result =
        service.execute(new SearchUsersQuery(" ana ", "admin", "", 10));

    // Assert
    assertAll(
        "criterios",
        () -> assertSame(expected, result),
        () -> assertEquals(Optional.of("ana"), criteria.getValue().getText()),
        () -> assertEquals(Optional.of(UserRole.ADMIN), criteria.getValue().getRole()),
        () -> assertTrue(criteria.getValue().getStatus().isEmpty(), "status vacío = sin filtro"),
        () -> assertEquals(10, criteria.getValue().getSize()));
  }

  @Test
  @DisplayName("execute() rechaza un rol inexistente")
  void shouldRejectUnknownRole() {
    // Arrange
    final SearchUsersQuery query = new SearchUsersQuery("ana", "SUPERADMIN", null, 10);

    // Act & Assert
    assertThrows(InvalidUserRoleException.class, () -> service.execute(query));
    verifyNoInteractions(searchUsersPort);
  }

  @Test
  @DisplayName("execute() rechaza un tamaño de página fuera de 1..50")
  void shouldRejectInvalidSize() {
    // Arrange
    final SearchUsersQuery query = new SearchUsersQuery("ana", null, null, 500);

    // Act & Assert
    assertThrows(ConstraintViolationException.class, () -> service.execute(query));
    verifyNoInteractions(searchUsersPort);
  }
}
