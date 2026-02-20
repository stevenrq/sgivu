package com.sgivu.client.controller.api;

import com.sgivu.client.dto.CompanyResponse;
import com.sgivu.client.entity.Company;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Empresas", description = "Operaciones CRUD y búsqueda de empresas cliente")
@RequestMapping("/v1/companies")
public interface CompanyApi {

  @Operation(summary = "Alta de empresa", description = "Registra una nueva empresa cliente.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Empresa creada exitosamente",
            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "No autorizado",
            content = @Content)
      })
  @PostMapping
  ResponseEntity<CompanyResponse> create(
      @Parameter(description = "Datos de la empresa a registrar", required = true)
          @Valid
          @RequestBody
          Company company,
      BindingResult bindingResult);

  @Operation(summary = "Consultar empresa por ID", description = "Recupera una empresa por su ID")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Empresa encontrada",
            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No encontrada",
            content = @Content)
      })
  @GetMapping("/{id}")
  ResponseEntity<CompanyResponse> getById(
      @Parameter(description = "ID de la empresa", required = true) @PathVariable Long id);

  @Operation(summary = "Listar empresas", description = "Retorna la lista completa de empresas")
  @GetMapping
  ResponseEntity<List<CompanyResponse>> getAll();

  @Operation(summary = "Listar empresas paginadas", description = "Retorna empresas paginadas")
  @GetMapping("/page/{page}")
  ResponseEntity<Page<CompanyResponse>> getAllPaginated(
      @Parameter(description = "Número de página (base 0)") @PathVariable Integer page);

  @Operation(summary = "Actualizar empresa", description = "Actualiza una empresa existente")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Empresa actualizada",
            content = @Content(schema = @Schema(implementation = CompanyResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Datos inválidos",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No encontrada",
            content = @Content)
      })
  @PutMapping("/{id}")
  ResponseEntity<CompanyResponse> update(
      @Parameter(description = "ID de la empresa", required = true) @PathVariable Long id,
      @Parameter(description = "Datos para actualizar", required = true) @Valid @RequestBody
          Company company,
      BindingResult bindingResult);

  @Operation(summary = "Eliminar empresa", description = "Elimina una empresa por su ID")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "204",
            description = "Eliminada",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No encontrada",
            content = @Content)
      })
  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteById(
      @Parameter(description = "ID de la empresa", required = true) @PathVariable Long id);

  @Operation(summary = "Cambiar estado", description = "Habilita/Deshabilita empresa")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Estado actualizado",
            content = @Content(schema = @Schema(implementation = java.util.Map.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No encontrada",
            content = @Content)
      })
  @PatchMapping("/{id}/status")
  ResponseEntity<java.util.Map<String, Boolean>> changeStatus(
      @Parameter(description = "ID de la empresa", required = true) @PathVariable Long id,
      @RequestBody boolean enabled);

  @Operation(
      summary = "Contadores de empresas",
      description = "Obtiene totales, activos e inactivos")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Contadores obtenidos",
            content = @Content(schema = @Schema(implementation = java.util.Map.class)))
      })
  @GetMapping("/count")
  ResponseEntity<java.util.Map<String, Long>> getCompanyCounts();

  @Operation(
      summary = "Búsqueda de empresas",
      description = "Búsqueda por múltiples criterios opcionales")
  @GetMapping("/search")
  ResponseEntity<List<CompanyResponse>> searchCompanies(
      @RequestParam(required = false) String taxId,
      @RequestParam(required = false) String companyName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city);

  @GetMapping("/search/page/{page}")
  ResponseEntity<Page<CompanyResponse>> searchCompaniesPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String taxId,
      @RequestParam(required = false) String companyName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city);
}
