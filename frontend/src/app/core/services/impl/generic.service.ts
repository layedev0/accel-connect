import { Observable } from 'rxjs';
import { IGeneriqueService } from '../IGeneriqueService';
import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class GenericService<T> implements IGeneriqueService<T> {
  protected apiUrl!: string;
  protected endPoint!: string;
  protected httpClient: HttpClient = inject(HttpClient);
  getAllWithPagination(
    page: number = environment.page,
    size: number = environment.size
  ): Observable<T[]> {
    return this.httpClient.get<T[]>(`${this.apiUrl}?page=${page}&size=${size}`);
  }
  create(item: T): Observable<T> {
    return this.httpClient.post<T>(this.apiUrl, item);
  }
  update(item: T, id: number): Observable<T> {
    return this.httpClient.put<T>(`${this.apiUrl}/${id}`, item);
  }
  delete(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${environment.apiUrl}/${this.apiUrl}/${id}`);
  }
  getOne(id: number): Observable<T> {
    return this.httpClient.get<T>(`${this.apiUrl}/${id}`);
  }
  getAll(): Observable<T[]> {
    return this.httpClient.get<T[]>(this.apiUrl);
  }
}
