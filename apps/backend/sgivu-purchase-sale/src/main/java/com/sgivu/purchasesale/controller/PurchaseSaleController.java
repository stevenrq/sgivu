package com.sgivu.purchasesale.controller;

import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.PurchaseSaleFilterCriteria;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.PurchaseSaleResponse;
import com.sgivu.purchasesale.entity.PurchaseSale;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import com.sgivu.purchasesale.mapper.PurchaseSaleMapper;
import com.sgivu.purchasesale.service.PurchaseSaleDetailService;
import com.sgivu.purchasesale.service.PurchaseSaleReportService;
import com.sgivu.purchasesale.service.PurchaseSaleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API REST para gestionar contratos de compra y venta. Expone operaciones CRUD, búsqueda paginada
 * con múltiples filtros y exportación de reportes en diferentes formatos. También delega la
 * construcción de respuestas detalladas que combinan información de usuarios, clientes y vehículos.
 */
@RestController
@RequestMapping("/v1/purchase-sales")
public class PurchaseSaleController {

  private final PurchaseSaleService purchaseSaleService;
  private final PurchaseSaleMapper purchaseSaleMapper;
  private final PurchaseSaleReportService purchaseSaleReportService;
  private final PurchaseSaleDetailService purchaseSaleDetailService;

  public PurchaseSaleController(
      PurchaseSaleService purchaseSaleService,
      PurchaseSaleMapper purchaseSaleMapper,
      PurchaseSaleReportService purchaseSaleReportService,
      PurchaseSaleDetailService purchaseSaleDetailService) {
    this.purchaseSaleService = purchaseSaleService;
    this.purchaseSaleMapper = purchaseSaleMapper;
    this.purchaseSaleReportService = purchaseSaleReportService;
    this.purchaseSaleDetailService = purchaseSaleDetailService;
  }

