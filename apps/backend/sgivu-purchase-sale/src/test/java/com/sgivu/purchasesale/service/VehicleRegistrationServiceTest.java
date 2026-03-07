package com.sgivu.purchasesale.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Motorcycle;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.VehicleCreationRequest;
import com.sgivu.purchasesale.enums.VehicleType;
import com.sgivu.purchasesale.exception.VehicleRegistrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class VehicleRegistrationServiceTest {

  @Mock private VehicleServiceClient vehicleServiceClient;

  @InjectMocks private VehicleRegistrationService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  // === Helpers para construir VehicleCreationRequest con datos mínimos válidos ===

  private VehicleCreationRequest validCarData() {
    VehicleCreationRequest data = new VehicleCreationRequest();
    data.setVehicleType(VehicleType.CAR);
    data.setBrand("Brand");
    data.setModel("Model");
    data.setCapacity(4);
    data.setLine("Line");
    data.setPlate("ABC123");
    data.setMotorNumber("M1");
    data.setSerialNumber("S1");
    data.setChassisNumber("C1");
    data.setColor("Red");
    data.setCityRegistered("City");
    data.setYear(2020);
    data.setMileage(1000);
    data.setTransmission("AUTO");
    data.setPurchasePrice(1000d);
    data.setSalePrice(1200d);
    data.setBodyType("SEDAN");
    data.setFuelType("GAS");
    data.setNumberOfDoors(4);
    return data;
  }

  private VehicleCreationRequest validMotorcycleData() {
    VehicleCreationRequest data = new VehicleCreationRequest();
    data.setVehicleType(VehicleType.MOTORCYCLE);
    data.setBrand("Brand");
    data.setModel("Model");
    data.setCapacity(2);
    data.setLine("Line");
    data.setPlate("ABC123");
    data.setMotorNumber("M1");
    data.setSerialNumber("S1");
    data.setChassisNumber("C1");
    data.setColor("Blue");
    data.setCityRegistered("City");
    data.setYear(2021);
    data.setMileage(500);
    data.setTransmission("Manual");
    data.setPurchasePrice(800d);
    data.setSalePrice(1000d);
    data.setMotorcycleType("Sport");
    return data;
  }

  @Nested
  @DisplayName("registerVehicle(PurchaseSaleRequest)")
  class RegisterVehicleTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando vehicleData es nulo")
    void shouldThrowWhenVehicleDataIsNull() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();

      VehicleRegistrationException ex =
          assertThrows(VehicleRegistrationException.class, () -> service.registerVehicle(request));
      assertEquals(
          "Debes proporcionar los datos del vehículo para registrar una compra.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando vehicleType no está especificado")
    void shouldThrowWhenVehicleTypeNotSpecified() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      request.setVehicleData(data);

      VehicleRegistrationException ex =
          assertThrows(VehicleRegistrationException.class, () -> service.registerVehicle(request));
      assertEquals(
          "Debes especificar si se trata de un automóvil o una motocicleta.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe registrar auto cuando los datos son válidos")
    void shouldRegisterCarWhenDataIsValid() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleData(validCarData());

      doAnswer(
              invocation -> {
                Car arg = invocation.getArgument(0);
                arg.setId(21L);
                return arg;
              })
          .when(vehicleServiceClient)
          .createCar(any(Car.class));

      Long result = service.registerVehicle(request);

      assertEquals(21L, result);
      verify(vehicleServiceClient).createCar(any(Car.class));
      verify(vehicleServiceClient)
          .createCar(argThat(c -> c.getBrand().equals("Brand") && c.getBodyType().equals("SEDAN")));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta bodyType del auto")
    void shouldThrowWhenCarBodyTypeMissing() {
      VehicleCreationRequest data = validCarData();
      data.setBodyType(null);

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleData(data);

      VehicleRegistrationException ex =
          assertThrows(VehicleRegistrationException.class, () -> service.registerVehicle(request));
      assertEquals("La carrocería del automóvil es obligatoria.", ex.getMessage());
    }

    @Test
    @DisplayName("Debe registrar motocicleta cuando los datos son válidos")
    void shouldRegisterMotorcycleWhenDataIsValid() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleData(validMotorcycleData());

      doAnswer(
              invocation -> {
                Motorcycle arg = invocation.getArgument(0);
                arg.setId(22L);
                return arg;
              })
          .when(vehicleServiceClient)
          .createMotorcycle(any());

      Long result = service.registerVehicle(request);

      assertEquals(22L, result);
      verify(vehicleServiceClient).createMotorcycle(any());
      verify(vehicleServiceClient)
          .createMotorcycle(
              argThat(m -> m.getBrand().equals("Brand") && m.getMotorcycleType().equals("Sport")));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando falta tipo de motocicleta")
    void shouldThrowWhenMotorcycleTypeMissing() {
      VehicleCreationRequest data = validMotorcycleData();
      data.setMotorcycleType(null);

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleData(data);

      VehicleRegistrationException ex =
          assertThrows(VehicleRegistrationException.class, () -> service.registerVehicle(request));
      assertEquals("Debes indicar el tipo de motocicleta.", ex.getMessage());
    }
  }

  @Nested
  @DisplayName("requireText(String, String)")
  class RequireTextTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando el valor es nulo o vacío")
    void shouldThrowWhenValueNullOrEmpty() {
      String msg = "Custom message";

      assertThrows(VehicleRegistrationException.class, () -> service.requireText(null, msg));
      assertThrows(VehicleRegistrationException.class, () -> service.requireText("", msg));
      assertThrows(VehicleRegistrationException.class, () -> service.requireText("   ", msg));
    }

    @Test
    @DisplayName("Debe retornar cadena recortada cuando el valor tiene espacios alrededor")
    void shouldReturnTrimmedValue() {
      assertEquals("abc", service.requireText("  abc  ", "irrelevant"));
    }

    @Test
    @DisplayName("Debe retornar misma cadena cuando no tiene espacios extra")
    void shouldReturnSameWhenNoWhitespace() {
      assertEquals("a", service.requireText("a", "irrelevant"));
    }
  }

  @Nested
  @DisplayName("requirePositiveInteger(Integer, String)")
  class RequirePositiveIntegerTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando el valor es nulo")
    void shouldThrowWhenValueNull() {
      assertThrows(
          VehicleRegistrationException.class,
          () -> service.requirePositiveInteger(null, "Number required"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el valor es cero o negativo")
    void shouldThrowWhenZeroOrNegative() {
      assertThrows(
          VehicleRegistrationException.class,
          () -> service.requirePositiveInteger(0, "Must be positive"));
      assertThrows(
          VehicleRegistrationException.class,
          () -> service.requirePositiveInteger(-3, "Must be positive"));
    }

    @Test
    @DisplayName("Debe retornar valor cuando es positivo")
    void shouldReturnValueWhenPositive() {
      assertEquals(7, service.requirePositiveInteger(7, "irrelevant"));
    }
  }

  @Nested
  @DisplayName("requireValidYear(Integer)")
  class RequireValidYearTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando el año es nulo o no positivo")
    void shouldThrowWhenYearNullOrNonPositive() {
      assertThrows(VehicleRegistrationException.class, () -> service.requireValidYear(null));
      assertThrows(VehicleRegistrationException.class, () -> service.requireValidYear(0));
      assertThrows(VehicleRegistrationException.class, () -> service.requireValidYear(-5));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el año está fuera de rango")
    void shouldThrowWhenYearOutOfRange() {
      assertThrows(VehicleRegistrationException.class, () -> service.requireValidYear(1949));
      assertThrows(VehicleRegistrationException.class, () -> service.requireValidYear(2051));
    }

    @Test
    @DisplayName("Debe retornar el mismo valor cuando el año es válido")
    void shouldReturnValueWhenYearValid() {
      assertEquals(1950, service.requireValidYear(1950));
      assertEquals(2000, service.requireValidYear(2000));
      assertEquals(2050, service.requireValidYear(2050));
    }
  }

  @Nested
  @DisplayName("requireNonNegativeInteger(Integer)")
  class RequireNonNegativeIntegerTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando el valor es nulo")
    void shouldThrowWhenValueNull() {
      assertThrows(
          VehicleRegistrationException.class, () -> service.requireNonNegativeInteger(null));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el valor es negativo")
    void shouldThrowWhenNegative() {
      assertThrows(VehicleRegistrationException.class, () -> service.requireNonNegativeInteger(-1));
    }

    @Test
    @DisplayName("Debe retornar valor cuando es cero o positivo")
    void shouldReturnWhenZeroOrPositive() {
      assertEquals(0, service.requireNonNegativeInteger(0));
      assertEquals(15, service.requireNonNegativeInteger(15));
    }
  }

  @Nested
  @DisplayName("resolveVehiclePurchasePrice(VehicleCreationRequest, Double)")
  class ResolveVehiclePurchasePriceTests {

    @Test
    @DisplayName("Debe usar precio de vehicleData cuando está presente")
    void shouldUseVehicleDataPrice() {
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setPurchasePrice(500d);

      assertEquals(500d, service.resolveVehiclePurchasePrice(data, 300d));
    }

    @Test
    @DisplayName("Debe usar fallback cuando vehicleData.purchasePrice es nulo")
    void shouldUseFallbackWhenVehicleDataPriceNull() {
      VehicleCreationRequest data = new VehicleCreationRequest();

      assertEquals(300d, service.resolveVehiclePurchasePrice(data, 300d));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ambas fuentes son nulas o inválidas")
    void shouldThrowWhenBothSourcesInvalid() {
      VehicleCreationRequest data = new VehicleCreationRequest();

      assertThrows(
          VehicleRegistrationException.class,
          () -> service.resolveVehiclePurchasePrice(data, null));
      assertThrows(
          VehicleRegistrationException.class, () -> service.resolveVehiclePurchasePrice(data, 0d));
    }
  }

  @Nested
  @DisplayName("resolveVehicleSalePrice(Double)")
  class ResolveVehicleSalePriceTests {

    @Test
    @DisplayName("Debe retornar 0 cuando salePrice es nulo")
    void shouldReturnZeroWhenNull() {
      assertEquals(0d, service.resolveVehicleSalePrice(null));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando salePrice es negativo")
    void shouldThrowWhenNegative() {
      assertThrows(VehicleRegistrationException.class, () -> service.resolveVehicleSalePrice(-1d));
    }

    @Test
    @DisplayName("Debe retornar el valor cuando salePrice es cero o positivo")
    void shouldReturnValueWhenValid() {
      assertEquals(0d, service.resolveVehicleSalePrice(0d));
      assertEquals(1200d, service.resolveVehicleSalePrice(1200d));
    }
  }
}
