package com.sgivu.purchasesale.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sgivu.purchasesale.dto.ClientSummary;
import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.UserSummary;
import com.sgivu.purchasesale.dto.VehicleSummary;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import com.sgivu.purchasesale.repository.PurchaseSaleRepository;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

class PurchaseSaleReportServiceTest {

  private PurchaseSaleReportService service;

  @BeforeEach
  void setUp() {
    // Las dependencias no se usan en formatDate, por lo que podemos usar mocks sencillos
    PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
    PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
    service = new PurchaseSaleReportService(repo, detailService);
  }

  private String invokeFormatDate(LocalDateTime dateTime) throws Exception {
    Method m = PurchaseSaleReportService.class.getDeclaredMethod("formatDate", LocalDateTime.class);
    m.setAccessible(true);
    return (String) m.invoke(service, dateTime);
  }

  @Nested
  @DisplayName("formatDate(LocalDateTime)")
  class FormatDateTests {

    @Test
    @DisplayName("Debe retornar cadena vacía cuando el argumento es nulo")
    void shouldReturnEmptyStringWhenArgumentIsNull() throws Exception {
      assertEquals("", invokeFormatDate(null));
    }

    @Test
    @DisplayName("Debe convertir UTC a America/Bogota y formatear correctamente (hora sin minutos)")
    void shouldConvertUtcToBogotaAndFormatHour() throws Exception {
      // 2020-01-01T12:00 UTC corresponde a 2020-01-01 07:00 en America/Bogota (UTC-5)
      LocalDateTime utc = LocalDateTime.of(2020, 1, 1, 12, 0);
      assertEquals("01/01/2020 07:00", invokeFormatDate(utc));
    }

    @Test
    @DisplayName("Debe convertir UTC a America/Bogota y formatear correctamente (con minutos)")
    void shouldConvertUtcToBogotaAndFormatWithMinutes() throws Exception {
      // 2021-06-15T18:30 UTC corresponde a 2021-06-15 13:30 en America/Bogota (UTC-5)
      LocalDateTime utc = LocalDateTime.of(2021, 6, 15, 18, 30);
      assertEquals("15/06/2021 13:30", invokeFormatDate(utc));
    }
  }

  @Nested
  @DisplayName("buildPeriodText(LocalDate, LocalDate)")
  class BuildPeriodTextTests {

    @Test
    @DisplayName("Debe retornar texto 'todos los registros' cuando ambas fechas son nulas")
    void shouldReturnAllRecordsWhenBothDatesNull() throws Exception {
      assertEquals("Periodo: todos los registros disponibles", invokeBuildPeriodText(null, null));
    }

    @Test
    @DisplayName("Debe formatear correctamente cuando ambas fechas están presentes")
    void shouldFormatCorrectlyWhenBothDatesPresent() throws Exception {
      // 2021-01-01 y 2021-12-31 -> "Periodo: 2021-01-01 - 2021-12-31"
      assertEquals(
          "Periodo: 2021-01-01 - 2021-12-31",
          invokeBuildPeriodText(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)));
    }

    @Test
    @DisplayName("Debe formatear con inicio vacío cuando startDate es nulo")
    void shouldFormatWithStartNull() throws Exception {
      assertEquals(
          "Periodo: ... - 2022-03-05", invokeBuildPeriodText(null, LocalDate.of(2022, 3, 5)));
    }

