package com.sgivu.vehicle.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface S3Service {
  String createBucket(String bucket);

  String checkIfBucketExists(String bucket);

  List<String> getAllBuckets();

  Boolean uploadFile(String bucket, String key, Path fileLocation);

  void downloadFile(String bucket, String key) throws IOException;

  String generatePresignedUploadUrl(
      String bucket, String key, Duration duration, String contentType);

  String generatePresignedDownloadUrl(String bucket, String key, Duration duration);

  void deleteObject(String bucket, String key);
}
