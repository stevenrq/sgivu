package com.sgivu.purchasesale.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Person;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.User;
import com.sgivu.purchasesale.dto.VehicleCreationRequest;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import com.sgivu.purchasesale.enums.VehicleType;
import com.sgivu.purchasesale.mapper.PurchaseSaleMapper;
import com.sgivu.purchasesale.repository.PurchaseSaleRepository;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

public class PurchaseSaleServiceImplTest {

  @Mock private PurchaseSaleRepository purchaseSaleRepository;
  @Mock private PurchaseSaleMapper purchaseSaleMapper;
  @Mock private ClientServiceClient clientServiceClient;
  @Mock private VehicleServiceClient vehicleServiceClient;
  @Mock private UserServiceClient userServiceClient;

  @InjectMocks private PurchaseSaleServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("create(PurchaseSaleRequest)")
  class CreateTests {

    @Test
    @DisplayName("should register vehicle for purchase and save contract")
    void shouldRegisterVehicleForPurchaseAndSave() {
      Long clientId = 1L;
      Long userId = 2L;

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(clientId);
      request.setUserId(userId);
      request.setPurchasePrice(1000d);
      request.setSalePrice(1200d);
      request.setContractType(null); // debería tener como opción predeterminada COMPRA
      request.setContractStatus(null); // debería tener como opción predeterminada PENDIENTE
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      VehicleCreationRequest vehicleData = new VehicleCreationRequest();
      vehicleData.setVehicleType(VehicleType.CAR);
      vehicleData.setBrand("Brand");
      vehicleData.setModel("Model");
      vehicleData.setCapacity(4);
      vehicleData.setLine("Line");
      vehicleData.setPlate("ABC123");
      vehicleData.setMotorNumber("MOTOR");
      vehicleData.setSerialNumber("SERIAL");
      vehicleData.setChassisNumber("CHASSIS");
      vehicleData.setColor("Red");
      vehicleData.setCityRegistered("City");
      vehicleData.setYear(2020);
      vehicleData.setMileage(1000);
      vehicleData.setTransmission("AUTO");
      vehicleData.setPurchasePrice(1000d);
      vehicleData.setSalePrice(1200d);
      vehicleData.setBodyType("SEDAN");
      vehicleData.setFuelType("GAS");
      vehicleData.setNumberOfDoors(4);

      request.setVehicleData(vehicleData);

      Person person = new Person();
      person.setId(clientId);
      when(clientServiceClient.getPersonById(clientId)).thenReturn(person);
      when(userServiceClient.getUserById(userId))
          .thenReturn(new User(userId, null, null, null, null, null, null, null));

      Car createdCar = new Car();
      createdCar.setId(10L);
      when(vehicleServiceClient.createCar(any(Car.class))).thenReturn(createdCar);

      when(purchaseSaleRepository.findByVehicleId(10L)).thenReturn(Collections.emptyList());

      PurchaseSale mapped = new PurchaseSale();
      mapped.setPurchasePrice(1000d);
      mapped.setSalePrice(1200d);
      mapped.setContractType(ContractType.PURCHASE);
      mapped.setContractStatus(ContractStatus.PENDING);
      mapped.setPaymentLimitations("pl");
      mapped.setPaymentTerms("pt");
      mapped.setPaymentMethod(PaymentMethod.CASH);

      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);
      when(purchaseSaleRepository.save(mapped)).thenReturn(mapped);

      PurchaseSale result = service.create(request);

      assertNotNull(result);
      assertEquals(clientId, result.getClientId());
      assertEquals(userId, result.getUserId());
      assertEquals(10L, result.getVehicleId());
      verify(vehicleServiceClient).createCar(any(Car.class));
      verify(purchaseSaleRepository).save(mapped);
    }

    @Test
    @DisplayName("should resolve provided vehicleId and save purchase")
    void shouldResolveProvidedVehicleIdAndSave() {
      Long clientId = 1L;
      Long userId = 2L;
      Long vehicleId = 5L;

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(clientId);
      request.setUserId(userId);
      request.setVehicleId(vehicleId);
      request.setPurchasePrice(500d);
      request.setSalePrice(700d);
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      Person person = new Person();
      person.setId(clientId);
      when(clientServiceClient.getPersonById(clientId)).thenReturn(person);
      when(userServiceClient.getUserById(userId))
          .thenReturn(new User(userId, null, null, null, null, null, null, null));

      Car car = new Car();
      car.setId(vehicleId);
      when(vehicleServiceClient.getCarById(vehicleId)).thenReturn(car);

      when(purchaseSaleRepository.findByVehicleId(vehicleId)).thenReturn(Collections.emptyList());

      PurchaseSale mapped = new PurchaseSale();
      mapped.setPurchasePrice(500d);
      mapped.setSalePrice(700d);
      mapped.setContractType(ContractType.PURCHASE);
      mapped.setContractStatus(ContractStatus.PENDING);
      mapped.setPaymentLimitations("pl");
      mapped.setPaymentTerms("pt");
      mapped.setPaymentMethod(PaymentMethod.CASH);

      when(purchaseSaleMapper.toPurchaseSale(request)).thenReturn(mapped);
      when(purchaseSaleRepository.save(mapped)).thenReturn(mapped);

      PurchaseSale result = service.create(request);

      assertNotNull(result);
      assertEquals(clientId, result.getClientId());
      assertEquals(userId, result.getUserId());
      assertEquals(vehicleId, result.getVehicleId());
      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(purchaseSaleRepository).save(mapped);
    }

