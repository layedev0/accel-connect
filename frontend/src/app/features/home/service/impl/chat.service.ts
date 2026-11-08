import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import { IChatService } from '../IChatService';
import { ChatRequest, ChatResponse } from '../../model/alex.model';

@Injectable({
  providedIn: 'root',
})
export class ChatServiceImpl implements IChatService {
  constructor(private readonly http: HttpClient) {}

  sendMessage(message: string): Observable<ChatResponse> {
    const body: ChatRequest = { message };

    return this.http
      .post<ChatResponse>(`${environment.apiUrlForChat}/chat`, body, {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .pipe(retry(1), catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    let errorMessage = "Une erreur s'est produite lors de la communication avec le serveur.";

    if (error.error instanceof ErrorEvent) {
      // Erreur côté client
      errorMessage = `Erreur: ${error.error.message}`;
    } else {
      // Erreur côté serveur
      errorMessage = error.error?.response || errorMessage;
    }

    console.error('Erreur du service chat:', error);
    return throwError(() => new Error(errorMessage));
  }
}
