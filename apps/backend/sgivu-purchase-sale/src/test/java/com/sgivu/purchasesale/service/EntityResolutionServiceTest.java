package com.sgivu.purchasesale.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Company;
import com.sgivu.purchasesale.dto.Motorcycle;
import com.sgivu.purchasesale.dto.Person;
import com.sgivu.purchasesale.dto.User;
import com.sgivu.purchasesale.exception.ContractValidationException;
import com.sgivu.purchasesale.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class EntityResolutionServiceTest {

  @Mock private ClientServiceClient clientServiceClient;
  @Mock private VehicleServiceClient vehicleServiceClient;
  @Mock private UserServiceClient userServiceClient;

  @InjectMocks private EntityResolutionService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("resolveClientId(Long)")
  class ResolveClientIdTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando clientId es nulo")
    void shouldThrowWhenClientIdNull() {
      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.resolveClientId(null));
      assertEquals("El ID del cliente debe ser proporcionado.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe retornar id de persona cuando el endpoint de persona encuentra al cliente")
    void shouldReturnPersonIdWhenPersonFound() {
      Long clientId = 5L;
      Person person = new Person();
      person.setId(clientId);
      when(clientServiceClient.getPersonById(clientId)).thenReturn(person);

      Long result = service.resolveClientId(clientId);

      assertEquals(clientId, result);
      verify(clientServiceClient).getPersonById(clientId);
      verify(clientServiceClient, never()).getCompanyById(any());
    }

    @Test
    @DisplayName("Debe retornar id de empresa cuando persona retorna 404 y empresa existe")
    void shouldReturnCompanyIdWhenPersonNotFound() {
      Long clientId = 6L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      Company company = new Company();
      company.setId(clientId);
      when(clientServiceClient.getCompanyById(clientId)).thenReturn(company);

      Long result = service.resolveClientId(clientId);

      assertEquals(clientId, result);
      verify(clientServiceClient).getPersonById(clientId);
      verify(clientServiceClient).getCompanyById(clientId);
    }

    @Test
    @DisplayName(
        "Debe lanzar EntityNotFoundException cuando ni persona ni empresa encuentran al cliente")
    void shouldThrowWhenPersonAndCompanyNotFound() {
      Long clientId = 99L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(clientId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      EntityNotFoundException ex =
          assertThrows(EntityNotFoundException.class, () -> service.resolveClientId(clientId));
      assertTrue(ex.getMessage().contains("Cliente no encontrado con id: 99"));

      verify(clientServiceClient).getPersonById(clientId);
      verify(clientServiceClient).getCompanyById(clientId);
    }

    @Test
    @DisplayName("Debe relanzar HttpClientErrorException no-404 del endpoint de persona")
    void shouldRethrowNon404FromPerson() {
      Long clientId = 77L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

      HttpClientErrorException ex =
          assertThrows(HttpClientErrorException.class, () -> service.resolveClientId(clientId));
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe relanzar HttpClientErrorException no-404 del endpoint de empresa")
    void shouldRethrowNon404FromCompany() {
      Long clientId = 88L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(clientId))
          .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

      HttpClientErrorException ex =
          assertThrows(HttpClientErrorException.class, () -> service.resolveClientId(clientId));
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
  }

  @Nested
  @DisplayName("resolveVehicleId(Long)")
  class ResolveVehicleIdTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando vehicleId es nulo")
    void shouldThrowWhenVehicleIdNull() {
      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.resolveVehicleId(null));
      assertEquals("El ID del vehículo debe ser proporcionado.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe retornar id del auto cuando el endpoint de auto encuentra el vehículo")
    void shouldReturnCarIdWhenCarFound() {
      Long vehicleId = 10L;
      Car car = new Car();
      car.setId(vehicleId);
      when(vehicleServiceClient.getCarById(vehicleId)).thenReturn(car);

      Long result = service.resolveVehicleId(vehicleId);

      assertEquals(vehicleId, result);
      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(vehicleServiceClient, never()).getMotorcycleById(any());
    }

    @Test
    @DisplayName("Debe retornar id de motocicleta cuando auto retorna 404 y motocicleta existe")
    void shouldReturnMotorcycleWhenCar404() {
      Long vehicleId = 11L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      Motorcycle motorcycle = new Motorcycle();
      motorcycle.setId(vehicleId);
      when(vehicleServiceClient.getMotorcycleById(vehicleId)).thenReturn(motorcycle);

      Long result = service.resolveVehicleId(vehicleId);

      assertEquals(vehicleId, result);
      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(vehicleServiceClient).getMotorcycleById(vehicleId);
    }

    @Test
    @DisplayName("Debe lanzar EntityNotFoundException cuando ni auto ni motocicleta existen")
    void shouldThrowWhenCarAndMotorcycleNotFound() {
      Long vehicleId = 99L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(vehicleServiceClient.getMotorcycleById(vehicleId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      EntityNotFoundException ex =
          assertThrows(EntityNotFoundException.class, () -> service.resolveVehicleId(vehicleId));
      assertTrue(ex.getMessage().contains("Vehículo no encontrado con id: 99"));

      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(vehicleServiceClient).getMotorcycleById(vehicleId);
    }

    @Test
    @DisplayName("Debe relanzar HttpClientErrorException no-404 del endpoint de auto")
    void shouldRethrowNon404FromCar() {
      Long vehicleId = 77L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

      HttpClientErrorException ex =
          assertThrows(HttpClientErrorException.class, () -> service.resolveVehicleId(vehicleId));
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe relanzar HttpClientErrorException no-404 del endpoint de motocicleta")
    void shouldRethrowNon404FromMotorcycle() {
      Long vehicleId = 88L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(vehicleServiceClient.getMotorcycleById(vehicleId))
          .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

      HttpClientErrorException ex =
          assertThrows(HttpClientErrorException.class, () -> service.resolveVehicleId(vehicleId));
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
  }

  @Nested
  @DisplayName("resolveUserId(Long)")
  class ResolveUserIdTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando userId es nulo")
    void shouldThrowWhenUserIdNull() {
      ContractValidationException ex =
          assertThrows(ContractValidationException.class, () -> service.resolveUserId(null));
      assertEquals("El ID del usuario debe ser proporcionado.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe retornar id de usuario cuando el usuario existe")
    void shouldReturnUserIdWhenUserExists() {
      Long userId = 7L;
      when(userServiceClient.getUserById(userId))
          .thenReturn(new User(userId, null, null, null, null, null, null, null));

      Long result = service.resolveUserId(userId);

      assertEquals(userId, result);
      verify(userServiceClient).getUserById(userId);
    }

    @Test
    @DisplayName("Debe relanzar excepciones del cliente del servicio de usuario")
    void shouldRethrowExceptionFromUserService() {
      Long userId = 88L;
      when(userServiceClient.getUserById(userId))
          .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

      HttpClientErrorException ex =
          assertThrows(HttpClientErrorException.class, () -> service.resolveUserId(userId));
      assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
    }
  }
}
