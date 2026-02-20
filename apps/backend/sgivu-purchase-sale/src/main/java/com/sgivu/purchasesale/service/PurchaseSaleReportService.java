package com.sgivu.purchasesale.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sgivu.purchasesale.dto.ClientSummary;
import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.UserSummary;
import com.sgivu.purchasesale.dto.VehicleSummary;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import com.sgivu.purchasesale.repository.PurchaseSaleRepository;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Servicio para generar reportes de compras y ventas de vehículos en formatos PDF, Excel y CSV. */
@Service
public class PurchaseSaleReportService {

  private static final String LABEL_TIPO = "Tipo: ";

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final ZoneId SOURCE_ZONE = ZoneId.of("UTC");
  private static final ZoneId TARGET_ZONE = ZoneId.of("America/Bogota");
  private static final Locale REPORT_LOCALE = Locale.of("es", "CO");
  private static final String[] DATASET_HEADERS = {
    "Tipo de contrato",
    "Estado del contrato",
    "Cliente",
    "Tipo de cliente",
    "Documento del cliente",
    "Email del cliente",
    "Teléfono del cliente",
    "Usuario responsable",
    "Usuario (username)",
    "Email del usuario",
    "Marca del vehículo",
    "Línea del vehículo",
    "Modelo del vehículo",
    "Placa del vehículo",
    "Tipo de vehículo",
    "Estado del vehículo",
    "Precio de compra",
    "Precio de venta",
    "Método de pago",
    "Términos de pago",
    "Limitaciones de pago",
    "Observaciones",
    "Fecha de creación",
    "Última actualización"
  };

  private final PurchaseSaleRepository purchaseSaleRepository;
  private final PurchaseSaleDetailService purchaseSaleDetailService;
  private final Map<ContractStatus, String> statusLabels = new EnumMap<>(ContractStatus.class);
  private final Map<ContractType, String> typeLabels = new EnumMap<>(ContractType.class);
  private final Map<PaymentMethod, String> paymentMethodLabels = new EnumMap<>(PaymentMethod.class);

  public PurchaseSaleReportService(
      PurchaseSaleRepository purchaseSaleRepository,
      PurchaseSaleDetailService purchaseSaleDetailService) {
    this.purchaseSaleRepository = purchaseSaleRepository;
    this.purchaseSaleDetailService = purchaseSaleDetailService;
    initialiseLabels();
  }

  public byte[] generatePdf(LocalDate startDate, LocalDate endDate) {
    List<PurchaseSale> contracts = findContracts(startDate, endDate);
    List<PurchaseSaleDetailResponse> details = purchaseSaleDetailService.toDetails(contracts);

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Document document = new Document();
      PdfWriter.getInstance(document, outputStream);

      document.open();
      document.addTitle("Reporte de compras y ventas");
      document.addAuthor("SGIVU");

      Paragraph title =
          new Paragraph(
              "Reporte de compras y ventas de vehículos",
              FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
      title.setAlignment(Element.ALIGN_CENTER);
      title.setSpacingAfter(10f);
      document.add(title);

      Paragraph period = new Paragraph(buildPeriodText(startDate, endDate));
      period.setAlignment(Element.ALIGN_CENTER);
      period.setSpacingAfter(20f);
      document.add(period);

      PdfPTable table = buildPdfTable(details);
      document.add(table);

      document.close();
      return outputStream.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Error generating PDF report", ex);
    }
  }

  public byte[] generateExcel(LocalDate startDate, LocalDate endDate) {
    List<PurchaseSale> contracts = findContracts(startDate, endDate);
    List<PurchaseSaleDetailResponse> details = purchaseSaleDetailService.toDetails(contracts);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Compras y ventas");

      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFont(headerFont);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);

      Font periodFont = workbook.createFont();
      periodFont.setBold(true);
      CellStyle periodStyle = workbook.createCellStyle();
      periodStyle.setFont(periodFont);

      Row periodRow = sheet.createRow(0);
      Cell periodCell = periodRow.createCell(0);
      periodCell.setCellValue(buildPeriodText(startDate, endDate));
      periodCell.setCellStyle(periodStyle);

