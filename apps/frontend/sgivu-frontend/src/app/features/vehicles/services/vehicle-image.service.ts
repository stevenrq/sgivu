import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpBackend, HttpClient, HttpResponse } from '@angular/common/http';
import { VehicleImageResponse } from '../models/vehicle-image-response';
import {
  VehicleImagePresignedUploadRequest,
  VehicleImagePresignedUploadResponse,
} from '../models/vehicle-image-presigned-upload';
import { VehicleImageConfirmUploadRequest } from '../models/vehicle-image-confirm-upload';
import { defer, from } from 'rxjs';
import { map } from 'rxjs/operators';

/**
 * Servicio de bajo nivel para el flujo de subida de imágenes a S3 en 3 pasos:
 * 1. Solicitar presigned URL al backend
 * 2. Subir el archivo directamente a S3
 * 3. Confirmar la subida al backend para registrar la imagen
 */
@Injectable({
  providedIn: 'root',
})
export class VehicleImageService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = `${environment.apiUrl}/v1/vehicles`;

  /**
   * HttpClient sin interceptores. Se usa para subidas a S3 porque las presigned URLs
   * rechazan headers de autenticación que el `defaultOAuthInterceptor` añade.
   */
  private readonly rawHttp: HttpClient = new HttpClient(inject(HttpBackend));

  getImages(vehicleId: number) {
    return this.http.get<VehicleImageResponse[]>(
      `${this.apiUrl}/${vehicleId}/images`,
    );
  }

  createPresignedUploadUrl(vehicleId: number, contentType: string) {
    const body: VehicleImagePresignedUploadRequest = { contentType };
    return this.http.post<VehicleImagePresignedUploadResponse>(
      `${this.apiUrl}/${vehicleId}/images/presigned-upload`,
      body,
    );
  }

  uploadToPresignedUrl(url: string, file: File, contentType: string) {
    return defer(() => from(this.uploadWithFetch(url, file, contentType))).pipe(
      map((resp) => resp.body ?? ''),
    );
  }

  confirmUpload(vehicleId: number, payload: VehicleImageConfirmUploadRequest) {
    return this.http.post(
      `${this.apiUrl}/${vehicleId}/images/confirm-upload`,
      payload,
    );
  }

  deleteImage(vehicleId: number, imageId: number) {
    return this.http.delete<void>(
      `${this.apiUrl}/${vehicleId}/images/${imageId}`,
    );
  }

  /**
   * Usa `fetch` nativo en vez de `HttpClient` porque las presigned URLs de S3 requieren
   * `credentials: 'omit'` y `mode: 'cors'`, que `HttpClient` no permite configurar.
   */
  private async uploadWithFetch(
    url: string,
    file: File,
    contentType: string,
  ): Promise<HttpResponse<string>> {
    const response = await fetch(url, {
      method: 'PUT',
      headers: {
        'Content-Type': contentType,
      },
      body: file,
      credentials: 'omit',
      mode: 'cors',
      cache: 'no-store',
    });

    const textBody = await response.text();

    if (!response.ok) {
      const error = new Error('Fallo en la subida de la imagen') as Error & {
        status?: number;
        error?: string;
      };
      error.status = response.status;
      error.error = textBody;
      throw error;
    }

    return new HttpResponse({
      status: response.status,
      body: textBody,
    });
  }
}
