package com.sgivu.purchasesale.controller;

import com.sgivu.purchasesale.controller.api.PurchaseSaleApi;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PurchaseSaleController implements PurchaseSaleApi {

  private final Environment env;

  private final PurchaseSaleService purchaseSaleService;
  private final PurchaseSaleMapper purchaseSaleMapper;
  private final PurchaseSaleReportService purchaseSaleReportService;
  private final PurchaseSaleDetailService purchaseSaleDetailService;

  public PurchaseSaleController(
      Environment env,
      PurchaseSaleService purchaseSaleService,
      PurchaseSaleMapper purchaseSaleMapper,
      PurchaseSaleReportService purchaseSaleReportService,
      PurchaseSaleDetailService purchaseSaleDetailService) {
    this.env = env;
    this.purchaseSaleService = purchaseSaleService;
    this.purchaseSaleMapper = purchaseSaleMapper;
    this.purchaseSaleReportService = purchaseSaleReportService;
    this.purchaseSaleDetailService = purchaseSaleDetailService;
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:create')")
  public ResponseEntity<PurchaseSaleResponse> create(PurchaseSaleRequest purchaseSaleRequest) {
    PurchaseSaleResponse response =
        purchaseSaleMapper.toPurchaseSaleResponse(purchaseSaleService.create(purchaseSaleRequest));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<PurchaseSaleDetailResponse> getById(Long id) {
    return purchaseSaleService
        .findById(id)
        .map(contract -> ResponseEntity.ok(purchaseSaleDetailService.toDetail(contract)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleResponse>> getAll() {
    List<PurchaseSaleResponse> responses =
        purchaseSaleService.findAll().stream()
            .map(purchaseSaleMapper::toPurchaseSaleResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleDetailResponse>> getAllDetailed() {
    return ResponseEntity.ok(purchaseSaleDetailService.toDetails(purchaseSaleService.findAll()));
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<Page<PurchaseSaleResponse>> getByPage(Integer page) {
    Page<PurchaseSaleResponse> pagedResponse =
        purchaseSaleService
            .findAll(PageRequest.of(page, 10))
            .map(purchaseSaleMapper::toPurchaseSaleResponse);
    return ResponseEntity.ok(pagedResponse);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<Page<PurchaseSaleDetailResponse>> getDetailedPage(Integer page) {
    var pageable = PageRequest.of(page, 10);
    var pagedContracts = purchaseSaleService.findAll(pageable);
    return ResponseEntity.ok(toDetailPage(pagedContracts));
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<Page<PurchaseSaleDetailResponse>> searchContracts(
      Integer page,
      Integer size,
      boolean detailed,
      ContractType contractType,
      ContractStatus contractStatus,
      Long clientId,
      Long userId,
      Long vehicleId,
      PaymentMethod paymentMethod,
      LocalDate startDate,
      LocalDate endDate,
      Double minPurchasePrice,
      Double maxPurchasePrice,
      Double minSalePrice,
      Double maxSalePrice,
      String term) {

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

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:update')")
  public ResponseEntity<PurchaseSaleResponse> update(
      Long id, PurchaseSaleRequest purchaseSaleRequest) {
    return purchaseSaleService
        .update(id, purchaseSaleRequest)
        .map(
            purchaseSale ->
                ResponseEntity.ok(purchaseSaleMapper.toPurchaseSaleResponse(purchaseSale)))
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:delete')")
  public ResponseEntity<Void> deleteById(Long id) {
    if (!Arrays.asList(env.getActiveProfiles()).contains("dev")) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    return purchaseSaleService
        .findById(id)
        .map(
            purchaseSale -> {
              purchaseSaleService.deleteById(id);
              return ResponseEntity.noContent().<Void>build();
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleResponse>> getByClientId(Long clientId) {
    List<PurchaseSaleResponse> responses =
        purchaseSaleService.findByClientId(clientId).stream()
            .map(purchaseSaleMapper::toPurchaseSaleResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleResponse>> getByUserId(Long userId) {
    List<PurchaseSaleResponse> responses =
        purchaseSaleService.findByUserId(userId).stream()
            .map(purchaseSaleMapper::toPurchaseSaleResponse)
            .toList();
    return ResponseEntity.ok(responses);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<List<PurchaseSaleDetailResponse>> getByVehicleId(Long vehicleId) {
    List<PurchaseSaleDetailResponse> responses =
        purchaseSaleDetailService.toDetails(purchaseSaleService.findByVehicleId(vehicleId));
    return ResponseEntity.ok(responses);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<byte[]> exportPdfReport(LocalDate startDate, LocalDate endDate) {
    byte[] report = purchaseSaleReportService.generatePdf(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("pdf"))
        .contentType(MediaType.valueOf(MediaType.APPLICATION_PDF_VALUE))
        .body(report);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<byte[]> exportExcelReport(LocalDate startDate, LocalDate endDate) {
    byte[] report = purchaseSaleReportService.generateExcel(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("xlsx"))
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(report);
  }

  @Override
  @PreAuthorize("hasAuthority('purchase_sale:read')")
  public ResponseEntity<byte[]> exportCsvReport(LocalDate startDate, LocalDate endDate) {
    byte[] report = purchaseSaleReportService.generateCsv(startDate, endDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, buildContentDisposition("csv"))
        .contentType(MediaType.parseMediaType("text/csv"))
        .body(report);
  }

  private String buildContentDisposition(String extension) {
    String timestamp = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    return "attachment; filename=\"reporte-compras-ventas-" + timestamp + "." + extension + "\"";
  }

  private Page<PurchaseSaleDetailResponse> toDetailPage(Page<PurchaseSale> contracts) {
    List<PurchaseSaleDetailResponse> detailed =
        Objects.requireNonNull(purchaseSaleDetailService.toDetails(contracts.getContent()));
    return new PageImpl<>(detailed, contracts.getPageable(), contracts.getTotalElements());
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}
