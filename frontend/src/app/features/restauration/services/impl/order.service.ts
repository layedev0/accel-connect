import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { OrderCreationPayload } from './cart.service';
import { OrderDTO } from '../../model/order.model';

interface ApiErrorResponse {
  error: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root',
})
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/orders`;

  /**
   * 🛒 Crée une commande
   */
  createOrder(payload: OrderCreationPayload): Observable<OrderDTO> {
    return this.http.post<OrderDTO[]>(this.apiUrl, payload).pipe(
      map((response) => {
        // L'API retourne un array, on prend le premier élément
        if (Array.isArray(response) && response.length > 0) {
          return response[0];
        }
        throw new Error('Réponse invalide du serveur');
      })
    );
  }

  /**
   * 📋 Récupère les commandes de l'utilisateur
   */
  getMyOrders(): Observable<OrderDTO[]> {
    return this.http.get<OrderDTO[]>(this.apiUrl);
  }

  /**
   * ❌ Annule une commande
   */
  cancelOrder(orderId: number): Observable<OrderDTO> {
    return this.http.put<OrderDTO>(`${this.apiUrl}/${orderId}/cancel`, {});
  }

  /**
   * 🎨 Obtient la couleur du statut
   */
  getStatusColor(status: string): string {
    const colors: Record<string, string> = {
      CONFIRMED: 'from-green-500 to-emerald-600',
      CANCELLED: 'from-red-500 to-red-600',
    };
    return colors[status] || 'from-gray-500 to-gray-600';
  }

  /**
   * 📝 Obtient le label du statut
   */
  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      CONFIRMED: 'Confirmée',
      CANCELLED: 'Annulée',
    };
    return labels[status] || status;
  }

  /**
   * 🎯 Obtient l'icône du statut
   */
  getStatusIcon(status: string): string {
    const icons: Record<string, string> = {
      CONFIRMED: 'check-circle',
      CANCELLED: 'times-circle',
    };
    return icons[status] || 'circle';
  }

  /**
   * 🔧 Extrait le message d'erreur depuis la réponse
   */
  extractErrorMessage(error: any): string {
    // Si l'erreur contient un objet error avec un message
    if (error.error && typeof error.error === 'object' && error.error.error) {
      return error.error.error;
    }

    // Si l'erreur est une chaîne directe
    if (error.error && typeof error.error === 'string') {
      return error.error;
    }

    // Message par défaut selon le code d'erreur
    switch (error.status) {
      case 400:
        return 'Requête invalide. Veuillez vérifier vos informations.';
      case 401:
        return 'Vous devez être connecté pour effectuer cette action.';
      case 403:
        return "Vous n'êtes pas autorisé à effectuer cette action.";
      case 404:
        return "La ressource demandée n'existe pas.";
      case 500:
        return 'Une erreur serveur est survenue. Veuillez réessayer plus tard.';
      default:
        return 'Une erreur inattendue est survenue.';
    }
  }
}
