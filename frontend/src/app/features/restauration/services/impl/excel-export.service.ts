import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { NotificationService } from '../../../../core/services/impl/notification.service';

@Injectable({
  providedIn: 'root',
})
export class ExcelExportService {
  private readonly http = inject(HttpClient);
  private readonly notificationService = inject(NotificationService);
  private readonly apiUrl = `${environment.apiUrl}/admin/export`;

  /**
   * 📥 Télécharge le rapport hebdomadaire en Excel
   * @param startDate Date de début (optionnel, défaut: lundi de la semaine courante)
   * @param endDate Date de fin (optionnel, défaut: vendredi de la semaine courante)
   */
  downloadWeeklyReport(startDate?: string, endDate?: string): void {
    this.notificationService.info('📥 Téléchargement en cours', 'Génération du fichier Excel...');

    let url = `${this.apiUrl}/weekly-report`;

    // Ajouter les paramètres de date si fournis
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);

    if (params.toString()) {
      url += `?${params.toString()}`;
    }

    // Utiliser HttpClient pour télécharger le fichier avec gestion d'erreurs
    this.http
      .get(url, {
        responseType: 'blob',
        observe: 'response',
      })
      .subscribe({
        next: (response) => {
          // Extraire le nom du fichier depuis les headers
          const contentDisposition = response.headers.get('Content-Disposition');
          let fileName = 'Commandes_Semaine.xlsx';

          if (contentDisposition) {
            const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
            if (matches != null && matches[1]) {
              fileName = matches[1].replace(/['"]/g, '');
            }
          }

          // Créer un blob et déclencher le téléchargement
          const blob = response.body;
          if (blob) {
            const blobUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = blobUrl;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(blobUrl);

            this.notificationService.success(
              '✅ Téléchargement réussi',
              `Le fichier "${fileName}" a été téléchargé avec succès`
            );
          }
        },
        error: (error) => {
          console.error('Erreur lors du téléchargement', error);
          this.notificationService.error(
            '❌ Erreur de téléchargement',
            'Impossible de télécharger le rapport Excel'
          );
        },
      });
  }

  /**
   * 📊 Prévisualise les données qui seront exportées
   */
  previewWeeklyReport(startDate?: string, endDate?: string): Observable<any> {
    let url = `${this.apiUrl}/weekly-preview`;

    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);

    if (params.toString()) {
      url += `?${params.toString()}`;
    }

    return this.http.get(url);
  }

  /**
   * 📅 Retourne les dates de la semaine en cours (Lundi-Vendredi)
   */
  getCurrentWeek(): { monday: string; friday: string } {
    const today = new Date();
    const dayOfWeek = today.getDay();

    // Calculer le lundi de la semaine
    const monday = new Date(today);
    const daysToMonday = dayOfWeek === 0 ? -6 : 1 - dayOfWeek;
    monday.setDate(today.getDate() + daysToMonday);

    // Calculer le vendredi de la semaine
    const friday = new Date(monday);
    friday.setDate(monday.getDate() + 4);

    return {
      monday: this.formatDate(monday),
      friday: this.formatDate(friday),
    };
  }

  /**
   * 💱 Formate une date au format YYYY-MM-DD
   */
  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * 📆 Vérifie si aujourd'hui est vendredi
   */
  isFriday(): boolean {
    return new Date().getDay() === 5;
  }

  /**
   * 📅 Retourne la date du prochain vendredi
   */
  getNextFriday(): string {
    const today = new Date();
    const dayOfWeek = today.getDay();
    const daysUntilFriday = dayOfWeek === 5 ? 7 : (5 - dayOfWeek + 7) % 7;

    const nextFriday = new Date(today);
    nextFriday.setDate(today.getDate() + daysUntilFriday);

    return this.formatDate(nextFriday);
  }
}
