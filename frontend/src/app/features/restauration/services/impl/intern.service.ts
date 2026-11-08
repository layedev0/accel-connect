import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface InternUser {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  intern: boolean;
  email: string;
  subsidyAmount: number;
  location?: string;
}

export interface InternActionResponse {
  message: string;
  userId: number;
  intern: boolean;
  subsidyAmount: number;
}

@Injectable({
  providedIn: 'root',
})
export class InternService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/users`;

  getAllUsers(): Observable<InternUser[]> {
    return this.http.get<InternUser[]>(`${this.apiUrl}/all`);
  }

  getInterns(): Observable<InternUser[]> {
    return this.http.get<InternUser[]>(`${this.apiUrl}/interns`);
  }

  setIntern(userId: number): Observable<InternActionResponse> {
    return this.http.put<InternActionResponse>(`${this.apiUrl}/${userId}/set-intern`, {});
  }

  removeIntern(userId: number): Observable<InternActionResponse> {
    return this.http.put<InternActionResponse>(`${this.apiUrl}/${userId}/remove-intern`, {});
  }

  formatAmount(amount: number): string {
    return (
      new Intl.NumberFormat('fr-FR', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(amount) + ' FCFA'
    );
  }
}
