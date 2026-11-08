import { Injectable } from '@angular/core';
import { Meal } from '../../model/plat.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import { IPlatService } from '../IPlatService';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class PlatService implements IPlatService {
  constructor(private readonly httpClient: HttpClient) {}

  getPlats(): Observable<Meal[]> {
    return this.httpClient.get<Meal[]>(`${environment.apiUrl}/meals/all`);
  }

  createPlats(plat: Meal): Observable<Meal> {
    return this.httpClient.post<Meal>(`${environment.apiUrl}/meals`, plat);
  }

  updatePlat(id: number, plat: Meal): Observable<Meal> {
    return this.httpClient.put<Meal>(`${environment.apiUrl}/meals/${id}`, plat);
  }

  deletePlat(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${environment.apiUrl}/meals/${id}`);
  }
}
