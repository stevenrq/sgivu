package com.sgivu.vehicle.service.impl;

import com.sgivu.vehicle.dto.VehicleImageConfirmUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadRequest;
import com.sgivu.vehicle.dto.VehicleImagePresignedUploadResponse;
import com.sgivu.vehicle.dto.VehicleImageResponse;
import com.sgivu.vehicle.entity.Vehicle;
import com.sgivu.vehicle.entity.VehicleImage;
import com.sgivu.vehicle.repository.VehicleBaseRepository;
import com.sgivu.vehicle.repository.VehicleImageRepository;
import com.sgivu.vehicle.service.S3Service;
import com.sgivu.vehicle.service.VehicleImageService;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Service
@Transactional
public class VehicleImageServiceImpl implements VehicleImageService {

  private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

  private final VehicleBaseRepository vehicleBaseRepository;
  private final VehicleImageRepository vehicleImageRepository;
  private final S3Service s3Service;
  private final S3Client s3Client;

  @Value("${aws.s3.vehicles-bucket}")
  private String bucket;

  public VehicleImageServiceImpl(
      VehicleBaseRepository vehicleBaseRepository,
      VehicleImageRepository vehicleImageRepository,
      S3Service s3Service,
      S3Client s3Client) {
    this.vehicleBaseRepository = vehicleBaseRepository;
    this.vehicleImageRepository = vehicleImageRepository;
    this.s3Service = s3Service;
    this.s3Client = s3Client;
  }

  @Override
  public VehicleImagePresignedUploadResponse createPresignedUploadUrl(
      Long vehicleId, VehicleImagePresignedUploadRequest request) {
    if (request.contentType() == null || request.contentType().isBlank()) {
      throw new IllegalArgumentException("contentType es requerido para generar la URL.");
    }

    if (!ALLOWED_TYPES.contains(request.contentType())) {
      throw new IllegalArgumentException("Tipo de imagen no permitido: " + request.contentType());
    }

    String extension = getExtensionFromContentType(request.contentType());

    // Construir key con convención estable para integraciones: vehicles/{vehicleId}/{uuid}.ext
    String key = "vehicles/" + vehicleId + "/" + UUID.randomUUID() + extension;

    var url =
        s3Service.generatePresignedUploadUrl(
            bucket, key, Duration.ofMinutes(10), request.contentType());

    return new VehicleImagePresignedUploadResponse(bucket, key, url);
  }

  @Override
  public VehicleImage confirmUpload(Long vehicleId, VehicleImageConfirmUploadRequest request) {
    Vehicle vehicle =
        vehicleBaseRepository
            .findById(vehicleId)
            .orElseThrow(() -> new IllegalArgumentException("Vehicle no encontrado: " + vehicleId));

    if (request.key() == null || request.key().isBlank()) {
      throw new IllegalArgumentException(
          "La key es requerida para confirmar la subida de la imagen.");
    }

    if (!request.key().startsWith("vehicles/" + vehicleId + "/")) {
      throw new IllegalArgumentException("Key inválida para este vehículo: " + request.key());
    }

    try {
      HeadObjectRequest headObjectRequest =
          HeadObjectRequest.builder().bucket(bucket).key(request.key()).build();

      s3Client.headObject(headObjectRequest);
    } catch (NoSuchKeyException e) {
      throw new IllegalArgumentException("Key no encontrada para este vehículo: " + request.key());
    }

    if (vehicleImageRepository.existsByVehicleIdAndFileName(vehicleId, request.fileName())) {
      s3Service.deleteObject(
          bucket, request.key()); // limpia subida huérfana para evitar basura en el bucket
      throw new IllegalArgumentException(
          "Ya existe una imagen con el mismo nombre de archivo para este vehículo.");
    }

    if (vehicleImageRepository.existsByKey(request.key())) {
      s3Service.deleteObject(bucket, request.key());
      throw new IllegalArgumentException("Ya existe una imagen registrada con esta clave.");
    }

    boolean isPrimary = Boolean.TRUE.equals(request.primary()) || vehicle.getImages().isEmpty();

    VehicleImage image = new VehicleImage();
    image.setVehicle(vehicle);
    image.setBucket(bucket);
    image.setKey(request.key());
    image.setFileName(request.fileName());
    image.setMimeType(request.contentType());
    image.setSize(request.size());
    image.setPrimaryImage(isPrimary);

    if (isPrimary) {
      vehicle.getImages().forEach(img -> img.setPrimaryImage(false));
    }

    return vehicleImageRepository.save(image);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VehicleImageResponse> getImagesByVehicle(Long vehicleId) {
    List<VehicleImage> images =
        vehicleImageRepository.findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(vehicleId);

    return images.stream()
        .map(
            img -> {
              String downloadUrl =
                  s3Service.generatePresignedDownloadUrl(
                      img.getBucket(), img.getKey(), Duration.ofMinutes(15));
              return new VehicleImageResponse(img.getId(), downloadUrl, img.isPrimaryImage());
            })
        .toList();
  }

  @Override
  public void deleteImage(Long imageId) {
    VehicleImage image =
        vehicleImageRepository
            .findById(imageId)
            .orElseThrow(() -> new IllegalArgumentException("Imagen no encontrada: " + imageId));

    boolean wasPrimary = image.isPrimaryImage();
    Long vehicleId = image.getVehicle().getId();

    s3Service.deleteObject(image.getBucket(), image.getKey());
    vehicleImageRepository.delete(image);

    if (wasPrimary) {
      List<VehicleImage> remaining =
          vehicleImageRepository.findByVehicleIdOrderByPrimaryImageDescCreatedAtAsc(vehicleId);

      if (!remaining.isEmpty()) {
        VehicleImage nextPrimary = remaining.getFirst();
        nextPrimary.setPrimaryImage(true);
        for (int i = 1; i < remaining.size(); i++) {
          remaining.get(i).setPrimaryImage(false);
        }
        vehicleImageRepository.saveAll(remaining);
      }
    }
  }

  private String getExtensionFromContentType(String contentType) {
    return switch (contentType) {
      case "image/jpeg" -> ".jpg";
      case "image/png" -> ".png";
      case "image/webp" -> ".webp";
      default -> "";
    };
  }
}