    @Test
    @DisplayName("should throw when creating purchase without vehicle data")
    void shouldThrowWhenPurchaseWithoutVehicleData() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(1L);
      request.setUserId(2L);
      request.setPurchasePrice(100d);
      request.setSalePrice(120d);
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      Person person = new Person();
      person.setId(1L);
      when(clientServiceClient.getPersonById(1L)).thenReturn(person);
      when(userServiceClient.getUserById(2L))
          .thenReturn(new User(2L, null, null, null, null, null, null, null));

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.create(request));
      assertEquals(
          "Debes proporcionar los datos del vehículo para registrar una compra.", ex.getMessage());
    }

    @Test
    @DisplayName("should throw when sale price is invalid")
    void shouldThrowWhenSalePriceInvalid() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(1L);
      request.setUserId(2L);
      request.setVehicleId(5L);
      request.setContractType(ContractType.SALE);
      request.setSalePrice(0d);
      request.setPurchasePrice(0d);
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      Person person = new Person();
      person.setId(1L);
      when(clientServiceClient.getPersonById(1L)).thenReturn(person);
      when(userServiceClient.getUserById(2L))
          .thenReturn(new User(2L, null, null, null, null, null, null, null));

      Car car = new Car();
      car.setId(5L);
      when(vehicleServiceClient.getCarById(5L)).thenReturn(car);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.create(request));
      assertEquals("El precio de venta debe ser mayor a cero.", ex.getMessage());
    }

    @Test
    @DisplayName("should throw when client not found in person nor company endpoints")
    void shouldThrowWhenClientNotFound() {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(99L);
      request.setUserId(2L);
      request.setPurchasePrice(100d);
      request.setSalePrice(120d);
      request.setVehicleId(5L);
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      when(clientServiceClient.getPersonById(99L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(99L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.create(request));
      assertTrue(ex.getMessage().contains("Cliente no encontrado con id: 99"));
      verify(clientServiceClient).getPersonById(99L);
      verify(clientServiceClient).getCompanyById(99L);
    }
  }

  @Nested
  @DisplayName("update(Long, PurchaseSaleRequest)")
  class UpdateTests {

    @Test
    @DisplayName("should update existing purchase and save")
    void shouldUpdateExistingPurchaseAndSave() {
      Long id = 1L;
      Long clientId = 10L;
      Long userId = 20L;
      Long vehicleId = 5L;

      PurchaseSale existing = new PurchaseSale();
      existing.setId(id);
      existing.setContractType(ContractType.PURCHASE);
      existing.setPurchasePrice(1000d);
      existing.setSalePrice(1100d);
      existing.setClientId(999L);
      existing.setUserId(999L);
      existing.setVehicleId(vehicleId);

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(clientId);
      request.setUserId(userId);
      request.setVehicleId(vehicleId);
      request.setPurchasePrice(1500d);
      request.setSalePrice(1700d);
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      Person person = new Person();
      person.setId(clientId);
      when(clientServiceClient.getPersonById(clientId)).thenReturn(person);
      when(userServiceClient.getUserById(userId))
          .thenReturn(new User(userId, null, null, null, null, null, null, null));

      Car car = new Car();
      car.setId(vehicleId);
      when(vehicleServiceClient.getCarById(vehicleId)).thenReturn(car);

      when(purchaseSaleRepository.findByVehicleId(vehicleId)).thenReturn(Collections.emptyList());
      when(purchaseSaleRepository.findById(id)).thenReturn(Optional.of(existing));

      // Simular la actualización de campos del mapper
      doAnswer(
              invocation -> {
                PurchaseSaleRequest r = invocation.getArgument(0);
                PurchaseSale p = invocation.getArgument(1);
                p.setPurchasePrice(r.getPurchasePrice());
                p.setSalePrice(r.getSalePrice());
                p.setPaymentLimitations(r.getPaymentLimitations());
                p.setPaymentTerms(r.getPaymentTerms());
                p.setPaymentMethod(r.getPaymentMethod());
                return null;
              })
          .when(purchaseSaleMapper)
          .updatePurchaseSaleFromRequest(any(PurchaseSaleRequest.class), any(PurchaseSale.class));

      when(purchaseSaleRepository.save(existing)).thenReturn(existing);

      Optional<PurchaseSale> result = service.update(id, request);

      assertTrue(result.isPresent());
      PurchaseSale saved = result.get();
      assertEquals(1500d, saved.getPurchasePrice());
      assertEquals(1700d, saved.getSalePrice());
      assertEquals(clientId, saved.getClientId());
      assertEquals(userId, saved.getUserId());
      assertEquals(vehicleId, saved.getVehicleId());
      verify(purchaseSaleRepository).save(existing);
    }

    @Test
    @DisplayName("should update sale and compute purchase price from latest purchase")
    void shouldUpdateSaleAndComputePurchasePriceFromLatestPurchase() {
      Long id = 2L;
      Long clientId = 11L;
      Long userId = 21L;
      Long vehicleId = 6L;

      PurchaseSale existing = new PurchaseSale();
      existing.setId(id);
      existing.setContractType(ContractType.SALE);
      existing.setPurchasePrice(0d);
      existing.setSalePrice(3000d);
      existing.setVehicleId(vehicleId);

      PurchaseSale latestPurchase = new PurchaseSale();
      latestPurchase.setId(100L);
      latestPurchase.setContractType(ContractType.PURCHASE);
      latestPurchase.setContractStatus(ContractStatus.ACTIVE);
      latestPurchase.setPurchasePrice(2000d);
      latestPurchase.setUpdatedAt(LocalDateTime.now());

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(clientId);
      request.setUserId(userId);
      request.setVehicleId(vehicleId);
      request.setContractType(ContractType.SALE);
      request.setSalePrice(2500d); // valido > 0
      request.setPaymentLimitations("pl");
      request.setPaymentTerms("pt");
      request.setPaymentMethod(PaymentMethod.CASH);

      Person person = new Person();
      person.setId(clientId);
      when(clientServiceClient.getPersonById(clientId)).thenReturn(person);
      when(userServiceClient.getUserById(userId))
          .thenReturn(new User(userId, null, null, null, null, null, null, null));

      Car car = new Car();
      car.setId(vehicleId);
      when(vehicleServiceClient.getCarById(vehicleId)).thenReturn(car);

      when(purchaseSaleRepository.findByVehicleId(vehicleId)).thenReturn(List.of(latestPurchase));
      when(purchaseSaleRepository.findById(id)).thenReturn(Optional.of(existing));

      doAnswer(
              invocation -> {
                PurchaseSaleRequest r = invocation.getArgument(0);
                PurchaseSale p = invocation.getArgument(1);
                p.setPurchasePrice(r.getPurchasePrice());
                p.setSalePrice(r.getSalePrice());
                p.setPaymentLimitations(r.getPaymentLimitations());
                p.setPaymentTerms(r.getPaymentTerms());
                p.setPaymentMethod(r.getPaymentMethod());
                return null;
              })
          .when(purchaseSaleMapper)
          .updatePurchaseSaleFromRequest(any(PurchaseSaleRequest.class), any(PurchaseSale.class));

      when(purchaseSaleRepository.save(existing)).thenReturn(existing);

      Optional<PurchaseSale> result = service.update(id, request);

      assertTrue(result.isPresent());
      PurchaseSale saved = result.get();
      // purchasePrice debe venir de latestPurchase
      assertEquals(2000d, saved.getPurchasePrice());
      assertEquals(2500d, saved.getSalePrice());
      verify(purchaseSaleRepository).save(existing);
    }

    @Test
    @DisplayName("should return empty Optional if contract not found")
    void shouldReturnEmptyIfContractNotFound() {
      Long id = 99L;
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(1L);
      request.setUserId(2L);
      request.setVehicleId(5L);

      Person person = new Person();
      person.setId(1L);
      when(clientServiceClient.getPersonById(1L)).thenReturn(person);
      when(userServiceClient.getUserById(2L))
          .thenReturn(new User(2L, null, null, null, null, null, null, null));
      Car car = new Car();
      car.setId(5L);
      when(vehicleServiceClient.getCarById(5L)).thenReturn(car);

      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());
      when(purchaseSaleRepository.findById(id)).thenReturn(Optional.empty());
      Optional<PurchaseSale> result = service.update(id, request);
      assertFalse(result.isPresent());
      verify(purchaseSaleRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw when trying to change contract type")
    void shouldThrowWhenChangingContractType() {
      Long id = 3L;
      PurchaseSale existing = new PurchaseSale();
      existing.setId(id);
      existing.setContractType(ContractType.PURCHASE);
      existing.setPurchasePrice(100d);

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setClientId(1L);
      request.setUserId(2L);
      request.setVehicleId(5L);

      Person person = new Person();
      person.setId(1L);
      when(clientServiceClient.getPersonById(1L)).thenReturn(person);
      when(userServiceClient.getUserById(2L))
          .thenReturn(new User(2L, null, null, null, null, null, null, null));
      Car car = new Car();
      car.setId(5L);
      when(vehicleServiceClient.getCarById(5L)).thenReturn(car);
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());

      when(purchaseSaleRepository.findById(id)).thenReturn(Optional.of(existing));
      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.update(id, request));
      assertEquals("No es posible cambiar el tipo de contrato una vez creado.", ex.getMessage());
    }

    @Test
    @DisplayName("should propagate exception when save fails")
    void shouldPropagateExceptionWhenSaveFails() {
      Long id = 4L;
      PurchaseSale existing = new PurchaseSale();
      existing.setId(id);
      existing.setContractType(ContractType.PURCHASE);
      existing.setPurchasePrice(100d);

      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setClientId(1L);
      request.setUserId(2L);
      request.setVehicleId(5L);

      Person person = new Person();
      person.setId(1L);
      when(clientServiceClient.getPersonById(1L)).thenReturn(person);
      when(userServiceClient.getUserById(2L))
          .thenReturn(new User(2L, null, null, null, null, null, null, null));

      Car car = new Car();
      car.setId(5L);
      when(vehicleServiceClient.getCarById(5L)).thenReturn(car);

      when(purchaseSaleRepository.findById(id)).thenReturn(Optional.of(existing));
      when(purchaseSaleRepository.findByVehicleId(5L)).thenReturn(Collections.emptyList());
      doNothing().when(purchaseSaleMapper).updatePurchaseSaleFromRequest(any(), any());
      when(purchaseSaleRepository.save(existing)).thenThrow(new RuntimeException("DB error"));

      assertThrows(RuntimeException.class, () -> service.update(id, request));
      verify(purchaseSaleRepository).save(existing);
    }
  }

  @Nested
  @DisplayName("resolveVehicleReference(ContractType, PurchaseSaleRequest)")
  class ResolveVehicleReferenceTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "resolveVehicleReference", ContractType.class, PurchaseSaleRequest.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("purchase with provided vehicleId resolves via vehicle lookup")
    void purchaseWithProvidedVehicleIdResolves() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(5L);

      Car car = new Car();
      car.setId(5L);
      when(vehicleServiceClient.getCarById(5L)).thenReturn(car);

      Long result = (Long) method.invoke(service, ContractType.PURCHASE, request);
      assertEquals(5L, result);
      verify(vehicleServiceClient).getCarById(5L);
    }

    @Test
    @DisplayName("purchase without vehicleId registers a new car and sets vehicleId")
    void purchaseRegistersNewCar() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setVehicleType(VehicleType.CAR);
      data.setBrand("B");
      data.setModel("M");
      data.setCapacity(4);
      data.setLine("L");
      data.setPlate("P");
      data.setMotorNumber("MN");
      data.setSerialNumber("SN");
      data.setChassisNumber("CN");
      data.setColor("C");
      data.setCityRegistered("City");
      data.setYear(2020);
      data.setMileage(10);
      data.setTransmission("T");
      data.setPurchasePrice(1000d);
      data.setSalePrice(1200d);
      data.setBodyType("BT");
      data.setFuelType("FT");
      data.setNumberOfDoors(4);
      request.setVehicleData(data);

      Car created = new Car();
      created.setId(11L);
      when(vehicleServiceClient.createCar(any(Car.class))).thenReturn(created);

      Long result = (Long) method.invoke(service, ContractType.PURCHASE, request);
      assertEquals(11L, result);
      assertEquals(11L, request.getVehicleId());
      verify(vehicleServiceClient).createCar(any(Car.class));
    }

    @Test
    @DisplayName("purchase without vehicleId registers a new motorcycle when type is MOTORCYCLE")
    void purchaseRegistersNewMotorcycle() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setVehicleType(VehicleType.MOTORCYCLE);
      data.setBrand("B");
      data.setModel("M");
      data.setCapacity(2);
      data.setLine("L");
      data.setPlate("P");
      data.setMotorNumber("MN");
      data.setSerialNumber("SN");
      data.setChassisNumber("CN");
      data.setColor("C");
      data.setCityRegistered("City");
      data.setYear(2020);
      data.setMileage(10);
      data.setTransmission("T");
      data.setPurchasePrice(500d);
      data.setSalePrice(700d);
      data.setMotorcycleType("Sport");
      request.setVehicleData(data);

      com.sgivu.purchasesale.dto.Motorcycle created = new com.sgivu.purchasesale.dto.Motorcycle();
      created.setId(12L);
      when(vehicleServiceClient.createMotorcycle(any())).thenReturn(created);

      Long result = (Long) method.invoke(service, ContractType.PURCHASE, request);
      assertEquals(12L, result);
      assertEquals(12L, request.getVehicleId());
      verify(vehicleServiceClient).createMotorcycle(any());
    }

    @Test
    @DisplayName("sale without vehicleId throws exception")
    void saleWithoutVehicleIdThrows() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();

      try {
        method.invoke(service, ContractType.SALE, request);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(
            "Debes seleccionar el vehículo asociado al contrato de venta.",
            ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("sale with vehicleData present throws exception")
    void saleWithVehicleDataThrows() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(5L);
      request.setVehicleData(new VehicleCreationRequest());

      // Invocar y esperar IllegalArgumentException
      try {
        method.invoke(service, ContractType.SALE, request);
        fail("Expected IllegalArgumentException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(
            "Los datos detallados del vehículo solo deben enviarse para contratos de compra.",
            ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("sale with vehicleId resolves via vehicle lookup")
    void saleWithVehicleIdResolves() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setVehicleId(7L);

      Car car = new Car();
      car.setId(7L);
      when(vehicleServiceClient.getCarById(7L)).thenReturn(car);

      Long result = (Long) method.invoke(service, ContractType.SALE, request);
      assertEquals(7L, result);
      verify(vehicleServiceClient).getCarById(7L);
    }
  }

  @Nested
  @DisplayName("registerVehicleForPurchase(PurchaseSaleRequest)")
  class RegisterVehicleForPurchaseTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "registerVehicleForPurchase", PurchaseSaleRequest.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("should throw when vehicleData is null")
    void shouldThrowWhenVehicleDataIsNull() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();

      try {
        method.invoke(service, request);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(
            "Debes proporcionar los datos del vehículo para registrar una compra.",
            ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("should throw when vehicleType not specified")
    void shouldThrowWhenVehicleTypeNotSpecified() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      // no se ha establecido vehicleType
      request.setVehicleData(data);

      try {
        method.invoke(service, request);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(
            "Debes especificar si se trata de un automóvil o una motocicleta.",
            ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("should register car when data is valid")
    void shouldRegisterCarWhenDataIsValid() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
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
      request.setVehicleData(data);

      doAnswer(
              invocation -> {
                Car arg = invocation.getArgument(0);
                arg.setId(21L);
                return arg;
              })
          .when(vehicleServiceClient)
          .createCar(any(Car.class));

      Long result = (Long) method.invoke(service, request);
      // Asegurar de que el servicio haya intentado crear un automóvil con los atributos esperados
      verify(vehicleServiceClient).createCar(any(Car.class));
      verify(vehicleServiceClient)
          .createCar(argThat(c -> c.getBrand().equals("Brand") && c.getBodyType().equals("SEDAN")));
      // Asegurar que el id devuelto se use en una variable local y validar la creación
      assertEquals(21L, result);
    }

    @Test
    @DisplayName("should throw when car bodyType missing")
    void shouldThrowWhenCarBodyTypeMissing() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setVehicleType(VehicleType.CAR);
      data.setBrand("B");
      data.setModel("M");
      data.setCapacity(2);
      data.setLine("L");
      data.setPlate("P");
      data.setMotorNumber("MN");
      data.setSerialNumber("SN");
      data.setChassisNumber("CN");
      data.setColor("C");
      data.setCityRegistered("City");
      data.setYear(2020);
      data.setMileage(10);
      data.setTransmission("T");
      data.setPurchasePrice(500d);
      data.setSalePrice(700d);
      // bodyType no establecido
      data.setFuelType("GAS");
      data.setNumberOfDoors(2);
      request.setVehicleData(data);

      try {
        method.invoke(service, request);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("La carrocería del automóvil es obligatoria.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("should register motorcycle when data is valid")
    void shouldRegisterMotorcycleWhenDataIsValid() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
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
      request.setVehicleData(data);

      doAnswer(
              invocation -> {
                com.sgivu.purchasesale.dto.Motorcycle arg = invocation.getArgument(0);
                arg.setId(22L);
                return arg;
              })
          .when(vehicleServiceClient)
          .createMotorcycle(any());

      Long result = (Long) method.invoke(service, request);
      verify(vehicleServiceClient).createMotorcycle(any());
      verify(vehicleServiceClient)
          .createMotorcycle(
              argThat(m -> m.getBrand().equals("Brand") && m.getMotorcycleType().equals("Sport")));
      // Asegurar que el id devuelto se use en una variable local y validar la creación
      assertEquals(22L, result);
    }

    @Test
    @DisplayName("should throw when motorcycle type missing")
    void shouldThrowWhenMotorcycleTypeMissing() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setVehicleType(VehicleType.MOTORCYCLE);
      // no se ha establecido motorcycleType
      data.setBrand("B");
      data.setModel("M");
      data.setCapacity(2);
      data.setLine("L");
      data.setPlate("P");
      data.setMotorNumber("MN");
      data.setSerialNumber("SN");
      data.setChassisNumber("CN");
      data.setColor("C");
      data.setCityRegistered("City");
      data.setYear(2020);
      data.setMileage(10);
      data.setTransmission("T");
      data.setPurchasePrice(500d);
      data.setSalePrice(700d);
      request.setVehicleData(data);

      try {
        method.invoke(service, request);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("Debes indicar el tipo de motocicleta.", ite.getCause().getMessage());
      }
    }
  }

  @Nested
  @DisplayName("applyBusinessRules(...)")
  class ApplyBusinessRulesTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "applyBusinessRules",
              ContractType.class,
              PurchaseSaleRequest.class,
              List.class,
              Long.class,
              Long.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("purchase sets salePrice from vehicleData when present and no conflicts")
    void purchaseSetsSalePriceFromVehicleData() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(555d);
      request.setVehicleData(data);

      List<PurchaseSale> contracts = Collections.emptyList();

      method.invoke(service, ContractType.PURCHASE, request, contracts, null, 1L);

      assertEquals(555d, request.getSalePrice());
    }

    @Test
    @DisplayName("purchase throws when there is an active or pending purchase")
    void purchaseThrowsWhenActiveOrPending() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(100d);
      request.setVehicleData(data);

      PurchaseSale conflicting = new PurchaseSale();
      conflicting.setId(10L);
      conflicting.setContractType(ContractType.PURCHASE);
      conflicting.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(conflicting);

      try {
        method.invoke(service, ContractType.PURCHASE, request, contracts, null, 1L);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("ya tiene una compra registrada"));
      }
    }

    @Test
    @DisplayName("purchase ignores excluded contract id when checking conflicts")
    void purchaseIgnoresExcludedContractId() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(100d);
      request.setVehicleData(data);

      PurchaseSale conflicting = new PurchaseSale();
      conflicting.setId(10L);
      conflicting.setContractType(ContractType.PURCHASE);
      conflicting.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(conflicting);

      // id excluido coincide con conflicting.id -> no debe lanzar excepción
      method.invoke(service, ContractType.PURCHASE, request, contracts, 10L, 1L);
    }

    @Test
    @DisplayName("sale throws when salePrice missing or <= 0")
    void saleThrowsWhenInvalidSalePrice() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(0d);

      try {
        method.invoke(service, ContractType.SALE, request, Collections.emptyList(), null, 2L);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El precio de venta debe ser mayor a cero.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("sale throws if no available purchase stock")
    void saleThrowsWhenNoAvailableStock() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(2000d);

      // los contratos contienen compras pero ninguna en estado ACTIVE/COMPLETED
      PurchaseSale past = new PurchaseSale();
      past.setContractType(ContractType.PURCHASE);
      past.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(past);

      try {
        method.invoke(service, ContractType.SALE, request, contracts, null, 3L);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        // prepareSaleRequest -> findLatestPurchasePrice lanza cuando no hay compras válidas
        assertTrue(
            ite.getCause()
                .getMessage()
                .contains("No se encontró una compra válida asociada al vehículo."));
      }
    }

    @Test
    @DisplayName("sale throws when there is a conflicting sale")
    void saleThrowsWhenConflictingSaleExists() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(2500d);

      PurchaseSale purchase = new PurchaseSale();
      purchase.setContractType(ContractType.PURCHASE);
      purchase.setContractStatus(ContractStatus.ACTIVE);
      purchase.setPurchasePrice(1800d);

      PurchaseSale otherSale = new PurchaseSale();
      otherSale.setId(99L);
      otherSale.setContractType(ContractType.SALE);
      otherSale.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(purchase, otherSale);

      try {
        method.invoke(service, ContractType.SALE, request, contracts, null, 4L);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("ya cuenta con una venta registrada"));
      }
    }

    @Test
    @DisplayName("sale sets purchasePrice from latest purchase when available")
    void saleSetsPurchasePriceFromLatestPurchase() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setContractStatus(ContractStatus.PENDING);
      request.setSalePrice(3000d);

      PurchaseSale purchase1 = new PurchaseSale();
      purchase1.setContractType(ContractType.PURCHASE);
      purchase1.setContractStatus(ContractStatus.COMPLETED);
      purchase1.setPurchasePrice(1500d);
      purchase1.setUpdatedAt(LocalDateTime.now().minusDays(2));

      PurchaseSale purchase2 = new PurchaseSale();
      purchase2.setContractType(ContractType.PURCHASE);
      purchase2.setContractStatus(ContractStatus.ACTIVE);
      purchase2.setPurchasePrice(2000d);
      purchase2.setUpdatedAt(LocalDateTime.now());

      List<PurchaseSale> contracts = List.of(purchase1, purchase2);

      method.invoke(service, ContractType.SALE, request, contracts, null, 5L);

      assertEquals(2000d, request.getPurchasePrice());
    }
  }

  @Nested
  @DisplayName("preparePurchaseRequest(PurchaseSaleRequest)")
  class PreparePurchaseRequestTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "preparePurchaseRequest", PurchaseSaleRequest.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("uses vehicleData.salePrice when present")
    void usesVehicleDataSalePrice() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(1550d);
      request.setVehicleData(data);
      request.setSalePrice(1000d);

      method.invoke(service, request);
      assertEquals(1550d, request.getSalePrice());
    }

    @Test
    @DisplayName("retains request salePrice when vehicleData is null")
    void retainsRequestSalePriceWhenVehicleDataNull() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setSalePrice(1200d);

      method.invoke(service, request);
      assertEquals(1200d, request.getSalePrice());
    }

    @Test
    @DisplayName("defaults to zero when both vehicleData and salePrice absent")
    void defaultsToZeroWhenMissing() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();

      method.invoke(service, request);
      assertEquals(0d, request.getSalePrice());
    }

    @Test
    @DisplayName("uses request salePrice when vehicleData.salePrice is null")
    void usesRequestSalePriceWhenVehicleDataSalePriceNull() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      VehicleCreationRequest data = new VehicleCreationRequest();
      data.setSalePrice(null);
      request.setVehicleData(data);
      request.setSalePrice(900d);

      method.invoke(service, request);
      assertEquals(900d, request.getSalePrice());
    }
  }

  @Nested
  @DisplayName("prepareSaleRequest(PurchaseSaleRequest, List<PurchaseSale>)")
  class PrepareSaleRequestTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "prepareSaleRequest", PurchaseSaleRequest.class, List.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when salePrice is null")
    void throwsWhenSalePriceIsNull() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setSalePrice(null);

      try {
        method.invoke(service, request, Collections.emptyList());
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El precio de venta debe ser mayor a cero.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("throws when salePrice is zero or negative")
    void throwsWhenSalePriceInvalid() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setSalePrice(0d);

      try {
        method.invoke(service, request, Collections.emptyList());
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El precio de venta debe ser mayor a cero.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("sets purchasePrice from latest purchase when available")
    void setsPurchasePriceFromLatestPurchase() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setSalePrice(2500d);

      PurchaseSale old = new PurchaseSale();
      old.setContractType(ContractType.PURCHASE);
      old.setContractStatus(ContractStatus.COMPLETED);
      old.setPurchasePrice(1400d);
      old.setUpdatedAt(LocalDateTime.now().minusDays(3));

      PurchaseSale recent = new PurchaseSale();
      recent.setContractType(ContractType.PURCHASE);
      recent.setContractStatus(ContractStatus.ACTIVE);
      recent.setPurchasePrice(1800d);
      recent.setUpdatedAt(LocalDateTime.now());

      List<PurchaseSale> contracts = List.of(old, recent);

      method.invoke(service, request, contracts);

      assertEquals(1800d, request.getPurchasePrice());
    }

    @Test
    @DisplayName("uses fallback purchasePrice from request when no purchases available")
    void usesFallbackPurchasePriceWhenNoPurchases() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setSalePrice(2000d);
      request.setPurchasePrice(1700d);

      method.invoke(service, request, Collections.emptyList());

      assertEquals(1700d, request.getPurchasePrice());
    }

    @Test
    @DisplayName("throws when no purchases and fallback missing or invalid")
    void throwsWhenNoPurchasesAndNoFallback() throws Exception {
      PurchaseSaleRequest request = new PurchaseSaleRequest();
      request.setContractType(ContractType.SALE);
      request.setSalePrice(2000d);
      request.setPurchasePrice(null);

      try {
        method.invoke(service, request, Collections.emptyList());
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(
            ite.getCause()
                .getMessage()
                .contains("No se encontró una compra válida asociada al vehículo."));
      }
    }
  }

  @Nested
  @DisplayName("findLatestPurchasePrice(List<PurchaseSale>, Double)")
  class FindLatestPurchasePriceTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "findLatestPurchasePrice", List.class, Double.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("returns latest purchase price among ACTIVE/COMPLETED purchases")
    void returnsLatestPurchasePriceFromActiveOrCompleted() throws Exception {
      PurchaseSale old = new PurchaseSale();
      old.setContractType(ContractType.PURCHASE);
      old.setContractStatus(ContractStatus.COMPLETED);
      old.setPurchasePrice(1500d);
      old.setUpdatedAt(LocalDateTime.now().minusDays(5));

      PurchaseSale recent = new PurchaseSale();
      recent.setContractType(ContractType.PURCHASE);
      recent.setContractStatus(ContractStatus.ACTIVE);
      recent.setPurchasePrice(2000d);
      recent.setUpdatedAt(LocalDateTime.now());

      // Una compra PENDING con updatedAt posterior debe ser ignorada
      PurchaseSale pending = new PurchaseSale();
      pending.setContractType(ContractType.PURCHASE);
      pending.setContractStatus(ContractStatus.PENDING);
      pending.setPurchasePrice(3000d);
      pending.setUpdatedAt(LocalDateTime.now().plusDays(1));

      List<PurchaseSale> contracts = List.of(old, recent, pending);

      Object res = method.invoke(service, contracts, null);
      assertEquals(2000d, ((Double) res));
    }

    @Test
    @DisplayName("returns fallback when no valid purchases and fallback > 0")
    void returnsFallbackWhenNoValidPurchases() throws Exception {
      Double fallback = 1800d;
      Object res = method.invoke(service, Collections.emptyList(), fallback);
      assertEquals(1800d, ((Double) res));
    }

    @Test
    @DisplayName("throws when no valid purchases and fallback missing or invalid")
    void throwsWhenNoValidPurchasesAndNoFallback() throws Exception {
      try {
        method.invoke(service, Collections.emptyList(), null);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(
            ite.getCause()
                .getMessage()
                .contains("No se encontró una compra válida asociada al vehículo."));
      }

      try {
        method.invoke(service, Collections.emptyList(), 0d);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(
            ite.getCause()
                .getMessage()
                .contains("No se encontró una compra válida asociada al vehículo."));
      }
    }
  }

  @Nested
  @DisplayName("ensureNoActivePurchase(List<PurchaseSale>, Long, Long)")
  class EnsureNoActivePurchaseTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "ensureNoActivePurchase", List.class, Long.class, Long.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when there is a PENDING purchase")
    void throwsWhenPendingPurchaseExists() throws Exception {
      PurchaseSale pending = new PurchaseSale();
      pending.setId(10L);
      pending.setContractType(ContractType.PURCHASE);
      pending.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(pending);

      try {
        method.invoke(service, contracts, null, 5L);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("ya tiene una compra registrada"));
        assertTrue(ite.getCause().getMessage().contains("5"));
      }
    }

    @Test
    @DisplayName("throws when there is an ACTIVE purchase")
    void throwsWhenActivePurchaseExists() throws Exception {
      PurchaseSale active = new PurchaseSale();
      active.setId(11L);
      active.setContractType(ContractType.PURCHASE);
      active.setContractStatus(ContractStatus.ACTIVE);

      List<PurchaseSale> contracts = List.of(active);

      try {
        method.invoke(service, contracts, null, 6L);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("ya tiene una compra registrada"));
        assertTrue(ite.getCause().getMessage().contains("6"));
      }
    }

    @Test
    @DisplayName("ignores purchase matching excludedContractId")
    void ignoresExcludedContractId() throws Exception {
      PurchaseSale pending = new PurchaseSale();
      pending.setId(20L);
      pending.setContractType(ContractType.PURCHASE);
      pending.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(pending);

      // id excluido coincide con pending.id -> no debe lanzar excepción
      method.invoke(service, contracts, 20L, 7L);
    }

    @Test
    @DisplayName("does not throw when only COMPLETED purchases exist")
    void doesNotThrowForCompletedPurchases() throws Exception {
      PurchaseSale completed = new PurchaseSale();
      completed.setId(30L);
      completed.setContractType(ContractType.PURCHASE);
      completed.setContractStatus(ContractStatus.COMPLETED);

      List<PurchaseSale> contracts = List.of(completed);

      method.invoke(service, contracts, null, 8L);
    }
  }

  @Nested
  @DisplayName("ensureSalePrerequisites(List<PurchaseSale>, Long, Long, ContractStatus)")
  class EnsureSalePrerequisitesTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "ensureSalePrerequisites", List.class, Long.class, Long.class, ContractStatus.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when no available ACTIVE/COMPLETED purchase exists for validated statuses")
    void throwsWhenNoAvailablePurchase() throws Exception {
      PurchaseSale onlyPending = new PurchaseSale();
      onlyPending.setContractType(ContractType.PURCHASE);
      onlyPending.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(onlyPending);

      try {
        method.invoke(service, contracts, null, 42L, ContractStatus.PENDING);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(
            ite.getCause()
                .getMessage()
                .contains("no cuenta con una compra activa o completada registrada"));
        assertTrue(ite.getCause().getMessage().contains("42"));
      }
    }

    @Test
    @DisplayName("does not throw when an ACTIVE purchase is present")
    void doesNotThrowWhenActivePurchaseExists() throws Exception {
      PurchaseSale active = new PurchaseSale();
      active.setContractType(ContractType.PURCHASE);
      active.setContractStatus(ContractStatus.ACTIVE);

      List<PurchaseSale> contracts = List.of(active);

      // targetStatus activa la validación de disponibilidad; no debe lanzar excepción
      method.invoke(service, contracts, null, 43L, ContractStatus.PENDING);
    }

    @Test
    @DisplayName("throws when a conflicting sale exists")
    void throwsWhenConflictingSaleExists() throws Exception {
      PurchaseSale purchase = new PurchaseSale();
      purchase.setContractType(ContractType.PURCHASE);
      purchase.setContractStatus(ContractStatus.ACTIVE);

      PurchaseSale otherSale = new PurchaseSale();
      otherSale.setId(99L);
      otherSale.setContractType(ContractType.SALE);
      otherSale.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(purchase, otherSale);

      try {
        // disponibilidad satisfecha por compra, pero hay una venta en conflicto
        method.invoke(service, contracts, null, 50L, ContractStatus.PENDING);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("ya cuenta con una venta registrada"));
        assertTrue(ite.getCause().getMessage().contains("50"));
      }
    }

    @Test
    @DisplayName("ignores conflicting sale that matches excludedContractId")
    void ignoresExcludedConflictingSale() throws Exception {
      PurchaseSale purchase = new PurchaseSale();
      purchase.setContractType(ContractType.PURCHASE);
      purchase.setContractStatus(ContractStatus.ACTIVE);

      PurchaseSale otherSale = new PurchaseSale();
      otherSale.setId(123L);
      otherSale.setContractType(ContractType.SALE);
      otherSale.setContractStatus(ContractStatus.PENDING);

      List<PurchaseSale> contracts = List.of(purchase, otherSale);

      // id excluido coincide con la venta -> no debe lanzar excepción
      method.invoke(service, contracts, 123L, 51L, ContractStatus.PENDING);
    }

    @Test
    @DisplayName(
        "validates conflicting sales even when availability validation is skipped (targetStatus"
            + " null)")
    void validatesSalesWhenAvailabilitySkipped() throws Exception {
      PurchaseSale onlySale = new PurchaseSale();
      onlySale.setId(77L);
      onlySale.setContractType(ContractType.SALE);
      onlySale.setContractStatus(ContractStatus.ACTIVE);

      List<PurchaseSale> contracts = List.of(onlySale);

      try {
        // targetStatus null => la verificación de disponibilidad se omite, pero el conflicto de
        // venta aún debe ser
        // detectado
        method.invoke(service, contracts, null, 52L, (ContractStatus) null);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(ite.getCause().getMessage().contains("ya cuenta con una venta registrada"));
      }
    }
  }

  @Nested
  @DisplayName("resolveClientId(Long)")
  class ResolveClientIdTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method = PurchaseSaleServiceImpl.class.getDeclaredMethod("resolveClientId", Long.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when clientId is null")
    void throwsWhenClientIdNull() throws Exception {
      try {
        method.invoke(service, (Long) null);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El ID del cliente debe ser proporcionado.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns person id when person endpoint finds the client")
    void returnsPersonIdWhenPersonFound() throws Exception {
      Long clientId = 5L;
      com.sgivu.purchasesale.dto.Person p = new com.sgivu.purchasesale.dto.Person();
      p.setId(clientId);
      when(clientServiceClient.getPersonById(clientId)).thenReturn(p);

      Object res = method.invoke(service, clientId);
      assertEquals(clientId, ((Long) res));
      verify(clientServiceClient).getPersonById(clientId);
      verify(clientServiceClient, never()).getCompanyById(any());
    }

    @Test
    @DisplayName("returns company id when person not found but company exists")
    void returnsCompanyIdWhenPersonNotFound() throws Exception {
      Long clientId = 6L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));

      com.sgivu.purchasesale.dto.Company c = new com.sgivu.purchasesale.dto.Company();
      c.setId(clientId);
      when(clientServiceClient.getCompanyById(clientId)).thenReturn(c);

      Object res = method.invoke(service, clientId);
      assertEquals(clientId, ((Long) res));
      verify(clientServiceClient).getPersonById(clientId);
      verify(clientServiceClient).getCompanyById(clientId);
    }

    @Test
    @DisplayName("throws when neither person nor company endpoints find the client")
    void throwsWhenPersonAndCompanyNotFound() throws Exception {
      Long clientId = 99L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(clientId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));

      try {
        method.invoke(service, clientId);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(
            ite.getCause().getMessage().contains("Cliente no encontrado con id: " + clientId));
      }

      verify(clientServiceClient).getPersonById(clientId);
      verify(clientServiceClient).getCompanyById(clientId);
    }

    @Test
    @DisplayName("rethrows non-404 HttpClientErrorException from person endpoint")
    void rethrowsNon404FromPerson() throws Exception {
      Long clientId = 77L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

      try {
        method.invoke(service, clientId);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(
            ite.getCause() instanceof org.springframework.web.client.HttpClientErrorException);
        assertEquals(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            ((org.springframework.web.client.HttpClientErrorException) ite.getCause())
                .getStatusCode());
      }
    }

    @Test
    @DisplayName("rethrows non-404 HttpClientErrorException from company endpoint")
    void rethrowsNon404FromCompany() throws Exception {
      Long clientId = 88L;
      when(clientServiceClient.getPersonById(clientId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(clientId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

      try {
        method.invoke(service, clientId);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(
            ite.getCause() instanceof org.springframework.web.client.HttpClientErrorException);
        assertEquals(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            ((org.springframework.web.client.HttpClientErrorException) ite.getCause())
                .getStatusCode());
      }
    }
  }

  @Nested
  @DisplayName("resolveVehicleId(Long)")
  class ResolveVehicleIdTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method = PurchaseSaleServiceImpl.class.getDeclaredMethod("resolveVehicleId", Long.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when vehicleId is null")
    void throwsWhenVehicleIdNull() throws Exception {
      try {
        method.invoke(service, (Long) null);
        fail("Expected InvocationTargetException");
      } catch (InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El ID del vehículo debe ser proporcionado.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns car id when car endpoint finds the vehicle")
    void returnsCarIdWhenCarFound() throws Exception {
      Long vehicleId = 10L;
      Car car = new Car();
      car.setId(vehicleId);
      when(vehicleServiceClient.getCarById(vehicleId)).thenReturn(car);

      Object res = method.invoke(service, vehicleId);
      assertEquals(vehicleId, ((Long) res));
      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(vehicleServiceClient, never()).getMotorcycleById(any());
    }

    @Test
    @DisplayName("returns motorcycle id when car returns 404 and motorcycle exists")
    void returnsMotorcycleWhenCar404() throws Exception {
      Long vehicleId = 11L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));

      com.sgivu.purchasesale.dto.Motorcycle m = new com.sgivu.purchasesale.dto.Motorcycle();
      m.setId(vehicleId);
      when(vehicleServiceClient.getMotorcycleById(vehicleId)).thenReturn(m);

      Object res = method.invoke(service, vehicleId);
      assertEquals(vehicleId, ((Long) res));
      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(vehicleServiceClient).getMotorcycleById(vehicleId);
    }

    @Test
    @DisplayName("throws when neither car nor motorcycle endpoints find the vehicle")
    void throwsWhenCarAndMotorcycleNotFound() throws Exception {
      Long vehicleId = 99L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));
      when(vehicleServiceClient.getMotorcycleById(vehicleId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));

      try {
        method.invoke(service, vehicleId);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertTrue(
            ite.getCause().getMessage().contains("Vehículo no encontrado con id: " + vehicleId));
      }

      verify(vehicleServiceClient).getCarById(vehicleId);
      verify(vehicleServiceClient).getMotorcycleById(vehicleId);
    }

    @Test
    @DisplayName("rethrows non-404 HttpClientErrorException from car endpoint")
    void rethrowsNon404FromCar() throws Exception {
      Long vehicleId = 77L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

      try {
        method.invoke(service, vehicleId);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(
            ite.getCause() instanceof org.springframework.web.client.HttpClientErrorException);
        assertEquals(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            ((org.springframework.web.client.HttpClientErrorException) ite.getCause())
                .getStatusCode());
      }
    }

    @Test
    @DisplayName("rethrows non-404 HttpClientErrorException from motorcycle endpoint")
    void rethrowsNon404FromMotorcycle() throws Exception {
      Long vehicleId = 88L;
      when(vehicleServiceClient.getCarById(vehicleId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.NOT_FOUND));
      when(vehicleServiceClient.getMotorcycleById(vehicleId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

      try {
        method.invoke(service, vehicleId);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(
            ite.getCause() instanceof org.springframework.web.client.HttpClientErrorException);
        assertEquals(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            ((org.springframework.web.client.HttpClientErrorException) ite.getCause())
                .getStatusCode());
      }
    }
  }

  @Nested
  @DisplayName("resolveUserId(Long)")
  class ResolveUserIdTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method = PurchaseSaleServiceImpl.class.getDeclaredMethod("resolveUserId", Long.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when userId is null")
    void throwsWhenUserIdNull() throws Exception {
      try {
        method.invoke(service, (Long) null);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El ID del usuario debe ser proporcionado.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns user id when user exists")
    void returnsUserIdWhenUserExists() throws Exception {
      Long userId = 7L;
      when(userServiceClient.getUserById(userId))
          .thenReturn(
              new com.sgivu.purchasesale.dto.User(
                  userId, null, null, null, null, null, null, null));

      Object res = method.invoke(service, userId);
      assertEquals(userId, ((Long) res));
      verify(userServiceClient).getUserById(userId);
    }

    @Test
    @DisplayName("rethrows exceptions from user service client")
    void rethrowsExceptionFromUserService() throws Exception {
      Long userId = 88L;
      when(userServiceClient.getUserById(userId))
          .thenThrow(
              new org.springframework.web.client.HttpClientErrorException(
                  org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

      try {
        method.invoke(service, userId);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(
            ite.getCause() instanceof org.springframework.web.client.HttpClientErrorException);
        assertEquals(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            ((org.springframework.web.client.HttpClientErrorException) ite.getCause())
                .getStatusCode());
      }
    }
  }

  @Nested
  @DisplayName("requireContractId(Long)")
  class RequireContractIdTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method = PurchaseSaleServiceImpl.class.getDeclaredMethod("requireContractId", Long.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when contractId is null")
    void throwsWhenContractIdNull() throws Exception {
      try {
        method.invoke(service, (Long) null);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El ID del contrato debe ser proporcionado.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns long value when contractId provided")
    void returnsValueWhenContractIdProvided() throws Exception {
      Long contractId = 42L;
      Object res = method.invoke(service, contractId);
      assertEquals(contractId, ((Long) res));
    }
  }

  @Nested
  @DisplayName("requirePageable(Pageable)")
  class RequirePageableTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "requirePageable", org.springframework.data.domain.Pageable.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when pageable is null")
    void throwsWhenPageableNull() throws Exception {
      try {
        method.invoke(service, (Object) null);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(
            ite.getCause() instanceof NullPointerException
                || ite.getCause() instanceof IllegalArgumentException);
        assertEquals("La configuración de paginación es obligatoria.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns pageable when provided")
    void returnsPageableWhenProvided() throws Exception {
      org.springframework.data.domain.Pageable pageable =
          org.springframework.data.domain.PageRequest.of(0, 10);
      Object res = method.invoke(service, pageable);
      assertSame(pageable, res);
    }
  }

  @Nested
  @DisplayName("validatePurchasePrice(Double)")
  class ValidatePurchasePriceTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod("validatePurchasePrice", Double.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when purchasePrice is null")
    void throwsWhenPurchasePriceNull() throws Exception {
      try {
        method.invoke(service, (Double) null);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El precio de compra debe ser mayor a cero.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("throws when purchasePrice is zero or negative")
    void throwsWhenPurchasePriceInvalid() throws Exception {
      try {
        method.invoke(service, 0d);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El precio de compra debe ser mayor a cero.", ite.getCause().getMessage());
      }

      try {
        method.invoke(service, -10d);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El precio de compra debe ser mayor a cero.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("accepts positive purchasePrice")
    void acceptsPositivePurchasePrice() throws Exception {
      Object res = method.invoke(service, 1500d);
      assertNull(res); // el método devuelve void; la invocación devuelve null
    }
  }

  @Nested
  @DisplayName("requireText(String, String)")
  class RequireTextTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "requireText", String.class, String.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when value is null or empty")
    void throwsWhenValueNullOrEmpty() throws Exception {
      String msg = "Custom message";

      try {
        method.invoke(service, (String) null, msg);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(msg, ite.getCause().getMessage());
      }

      try {
        method.invoke(service, "", msg);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(msg, ite.getCause().getMessage());
      }

      try {
        method.invoke(service, "   ", msg);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(msg, ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns trimmed string when value has surrounding whitespace")
    void returnsTrimmedWhenWhitespace() throws Exception {
      Object res = method.invoke(service, "  abc  ", "irrelevant");
      assertEquals("abc", res);
    }

    @Test
    @DisplayName("returns same string when it has no extra whitespace")
    void returnsSameWhenNoWhitespace() throws Exception {
      Object res = method.invoke(service, "a", "irrelevant");
      assertEquals("a", res);
    }
  }

  @Nested
  @DisplayName("requirePositiveInteger(Integer, String)")
  class RequirePositiveIntegerTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "requirePositiveInteger", Integer.class, String.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when value is null")
    void throwsWhenValueNull() throws Exception {
      String msg = "Number required";
      try {
        method.invoke(service, (Integer) null, msg);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(msg, ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("throws when value is zero or negative")
    void throwsWhenZeroOrNegative() throws Exception {
      String msg = "Must be positive";

      try {
        method.invoke(service, 0, msg);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(msg, ite.getCause().getMessage());
      }

      try {
        method.invoke(service, -3, msg);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(msg, ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns value when positive")
    void returnsValueWhenPositive() throws Exception {
      Object res = method.invoke(service, 7, "irrelevant");
      assertEquals(7, ((Integer) res).intValue());
    }
  }

  @Nested
  @DisplayName("requireValidYear(Integer)")
  class RequireValidYearTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method = PurchaseSaleServiceImpl.class.getDeclaredMethod("requireValidYear", Integer.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when year is null or non-positive")
    void throwsWhenYearNullOrNonPositive() throws Exception {
      try {
        method.invoke(service, (Integer) null);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El año del vehículo es obligatorio.", ite.getCause().getMessage());
      }

      try {
        method.invoke(service, 0);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El año del vehículo es obligatorio.", ite.getCause().getMessage());
      }

      try {
        method.invoke(service, -5);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El año del vehículo es obligatorio.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("throws when year is below 1950 or above 2050")
    void throwsWhenYearOutOfRange() throws Exception {
      try {
        method.invoke(service, 1949);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(
            "El año del vehículo debe estar entre 1950 y 2050.", ite.getCause().getMessage());
      }

      try {
        method.invoke(service, 2051);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals(
            "El año del vehículo debe estar entre 1950 y 2050.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns the same value when year is valid")
    void returnsValueWhenYearValid() throws Exception {
      Object r1 = method.invoke(service, 1950);
      assertEquals(1950, ((Integer) r1).intValue());

      Object r2 = method.invoke(service, 2000);
      assertEquals(2000, ((Integer) r2).intValue());

      Object r3 = method.invoke(service, 2050);
      assertEquals(2050, ((Integer) r3).intValue());
    }
  }

  @Nested
  @DisplayName("requireNonNegativeInteger(Integer)")
  class RequireNonNegativeIntegerTests {

    private Method method;

    @BeforeEach
    void init() throws NoSuchMethodException {
      method =
          PurchaseSaleServiceImpl.class.getDeclaredMethod(
              "requireNonNegativeInteger", Integer.class);
      method.setAccessible(true);
    }

    @Test
    @DisplayName("throws when value is null")
    void throwsWhenValueNull() throws Exception {
      try {
        method.invoke(service, (Integer) null);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El kilometraje del vehículo es obligatorio.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("throws when value is negative")
    void throwsWhenNegative() throws Exception {
      try {
        method.invoke(service, -1);
        fail("Expected InvocationTargetException");
      } catch (java.lang.reflect.InvocationTargetException ite) {
        assertTrue(ite.getCause() instanceof IllegalArgumentException);
        assertEquals("El kilometraje del vehículo es obligatorio.", ite.getCause().getMessage());
      }
    }

    @Test
    @DisplayName("returns value when zero or positive")
    void returnsWhenZeroOrPositive() throws Exception {
      Object r0 = method.invoke(service, 0);
      assertEquals(0, ((Integer) r0).intValue());

      Object rPos = method.invoke(service, 15);
      assertEquals(15, ((Integer) rPos).intValue());
    }
  }
}
