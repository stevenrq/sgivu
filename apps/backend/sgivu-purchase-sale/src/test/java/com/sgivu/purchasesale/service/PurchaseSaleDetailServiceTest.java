package com.sgivu.purchasesale.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sgivu.purchasesale.client.ClientServiceClient;
import com.sgivu.purchasesale.client.UserServiceClient;
import com.sgivu.purchasesale.client.VehicleServiceClient;
import com.sgivu.purchasesale.dto.Car;
import com.sgivu.purchasesale.dto.Company;
import com.sgivu.purchasesale.dto.Motorcycle;
import com.sgivu.purchasesale.dto.Person;
import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.User;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.mapper.PurchaseSaleMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

class PurchaseSaleDetailServiceTest {

  @Mock private PurchaseSaleMapper purchaseSaleMapper;
  @Mock private ClientServiceClient clientServiceClient;
  @Mock private UserServiceClient userServiceClient;
  @Mock private VehicleServiceClient vehicleServiceClient;

  @InjectMocks private PurchaseSaleDetailService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(purchaseSaleMapper.toPurchaseSaleDetailResponse(any()))
        .thenAnswer(
            invocation -> {
              PurchaseSale p = invocation.getArgument(0);
              PurchaseSaleDetailResponse r = new PurchaseSaleDetailResponse();
              r.setId(p.getId());
              return r;
            });
  }

  private PurchaseSale baseContract() {
    PurchaseSale p = new PurchaseSale();
    p.setPurchasePrice(100d);
    p.setSalePrice(150d);
    p.setContractType(com.sgivu.purchasesale.enums.ContractType.PURCHASE);
    p.setContractStatus(com.sgivu.purchasesale.enums.ContractStatus.PENDING);
    p.setPaymentLimitations("none");
    p.setPaymentTerms("terms");
    p.setPaymentMethod(com.sgivu.purchasesale.enums.PaymentMethod.CASH);
    p.setCreatedAt(LocalDateTime.now());
    p.setUpdatedAt(LocalDateTime.now());
    return p;
  }

  @Nested
  @DisplayName("toDetails(List<PurchaseSale>)")
  class ToDetailsTests {

    @Test
    @DisplayName("Debe mapear contratos sin referencias externas")
    void shouldMapWithoutExternalReferences() {
      PurchaseSale p = baseContract();
      p.setId(1L);

      List<PurchaseSaleDetailResponse> result = service.toDetails(List.of(p));

      assertEquals(1, result.size());
      PurchaseSaleDetailResponse detail = result.get(0);
      assertNull(detail.getClientSummary());
      assertNull(detail.getUserSummary());
      assertNull(detail.getVehicleSummary());

      verifyNoInteractions(clientServiceClient);
      verifyNoInteractions(userServiceClient);
      verifyNoInteractions(vehicleServiceClient);
    }

    @Test
    @DisplayName("Debe enriquecer detalles con persona, usuario y auto y cachear llamadas")
    void shouldEnrichWithPersonUserAndCarAndCache() {
      PurchaseSale p1 = baseContract();
      p1.setId(1L);
      p1.setClientId(10L);
      p1.setUserId(20L);
      p1.setVehicleId(30L);

      PurchaseSale p2 = baseContract();
      p2.setId(2L);
      p2.setClientId(10L);
      p2.setUserId(20L);
      p2.setVehicleId(30L);

      Person person = new Person();
      person.setId(10L);
      person.setFirstName("Juan");
      person.setLastName("Perez");
      person.setNationalId(12345678L);
      person.setEmail("juan@example.com");
      person.setPhoneNumber(3001234567L);

      User user = new User();
      user.setId(20L);
      user.setFirstName("Carlos");
      user.setLastName("Lopez");
      user.setEmail("carlos@example.com");
      user.setUsername("clopez");

      Car car = new Car();
      car.setId(30L);
      car.setBrand("Toyota");
      car.setLine("Corolla");
      car.setModel("2020");
      car.setPlate("ABC123");

      when(clientServiceClient.getPersonById(10L)).thenReturn(person);
      when(userServiceClient.getUserById(20L)).thenReturn(user);
      when(vehicleServiceClient.getCarById(30L)).thenReturn(car);

      List<PurchaseSaleDetailResponse> result = service.toDetails(List.of(p1, p2));

      assertEquals(2, result.size());
      for (PurchaseSaleDetailResponse detail : result) {
        assertNotNull(detail.getClientSummary());
        assertEquals("PERSON", detail.getClientSummary().getType());
        assertEquals("Juan Perez", detail.getClientSummary().getName());
        assertTrue(detail.getClientSummary().getIdentifier().contains("CC 12345678"));

        assertNotNull(detail.getUserSummary());
        assertEquals("Carlos Lopez", detail.getUserSummary().getFullName());
        assertEquals("clopez", detail.getUserSummary().getUsername());

        assertNotNull(detail.getVehicleSummary());
        assertEquals("CAR", detail.getVehicleSummary().getType());
        assertEquals("Toyota", detail.getVehicleSummary().getBrand());
      }

      verify(clientServiceClient, times(1)).getPersonById(10L);
      verify(userServiceClient, times(1)).getUserById(20L);
      verify(vehicleServiceClient, times(1)).getCarById(30L);
    }

    @Test
    @DisplayName(
        "Debe recurrir a empresa cuando la persona no se encuentra y luego desconocido cuando ambos"
            + " faltan")
    void shouldHandleCompanyAndUnknownClient() {
      PurchaseSale p1 = baseContract();
      p1.setId(1L);
      p1.setClientId(11L);

      Company company = new Company();
      company.setId(11L);
      company.setCompanyName("Acme S.A.");
      company.setTaxId("900-111");
      company.setEmail("contact@acme.com");
      company.setPhoneNumber(312000111L);

      // Primera llamada: getPersonById lanza 404, getCompanyById devuelve la compañía
      when(clientServiceClient.getPersonById(11L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(11L)).thenReturn(company);

      List<PurchaseSaleDetailResponse> res1 = service.toDetails(List.of(p1));
      PurchaseSaleDetailResponse detail1 = res1.get(0);
      assertNotNull(detail1.getClientSummary());
      assertEquals("COMPANY", detail1.getClientSummary().getType());
      assertEquals("Acme S.A.", detail1.getClientSummary().getName());

      // Segunda llamada: ambos endpoints devuelven 404 -> se resuelve como UNKNOWN
      PurchaseSale p2 = baseContract();
      p2.setId(2L);
      p2.setClientId(12L);

      when(clientServiceClient.getPersonById(12L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(clientServiceClient.getCompanyById(12L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      List<PurchaseSaleDetailResponse> res2 = service.toDetails(List.of(p2));
      PurchaseSaleDetailResponse detail2 = res2.get(0);
      assertNotNull(detail2.getClientSummary());
      assertEquals("UNKNOWN", detail2.getClientSummary().getType());
      assertEquals("Cliente no disponible", detail2.getClientSummary().getName());
      assertTrue(detail2.getClientSummary().getIdentifier().contains("ID 12"));
    }

    @Test
    @DisplayName(
        "Debe intentar auto luego motocicleta luego desconocido para resolución de vehículo")
    void shouldTryCarThenMotorcycleThenUnknown() {
      PurchaseSale p = baseContract();
      p.setId(1L);
      p.setVehicleId(50L);

      // Automóvil no encontrado, se encuentra la motocicleta
      when(vehicleServiceClient.getCarById(50L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      Motorcycle m = new Motorcycle();
      m.setId(50L);
      m.setBrand("Yamaha");
      m.setLine("YZF");
      m.setModel("R3");
      m.setPlate("MOTO50");
      when(vehicleServiceClient.getMotorcycleById(50L)).thenReturn(m);

      PurchaseSaleDetailResponse detail = service.toDetails(List.of(p)).get(0);
      assertNotNull(detail.getVehicleSummary());
      assertEquals("MOTORCYCLE", detail.getVehicleSummary().getType());
      assertEquals("Yamaha", detail.getVehicleSummary().getBrand());

      // Ambos lanzan excepción -> se resuelve como UNKNOWN
      PurchaseSale p2 = baseContract();
      p2.setId(2L);
      p2.setVehicleId(99L);
      when(vehicleServiceClient.getCarById(99L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
      when(vehicleServiceClient.getMotorcycleById(99L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      PurchaseSaleDetailResponse detail2 = service.toDetails(List.of(p2)).get(0);
      assertNotNull(detail2.getVehicleSummary());
      assertEquals("UNKNOWN", detail2.getVehicleSummary().getType());
      assertEquals("Vehículo no disponible", detail2.getVehicleSummary().getBrand());
    }

    @Test
    @DisplayName("Debe usar resumen de usuario por defecto cuando el usuario no se encuentra")
    void shouldUseFallbackUserSummaryWhenNotFound() {
      PurchaseSale p = baseContract();
      p.setId(1L);
      p.setUserId(77L);

      when(userServiceClient.getUserById(77L))
          .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

      PurchaseSaleDetailResponse detail = service.toDetails(List.of(p)).get(0);
      assertNotNull(detail.getUserSummary());
      assertEquals(77L, detail.getUserSummary().getId());
      assertEquals("Usuario no disponible", detail.getUserSummary().getFullName());
      assertEquals("N/D", detail.getUserSummary().getUsername());
    }
  }

  @Nested
  @DisplayName("toDetail(PurchaseSale)")
  class ToDetailTests {

    @Test
    @DisplayName("toDetail retorna detalle mapeado para contrato individual")
    void toDetailReturnsMappedDetail() {
      PurchaseSale p = baseContract();
      p.setId(5L);
      p.setClientId(1L);

      Person person = new Person();
      person.setId(1L);
      person.setFirstName("Ana");
      person.setLastName("Gomez");
      when(clientServiceClient.getPersonById(1L)).thenReturn(person);

      PurchaseSaleDetailResponse detail = service.toDetail(p);
      assertNotNull(detail);
      assertEquals(5L, detail.getId());
      assertNotNull(detail.getClientSummary());
      assertEquals("Ana Gomez", detail.getClientSummary().getName());
    }

    @Test
    @DisplayName("toDetail retorna null cuando el mapper retorna null")
    void toDetailReturnsNullWhenMapperReturnsNull() {
      PurchaseSale p = baseContract();
      p.setId(9L);

      doReturn(null).when(purchaseSaleMapper).toPurchaseSaleDetailResponse(any());

      assertThrows(NullPointerException.class, () -> service.toDetail(p));
    }

    @Test
    @DisplayName("toDetail lanza NullPointerException cuando el contrato es nulo")
    void toDetailThrowsWhenContractIsNull() {
      assertThrows(NullPointerException.class, () -> service.toDetail(null));
    }
  }
}
