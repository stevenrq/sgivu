package com.sgivu.user.controller.api;

import com.sgivu.user.entity.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Permisos", description = "Catálogo de permisos del sistema")
@RequestMapping("/v1/permissions")
public interface PermissionApi {

  @Operation(
      summary = "Listar todos los permisos",
      description =
          "Retorna el catálogo completo de permisos granulares "
              + "para construir matrices de autorización.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Lista de permisos obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado", content = @Content)
      })
  @GetMapping
  ResponseEntity<List<Permission>> getAllPermissions();
}