  /**
   * Crea un contrato de compra o venta validando inventario, cliente y usuario en servicios
   * externos. El detalle del vehículo puede ser enviado solo para compras; las ventas deben
   * referenciar un vehículo ya adquirido.
   *
   * @apiNote Delegado en {@link PurchaseSaleService#create} para aplicar reglas de inventario y
   *     estados de contrato. Este endpoint utiliza JWT para asociar el gestor responsable.
   */
  @PostMapping
  @PreAuthorize("hasAuthority('purchase_sale:create')")
  public ResponseEntity<PurchaseSaleResponse> create(
      @Valid @RequestBody PurchaseSaleRequest purchaseSaleRequest) {
    PurchaseSaleResponse response =
        purchaseSaleMapper.toPurchaseSaleResponse(purchaseSaleService.create(purchaseSaleRequest));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /**
   * Recupera un contrato con datos enriquecidos de cliente, usuario e inventario desde servicios
   * externos. Útil para vistas de detalle y auditorías.
   *
   * @param id identificador del contrato
   * @return detalle completo o 404 cuando no existe
   */
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<PurchaseSaleDetailResponse> getById(@PathVariable Long id) {
    return purchaseSaleService
        .findById(id)
        .map(contract -> ResponseEntity.ok(purchaseSaleDetailService.toDetail(contract)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Lista los contratos en formato simple, pensado para listados rápidos o integraciones que no
   * requieren datos enriquecidos.
   */
  @GetMapping
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleResponse>> getAll() {
    List<PurchaseSaleResponse> responses =
        purchaseSaleService.findAll().stream()
            .map(purchaseSaleMapper::toPurchaseSaleResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }

  /**
   * Lista los contratos con enriquecimiento de cliente, usuario y vehículo, disparando llamadas a
   * microservicios externos y aplicando caché intra-request para reducir latencia.
   */
  @GetMapping("/detailed")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleDetailResponse>> getAllDetailed() {
    return ResponseEntity.ok(purchaseSaleDetailService.toDetails(purchaseSaleService.findAll()));
  }

  /**
   * Paginación básica sin enriquecer datos externos; útil para tableros que resuelven detalles vía
   * llamadas adicionales al front.
   */
  @GetMapping("/page/{page}")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<Page<PurchaseSaleResponse>> getByPage(@PathVariable Integer page) {
    Page<PurchaseSaleResponse> pagedResponse =
        purchaseSaleService
            .findAll(PageRequest.of(page, 10))
            .map(purchaseSaleMapper::toPurchaseSaleResponse);
    return ResponseEntity.ok(pagedResponse);
  }

  /**
   * Paginación con enriquecimiento de datos externos. Ideal para reportes operativos donde se
   * necesitan nombres de clientes/usuarios y metadatos de inventario.
   */
  @GetMapping("/page/{page}/detailed")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<Page<PurchaseSaleDetailResponse>> getDetailedPage(
      @PathVariable Integer page) {
    var pageable = PageRequest.of(page, 10);
    var pagedContracts = purchaseSaleService.findAll(pageable);
    return ResponseEntity.ok(toDetailPage(pagedContracts));
  }

  /**
   * Realiza una búsqueda paginada aplicando filtros opcionales sobre tipo/estado de contrato,
   * responsables, clientes, vehículo, rango de fechas y precios, así como un término libre que
   * consulta múltiples campos. Devuelve las respuestas enriquecidas con detalles relacionados.
   *
   * @param page índice de página (0-based)
   * @param size tamaño de página
   * @param contractType filtro por tipo de contrato
   * @param contractStatus filtro por estado
   * @param clientId filtro por cliente
   * @param userId filtro por usuario responsable
   * @param vehicleId filtro por vehículo
   * @param paymentMethod filtro por método de pago
   * @param startDate fecha mínima de actualización
   * @param endDate fecha máxima de actualización
   * @param minPurchasePrice precio mínimo de compra
   * @param maxPurchasePrice precio máximo de compra
   * @param minSalePrice precio mínimo de venta
   * @param maxSalePrice precio máximo de venta
   * @param term término de búsqueda libre
   * @return página de contratos detallados
   */
  @GetMapping("/search")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<Page<PurchaseSaleDetailResponse>> searchContracts(
      @RequestParam(defaultValue = "0") Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(defaultValue = "true") boolean detailed,
      @RequestParam(required = false) ContractType contractType,
      @RequestParam(required = false) ContractStatus contractStatus,
      @RequestParam(required = false) Long clientId,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long vehicleId,
      @RequestParam(required = false) PaymentMethod paymentMethod,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(required = false) Double minPurchasePrice,
      @RequestParam(required = false) Double maxPurchasePrice,
      @RequestParam(required = false) Double minSalePrice,
      @RequestParam(required = false) Double maxSalePrice,
      @RequestParam(required = false) String term) {

    var pageable = PageRequest.of(page, size);
    var criteria =
        PurchaseSaleFilterCriteria.builder()
            .contractType(contractType)
            .contractStatus(contractStatus)
            .clientId(clientId)
            .userId(userId)
            .vehicleId(vehicleId)
            .paymentMethod(paymentMethod)
            .startDate(startDate)
            .endDate(endDate)
            .minPurchasePrice(minPurchasePrice)
            .maxPurchasePrice(maxPurchasePrice)
            .minSalePrice(minSalePrice)
            .maxSalePrice(maxSalePrice)
            .term(trimToNull(term))
            .build();

    var filteredContracts = purchaseSaleService.search(criteria, pageable);
    if (!detailed) {
      var simplePage = filteredContracts.map(purchaseSaleMapper::toPurchaseSaleDetailResponse);
      return ResponseEntity.ok(simplePage);
    }
    return ResponseEntity.ok(toDetailPage(filteredContracts));
  }

  /**
   * Actualiza un contrato existente conservando su tipo original y revalidando los datos externos.
   *
   * @param id identificador del contrato a modificar
   * @param purchaseSaleRequest datos nuevos del contrato
   * @return contrato actualizado o 404 si no existe
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase_sale:update')")
  public ResponseEntity<PurchaseSaleResponse> update(
      @PathVariable Long id, @Valid @RequestBody PurchaseSaleRequest purchaseSaleRequest) {
    return purchaseSaleService
        .update(id, purchaseSaleRequest)
        .map(
            purchaseSale ->
                ResponseEntity.ok(purchaseSaleMapper.toPurchaseSaleResponse(purchaseSale)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Elimina un contrato por su identificador cuando existe. Opera de forma idempotente retornando
   * 404 si el recurso ya no se encuentra.
   *
   * @param id identificador del contrato
   * @return 204 en caso de eliminación exitosa o 404 si no existe
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase_sale:delete')")
  public ResponseEntity<Void> deleteById(@PathVariable Long id) {
    return purchaseSaleService
        .findById(id)
        .map(
            purchaseSale -> {
              purchaseSaleService.deleteById(id);
              return ResponseEntity.noContent().<Void>build();
            })
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Lista las operaciones realizadas por un cliente específico (persona o empresa).
   *
   * @param clientId identificador del cliente
   * @return lista de contratos asociados
   */
  @GetMapping("/client/{clientId}")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleResponse>> getByClientId(@PathVariable Long clientId) {
    List<PurchaseSaleResponse> responses =
        purchaseSaleService.findByClientId(clientId).stream()
            .map(purchaseSaleMapper::toPurchaseSaleResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }

  /**
   * Obtiene las operaciones gestionadas por un usuario interno específico (responsable comercial).
   */
  @GetMapping("/user/{userId}")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleResponse>> getByUserId(@PathVariable Long userId) {
    List<PurchaseSaleResponse> responses =
        purchaseSaleService.findByUserId(userId).stream()
            .map(purchaseSaleMapper::toPurchaseSaleResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }

  /**
   * Busca las operaciones asociadas a un vehículo, permitiendo diagnosticar su ciclo completo de
   * compra y venta dentro del inventario de usados.
   */
  @GetMapping("/vehicle/{vehicleId}")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleDetailResponse>> getByVehicleId(
      @PathVariable Long vehicleId) {
    List<PurchaseSaleDetailResponse> responses =
        purchaseSaleDetailService.toDetails(purchaseSaleService.findByVehicleId(vehicleId));
    return ResponseEntity.ok(responses);
  }

  /**
   * Genera un reporte PDF opcionalmente limitado por fechas. Sirve para distribución ejecutiva,
   * incluyendo totales y datos completos de cada contrato en el rango.
   *
   * @param startDate fecha mínima del reporte (opcional)
   * @param endDate fecha máxima del reporte (opcional)
   * @return archivo PDF listo para descarga
   */
  @GetMapping(value = "/report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<byte[]> exportPdfReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    byte[] report = purchaseSaleReportService.generatePdf(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("pdf"))
        .contentType(MediaType.valueOf(MediaType.APPLICATION_PDF_VALUE))
        .body(report);
  }

  /**
   * Genera el reporte en Excel, útil para análisis en hojas de cálculo.
   *
   * @param startDate fecha mínima del reporte (opcional)
   * @param endDate fecha máxima del reporte (opcional)
   * @return archivo XLSX listo para descarga
   */
  @GetMapping(
      value = "/report/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<byte[]> exportExcelReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    byte[] report = purchaseSaleReportService.generateExcel(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("xlsx"))
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(report);
  }

  /**
   * Produce un CSV plano para integraciones externas o cargas masivas.
   *
   * @param startDate fecha mínima del reporte (opcional)
   * @param endDate fecha máxima del reporte (opcional)
   * @return archivo CSV con los datos solicitados
   */
  @GetMapping(value = "/report/csv", produces = "text/csv")
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<byte[]> exportCsvReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate) {
    byte[] report = purchaseSaleReportService.generateCsv(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("csv"))
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(report);
  }

  /**
   * Construye el encabezado `Content-Disposition` para los reportes descargables.
   *
   * @param extension extensión del archivo (pdf/xlsx/csv)
   * @return encabezado listo para anexar en la respuesta HTTP
   */
  private String buildContentDisposition(String extension) {
    String timestamp = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    return "attachment; filename=\"reporte-compras-ventas-" + timestamp + "." + extension + "\"";
  }

  /**
   * Convierte una página de entidades en una página de detalles enriquecidos.
   *
   * @param contracts página original de contratos
   * @return página con DTOs detallados
   */
  private Page<PurchaseSaleDetailResponse> toDetailPage(Page<PurchaseSale> contracts) {
    List<PurchaseSaleDetailResponse> detailed =
        Objects.requireNonNull(purchaseSaleDetailService.toDetails(contracts.getContent()));
    return new PageImpl<>(detailed, contracts.getPageable(), contracts.getTotalElements());
  }

  /**
   * Normaliza cadenas para evitar filtros con espacios o cadenas vacías.
   *
   * @param value texto recibido desde query params
   * @return {@code null} si no contiene caracteres significativos; el texto recortado en caso
   *     contrario
   */
  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
