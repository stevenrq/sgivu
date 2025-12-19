package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sgivu.vehicle.dto.VehicleImageConfirmUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadResponse;
import com.sgivu.vehicle.dto.VehicleImageResponse;
import com.sgivu.vehicle.entity.Car;
import com.sgivu.vehicle.entity.Vehicle;
import com.sgivu.vehicle.entity.VehicleImage;
import com.sgivu.vehicle.repository.VehicleBaseRepository;
import com.sgivu.vehicle.repository.VehicleImageRepository;
import com.sgivu.vehicle.service.S3Service;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class VehicleImageServiceImplTests {

  private static final String BUCKET = "vehicles-bucket";

  @Mock private VehicleBaseRepository vehicleBaseRepository;
  @Mock private VehicleImageRepository vehicleImageRepository;
  @Mock private S3Service s3Service;
  @Mock private S3Client s3Client;

  @InjectMocks private VehicleImageServiceImpl vehicleImageService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(vehicleImageService, "bucket", BUCKET);
  }

  @Test
  void createPresignedUploadUrl_shouldRejectMissingContentType() {
    VehicleImagePresignedUploadRequest request =
        new VehicleImagePresignedUploadRequest("   "); // contenido en blanco

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> vehicleImageService.createPresignedUploadUrl(1L, request));

    assertEquals("contentType es requerido para generar la URL.", exception.getMessage());
  }

  @Test
  void createPresignedUploadUrl_shouldRejectUnsupportedType() {
    VehicleImagePresignedUploadRequest request =
        new VehicleImagePresignedUploadRequest("image/gif"); // no permitido

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> vehicleImageService.createPresignedUploadUrl(2L, request));

    assertEquals("Tipo de imagen no permitido: image/gif", exception.getMessage());
  }

  @Test
  void createPresignedUploadUrl_shouldGenerateKeyWithPrefixAndExtension() {
    when(s3Service.generatePresignedUploadUrl(
            eq(BUCKET), anyString(), eq(Duration.ofMinutes(10)), eq("image/jpeg")))
        .thenReturn("https://s3/presigned");

    VehicleImagePresignedUploadResponse response =
        vehicleImageService.createPresignedUploadUrl(
            123L, new VehicleImagePresignedUploadRequest("image/jpeg"));

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3Service)
        .generatePresignedUploadUrl(
            eq(BUCKET), keyCaptor.capture(), eq(Duration.ofMinutes(10)), eq("image/jpeg"));

    String generatedKey = keyCaptor.getValue();
    assertEquals(BUCKET, response.bucket());
    assertEquals(generatedKey, response.key());
    assertTrue(generatedKey.startsWith("vehicles/123/"));
    assertTrue(generatedKey.endsWith(".jpg"));
    assertEquals("https://s3/presigned", response.uploadUrl());
  }

  @Test
  void confirmUpload_shouldThrowWhenVehicleNotFound() {
    when(vehicleBaseRepository.findById(99L)).thenReturn(Optional.empty());
    VehicleImageConfirmUploadRequest request =
        new VehicleImageConfirmUploadRequest(
            "imagen.jpg", "image/jpeg", 10L, "vehicles/99/imagen.jpg", false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> vehicleImageService.confirmUpload(99L, request));

    assertEquals("Vehicle no encontrado: 99", exception.getMessage());
  }

  @Test
  void confirmUpload_shouldThrowWhenKeyNotBelongingToVehicle() {
    Vehicle vehicle = buildVehicle(10L);
    when(vehicleBaseRepository.findById(10L)).thenReturn(Optional.of(vehicle));

    VehicleImageConfirmUploadRequest request =
        new VehicleImageConfirmUploadRequest(
            "imagen.jpg", "image/jpeg", 10L, "otro/imagen.jpg", false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> vehicleImageService.confirmUpload(10L, request));

    assertEquals("Key inválida para este vehículo: otro/imagen.jpg", exception.getMessage());
    verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
  }

  @Test
  void confirmUpload_shouldThrowWhenKeyIsMissingInS3() {
    Vehicle vehicle = buildVehicle(5L);
    when(vehicleBaseRepository.findById(5L)).thenReturn(Optional.of(vehicle));
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenThrow(NoSuchKeyException.builder().message("missing").build());

    VehicleImageConfirmUploadRequest request =
        new VehicleImageConfirmUploadRequest(
            "frente.jpg", "image/jpeg", 10L, "vehicles/5/frente.jpg", false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> vehicleImageService.confirmUpload(5L, request));

    assertEquals(
        "Key no encontrada para este vehículo: vehicles/5/frente.jpg", exception.getMessage());
    verify(s3Service, never()).deleteObject(anyString(), anyString());
  }

  @Test
  void confirmUpload_shouldDeleteS3ObjectWhenFileNameExists() {
    Vehicle vehicle = buildVehicle(7L);
    when(vehicleBaseRepository.findById(7L)).thenReturn(Optional.of(vehicle));
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().build());
    when(vehicleImageRepository.existsByVehicleIdAndFileName(7L, "frente.jpg")).thenReturn(true);

    VehicleImageConfirmUploadRequest request =
        new VehicleImageConfirmUploadRequest(
            "frente.jpg", "image/jpeg", 10L, "vehicles/7/frente.jpg", false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> vehicleImageService.confirmUpload(7L, request));

    assertEquals(
        "Ya existe una imagen con el mismo nombre de archivo para este vehículo.",
        exception.getMessage());
    verify(s3Service).deleteObject(BUCKET, "vehicles/7/frente.jpg");
    verify(vehicleImageRepository, never()).save(any(VehicleImage.class));
  }

  @Test
  void confirmUpload_shouldPersistPrimaryAndDemoteExisting() {
    Vehicle vehicle = buildVehicle(5L);
    VehicleImage currentPrimary = buildVehicleImage(vehicle, true);
    VehicleImage secondary = buildVehicleImage(vehicle, false);
    vehicle.getImages().addAll(List.of(currentPrimary, secondary));

    when(vehicleBaseRepository.findById(5L)).thenReturn(Optional.of(vehicle));
    when(s3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().build());
    when(vehicleImageRepository.save(any(VehicleImage.class)))
        .thenAnswer(
            invocation -> {
              VehicleImage image = invocation.getArgument(0);
              image.setId(99L);
              return image;
            });

    VehicleImageConfirmUploadRequest request =
        new VehicleImageConfirmUploadRequest(
            "nueva.jpg", "image/jpeg", 100L, "vehicles/5/nueva.jpg", true);

    VehicleImage result = vehicleImageService.confirmUpload(5L, request);

    assertEquals(99L, result.getId());
    assertEquals(BUCKET, result.getBucket());
    assertEquals("vehicles/5/nueva.jpg", result.getKey());
    assertEquals("nueva.jpg", result.getFileName());
    assertTrue(result.isPrimaryImage());
    assertFalse(currentPrimary.isPrimaryImage());
    assertFalse(secondary.isPrimaryImage());
    verify(vehicleImageRepository).save(any(VehicleImage.class));
    verify(s3Service, never()).deleteObject(anyString(), anyString());
  }

  @Test
  void getImagesByVehicle_shouldReturnPresignedUrlsOrdered() {
    VehicleImage primaryImage = new VehicleImage();
    primaryImage.setId(1L);
    primaryImage.setBucket(BUCKET);
    primaryImage.setKey("vehicles/1/primaria.jpg");
    primaryImage.setPrimaryImage(true);

    VehicleImage secondaryImage = new VehicleImage();
    secondaryImage.setId(2L);
    secondaryImage.setBucket(BUCKET);
    secondaryImage.setKey("vehicles/1/secundaria.jpg");
    secondaryImage.setPrimaryImage(false);

    when(vehicleImageRepository.findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(1L))
        .thenReturn(List.of(primaryImage, secondaryImage));
    when(s3Service.generatePresignedDownloadUrl(
            BUCKET, "vehicles/1/primaria.jpg", Duration.ofMinutes(15)))
        .thenReturn("url-primaria");
    when(s3Service.generatePresignedDownloadUrl(
            BUCKET, "vehicles/1/secundaria.jpg", Duration.ofMinutes(15)))
        .thenReturn("url-secundaria");

    List<VehicleImageResponse> responses = vehicleImageService.getImagesByVehicle(1L);

    assertEquals(2, responses.size());
    assertEquals(new VehicleImageResponse(1L, "url-primaria", true), responses.getFirst());
    assertEquals(new VehicleImageResponse(2L, "url-secundaria", false), responses.getLast());
  }

  @Test
  void deleteImage_shouldReassignPrimaryWhenNeeded() {
    Vehicle vehicle = buildVehicle(1L);
    VehicleImage imageToDelete = buildVehicleImage(vehicle, true);
    imageToDelete.setId(10L);
    imageToDelete.setBucket(BUCKET);
    imageToDelete.setKey("vehicles/1/primaria.jpg");

    VehicleImage nextPrimary = buildVehicleImage(vehicle, false);
    VehicleImage another = buildVehicleImage(vehicle, true);

    when(vehicleImageRepository.findById(10L)).thenReturn(Optional.of(imageToDelete));
    doNothing().when(s3Service).deleteObject(BUCKET, "vehicles/1/primaria.jpg");
    when(vehicleImageRepository.findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(1L))
        .thenReturn(List.of(nextPrimary, another));
    when(vehicleImageRepository.saveAll(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    vehicleImageService.deleteImage(10L);

    assertTrue(nextPrimary.isPrimaryImage());
    assertFalse(another.isPrimaryImage());
    verify(vehicleImageRepository).delete(imageToDelete);
    verify(vehicleImageRepository).saveAll(List.of(nextPrimary, another));
  }

  @Test
  void deleteImage_shouldThrowWhenImageNotFound() {
    when(vehicleImageRepository.findById(50L)).thenReturn(Optional.empty());

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> vehicleImageService.deleteImage(50L));

    assertEquals("Imagen no encontrada: 50", exception.getMessage());
    verify(s3Service, never()).deleteObject(anyString(), anyString());
    verify(vehicleImageRepository, never()).delete(any(VehicleImage.class));
  }

  private Vehicle buildVehicle(Long id) {
    Car car = new Car();
    car.setId(id);
    return car;
  }

  private VehicleImage buildVehicleImage(Vehicle vehicle, boolean primary) {
    VehicleImage image = new VehicleImage();
    image.setVehicle(vehicle);
    image.setPrimaryImage(primary);
    return image;
  }
}
