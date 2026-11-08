import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import {
  PaymentSummary,
  PaymentValidationRequest,
  PaymentValidationResponse,
} from '../../model/paiement.model';

interface PaymentValidationState {
  canValidate: boolean;
  isFriday: boolean;
  currentDay: string;
  weekStartDate: string;
  weekEndDate: string;
  message: string;
  nextFriday?: string;
}

interface ValidateAndDownloadResponse {
  success: boolean;
  validation: PaymentValidationResponse;
  excel: {
    fileName: string;
    data: string; // Base64
    size: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class PaiementService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/payments`;

  /**
   * ✅ Vérifie si on peut valider les paiements (vendredi uniquement)
   */
  checkCanValidatePayments(): Observable<PaymentValidationState> {
    return this.http.get<PaymentValidationState>(`${this.apiUrl}/can-validate`);
  }

  /**
   * 📊 Récupère le récapitulatif de paiement de la semaine en cours
   */
  getPaymentSummary(startDate: string, endDate: string): Observable<PaymentSummary> {
    return this.http.get<PaymentSummary>(`${this.apiUrl}/summary`);
  }

  /**
   * ✅ Valide et enregistre le paiement (ancienne méthode)
   */
  validatePayment(request: PaymentValidationRequest): Observable<PaymentValidationResponse> {
    return this.http.post<PaymentValidationResponse>(`${this.apiUrl}/validate`, request);
  }

  /**
   * 🎯 NOUVELLE: Valide le paiement ET télécharge automatiquement le rapport Excel
   */
  validatePaymentAndDownload(
    request: PaymentValidationRequest
  ): Observable<ValidateAndDownloadResponse> {
    return this.http
      .post<ValidateAndDownloadResponse>(`${this.apiUrl}/validate-and-download`, request)
      .pipe(
        tap((response) => {
          if (response.success && response.excel) {
            this.downloadExcelFromBase64(response.excel.fileName, response.excel.data);
          }
        })
      );
  }

  /**
   * 📥 Télécharge le fichier Excel depuis une chaîne Base64
   */
  private downloadExcelFromBase64(fileName: string, base64Data: string): void {
    try {
      // Décoder Base64
      const binaryString = window.atob(base64Data);
      const bytes = new Uint8Array(binaryString.length);
      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      // Créer un Blob
      const blob = new Blob([bytes], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });

      // Déclencher le téléchargement
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);

      console.log('✅ Fichier Excel téléchargé:', fileName);
    } catch (error) {
      console.error('❌ Erreur téléchargement Excel:', error);
    }
  }

  /**
   * 📅 Récupère la date du dernier paiement
   */
  getLastPaymentDate(): Observable<{ lastPaymentDate: string | null }> {
    return this.http.get<{ lastPaymentDate: string | null }>(`${this.apiUrl}/last-payment-date`);
  }

  /**
   * 💱 Formate un montant
   */
  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  }
}
