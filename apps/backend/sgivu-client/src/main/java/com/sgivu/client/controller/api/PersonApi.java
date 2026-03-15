package com.sgivu.client.controller.api;

import com.sgivu.client.dto.PersonResponse;
import com.sgivu.client.entity.Person;
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

@Tag(name = "Personas", description = "Operaciones CRUD y búsqueda de personas cliente")
@RequestMapping("/v1/persons")
public interface PersonApi {

  @Operation(summary = "Alta de persona", description = "Registra una nueva persona cliente.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Persona creada exitosamente",
            content = @Content(schema = @Schema(implementation = PersonResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Datos de entrada inválidos",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "No autorizado",
            content = @Content)
      })
  @PostMapping
  ResponseEntity<PersonResponse> create(
      @Parameter(description = "Datos de la persona a registrar", required = true)
          @Valid
          @RequestBody
          Person person,
      BindingResult bindingResult);

  @Operation(summary = "Consultar persona por ID", description = "Recupera una persona por su ID")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Persona encontrada",
            content = @Content(schema = @Schema(implementation = PersonResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "No encontrada",
            content = @Content)
      })
  @GetMapping("/{id}")
  ResponseEntity<PersonResponse> getById(
      @Parameter(description = "ID de la persona", required = true) @PathVariable Long id);

  @Operation(summary = "Listar personas", description = "Retorna la lista completa de personas")
  @GetMapping
  ResponseEntity<List<PersonResponse>> getAll();

  @Operation(summary = "Listar personas paginadas", description = "Retorna personas paginadas")
  @GetMapping("/page/{page}")
  ResponseEntity<Page<PersonResponse>> getAllPaginated(
      @Parameter(description = "Número de página (base 0)") @PathVariable Integer page);

  @Operation(summary = "Actualizar persona", description = "Actualiza una persona existente")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Persona actualizada",
            content = @Content(schema = @Schema(implementation = PersonResponse.class))),
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
  ResponseEntity<PersonResponse> update(
      @Parameter(description = "ID de la persona", required = true) @PathVariable Long id,
      @Parameter(description = "Datos para actualizar", required = true) @Valid @RequestBody
          Person person,
      BindingResult bindingResult);

  @Operation(summary = "Eliminar persona", description = "Elimina una persona por su ID")
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
      @Parameter(description = "ID de la persona", required = true) @PathVariable Long id);

  @Operation(summary = "Cambiar estado", description = "Habilita/Deshabilita persona")
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
      @Parameter(description = "ID de la persona", required = true) @PathVariable Long id,
      @RequestBody boolean enabled);

  @Operation(
      summary = "Contadores de personas",
      description = "Obtiene totales, activos e inactivos")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Contadores obtenidos",
            content = @Content(schema = @Schema(implementation = java.util.Map.class)))
      })
  @GetMapping("/count")
  ResponseEntity<java.util.Map<String, Long>> getPersonCounts();

  @Operation(
      summary = "Búsqueda de personas",
      description = "Búsqueda por múltiples criterios opcionales")
  @GetMapping("/search")
  ResponseEntity<List<PersonResponse>> searchPersons(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long nationalId,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city);

  @GetMapping("/search/page/{page}")
  ResponseEntity<Page<PersonResponse>> searchPersonsPaginated(
      @PathVariable Integer page,
      @RequestParam(defaultValue = "10") Integer size,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) Long nationalId,
      @RequestParam(required = false) Long phoneNumber,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(required = false) String city);
}
