package com.sgivu.purchasesale.controller.api;

import com.sgivu.purchasesale.dto.PurchaseSaleDetailResponse;
import com.sgivu.purchasesale.dto.PurchaseSaleRequest;
import com.sgivu.purchasesale.dto.PurchaseSaleResponse;
import com.sgivu.purchasesale.enums.ContractStatus;
import com.sgivu.purchasesale.enums.ContractType;
import com.sgivu.purchasesale.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "purchase-sale", description = "Gestión de contratos de compras y ventas")
@RequestMapping("/v1/purchase-sales")
public interface PurchaseSaleApi {

  @Operation(
      summary = "Crear contrato",
      description = "Crea un contrato de compra o venta.",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Creado",
            content = @Content(schema = @Schema(implementation = PurchaseSaleResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Prohibido")
      })
  @PostMapping
  ResponseEntity<PurchaseSaleResponse> create(
      @Valid @RequestBody PurchaseSaleRequest purchaseSaleRequest);

  @Operation(
      summary = "Obtener contrato por ID",
      description = "Recupera un contrato con datos enriquecidos.")
  @ApiResponse(
      responseCode = "200",
      description = "OK",
      content = @Content(schema = @Schema(implementation = PurchaseSaleDetailResponse.class)))
  @ApiResponse(responseCode = "404", description = "No encontrado")
  @ApiResponse(responseCode = "401", description = "No autorizado")
  @GetMapping("/{id}")
  ResponseEntity<PurchaseSaleDetailResponse> getById(@PathVariable Long id);

  @Operation(
      summary = "Listar contratos",
      description = "Lista todos los contratos en formato simple.")
  @GetMapping
  ResponseEntity<List<PurchaseSaleResponse>> getAll();

  @Operation(
      summary = "Listar contratos detallados",
      description = "Lista contratos enriquecidos con cliente, usuario y vehículo.")
  @GetMapping("/detailed")
  ResponseEntity<List<PurchaseSaleDetailResponse>> getAllDetailed();

  @GetMapping("/page/{page}")
  ResponseEntity<Page<PurchaseSaleResponse>> getByPage(@PathVariable Integer page);

  @GetMapping("/page/{page}/detailed")
  ResponseEntity<Page<PurchaseSaleDetailResponse>> getDetailedPage(@PathVariable Integer page);

  @Operation(
      summary = "Buscar contratos",
      description =
          "Búsqueda paginada con múltiples filtros que devuelve contratos detallados o simples"
              + " según flag.")
  @GetMapping("/search")
  ResponseEntity<Page<PurchaseSaleDetailResponse>> searchContracts(
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
      @RequestParam(required = false) String term);

  @PutMapping("/{id}")
  ResponseEntity<PurchaseSaleResponse> update(
      @PathVariable Long id, @Valid @RequestBody PurchaseSaleRequest purchaseSaleRequest);

  @Operation(
      summary = "Eliminar contrato",
      description = "Elimina un contrato por su ID.",
      responses = {
        @ApiResponse(responseCode = "204", description = "Contrato eliminado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Prohibido"),
        @ApiResponse(responseCode = "404", description = "Contrato no encontrado")
      })
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteById(@PathVariable Long id);

  @GetMapping("/client/{clientId}")
  ResponseEntity<List<PurchaseSaleResponse>> getByClientId(@PathVariable Long clientId);

  @GetMapping("/user/{userId}")
  ResponseEntity<List<PurchaseSaleResponse>> getByUserId(@PathVariable Long userId);

  @GetMapping("/vehicle/{vehicleId}")
  ResponseEntity<List<PurchaseSaleDetailResponse>> getByVehicleId(@PathVariable Long vehicleId);

  @Operation(
      summary = "Exportar reporte PDF",
      description = "Genera y descarga un reporte en PDF para el rango de fechas indicado.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "PDF generado",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_PDF_VALUE,
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Prohibido")
      })
  @GetMapping(value = "/report/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  ResponseEntity<byte[]> exportPdfReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate);

  @Operation(
      summary = "Exportar reporte Excel",
      description =
          "Genera y descarga un reporte en formato XLSX para el rango de fechas indicado.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "XLSX generado",
            content =
                @Content(
                    mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Prohibido")
      })
  @GetMapping(
      value = "/report/excel",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  ResponseEntity<byte[]> exportExcelReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate);

  @Operation(
      summary = "Exportar reporte CSV",
      description = "Genera y descarga un CSV con los datos solicitados.",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "CSV generado",
            content =
                @Content(
                    mediaType = "text/csv",
                    schema = @Schema(type = "string", format = "binary"))),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Prohibido")
      })
  @GetMapping(value = "/report/csv", produces = "text/csv")
  ResponseEntity<byte[]> exportCsvReport(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate);
}