      Row headerRow = sheet.createRow(2);
      for (int i = 0; i < DATASET_HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(DATASET_HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIdx = 3;
      for (PurchaseSaleDetailResponse contract : details) {
        Row row = sheet.createRow(rowIdx++);
        int column = 0;
        setCellValue(row, column++, getContractTypeLabel(contract.getContractType()));
        setCellValue(row, column++, getStatusLabel(contract.getContractStatus()));
        setCellValue(row, column++, getClientName(contract.getClientSummary()));
        setCellValue(
            row,
            column++,
            getClientTypeLabel(
                contract.getClientSummary() == null
                    ? null
                    : contract.getClientSummary().getType()));
        setCellValue(row, column++, getClientIdentifier(contract.getClientSummary()));
        setCellValue(row, column++, getClientEmail(contract.getClientSummary()));
        setCellValue(row, column++, getClientPhone(contract.getClientSummary()));
        setCellValue(row, column++, getUserFullName(contract.getUserSummary()));
        setCellValue(row, column++, getUsername(contract.getUserSummary()));
        setCellValue(row, column++, getUserEmail(contract.getUserSummary()));
        setCellValue(row, column++, getVehicleBrand(contract.getVehicleSummary()));
        setCellValue(row, column++, getVehicleLine(contract.getVehicleSummary()));
        setCellValue(row, column++, getVehicleModel(contract.getVehicleSummary()));
        setCellValue(row, column++, getVehiclePlate(contract.getVehicleSummary()));
        setCellValue(
            row,
            column++,
            getVehicleTypeLabel(
                contract.getVehicleSummary() == null
                    ? null
                    : contract.getVehicleSummary().getType()));
        setCellValue(row, column++, getVehicleStatus(contract.getVehicleSummary()));
        setNumericCellValue(row, column++, contract.getPurchasePrice());
        setNumericCellValue(row, column++, contract.getSalePrice());
        setCellValue(row, column++, getPaymentMethodLabel(contract.getPaymentMethod()));
        setCellValue(row, column++, safeText(contract.getPaymentTerms(), ""));
        setCellValue(row, column++, safeText(contract.getPaymentLimitations(), ""));
        setCellValue(row, column++, safeText(contract.getObservations(), ""));
        setCellValue(row, column++, formatDate(contract.getCreatedAt()));
        setCellValue(row, column++, formatDate(contract.getUpdatedAt()));
      }

      autoSizeColumns(sheet, DATASET_HEADERS.length);

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Error generating Excel report", ex);
    }
  }

