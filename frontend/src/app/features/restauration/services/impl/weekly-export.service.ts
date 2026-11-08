import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, map } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import { NotificationService } from '../../../../core/services/impl/notification.service';

/**
 * 📊 Informations sur une semaine disponible
 */
export interface WeekInfo {
  weekNumber: number;
  year: number;
  startDate: string;
  endDate: string;
  label: string;
  totalOrders: number;
  totalAmount: number;
  hasBeenDownloaded: boolean;
  downloadDate?: string;
}

/**
 * 📥 Historique des exports
 */
export interface ExportHistory {
  id: number;
  weekStartDate: string;
  weekEndDate: string;
  exportDate: string;
  fileName: string;
  fileSize: number;
  exportedBy: string;
}

/**
 * 🎯 Réponse de téléchargement
 */
interface DownloadResponse {
  success: boolean;
  excel: {
    fileName: string;
    data: string; // Base64
    size: number;
  };
  weekInfo: {
    startDate: string;
    endDate: string;
    totalOrders: number;
    totalAmount: number;
  };
}

@Injectable({
  providedIn: 'root',
})
export class WeeklyExportService {
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly apiUrl = `${environment.apiUrl}/admin/export`;

  /**
   * 📋 Récupère la liste des semaines disponibles pour export
   */
  getAvailableWeeks(): Observable<WeekInfo[]> {
    return this.http.get<WeekInfo[]>(`${this.apiUrl}/available-weeks`);
  }

  /**
   * 📥 Télécharge le rapport Excel pour une semaine spécifique
   */
  downloadWeeklyReport(startDate: string, endDate: string): Observable<DownloadResponse> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    this.notificationService.info(
      '📥 Téléchargement en cours',
      'Génération du rapport Excel...'
    );

    return this.http
      .get<DownloadResponse>(`${this.apiUrl}/download-week`, { params })
      .pipe(
        tap((response) => {
          if (response.success && response.excel) {
            this.downloadExcelFromBase64(response.excel.fileName, response.excel.data);
            this.notificationService.success(
              '✅ Téléchargement réussi',
              `${response.excel.fileName} (${this.formatFileSize(response.excel.size)})`
            );
          }
        })
      );
  }

  /**
   * 📥 Télécharge le rapport de la semaine en cours
   */
  downloadCurrentWeek(): Observable<DownloadResponse> {
    const today = new Date();
    const monday = this.getMonday(today);
    const friday = this.getFriday(today);

    return this.downloadWeeklyReport(
      this.formatDate(monday),
      this.formatDate(friday)
    );
  }

  /**
   * 📜 Récupère l'historique des exports
   */
  getExportHistory(): Observable<ExportHistory[]> {
    return this.http.get<ExportHistory[]>(`${this.apiUrl}/history`);
  }

  /**
   * 📥 Re-télécharge un export depuis l'historique
   */
  redownloadFromHistory(historyId: number): Observable<DownloadResponse> {
    this.notificationService.info(
      '📥 Re-téléchargement',
      'Récupération du rapport...'
    );

    return this.http
      .get<DownloadResponse>(`${this.apiUrl}/redownload/${historyId}`)
      .pipe(
        tap((response) => {
          if (response.success && response.excel) {
            this.downloadExcelFromBase64(response.excel.fileName, response.excel.data);
            this.notificationService.success(
              '✅ Re-téléchargement réussi',
              response.excel.fileName
            );
          }
        })
      );
  }

  /**
   * 📥 Télécharge le fichier Excel depuis Base64
   */
  private downloadExcelFromBase64(fileName: string, base64Data: string): void {
    try {
      const binaryString = window.atob(base64Data);
      const bytes = new Uint8Array(binaryString.length);

      for (let i = 0; i < binaryString.length; i++) {
        bytes[i] = binaryString.charCodeAt(i);
      }

      const blob = new Blob([bytes], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('❌ Erreur téléchargement Excel:', error);
      this.notificationService.error(
        '❌ Erreur',
        'Impossible de télécharger le fichier'
      );
    }
  }

  /**
   * 📅 Obtient le lundi d'une date
   */
  private getMonday(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = d.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(d.setDate(diff));
  }

  /**
   * 📅 Obtient le vendredi d'une date
   */
  private getFriday(date: Date): Date {
    const monday = this.getMonday(date);
    return new Date(monday.getTime() + 4 * 24 * 60 * 60 * 1000);
  }

  /**
   * 💱 Formate une date en YYYY-MM-DD
   */
  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * 📦 Formate la taille d'un fichier
   */
  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
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

  /**
   * 📊 Télécharge le rapport de consommation complet (Excel formaté avec toutes les vues)
   */
  downloadConsumptionReport(periodType: 'all' | 'week' | 'day', startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams().set('periodType', periodType);

    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    this.notificationService.info(
      '📥 Téléchargement en cours',
      'Génération du rapport de consommation Excel...'
    );

    return this.http
      .get(`${environment.apiUrl}/reports/consumption/excel`, {
        params,
        responseType: 'blob',
        observe: 'response'
      })
      .pipe(
        tap((response) => {
          if (response.body) {
            const contentDisposition = response.headers.get('content-disposition');
            let fileName = 'rapport_consommation.xlsx';

            if (contentDisposition) {
              const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
              if (matches != null && matches[1]) {
                fileName = matches[1].replace(/['"]/g, '');
              }
            }

            this.downloadExcelBlob(fileName, response.body);
            this.notificationService.success(
              '✅ Téléchargement réussi',
              fileName
            );
          }
        }),
        map(response => response.body as Blob)
      );
  }

  /**
   * 📥 Télécharge le fichier Excel depuis un Blob
   */
  private downloadExcelBlob(fileName: string, blob: Blob): void {
    try {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('❌ Erreur téléchargement Excel:', error);
      this.notificationService.error(
        '❌ Erreur',
        'Impossible de télécharger le fichier'
      );
    }
  }
}
