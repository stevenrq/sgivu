package com.sgivu.vehicle.service.impl;

import com.sgivu.vehicle.service.S3Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@Service
public class S3ServiceImpl implements S3Service {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  @Value("${spring.destination.folder}")
  private String destinationFolder;

  public S3ServiceImpl(S3Client s3Client, S3Presigner s3Presigner) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
  }

  @Override
  public String createBucket(String bucket) {
    CreateBucketResponse createBucketResponse =
        s3Client.createBucket(createBucketRequest -> createBucketRequest.bucket(bucket));

    return "Bucket creado correctamente en " + createBucketResponse.location();
  }

  @Override
  public String checkIfBucketExists(String bucket) {
    try {
      s3Client.headBucket(headBucketRequest -> headBucketRequest.bucket(bucket));
      return ("El bucket " + bucket + " existe");
    } catch (S3Exception e) {
      return "El bucket no existe";
    }
  }

  @Override
  public List<String> getAllBuckets() {
    ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
    if (listBucketsResponse.hasBuckets()) {
      return listBucketsResponse.buckets().stream().map(Bucket::name).toList();
    } else {
      return List.of();
    }
  }

  @Override
  public Boolean uploadFile(String bucket, String key, Path fileLocation) {
    PutObjectRequest putObjectRequest = PutObjectRequest.builder().bucket(bucket).key(key).build();
    PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, fileLocation);

    return putObjectResponse.sdkHttpResponse().isSuccessful();
  }

  @Override
  public void downloadFile(String bucket, String key) throws IOException {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucket).key(key).build();

    ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(getObjectRequest);

    String filename = key.contains("/") ? key.substring(key.lastIndexOf("/") + 1) : key;

    Path filePath = Paths.get(destinationFolder, filename);
    File parentDir = filePath.getParent().toFile();

    if (!parentDir.exists() && !parentDir.mkdirs()) {
      throw new IOException("Could not create directory: " + parentDir.getAbsolutePath());
    }

    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
      fos.write(bytes.asByteArray());
    } catch (IOException e) {
      throw new IOException("Error downloading file from S3", e);
    }
  }

  @Override
  public String generatePresignedUploadUrl(
      String bucket, String key, Duration duration, String contentType) {
    PresignedPutObjectRequest presignedRequest =
        s3Presigner.presignPutObject(
            builder ->
                builder
                    .signatureDuration(duration)
                    .putObjectRequest(po -> po.bucket(bucket).key(key).contentType(contentType)));

    return presignedRequest.url().toString();
  }

  @Override
  public String generatePresignedDownloadUrl(String bucket, String key, Duration duration) {
    PresignedGetObjectRequest presignedRequest =
        s3Presigner.presignGetObject(
            builder ->
                builder
                    .signatureDuration(duration)
                    .getObjectRequest(go -> go.bucket(bucket).key(key)));

    return presignedRequest.url().toString();
  }

  @Override
  public void deleteObject(String bucket, String key) {
    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(bucket).key(key).build();
    s3Client.deleteObject(deleteObjectRequest);
  }
}