  public byte[] generateCsv(LocalDate startDate, LocalDate endDate) {
    List<PurchaseSale> contracts = findContracts(startDate, endDate);
    List<PurchaseSaleDetailResponse> details = purchaseSaleDetailService.toDetails(contracts);

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter =
            new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        PrintWriter writer = new PrintWriter(outputStreamWriter)) {
      writer.println("Periodo," + escapeCsvValue(buildPeriodText(startDate, endDate)));
      writer.println();
      writeCsvRow(writer, DATASET_HEADERS);

      if (details.isEmpty()) {
        writer.println(escapeCsvValue("No existen registros para el periodo seleccionado."));
      } else {
        for (PurchaseSaleDetailResponse contract : details) {
          writeCsvRow(writer, buildCsvRow(contract));
        }
      }

      writer.flush();
      return outputStream.toByteArray();
    } catch (Exception ex) {
      throw new IllegalStateException("Error generating CSV report", ex);
    }
  }

  private List<PurchaseSale> findContracts(LocalDate startDate, LocalDate endDate) {
    return purchaseSaleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .filter(contract -> filterByDateRange(contract.getCreatedAt(), startDate, endDate))
        .toList();
  }

  private boolean filterByDateRange(LocalDateTime value, LocalDate startDate, LocalDate endDate) {
    if (value == null) {
      return false;
    }
    LocalDate date = value.toLocalDate();
    boolean afterStart = startDate == null || !date.isBefore(startDate);
    boolean beforeEnd = endDate == null || !date.isAfter(endDate);
    return afterStart && beforeEnd;
  }

  private PdfPTable buildPdfTable(List<PurchaseSaleDetailResponse> contracts) {
    float[] columnWidths = {1.5f, 2.0f, 1.7f, 1.9f, 1.9f, 1.6f};
    PdfPTable table = new PdfPTable(columnWidths);
    table.setWidthPercentage(100);

    String[] headers = {
      "Contrato", "Cliente", "Usuario responsable", "Vehículo", "Condiciones financieras", "Fechas"
    };

    for (String header : headers) {
      PdfPCell headerCell =
          new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
      headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
      headerCell.setPaddingTop(10f);
      headerCell.setPaddingBottom(10f);
      headerCell.setPaddingLeft(6f);
      headerCell.setPaddingRight(6f);
      headerCell.setBackgroundColor(new java.awt.Color(242, 242, 242));
      headerCell.setBorder(Rectangle.BOX);
      table.addCell(headerCell);
    }

    for (PurchaseSaleDetailResponse contract : contracts) {
      addCell(table, formatContractBlock(contract));
      addCell(table, formatClientBlock(contract.getClientSummary()));
      addCell(table, formatUserBlock(contract.getUserSummary()));
      addCell(table, formatVehicleBlock(contract.getVehicleSummary()));
      addCell(table, formatFinanceBlock(contract));
      addCell(table, formatTimelineBlock(contract));
    }

    if (contracts.isEmpty()) {
      PdfPCell emptyCell =
          new PdfPCell(new Phrase("No existen registros para el periodo seleccionado."));
      emptyCell.setColspan(headers.length);
      emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
      emptyCell.setPadding(12f);
      table.addCell(emptyCell);
    }

    return table;
  }

  private void addCell(PdfPTable table, String value) {
    Paragraph paragraph =
        new Paragraph(value != null ? value : "", FontFactory.getFont(FontFactory.HELVETICA, 9));
    paragraph.setMultipliedLeading(1.3f);
    paragraph.setSpacingBefore(2f);
    paragraph.setSpacingAfter(4f);

    PdfPCell cell = new PdfPCell();
    cell.addElement(paragraph);
    cell.setPaddingTop(10f);
    cell.setPaddingBottom(10f);
    cell.setPaddingLeft(8f);
    cell.setPaddingRight(8f);
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    table.addCell(cell);
  }

  private String formatDate(LocalDateTime dateTime) {
    if (dateTime == null) {
      return "";
    }

    return dateTime
        .atZone(SOURCE_ZONE)
        .withZoneSameInstant(TARGET_ZONE)
        .format(DATE_TIME_FORMATTER);
  }

  private String buildPeriodText(LocalDate startDate, LocalDate endDate) {
    if (startDate == null && endDate == null) {
      return "Periodo: todos los registros disponibles";
    }

    String start = startDate != null ? startDate.format(DateTimeFormatter.ISO_DATE) : "...";
    String end = endDate != null ? endDate.format(DateTimeFormatter.ISO_DATE) : "...";
    return "Periodo: " + start + " - " + end;
  }

  private String formatContractBlock(PurchaseSaleDetailResponse contract) {
    return LABEL_TIPO
        + getContractTypeLabel(contract.getContractType())
        + '\n'
        + "Estado: "
        + getStatusLabel(contract.getContractStatus())
        + '\n'
        + "Método de pago: "
        + getPaymentMethodLabel(contract.getPaymentMethod())
        + '\n'
        + "Observaciones: "
        + safeText(contract.getObservations(), "Sin observaciones");
  }

  private String formatClientBlock(ClientSummary clientSummary) {
    if (clientSummary == null) {
      return "Nombre: N/D";
    }

    return "Nombre: "
        + safeText(clientSummary.getName(), "No registrado")
        + '\n'
        + LABEL_TIPO
        + safeText(getClientTypeLabel(clientSummary.getType()), "N/D")
        + '\n'
        + "Documento: "
        + safeText(clientSummary.getIdentifier(), "N/D")
        + '\n'
        + "Email: "
        + safeText(clientSummary.getEmail(), "N/D")
        + '\n'
        + "Teléfono: "
        + formatPhoneNumber(clientSummary.getPhoneNumber());
  }

  private String formatUserBlock(UserSummary userSummary) {
    if (userSummary == null) {
      return "Gestor: N/D";
    }

    return "Gestor: "
        + safeText(userSummary.getFullName(), "Sin asignar")
        + '\n'
        + "Usuario: @"
        + safeText(userSummary.getUsername(), "N/D")
        + '\n'
        + "Email: "
        + safeText(userSummary.getEmail(), "N/D");
  }

  private String formatVehicleBlock(VehicleSummary vehicleSummary) {
    if (vehicleSummary == null) {
      return "Vehículo: N/D";
    }

    return LABEL_TIPO
        + safeText(getVehicleTypeLabel(vehicleSummary.getType()), "N/D")
        + '\n'
        + "Marca: "
        + safeText(vehicleSummary.getBrand(), "Marca")
        + '\n'
        + "Línea: "
        + safeText(vehicleSummary.getLine(), "N/D")
        + '\n'
        + "Modelo: "
        + safeText(vehicleSummary.getModel(), "Modelo")
        + '\n'
        + "Placa: "
        + safeText(vehicleSummary.getPlate(), "N/D")
        + '\n'
        + "Estado: "
        + safeText(vehicleSummary.getStatus(), "N/D");
  }

  private String formatFinanceBlock(PurchaseSaleDetailResponse contract) {
    return "Precio compra: "
        + formatCurrency(contract.getPurchasePrice())
        + '\n'
        + "Precio venta: "
        + formatCurrency(contract.getSalePrice())
        + '\n'
        + "Términos: "
        + safeText(contract.getPaymentTerms(), "No especificados")
        + '\n'
        + "Limitaciones: "
        + safeText(contract.getPaymentLimitations(), "Sin restricciones");
  }

  private String formatTimelineBlock(PurchaseSaleDetailResponse contract) {
    return "Creado: "
        + formatDate(contract.getCreatedAt())
        + '\n'
        + "Actualizado: "
        + formatDate(contract.getUpdatedAt());
  }

  private String formatCurrency(Double value) {
    if (value == null) {
      return "N/D";
    }
    NumberFormat formatter = NumberFormat.getCurrencyInstance(REPORT_LOCALE);
    formatter.setMaximumFractionDigits(2);
    formatter.setMinimumFractionDigits(0);
    return formatter.format(value);
  }

  private String formatDecimal(Double value) {
    if (value == null) {
      return "";
    }
    return String.format(Locale.US, "%.2f", value);
  }

  private String formatPhoneNumber(Long phoneNumber) {
    return phoneNumber == null ? "N/D" : String.valueOf(phoneNumber);
  }

  private String getClientTypeLabel(String type) {
    if (type == null) {
      return "";
    }
    return switch (type.toUpperCase()) {
      case "PERSON" -> "Persona";
      case "COMPANY" -> "Empresa";
      default -> type;
    };
  }

  private String getVehicleTypeLabel(String type) {
    if (type == null) {
      return "";
    }
    return switch (type.toUpperCase()) {
      case "CAR" -> "Automóvil";
      case "MOTORCYCLE" -> "Motocicleta";
      default -> type;
    };
  }

  private String safeText(String value, String fallback) {
    return (value == null || value.isBlank()) ? fallback : value;
  }

  private String getClientName(ClientSummary summary) {
    return summary == null ? "" : safeText(summary.getName(), "");
  }

  private String getClientIdentifier(ClientSummary summary) {
    return summary == null ? "" : safeText(summary.getIdentifier(), "");
  }

  private String getClientEmail(ClientSummary summary) {
    return summary == null ? "" : safeText(summary.getEmail(), "");
  }

  private String getClientPhone(ClientSummary summary) {
    return summary == null || summary.getPhoneNumber() == null
        ? ""
        : String.valueOf(summary.getPhoneNumber());
  }

  private String getUserFullName(UserSummary summary) {
    return summary == null ? "" : safeText(summary.getFullName(), "");
  }

  private String getUsername(UserSummary summary) {
    return summary == null ? "" : safeText(summary.getUsername(), "");
  }

  private String getUserEmail(UserSummary summary) {
    return summary == null ? "" : safeText(summary.getEmail(), "");
  }

  private String getVehicleBrand(VehicleSummary summary) {
    return summary == null ? "" : safeText(summary.getBrand(), "");
  }

  private String getVehicleLine(VehicleSummary summary) {
    return summary == null ? "" : safeText(summary.getLine(), "");
  }

  private String getVehicleModel(VehicleSummary summary) {
    return summary == null ? "" : safeText(summary.getModel(), "");
  }

  private String getVehiclePlate(VehicleSummary summary) {
    return summary == null ? "" : safeText(summary.getPlate(), "");
  }

  private String getVehicleStatus(VehicleSummary summary) {
    return summary == null ? "" : safeText(summary.getStatus(), "");
  }

  private void initialiseLabels() {
    statusLabels.put(ContractStatus.PENDING, "Pendiente");
    statusLabels.put(ContractStatus.ACTIVE, "Activa");
    statusLabels.put(ContractStatus.COMPLETED, "Completada");
    statusLabels.put(ContractStatus.CANCELED, "Cancelada");

    typeLabels.put(ContractType.PURCHASE, "Compra");
    typeLabels.put(ContractType.SALE, "Venta");

    paymentMethodLabels.put(PaymentMethod.CASH, "Efectivo");
    paymentMethodLabels.put(PaymentMethod.BANK_TRANSFER, "Transferencia bancaria");
    paymentMethodLabels.put(PaymentMethod.BANK_DEPOSIT, "Consignación bancaria");
    paymentMethodLabels.put(PaymentMethod.CASHIERS_CHECK, "Cheque de gerencia");
    paymentMethodLabels.put(PaymentMethod.MIXED, "Pago combinado");
    paymentMethodLabels.put(PaymentMethod.FINANCING, "Financiación");
    paymentMethodLabels.put(PaymentMethod.DIGITAL_WALLET, "Billetera digital");
    paymentMethodLabels.put(PaymentMethod.TRADE_IN, "Permuta");
    paymentMethodLabels.put(PaymentMethod.INSTALLMENT_PAYMENT, "Pago a plazos");
  }

  private String getStatusLabel(ContractStatus status) {
    return status == null ? "" : statusLabels.getOrDefault(status, status.name());
  }

  private String getContractTypeLabel(ContractType contractType) {
    return contractType == null ? "" : typeLabels.getOrDefault(contractType, contractType.name());
  }

  private String getPaymentMethodLabel(PaymentMethod paymentMethod) {
    return paymentMethod == null
        ? ""
        : paymentMethodLabels.getOrDefault(paymentMethod, paymentMethod.name());
  }

  private void autoSizeColumns(Sheet sheet, int columnCount) {
    for (int i = 0; i < columnCount; i++) {
      try {
        sheet.autoSizeColumn(i);
      } catch (Exception ex) {
        sheet.setColumnWidth(i, 20 * 256);
      }
    }
  }

  private void setCellValue(Row row, int columnIndex, String value) {
    row.createCell(columnIndex).setCellValue(value == null ? "" : value);
  }

  private void setNumericCellValue(Row row, int columnIndex, Double value) {
    if (value == null) {
      row.createCell(columnIndex).setBlank();
    } else {
      row.createCell(columnIndex).setCellValue(value);
    }
  }

  private void writeCsvRow(PrintWriter writer, String... values) {
    String row = Arrays.stream(values).map(this::escapeCsvValue).collect(Collectors.joining(","));
    writer.println(row);
  }

  private String[] buildCsvRow(PurchaseSaleDetailResponse contract) {
    ClientSummary client = contract.getClientSummary();
    UserSummary user = contract.getUserSummary();
    VehicleSummary vehicle = contract.getVehicleSummary();
    return new String[] {
      getContractTypeLabel(contract.getContractType()),
      getStatusLabel(contract.getContractStatus()),
      getClientName(client),
      safeText(getClientTypeLabel(client == null ? null : client.getType()), ""),
      getClientIdentifier(client),
      getClientEmail(client),
      getClientPhone(client),
      getUserFullName(user),
      getUsername(user),
      getUserEmail(user),
      getVehicleBrand(vehicle),
      getVehicleLine(vehicle),
      getVehicleModel(vehicle),
      getVehiclePlate(vehicle),
      safeText(getVehicleTypeLabel(vehicle == null ? null : vehicle.getType()), ""),
      getVehicleStatus(vehicle),
      formatDecimal(contract.getPurchasePrice()),
      formatDecimal(contract.getSalePrice()),
      getPaymentMethodLabel(contract.getPaymentMethod()),
      safeText(contract.getPaymentTerms(), ""),
      safeText(contract.getPaymentLimitations(), ""),
      safeText(contract.getObservations(), ""),
      formatDate(contract.getCreatedAt()),
      formatDate(contract.getUpdatedAt())
    };
  }

  private String escapeCsvValue(String value) {
    if (value == null) {
      return "\"\"";
    }
    String sanitized = value.replace("\"", "\"\"");
    return "\"" + sanitized + "\"";
  }
}
