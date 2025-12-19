package com.sgivu.purchasesale.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseSaleServiceImplTest {

  @Mock private PurchaseSaleRepository purchaseSaleRepository;
  @Mock private PurchaseSaleMapper purchaseSaleMapper;
  @Mock private ClientServiceClient clientServiceClient;
  @Mock private VehicleServiceClient vehicleServiceClient;
  @Mock private UserServiceClient userServiceClient;

  @InjectMocks private PurchaseSaleServiceImpl purchaseSaleService;

  @SuppressWarnings("null")
  @Test
  void createPurchase_registersVehicleAndSavesContractWithNormalizedFields() {
    PurchaseSaleRequest request = buildBaseRequest();
    request.setContractType(null);
    request.setContractStatus(null);
    request.setSalePrice(null);
    request.setVehicleId(null);
    request.setVehicleData(buildCarData());

    Car createdCar = new Car();
    createdCar.setId(55L);

    when(clientServiceClient.getPersonById(1L)).thenReturn(person(1L));
    when(userServiceClient.getUserById(2L)).thenReturn(user(2L));
    when(vehicleServiceClient.createCar(any(Car.class))).thenReturn(createdCar);
    when(purchaseSaleRepository.findByVehicleId(55L)).thenReturn(List.of());
    when(purchaseSaleMapper.toPurchaseSale(any(PurchaseSaleRequest.class)))
        .thenAnswer(invocation -> mapToEntity(invocation.getArgument(0)));
    when(purchaseSaleRepository.save(any(PurchaseSale.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PurchaseSale result = purchaseSaleService.create(request);

    ArgumentCaptor<Car> carCaptor = ArgumentCaptor.forClass(Car.class);
    verify(vehicleServiceClient).createCar(carCaptor.capture());
    Car sentCar = carCaptor.getValue();
    assertThat(sentCar.getPlate()).isEqualTo("ABC123");
    assertThat(sentCar.getStatus()).isEqualTo("AVAILABLE");
    assertThat(sentCar.getPurchasePrice()).isEqualTo(12000d);
    assertThat(sentCar.getSalePrice()).isEqualTo(18000d);

    assertThat(result.getVehicleId()).isEqualTo(55L);
    assertThat(result.getContractType()).isEqualTo(ContractType.PURCHASE);
    assertThat(result.getContractStatus()).isEqualTo(ContractStatus.PENDING);
    assertThat(result.getSalePrice()).isEqualTo(18000d);
    assertThat(result.getPurchasePrice()).isEqualTo(12000d);
  }

  @SuppressWarnings("null")
  @Test
  void createPurchase_rejectsWhenPendingOrActivePurchaseAlreadyExists() {
    PurchaseSaleRequest request = buildBaseRequest();
    request.setVehicleId(77L);
    request.setContractType(ContractType.PURCHASE);

    PurchaseSale existing =
        buildContract(
            10L, ContractType.PURCHASE, ContractStatus.ACTIVE, 9000d, 0d, 77L, LocalDateTime.now());

    Car car = new Car();
    car.setId(77L);

    when(clientServiceClient.getPersonById(1L)).thenReturn(person(1L));
    when(userServiceClient.getUserById(2L)).thenReturn(user(2L));
    when(vehicleServiceClient.getCarById(77L)).thenReturn(car);
    when(purchaseSaleRepository.findByVehicleId(77L)).thenReturn(List.of(existing));

    assertThatThrownBy(() -> purchaseSaleService.create(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ya tiene una compra registrada");

    verify(purchaseSaleRepository, never()).save(any(PurchaseSale.class));
  }

  @SuppressWarnings("null")
  @Test
  void createSale_usesLatestActivePurchasePriceAndPersistsSale() {
    PurchaseSaleRequest request = buildBaseRequest();
    request.setContractType(ContractType.SALE);
    request.setSalePrice(20000d);
    request.setPurchasePrice(1000d);
    request.setVehicleId(88L);
    request.setVehicleData(null);

    LocalDateTime now = LocalDateTime.now();
    PurchaseSale oldPurchase =
        buildContract(
            1L, ContractType.PURCHASE, ContractStatus.COMPLETED, 8000d, 0d, 88L, now.minusDays(2));
    PurchaseSale latestPurchase =
        buildContract(
            2L, ContractType.PURCHASE, ContractStatus.ACTIVE, 9500d, 0d, 88L, now.minusHours(1));
    PurchaseSale canceledSale =
        buildContract(
            3L, ContractType.SALE, ContractStatus.CANCELED, 0d, 0d, 88L, now.minusDays(1));

    Car car = new Car();
    car.setId(88L);

    when(clientServiceClient.getPersonById(1L)).thenReturn(person(1L));
    when(userServiceClient.getUserById(2L)).thenReturn(user(2L));
    when(vehicleServiceClient.getCarById(88L)).thenReturn(car);
    when(purchaseSaleRepository.findByVehicleId(88L))
        .thenReturn(List.of(oldPurchase, latestPurchase, canceledSale));
    when(purchaseSaleMapper.toPurchaseSale(any(PurchaseSaleRequest.class)))
        .thenAnswer(invocation -> mapToEntity(invocation.getArgument(0)));
    when(purchaseSaleRepository.save(any(PurchaseSale.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PurchaseSale result = purchaseSaleService.create(request);

    assertThat(result.getPurchasePrice()).isEqualTo(9500d);
    assertThat(result.getSalePrice()).isEqualTo(20000d);
    assertThat(result.getContractType()).isEqualTo(ContractType.SALE);
  }

  @SuppressWarnings("null")
  @Test
  void createSale_throwsWhenNoActiveOrCompletedPurchaseExists() {
    PurchaseSaleRequest request = buildBaseRequest();
    request.setContractType(ContractType.SALE);
    request.setSalePrice(18000d);
    request.setVehicleId(99L);

    PurchaseSale pendingPurchase =
        buildContract(
            1L,
            ContractType.PURCHASE,
            ContractStatus.PENDING,
            8000d,
            0d,
            99L,
            LocalDateTime.now().minusDays(1));

    Car car = new Car();
    car.setId(99L);

    when(clientServiceClient.getPersonById(1L)).thenReturn(person(1L));
    when(userServiceClient.getUserById(2L)).thenReturn(user(2L));
    when(vehicleServiceClient.getCarById(99L)).thenReturn(car);
    when(purchaseSaleRepository.findByVehicleId(99L)).thenReturn(List.of(pendingPurchase));

    assertThatThrownBy(() -> purchaseSaleService.create(request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no cuenta con una compra activa");

    verify(purchaseSaleRepository, never()).save(any(PurchaseSale.class));
  }

  @SuppressWarnings("null")
  @Test
  void update_rejectsContractTypeChanges() {
    PurchaseSaleRequest request = buildBaseRequest();
    request.setContractType(ContractType.SALE);
    request.setSalePrice(15000d);
    request.setVehicleId(40L);

    PurchaseSale existing =
        buildContract(
            10L,
            ContractType.PURCHASE,
            ContractStatus.PENDING,
            9000d,
            0d,
            40L,
            LocalDateTime.now().minusHours(5));

    Car car = new Car();
    car.setId(40L);

    when(clientServiceClient.getPersonById(1L)).thenReturn(person(1L));
    when(userServiceClient.getUserById(2L)).thenReturn(user(2L));
    when(vehicleServiceClient.getCarById(40L)).thenReturn(car);
    when(purchaseSaleRepository.findByVehicleId(40L)).thenReturn(List.of(existing));
    when(purchaseSaleRepository.findById(10L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> purchaseSaleService.update(10L, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cambiar el tipo de contrato");

    verify(purchaseSaleRepository, never()).save(any(PurchaseSale.class));
  }

  private PurchaseSaleRequest buildBaseRequest() {
    PurchaseSaleRequest request = new PurchaseSaleRequest();
    request.setClientId(1L);
    request.setUserId(2L);
    request.setPurchasePrice(12000d);
    request.setSalePrice(15000d);
    request.setContractType(ContractType.PURCHASE);
    request.setContractStatus(ContractStatus.PENDING);
    request.setPaymentLimitations("Ninguna");
    request.setPaymentTerms("Pago inmediato");
    request.setPaymentMethod(PaymentMethod.CASH);
    request.setObservations("Contrato base");
    return request;
  }

  private VehicleCreationRequest buildCarData() {
    VehicleCreationRequest vehicle = new VehicleCreationRequest();
    vehicle.setVehicleType(VehicleType.CAR);
    vehicle.setBrand("Toyota");
    vehicle.setModel("Corolla");
    vehicle.setCapacity(5);
    vehicle.setLine("SE");
    vehicle.setPlate("abc123");
    vehicle.setMotorNumber("MTR123");
    vehicle.setSerialNumber("SER123");
    vehicle.setChassisNumber("CHS123");
    vehicle.setColor("Azul");
    vehicle.setCityRegistered("Bogota");
    vehicle.setYear(2020);
    vehicle.setMileage(10000);
    vehicle.setTransmission("Automatica");
    vehicle.setPurchasePrice(12000d);
    vehicle.setSalePrice(18000d);
    vehicle.setPhotoUrl("http://example.com/photo");
    vehicle.setBodyType("Sedan");
    vehicle.setFuelType("Gasolina");
    vehicle.setNumberOfDoors(4);
    return vehicle;
  }

  private PurchaseSale mapToEntity(PurchaseSaleRequest request) {
    PurchaseSale entity = new PurchaseSale();
    entity.setPurchasePrice(request.getPurchasePrice());
    entity.setSalePrice(request.getSalePrice());
    entity.setContractType(request.getContractType());
    entity.setContractStatus(request.getContractStatus());
    entity.setPaymentLimitations(request.getPaymentLimitations());
    entity.setPaymentTerms(request.getPaymentTerms());
    entity.setPaymentMethod(request.getPaymentMethod());
    entity.setObservations(request.getObservations());
    return entity;
  }

  private PurchaseSale buildContract(
      Long id,
      ContractType contractType,
      ContractStatus status,
      Double purchasePrice,
      Double salePrice,
      Long vehicleId,
      LocalDateTime updatedAt) {
    PurchaseSale purchaseSale = new PurchaseSale();
    purchaseSale.setId(id);
    purchaseSale.setContractType(contractType);
    purchaseSale.setContractStatus(status);
    purchaseSale.setPurchasePrice(purchasePrice);
    purchaseSale.setSalePrice(salePrice);
    purchaseSale.setVehicleId(vehicleId);
    purchaseSale.setUpdatedAt(updatedAt);
    return purchaseSale;
  }

  private Person person(long id) {
    Person person = new Person();
    person.setId(id);
    return person;
  }

  private User user(long id) {
    User user = new User();
    user.setId(id);
    return user;
  }
}
