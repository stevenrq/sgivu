package com.sgivu.vehicle.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class S3ServiceImplTest {

  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;

  @InjectMocks private S3ServiceImpl s3Service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  @DisplayName("generatePresignedUploadUrl(String, String, Duration, String)")
  class GeneratePresignedUploadUrlTests {

    @Test
    @DisplayName("Debe retornar URL prefirmada cuando el pre-firmador tiene éxito")
    void shouldReturnPresignedUrlWhenPresignerSucceeds() throws Exception {
      PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
      URL expected = URI.create("https://example.com/upload").toURL();
      when(presigned.url()).thenReturn(expected);
      when(s3Presigner.presignPutObject(Mockito.<Consumer<PutObjectPresignRequest.Builder>>any()))
          .thenReturn(presigned);

      String result =
          s3Service.generatePresignedUploadUrl(
              "my-bucket", "path/to/key", Duration.ofMinutes(5), "text/plain");

      assertEquals(expected.toString(), result);
      verify(s3Presigner)
          .presignPutObject(Mockito.<Consumer<PutObjectPresignRequest.Builder>>any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando el pre-firmador falla")
    void shouldPropagateExceptionWhenPresignerFails() {
      when(s3Presigner.presignPutObject(Mockito.<Consumer<PutObjectPresignRequest.Builder>>any()))
          .thenThrow(new RuntimeException("Presign error"));

      assertThrows(
          RuntimeException.class,
          () ->
              s3Service.generatePresignedUploadUrl(
                  "my-bucket", "path/to/key", Duration.ofMinutes(5), "text/plain"));

      verify(s3Presigner)
          .presignPutObject(Mockito.<Consumer<PutObjectPresignRequest.Builder>>any());
    }
  }

  @Nested
  @DisplayName("generatePresignedDownloadUrl(String, String, Duration)")
  class GeneratePresignedDownloadUrlTests {

    @Test
    @DisplayName("Debe retornar URL prefirmada cuando el pre-firmador tiene éxito")
    void shouldReturnPresignedDownloadUrlWhenPresignerSucceeds() throws Exception {
      PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      URL expected = URI.create("https://example.com/download").toURL();
      when(presigned.url()).thenReturn(expected);
      when(s3Presigner.presignGetObject(Mockito.<Consumer<GetObjectPresignRequest.Builder>>any()))
          .thenReturn(presigned);

      String result =
          s3Service.generatePresignedDownloadUrl("my-bucket", "path/to/key", Duration.ofMinutes(5));

      assertEquals(expected.toString(), result);
      verify(s3Presigner)
          .presignGetObject(Mockito.<Consumer<GetObjectPresignRequest.Builder>>any());
    }

    @Test
    @DisplayName("Debe propagar excepción cuando el pre-firmador falla")
    void shouldPropagateExceptionWhenPresignerFailsForDownload() {
      when(s3Presigner.presignGetObject(Mockito.<Consumer<GetObjectPresignRequest.Builder>>any()))
          .thenThrow(new RuntimeException("Presign error"));

      assertThrows(
          RuntimeException.class,
          () ->
              s3Service.generatePresignedDownloadUrl(
                  "my-bucket", "path/to/key", Duration.ofMinutes(5)));

      verify(s3Presigner)
          .presignGetObject(Mockito.<Consumer<GetObjectPresignRequest.Builder>>any());
    }
  }

  @Nested
  @DisplayName("uploadFile(String, String, Path)")
  class UploadFileTests {

    @Test
    @DisplayName("Debe retornar true cuando la respuesta de putObject es exitosa")
    void shouldReturnTrueWhenPutObjectSuccessful() {
      var response = Mockito.mock(PutObjectResponse.class, Mockito.RETURNS_DEEP_STUBS);
      when(response.sdkHttpResponse().isSuccessful()).thenReturn(true);
      when(s3Client.putObject(any(PutObjectRequest.class), eq(Path.of("dummy"))))
          .thenReturn(response);

      Boolean result = s3Service.uploadFile("bucket", "key", Path.of("dummy"));

      assertTrue(result);
      verify(s3Client).putObject(any(PutObjectRequest.class), eq(Path.of("dummy")));
    }

    @Test
    @DisplayName("Debe retornar false cuando la respuesta de putObject no es exitosa")
    void shouldReturnFalseWhenPutObjectNotSuccessful() {
      var response = Mockito.mock(PutObjectResponse.class, Mockito.RETURNS_DEEP_STUBS);
      when(response.sdkHttpResponse().isSuccessful()).thenReturn(false);
      when(s3Client.putObject(any(PutObjectRequest.class), eq(Path.of("dummy"))))
          .thenReturn(response);

      Boolean result = s3Service.uploadFile("bucket", "key", Path.of("dummy"));

      assertFalse(result);
      verify(s3Client).putObject(any(PutObjectRequest.class), eq(Path.of("dummy")));
    }

    @Test
    @DisplayName("Debe propagar excepción cuando putObject lanza excepción")
    void shouldPropagateExceptionWhenPutObjectThrows() {
      when(s3Client.putObject(any(PutObjectRequest.class), eq(Path.of("dummy"))))
          .thenThrow(new RuntimeException("S3 error"));

      assertThrows(
          RuntimeException.class, () -> s3Service.uploadFile("bucket", "key", Path.of("dummy")));
      verify(s3Client).putObject(any(PutObjectRequest.class), eq(Path.of("dummy")));
    }
  }
}