    @Test
    @DisplayName("Debe formatear con fin vacío cuando endDate es nulo")
    void shouldFormatWithEndNull() throws Exception {
      assertEquals(
          "Periodo: 2019-07-10 - ...", invokeBuildPeriodText(LocalDate.of(2019, 7, 10), null));
    }
  }

  private String invokeBuildPeriodText(LocalDate start, LocalDate end) throws Exception {
    Method m =
        PurchaseSaleReportService.class.getDeclaredMethod(
            "buildPeriodText", LocalDate.class, LocalDate.class);
    m.setAccessible(true);
    return (String) m.invoke(service, start, end);
  }

  @Nested
  @DisplayName("formatCurrency(Double)")
  class FormatCurrencyTests {

    @Test
    @DisplayName("Debe retornar 'N/D' cuando el valor es nulo")
    void shouldReturnNDWhenValueIsNull() throws Exception {
      assertEquals("N/D", invokeFormatCurrency(null));
    }

    @Test
    @DisplayName("Debe formatear valores enteros según la localización es_CO")
    void shouldFormatIntegerValue() throws Exception {
      // 100 -> formato de moneda según locale es_CO
      Double value = 100d;
      NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
      nf.setMaximumFractionDigits(2);
      nf.setMinimumFractionDigits(0);
      String expected = nf.format(value);
      assertEquals(expected, invokeFormatCurrency(value));
    }

    @Test
    @DisplayName("Debe formatear valores decimales correctamente")
    void shouldFormatDecimalValue() throws Exception {
      // 1234.56 -> formato de moneda con decimales
      Double value = 1234.56;
      NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
      nf.setMaximumFractionDigits(2);
      nf.setMinimumFractionDigits(0);
      String expected = nf.format(value);
      assertEquals(expected, invokeFormatCurrency(value));
    }

    @Test
    @DisplayName("Debe formatear cero como moneda")
    void shouldFormatZero() throws Exception {
      Double value = 0d;
      NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
      nf.setMaximumFractionDigits(2);
      nf.setMinimumFractionDigits(0);
      String expected = nf.format(value);
      assertEquals(expected, invokeFormatCurrency(value));
    }
  }

  private String invokeFormatCurrency(Double value) throws Exception {
    Method m = PurchaseSaleReportService.class.getDeclaredMethod("formatCurrency", Double.class);
    m.setAccessible(true);
    return (String) m.invoke(service, value);
  }

  @Nested
  @DisplayName("formatDecimal(Double)")
  class FormatDecimalTests {

    @Test
    @DisplayName("Debe retornar cadena vacía cuando el argumento es nulo")
    void shouldReturnEmptyStringForNull() throws Exception {
      assertEquals("", invokeFormatDecimal(null));
    }

    @Test
    @DisplayName("Debe formatear valores enteros con dos decimales")
    void shouldFormatIntegerAsTwoDecimals() throws Exception {
      // 100 -> "100.00"
      Double value = 100d;
      assertEquals("100.00", invokeFormatDecimal(value));
    }

    @Test
    @DisplayName("Debe formatear valores decimales con dos decimales")
    void shouldFormatDecimalWithTwoDecimals() throws Exception {
      // 1234.5 -> "1234.50"
      Double value = 1234.5;
      assertEquals("1234.50", invokeFormatDecimal(value));
    }

    @Test
    @DisplayName("Debe redondear valores correctamente a dos decimales")
    void shouldRoundValuesCorrectly() throws Exception {
      // 1.237 -> "1.24" (redondeo hacia arriba)
      Double value = 1.237;
      assertEquals("1.24", invokeFormatDecimal(value));
    }

    @Test
    @DisplayName("Debe formatear números negativos correctamente")
    void shouldFormatNegativeNumbers() throws Exception {
      // -12.346 -> "-12.35"
      Double value = -12.346;
      assertEquals("-12.35", invokeFormatDecimal(value));
    }
  }

  private String invokeFormatDecimal(Double value) throws Exception {
    Method m = PurchaseSaleReportService.class.getDeclaredMethod("formatDecimal", Double.class);
    m.setAccessible(true);
    return (String) m.invoke(service, value);
  }

  @Nested
  @DisplayName("escapeCsvValue(String)")
  class EscapeCsvValueTests {

    @Test
    @DisplayName("Debe retornar vacío entrecomillado cuando el valor es nulo")
    void shouldReturnQuotedEmptyWhenNull() throws Exception {
      assertEquals("\"\"", invokeEscapeCsvValue(null));
    }

    @Test
    @DisplayName("Debe entrecomillar texto plano")
    void shouldQuotePlainText() throws Exception {
      // Texto sin comas ni comillas -> se encierra entre comillas
      assertEquals("\"Hello\"", invokeEscapeCsvValue("Hello"));
    }

    @Test
    @DisplayName("Debe escapar comillas dobles duplicándolas")
    void shouldEscapeDoubleQuotes() throws Exception {
      // He said "Hi" -> He said ""Hi"" envuelto entre comillas
      String input = "He said \"Hi\"";
      assertEquals("\"He said \"\"Hi\"\"\"", invokeEscapeCsvValue(input));
    }

    @Test
    @DisplayName("Debe entrecomillar valor con comas")
    void shouldQuoteValueWithCommas() throws Exception {
      // a,b -> "a,b"
      assertEquals("\"a,b\"", invokeEscapeCsvValue("a,b"));
    }

    @Test
    @DisplayName("Debe escapar comillas y comas")
    void shouldEscapeQuotesAndCommas() throws Exception {
      // a,"b" -> a,""b"" luego envuelto -> "a,""b"""
      String input = "a,\"b\"";
      assertEquals("\"a,\"\"b\"\"\"", invokeEscapeCsvValue(input));
    }

    @Test
    @DisplayName("Debe entrecomillar cadena vacía")
    void shouldQuoteEmptyString() throws Exception {
      assertEquals("\"\"", invokeEscapeCsvValue(""));
    }
  }

  private String invokeEscapeCsvValue(String value) throws Exception {
    Method m = PurchaseSaleReportService.class.getDeclaredMethod("escapeCsvValue", String.class);
    m.setAccessible(true);
    return (String) m.invoke(service, value);
  }

  @Nested
  @DisplayName("buildCsvRow(PurchaseSaleDetailResponse)")
  class BuildCsvRowTests {

    @Test
    @DisplayName("Debe construir fila CSV con todos los detalles")
    void shouldBuildCsvRowWithFullDetails() throws Exception {
      // Crear client, user y vehicle completos
      ClientSummary client =
          ClientSummary.builder()
              .id(10L)
              .type("PERSON")
              .name("Juan Perez")
              .identifier("CC 123")
              .email("juan@example.com")
              .phoneNumber(3001234567L)
              .build();

      UserSummary user =
          UserSummary.builder()
              .id(20L)
              .fullName("Carlos Lopez")
              .username("clopez")
              .email("carlos@example.com")
              .build();

      VehicleSummary vehicle =
          VehicleSummary.builder()
              .id(30L)
              .type("CAR")
              .brand("Toyota")
              .line("Corolla")
              .model("2020")
              .plate("ABC123")
              .status("Disponible")
              .build();

      PurchaseSaleDetailResponse r = new PurchaseSaleDetailResponse();
      r.setClientSummary(client);
      r.setUserSummary(user);
      r.setVehicleSummary(vehicle);
      r.setPurchasePrice(100d);
      r.setSalePrice(150d);
      r.setContractType(ContractType.PURCHASE);
      r.setContractStatus(ContractStatus.PENDING);
      r.setPaymentMethod(PaymentMethod.CASH);
      r.setPaymentTerms("terms");
      r.setPaymentLimitations("none");
      r.setObservations("observations");
      r.setCreatedAt(LocalDateTime.of(2020, 1, 1, 12, 0));
      r.setUpdatedAt(LocalDateTime.of(2021, 6, 15, 18, 30));

      Method m =
          PurchaseSaleReportService.class.getDeclaredMethod(
              "buildCsvRow", PurchaseSaleDetailResponse.class);
      m.setAccessible(true);
      String[] row = (String[]) m.invoke(service, r);

      String[] expected = {
        "Compra",
        "Pendiente",
        "Juan Perez",
        "Persona",
        "CC 123",
        "juan@example.com",
        "3001234567",
        "Carlos Lopez",
        "clopez",
        "carlos@example.com",
        "Toyota",
        "Corolla",
        "2020",
        "ABC123",
        "Automóvil",
        "Disponible",
        "100.00",
        "150.00",
        "Efectivo",
        "terms",
        "none",
        "observations",
        "01/01/2020 07:00",
        "15/06/2021 13:30"
      };

      assertArrayEquals(expected, row);
    }

    @Test
    @DisplayName("Debe manejar sub-objetos nulos y valores nulos correctamente")
    void shouldHandleNullSubObjectsAndNullValuesGracefully() throws Exception {
      PurchaseSaleDetailResponse r = new PurchaseSaleDetailResponse();
      r.setClientSummary(null);
      r.setUserSummary(null);
      r.setVehicleSummary(null);
      r.setPurchasePrice(null);
      r.setSalePrice(null);
      r.setContractType(null);
      r.setContractStatus(null);
      r.setPaymentMethod(null);
      r.setPaymentTerms(null);
      r.setPaymentLimitations(null);
      r.setObservations(null);
      r.setCreatedAt(null);
      r.setUpdatedAt(null);

      Method m =
          PurchaseSaleReportService.class.getDeclaredMethod(
              "buildCsvRow", PurchaseSaleDetailResponse.class);
      m.setAccessible(true);
      String[] row = (String[]) m.invoke(service, r);

      String[] expected = new String[24];
      for (int i = 0; i < expected.length; i++) {
        expected[i] = "";
      }

      assertArrayEquals(expected, row);
    }
  }

  @Nested
  @DisplayName("getVehicleTypeLabel(String)")
  class GetVehicleTypeLabelTests {

    @Test
    @DisplayName("Debe retornar cadena vacía cuando el tipo es nulo")
    void shouldReturnEmptyStringWhenTypeIsNull() throws Exception {
      assertEquals("", invokeGetVehicleTypeLabel(null));
    }

    @Test
    @DisplayName("Debe retornar 'Automóvil' para CAR")
    void shouldReturnAutomovilForCar() throws Exception {
      assertEquals("Automóvil", invokeGetVehicleTypeLabel("CAR"));
    }

    @Test
    @DisplayName("Debe retornar 'Motocicleta' para MOTORCYCLE")
    void shouldReturnMotocicletaForMotorcycle() throws Exception {
      assertEquals("Motocicleta", invokeGetVehicleTypeLabel("MOTORCYCLE"));
    }

    @Test
    @DisplayName("Debe ser insensible a mayúsculas y minúsculas")
    void shouldBeCaseInsensitive() throws Exception {
      assertEquals("Automóvil", invokeGetVehicleTypeLabel("car"));
      assertEquals("Motocicleta", invokeGetVehicleTypeLabel("Motorcycle"));
    }

    @Test
    @DisplayName("Debe retornar cadena original para tipo desconocido")
    void shouldReturnOriginalForUnknownType() throws Exception {
      assertEquals("BICYCLE", invokeGetVehicleTypeLabel("BICYCLE"));
      assertEquals("bicicleta", invokeGetVehicleTypeLabel("bicicleta"));
    }
  }

  @Nested
  @DisplayName("getClientTypeLabel(String)")
  class GetClientTypeLabelTests {

    @Test
    @DisplayName("Debe retornar cadena vacía cuando el tipo es nulo")
    void shouldReturnEmptyStringWhenClientTypeIsNull() throws Exception {
      assertEquals("", invokeGetClientTypeLabel(null));
    }

    @Test
    @DisplayName("Debe retornar 'Persona' para PERSON")
    void shouldReturnPersonaForPerson() throws Exception {
      assertEquals("Persona", invokeGetClientTypeLabel("PERSON"));
    }

    @Test
    @DisplayName("Debe retornar 'Empresa' para COMPANY")
    void shouldReturnEmpresaForCompany() throws Exception {
      assertEquals("Empresa", invokeGetClientTypeLabel("COMPANY"));
    }

    @Test
    @DisplayName("Debe ser insensible a mayúsculas y minúsculas")
    void shouldBeCaseInsensitiveForClientTypes() throws Exception {
      assertEquals("Persona", invokeGetClientTypeLabel("person"));
      assertEquals("Empresa", invokeGetClientTypeLabel("Company"));
    }

    @Test
    @DisplayName("Debe retornar cadena original para tipo de cliente desconocido")
    void shouldReturnOriginalForUnknownClientType() throws Exception {
      assertEquals("OTHER", invokeGetClientTypeLabel("OTHER"));
      assertEquals("otro", invokeGetClientTypeLabel("otro"));
    }
  }

  @Nested
  @DisplayName("safeText(String, String)")
  class SafeTextTests {

    @Test
    @DisplayName("Debe retornar valor por defecto cuando el valor es nulo")
    void shouldReturnFallbackWhenValueIsNull() throws Exception {
      // Valor nulo -> se usa el fallback
      assertEquals("fallback", invokeSafeText(null, "fallback"));
    }

    @Test
    @DisplayName("Debe retornar valor por defecto cuando el valor está vacío")
    void shouldReturnFallbackWhenValueIsBlank() throws Exception {
      // Cadena vacía o espacios en blanco -> se usa el fallback
      assertEquals("fallback", invokeSafeText("   ", "fallback"));
    }

    @Test
    @DisplayName("Debe retornar original cuando el valor no está vacío")
    void shouldReturnOriginalWhenNotBlank() throws Exception {
      // Mantiene el valor si no es blank
      assertEquals("value", invokeSafeText("value", "fallback"));
    }

    @Test
    @DisplayName("Debe retornar null cuando ambos son nulos")
    void shouldReturnNullWhenBothNull() throws Exception {
      assertEquals(null, invokeSafeText(null, null));
    }

    @Test
    @DisplayName("Debe preservar espacios en blanco alrededor del valor")
    void shouldPreserveWhitespaceAroundValue() throws Exception {
      assertEquals(" a ", invokeSafeText(" a ", "fallback"));
    }
  }

  @Nested
  @DisplayName("filterByDateRange(LocalDateTime, LocalDate, LocalDate)")
  class FilterByDateRangeTests {

    @Test
    @DisplayName("Debe retornar false cuando el valor es nulo")
    void shouldReturnFalseWhenValueIsNull() throws Exception {
      assertFalse(invokeFilterByDateRange(null, null, null));
    }

    @Test
    @DisplayName("Debe retornar true cuando no hay límites y el valor está presente")
    void shouldReturnTrueWhenNoBoundsAndValuePresent() throws Exception {
      LocalDateTime t = LocalDateTime.of(2021, 5, 1, 10, 0);
      assertTrue(invokeFilterByDateRange(t, null, null));
    }

    @Test
    @DisplayName("Debe ser inclusivo con la fecha de inicio")
    void shouldBeInclusiveOfStartDate() throws Exception {
      LocalDateTime t = LocalDateTime.of(2021, 1, 2, 0, 0);
      assertTrue(invokeFilterByDateRange(t, LocalDate.of(2021, 1, 2), null));
    }

    @Test
    @DisplayName("Debe ser inclusivo con la fecha de fin")
    void shouldBeInclusiveOfEndDate() throws Exception {
      LocalDateTime t = LocalDateTime.of(2021, 1, 1, 0, 0);
      assertTrue(invokeFilterByDateRange(t, null, LocalDate.of(2021, 1, 1)));
    }

    @Test
    @DisplayName("Debe retornar false cuando el valor es anterior al inicio")
    void shouldReturnFalseWhenBeforeStart() throws Exception {
      LocalDateTime t = LocalDateTime.of(2020, 12, 31, 23, 59);
      assertFalse(invokeFilterByDateRange(t, LocalDate.of(2021, 1, 1), null));
    }

    @Test
    @DisplayName("Debe retornar false cuando el valor es posterior al fin")
    void shouldReturnFalseWhenAfterEnd() throws Exception {
      LocalDateTime t = LocalDateTime.of(2021, 1, 3, 0, 0);
      assertFalse(invokeFilterByDateRange(t, null, LocalDate.of(2021, 1, 2)));
    }

    @Test
    @DisplayName("Debe retornar true cuando está entre inicio y fin")
    void shouldReturnTrueWhenBetween() throws Exception {
      LocalDateTime t = LocalDateTime.of(2021, 1, 2, 12, 0);
      assertTrue(invokeFilterByDateRange(t, LocalDate.of(2021, 1, 2), LocalDate.of(2021, 1, 3)));
    }
  }

  @Nested
  @DisplayName("findContracts(LocalDate, LocalDate)")
  class FindContractsTests {

    @Test
    @DisplayName("Debe retornar lista vacía cuando el repositorio no tiene contratos")
    void shouldReturnEmptyListWhenRepoEmpty() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Collections.emptyList());

      List<PurchaseSale> result = invokeFindContracts(localService, null, null);
      assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Debe ignorar contratos con createdAt nulo")
    void shouldIgnoreContractsWithNullCreatedAt() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      PurchaseSale s1 = new PurchaseSale();
      s1.setCreatedAt(null);

      PurchaseSale s2 = new PurchaseSale();
      s2.setCreatedAt(LocalDateTime.of(2021, 5, 10, 10, 0));

      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(s1, s2));

      List<PurchaseSale> result = invokeFindContracts(localService, null, null);
      assertEquals(1, result.size());
      assertEquals(s2, result.get(0));
    }

    @Test
    @DisplayName("Debe filtrar por fecha de inicio inclusiva")
    void shouldFilterByStartDateInclusive() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      PurchaseSale s1 = new PurchaseSale();
      s1.setCreatedAt(LocalDateTime.of(2021, 1, 1, 0, 0));
      PurchaseSale s2 = new PurchaseSale();
      s2.setCreatedAt(LocalDateTime.of(2021, 1, 2, 0, 0));

      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(s1, s2));

      List<PurchaseSale> result = invokeFindContracts(localService, LocalDate.of(2021, 1, 2), null);
      assertEquals(1, result.size());
      assertEquals(s2, result.get(0));
    }

    @Test
    @DisplayName("Debe filtrar por fecha de fin inclusiva")
    void shouldFilterByEndDateInclusive() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      PurchaseSale s1 = new PurchaseSale();
      s1.setCreatedAt(LocalDateTime.of(2021, 1, 1, 0, 0));
      PurchaseSale s2 = new PurchaseSale();
      s2.setCreatedAt(LocalDateTime.of(2021, 1, 2, 0, 0));

      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(s1, s2));

      List<PurchaseSale> result = invokeFindContracts(localService, null, LocalDate.of(2021, 1, 1));
      assertEquals(1, result.size());
      assertEquals(s1, result.get(0));
    }

    @Test
    @DisplayName("Debe filtrar entre fechas de inicio y fin")
    void shouldFilterBetweenStartAndEnd() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      PurchaseSale s1 = new PurchaseSale();
      s1.setCreatedAt(LocalDateTime.of(2021, 1, 1, 0, 0));
      PurchaseSale s2 = new PurchaseSale();
      s2.setCreatedAt(LocalDateTime.of(2021, 1, 2, 0, 0));
      PurchaseSale s3 = new PurchaseSale();
      s3.setCreatedAt(LocalDateTime.of(2021, 1, 3, 0, 0));

      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(s1, s2, s3));

      List<PurchaseSale> result =
          invokeFindContracts(localService, LocalDate.of(2021, 1, 2), LocalDate.of(2021, 1, 3));
      assertEquals(2, result.size());
      assertEquals(s2, result.get(0));
      assertEquals(s3, result.get(1));
    }
  }

  @Nested
  @DisplayName("generatePdf(LocalDate, LocalDate)")
  class GeneratePdfTests {

    @Test
    @DisplayName("Debe retornar PDF no vacío cuando no hay contratos")
    void shouldReturnNonEmptyPdfWhenNoContracts() throws Exception {
      // Repositorio vacío -> PDF mínimo
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Collections.emptyList());
      Mockito.when(detailService.toDetails(Mockito.any())).thenReturn(Collections.emptyList());

      byte[] pdf = localService.generatePdf(null, null);
      assertTrue(pdf != null && pdf.length > 0);
      Mockito.verify(detailService).toDetails(Collections.emptyList());
    }

    @Test
    @DisplayName("Debe generar PDF con datos y contener texto del título")
    void shouldGeneratePdfWithDataAndContainTitle() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);

      PurchaseSale s = new PurchaseSale();
      s.setCreatedAt(LocalDateTime.of(2022, 1, 1, 10, 0));
      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(s));

      PurchaseSaleDetailResponse detail = new PurchaseSaleDetailResponse();
      detail.setObservations("observations");
      Mockito.when(detailService.toDetails(Mockito.any())).thenReturn(Arrays.asList(detail));

      byte[] pdf = localService.generatePdf(null, null);
      assertTrue(pdf != null && pdf.length > 0);

      String asString = new String(pdf, StandardCharsets.UTF_8);
      assertTrue(
          asString.contains("Reporte de compras y ventas")
              || asString.contains("Reporte de compras y ventas de vehículos"));
      Mockito.verify(detailService).toDetails(Mockito.any());
    }
  }

  @Nested
  @DisplayName("generateCsv(LocalDate, LocalDate)")
  class GenerateCsvTests {

    @Test
    @DisplayName("Debe retornar CSV mencionando sin registros cuando no hay contratos")
    void shouldReturnCsvWithNoRecordsWhenNoContracts() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Collections.emptyList());
      Mockito.when(detailService.toDetails(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);
      byte[] csv = localService.generateCsv(null, null);
      String text = new String(csv, StandardCharsets.UTF_8);

      assertTrue(text.startsWith("Periodo,"));
      assertTrue(text.contains("Periodo: todos los registros disponibles"));
      assertTrue(text.contains("No existen registros para el periodo seleccionado."));
      assertTrue(text.contains("Tipo de contrato"));
    }

    @Test
    @DisplayName("Debe generar CSV con encabezados y una fila de datos")
    void shouldGenerateCsvWithHeadersAndData() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);

      PurchaseSale contract = Mockito.mock(PurchaseSale.class);
      Mockito.when(contract.getCreatedAt()).thenReturn(LocalDateTime.of(2021, 6, 15, 10, 0));
      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(contract));

      ClientSummary client =
          ClientSummary.builder()
              .id(10L)
              .type("PERSON")
              .name("Juan Perez")
              .identifier("CC 123")
              .email("juan@example.com")
              .phoneNumber(3001234567L)
              .build();

      UserSummary user =
          UserSummary.builder()
              .id(20L)
              .fullName("Carlos Lopez")
              .username("clopez")
              .email("carlos@example.com")
              .build();

      VehicleSummary vehicle =
          VehicleSummary.builder()
              .id(30L)
              .type("CAR")
              .brand("Toyota")
              .line("Corolla")
              .model("2020")
              .plate("ABC123")
              .status("Disponible")
              .build();

      PurchaseSaleDetailResponse r = new PurchaseSaleDetailResponse();
      r.setClientSummary(client);
      r.setUserSummary(user);
      r.setVehicleSummary(vehicle);
      r.setPurchasePrice(100d);
      r.setSalePrice(150d);
      r.setContractType(ContractType.PURCHASE);
      r.setContractStatus(ContractStatus.PENDING);
      r.setPaymentMethod(PaymentMethod.CASH);
      r.setPaymentTerms("terms");
      r.setPaymentLimitations("none");
      r.setObservations("observations");
      r.setCreatedAt(LocalDateTime.of(2021, 6, 15, 10, 0));
      r.setUpdatedAt(LocalDateTime.of(2021, 6, 16, 12, 0));

      Mockito.when(detailService.toDetails(Mockito.anyList())).thenReturn(Arrays.asList(r));

      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);
      byte[] csv = localService.generateCsv(null, null);
      String text = new String(csv, StandardCharsets.UTF_8);

      String[] lines = text.split("\r?\n");
      // lines[0] -> Periodo,lines[1] blank, lines[2] -> headers, lines[3] -> data
      assertTrue(lines.length >= 4);
      assertTrue(lines[2].contains("Tipo de contrato"));
      assertTrue(lines[3].contains("Juan Perez"));
      assertTrue(lines[3].contains("ABC123"));
      assertTrue(lines[3].contains("100.00"));
    }
  }

  @Nested
  @DisplayName("generateExcel(LocalDate, LocalDate)")
  class GenerateExcelTests {

    @Test
    @DisplayName("Debe retornar Excel no vacío cuando no hay contratos")
    void shouldReturnNonEmptyExcelWhenNoContracts() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);
      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Collections.emptyList());
      Mockito.when(detailService.toDetails(Collections.emptyList()))
          .thenReturn(Collections.emptyList());

      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);
      byte[] excel = localService.generateExcel(null, null);
      assertTrue(excel.length > 0);

      try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
        Sheet sheet = workbook.getSheetAt(0);
        assertEquals("Compras y ventas", sheet.getSheetName());
        Row periodRow = sheet.getRow(0);
        Cell periodCell = periodRow.getCell(0);
        assertEquals("Periodo: todos los registros disponibles", periodCell.getStringCellValue());
      }
    }

    @Test
    @DisplayName("Debe generar Excel con datos y contener encabezados y datos")
    void shouldGenerateExcelWithDataAndContainHeadersAndData() throws Exception {
      PurchaseSaleRepository repo = Mockito.mock(PurchaseSaleRepository.class);
      PurchaseSaleDetailService detailService = Mockito.mock(PurchaseSaleDetailService.class);

      PurchaseSale contract = Mockito.mock(PurchaseSale.class);
      Mockito.when(contract.getCreatedAt()).thenReturn(LocalDateTime.of(2021, 6, 15, 10, 0));
      Mockito.when(repo.findAll(Mockito.any(Sort.class))).thenReturn(Arrays.asList(contract));

      ClientSummary client =
          ClientSummary.builder()
              .id(10L)
              .type("PERSON")
              .name("Juan Perez")
              .identifier("CC 123")
              .email("juan@example.com")
              .phoneNumber(3001234567L)
              .build();

      UserSummary user =
          UserSummary.builder()
              .id(20L)
              .fullName("Carlos Lopez")
              .username("clopez")
              .email("carlos@example.com")
              .build();

      VehicleSummary vehicle =
          VehicleSummary.builder()
              .id(30L)
              .type("CAR")
              .brand("Toyota")
              .line("Corolla")
              .model("2020")
              .plate("ABC123")
              .status("Disponible")
              .build();

      PurchaseSaleDetailResponse r = new PurchaseSaleDetailResponse();
      r.setClientSummary(client);
      r.setUserSummary(user);
      r.setVehicleSummary(vehicle);
      r.setPurchasePrice(100d);
      r.setSalePrice(150d);
      r.setContractType(ContractType.PURCHASE);
      r.setContractStatus(ContractStatus.PENDING);
      r.setPaymentMethod(PaymentMethod.CASH);
      r.setPaymentTerms("terms");
      r.setPaymentLimitations("none");
      r.setObservations("observations");
      r.setCreatedAt(LocalDateTime.of(2021, 6, 15, 10, 0));
      r.setUpdatedAt(LocalDateTime.of(2021, 6, 16, 12, 0));

      Mockito.when(detailService.toDetails(Mockito.anyList())).thenReturn(Arrays.asList(r));

      PurchaseSaleReportService localService = new PurchaseSaleReportService(repo, detailService);
      byte[] excel = localService.generateExcel(null, null);

      try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
        Sheet sheet = workbook.getSheetAt(0);

        Row headerRow = sheet.getRow(2);
        assertEquals("Tipo de contrato", headerRow.getCell(0).getStringCellValue());
        assertEquals("Marca del vehículo", headerRow.getCell(10).getStringCellValue());

        Row dataRow = sheet.getRow(3);
        assertEquals("Compra", dataRow.getCell(0).getStringCellValue());
        assertEquals("Juan Perez", dataRow.getCell(2).getStringCellValue());
        assertEquals("ABC123", dataRow.getCell(13).getStringCellValue());
        assertEquals(100d, dataRow.getCell(16).getNumericCellValue());
      }
    }
  }

  @Nested
  @DisplayName("formatPhoneNumber(Long)")
  class FormatPhoneNumberTests {

    @Test
    @DisplayName("Debe retornar 'N/D' cuando el teléfono es nulo")
    void shouldReturnNDWhenPhoneIsNull() throws Exception {
      assertEquals("N/D", invokeFormatPhoneNumber(null));
    }

    @Test
    @DisplayName("Debe retornar dígitos cuando el teléfono está presente")
    void shouldReturnDigitsWhenPhonePresent() throws Exception {
      // Número estándar -> se convierte a cadena
      Long phone = 3001234567L;
      assertEquals("3001234567", invokeFormatPhoneNumber(phone));
    }

    @Test
    @DisplayName("Debe retornar cadena cero cuando el teléfono es cero")
    void shouldReturnZeroString() throws Exception {
      assertEquals("0", invokeFormatPhoneNumber(0L));
    }

    @Test
    @DisplayName("Debe retornar números negativos como cadena")
    void shouldReturnNegativeNumbers() throws Exception {
      assertEquals("-123", invokeFormatPhoneNumber(-123L));
    }
  }

  private String invokeFormatPhoneNumber(Long phone) throws Exception {
    Method m = PurchaseSaleReportService.class.getDeclaredMethod("formatPhoneNumber", Long.class);
    m.setAccessible(true);
    return (String) m.invoke(service, phone);
  }

  private String invokeGetVehicleTypeLabel(String type) throws Exception {
    Method m =
        PurchaseSaleReportService.class.getDeclaredMethod("getVehicleTypeLabel", String.class);
    m.setAccessible(true);
    return (String) m.invoke(service, type);
  }

  private String invokeGetClientTypeLabel(String type) throws Exception {
    Method m =
        PurchaseSaleReportService.class.getDeclaredMethod("getClientTypeLabel", String.class);
    m.setAccessible(true);
    return (String) m.invoke(service, type);
  }

  private String invokeSafeText(String value, String fallback) throws Exception {
    Method m =
        PurchaseSaleReportService.class.getDeclaredMethod("safeText", String.class, String.class);
    m.setAccessible(true);
    return (String) m.invoke(service, value, fallback);
  }

  @SuppressWarnings("unchecked")
  private List<PurchaseSale> invokeFindContracts(
      PurchaseSaleReportService localService, LocalDate start, LocalDate end) throws Exception {
    Method m =
        PurchaseSaleReportService.class.getDeclaredMethod(
            "findContracts", LocalDate.class, LocalDate.class);
    m.setAccessible(true);
    return (List<PurchaseSale>) m.invoke(localService, start, end);
  }

  private boolean invokeFilterByDateRange(LocalDateTime value, LocalDate start, LocalDate end)
      throws Exception {
    Method m =
        PurchaseSaleReportService.class.getDeclaredMethod(
            "filterByDateRange", LocalDateTime.class, LocalDate.class, LocalDate.class);
    m.setAccessible(true);
    return (Boolean) m.invoke(service, value, start, end);
  }
}
