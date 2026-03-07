package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sgivu.vehicle.dto.VehicleImageConfirmUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadResponse;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.entity.VehicleImage;
import com.sgivu.vehicle.repository.VehicleBaseRepository;
import com.sgivu.vehicle.repository.VehicleImageRepository;
import com.sgivu.vehicle.service.S3Service;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class VehicleImageServiceImplTest {

  @Mock private VehicleBaseRepository vehicleBaseRepository;
  @Mock private VehicleImageRepository vehicleImageRepository;
  @Mock private S3Service s3Service;
  @Mock private S3Client s3Client;

  @InjectMocks private VehicleImageServiceImpl service;

  @BeforeEach
  void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    // establecer el valor del bucket utilizado por el servicio
    Field bucketField = VehicleImageServiceImpl.class.getDeclaredField("bucket");
    bucketField.setAccessible(true);
    bucketField.set(service, "sgivu-vehicles");
  }

  @Nested
  @DisplayName("createPresignedUploadUrl(Long, VehicleImagePresignedUploadRequest)")
  class CreatePresignedUploadUrlTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando el contentType es nulo o vacío")
    void shouldThrowWhenContentTypeNullOrBlank() {
      VehicleImagePresignedUploadRequest nullReq = new VehicleImagePresignedUploadRequest(null);
      VehicleImagePresignedUploadRequest blankReq = new VehicleImagePresignedUploadRequest("");

      assertThrows(
          IllegalArgumentException.class, () -> service.createPresignedUploadUrl(1L, nullReq));
      assertThrows(
          IllegalArgumentException.class, () -> service.createPresignedUploadUrl(1L, blankReq));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el contentType no está permitido")
    void shouldThrowWhenContentTypeNotAllowed() {
      VehicleImagePresignedUploadRequest req = new VehicleImagePresignedUploadRequest("text/plain");

      IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> service.createPresignedUploadUrl(2L, req));

      assertTrue(ex.getMessage().contains("Tipo de imagen no permitido"));
    }

    @Test
    @DisplayName("Debe retornar respuesta prefirmada para tipo de contenido permitido")
    void shouldReturnPresignedResponseForAllowedType() {
      Long vehicleId = 3L;
      String contentType = "image/png";
      VehicleImagePresignedUploadRequest req = new VehicleImagePresignedUploadRequest(contentType);

      when(s3Service.generatePresignedUploadUrl(any(), any(), any(), any()))
          .thenReturn("https://presigned.example/upload");

      VehicleImagePresignedUploadResponse resp = service.createPresignedUploadUrl(vehicleId, req);

      assertEquals("sgivu-vehicles", resp.bucket());
      assertEquals("https://presigned.example/upload", resp.uploadUrl());

      // formato de la key: vehicles/{vehicleId}/{uuid}.{ext}
      assertTrue(resp.key().startsWith("vehicles/" + vehicleId + "/"));
      assertTrue(resp.key().endsWith(".png"));

      // Verificar que se llamó a s3Service con la clave generada y duración de 10 minutos y
      // contentType
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<java.time.Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

      verify(s3Service)
          .generatePresignedUploadUrl(
              eq("sgivu-vehicles"),
              keyCaptor.capture(),
              durationCaptor.capture(),
              contentTypeCaptor.capture());

      assertEquals(contentType, contentTypeCaptor.getValue());
      assertEquals(Duration.ofMinutes(10), durationCaptor.getValue());
      String capturedKey = keyCaptor.getValue();
      assertTrue(capturedKey.startsWith("vehicles/" + vehicleId + "/"));
      assertTrue(capturedKey.endsWith(".png"));

      // Asegurarse de que el UUID exista en la clave
      String uuidPart =
          capturedKey.substring(
              ("vehicles/" + vehicleId + "/").length(), capturedKey.lastIndexOf('.'));
      // debería ser un UUID válido
      UUID.fromString(uuidPart);
    }
  }

  @Nested
  @DisplayName("confirmUpload(Long, VehicleImageConfirmUploadRequest)")
  class ConfirmUploadTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando el vehículo no se encuentra")
    void shouldThrowWhenVehicleNotFound() {
      var req =
          new VehicleImageConfirmUploadRequest(
              "file.jpg", "image/png", 123L, "vehicles/1/file.jpg", Boolean.FALSE);

      when(vehicleBaseRepository.findById(1L)).thenReturn(java.util.Optional.empty());

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(1L, req));
      assertTrue(ex.getMessage().contains("Vehicle no encontrado"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la key es nula o vacía")
    void shouldThrowWhenKeyNullOrBlank() {
      var vehicle = new Car();
      vehicle.setId(2L);
      when(vehicleBaseRepository.findById(2L)).thenReturn(java.util.Optional.of(vehicle));

      var req1 =
          new VehicleImageConfirmUploadRequest("file.jpg", "image/png", 123L, null, Boolean.FALSE);
      var req2 =
          new VehicleImageConfirmUploadRequest("file.jpg", "image/png", 123L, "", Boolean.FALSE);

      assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(2L, req1));
      assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(2L, req2));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el prefijo de la key es inválido para el vehículo")
    void shouldThrowWhenKeyHasInvalidPrefix() {
      var vehicle = new Car();
      vehicle.setId(3L);
      when(vehicleBaseRepository.findById(3L)).thenReturn(java.util.Optional.of(vehicle));

      var req =
          new VehicleImageConfirmUploadRequest(
              "file.jpg", "image/png", 123L, "other/3/file.jpg", Boolean.FALSE);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(3L, req));
      assertTrue(ex.getMessage().contains("Key inválida"));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando S3 reporta key no encontrada")
    void shouldThrowWhenS3KeyNotFound() {
      var vehicle = new Car();
      vehicle.setId(4L);
      when(vehicleBaseRepository.findById(4L)).thenReturn(java.util.Optional.of(vehicle));

      var req =
          new VehicleImageConfirmUploadRequest(
              "file.jpg", "image/png", 123L, "vehicles/4/file.jpg", Boolean.FALSE);

      when(s3Client.headObject(any(HeadObjectRequest.class)))
          .thenThrow(NoSuchKeyException.builder().message("no").build());

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(4L, req));
      assertTrue(ex.getMessage().contains("Key no encontrada"));
    }

    @Test
    @DisplayName("Debe eliminar objeto y lanzar excepción cuando el nombre de archivo ya existe")
    void shouldDeleteAndThrowWhenFileNameExists() {
      Long vehicleId = 5L;
      var vehicle = new Car();
      vehicle.setId(vehicleId);
      when(vehicleBaseRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));

      var req =
          new VehicleImageConfirmUploadRequest(
              "file.jpg", "image/png", 123L, "vehicles/5/file.jpg", Boolean.FALSE);

      when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
      when(vehicleImageRepository.existsByVehicleIdAndFileName(vehicleId, "file.jpg"))
          .thenReturn(true);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(vehicleId, req));

      assertTrue(ex.getMessage().contains("Ya existe una imagen con el mismo nombre"));
      verify(s3Service).deleteObject(eq("sgivu-vehicles"), eq("vehicles/5/file.jpg"));
      verify(vehicleImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe eliminar objeto y lanzar excepción cuando la key ya existe")
    void shouldDeleteAndThrowWhenKeyExists() {
      Long vehicleId = 6L;
      var vehicle = new Car();
      vehicle.setId(vehicleId);
      when(vehicleBaseRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));

      var req =
          new VehicleImageConfirmUploadRequest(
              "file2.jpg", "image/png", 123L, "vehicles/6/file2.jpg", Boolean.FALSE);

      when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
      when(vehicleImageRepository.existsByVehicleIdAndFileName(vehicleId, "file2.jpg"))
          .thenReturn(false);
      when(vehicleImageRepository.existsByKey("vehicles/6/file2.jpg")).thenReturn(true);

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.confirmUpload(vehicleId, req));

      assertTrue(ex.getMessage().contains("Ya existe una imagen registrada"));
      verify(s3Service).deleteObject(eq("sgivu-vehicles"), eq("vehicles/6/file2.jpg"));
      verify(vehicleImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe guardar imagen y marcar como principal cuando el vehículo no tiene imágenes")
    void shouldSaveImageAndMarkPrimaryWhenNoExistingImages() {
      Long vehicleId = 7L;
      var vehicle = new Car();
      vehicle.setId(vehicleId);
      when(vehicleBaseRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));

      var req =
          new VehicleImageConfirmUploadRequest(
              "file3.jpg", "image/png", 123L, "vehicles/7/file3.jpg", Boolean.FALSE);

      when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
      when(vehicleImageRepository.existsByVehicleIdAndFileName(vehicleId, "file3.jpg"))
          .thenReturn(false);
      when(vehicleImageRepository.existsByKey("vehicles/7/file3.jpg")).thenReturn(false);
      when(vehicleImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      var saved = service.confirmUpload(vehicleId, req);

      assertEquals("sgivu-vehicles", saved.getBucket());
      assertEquals("vehicles/7/file3.jpg", saved.getKey());
      assertEquals("file3.jpg", saved.getFileName());
      assertTrue(saved.isPrimaryImage());
      verify(vehicleImageRepository).save(any());
    }

    @Test
    @DisplayName(
        "Debe desmarcar imágenes principales anteriores cuando la nueva imagen es principal")
    void shouldUnsetPreviousPrimaryWhenNewIsPrimary() {
      Long vehicleId = 8L;
      var vehicle = new Car();
      vehicle.setId(vehicleId);

      var existingImg = new VehicleImage();
      existingImg.setPrimaryImage(true);
      existingImg.setVehicle(vehicle);
      vehicle.getImages().add(existingImg);

      when(vehicleBaseRepository.findById(vehicleId)).thenReturn(java.util.Optional.of(vehicle));

      var req =
          new VehicleImageConfirmUploadRequest(
              "file4.jpg", "image/png", 123L, "vehicles/8/file4.jpg", Boolean.TRUE);

      when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(null);
      when(vehicleImageRepository.existsByVehicleIdAndFileName(vehicleId, "file4.jpg"))
          .thenReturn(false);
      when(vehicleImageRepository.existsByKey("vehicles/8/file4.jpg")).thenReturn(false);
      when(vehicleImageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      var saved = service.confirmUpload(vehicleId, req);

      // El anterior no debe configurarse como primario
      assertFalse(existingImg.isPrimaryImage());
      assertTrue(saved.isPrimaryImage());
      verify(vehicleImageRepository).save(any());
    }
  }

  @Nested
  @DisplayName("deleteImage(Long)")
  class DeleteImageTests {

    @Test
    @DisplayName("Debe lanzar excepción cuando la imagen no se encuentra")
    void shouldThrowWhenImageNotFound() {
      Long imageId = 10L;
      when(vehicleImageRepository.findById(imageId)).thenReturn(java.util.Optional.empty());

      IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> service.deleteImage(imageId));
      assertTrue(ex.getMessage().contains("Imagen no encontrada"));
      verify(s3Service, never()).deleteObject(any(), any());
      verify(vehicleImageRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debe eliminar imagen no principal sin promover")
    void shouldDeleteNonPrimaryImage() {
      Long imageId = 11L;
      var vehicle = new Car();
      vehicle.setId(11L);
      VehicleImage image = new VehicleImage();
      image.setId(imageId);
      image.setBucket("sgivu-vehicles");
      image.setKey("vehicles/11/file.jpg");
      image.setPrimaryImage(false);
      image.setVehicle(vehicle);

      when(vehicleImageRepository.findById(imageId)).thenReturn(java.util.Optional.of(image));

      service.deleteImage(imageId);

      verify(s3Service).deleteObject("sgivu-vehicles", "vehicles/11/file.jpg");
      verify(vehicleImageRepository).delete(image);
      verify(vehicleImageRepository, never())
          .findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(any());
      verify(vehicleImageRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName(
        "Debe promover siguiente imagen principal cuando la imagen eliminada era principal y quedan"
            + " imágenes")
    void shouldPromoteNextPrimaryWhenDeletedImageIsPrimaryAndRemainingNotEmpty() {
      Long imageId = 12L;
      var vehicle = new Car();
      vehicle.setId(12L);
      VehicleImage image = new VehicleImage();
      image.setId(imageId);
      image.setBucket("sgivu-vehicles");
      image.setKey("vehicles/12/primary.jpg");
      image.setPrimaryImage(true);
      image.setVehicle(vehicle);

      VehicleImage next = new VehicleImage();
      next.setId(21L);
      next.setPrimaryImage(false);
      next.setBucket("sgivu-vehicles");
      next.setKey("vehicles/12/next.jpg");
      next.setVehicle(vehicle);

      VehicleImage other = new VehicleImage();
      other.setId(22L);
      other.setPrimaryImage(true);
      other.setBucket("sgivu-vehicles");
      other.setKey("vehicles/12/other.jpg");
      other.setVehicle(vehicle);

      java.util.LinkedList<VehicleImage> remaining = new java.util.LinkedList<>();
      remaining.add(next);
      remaining.add(other);

      when(vehicleImageRepository.findById(imageId)).thenReturn(java.util.Optional.of(image));
      when(vehicleImageRepository.findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(12L))
          .thenReturn(remaining);

      service.deleteImage(imageId);

      verify(s3Service).deleteObject("sgivu-vehicles", "vehicles/12/primary.jpg");
      verify(vehicleImageRepository).delete(image);
      verify(vehicleImageRepository).findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(12L);
      verify(vehicleImageRepository).saveAll(remaining);

      assertTrue(remaining.getFirst().isPrimaryImage());
      assertFalse(remaining.get(1).isPrimaryImage());
    }
  }
}
