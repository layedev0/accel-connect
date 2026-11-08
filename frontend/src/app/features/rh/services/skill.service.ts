import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface Skill {
  id: number;
  name: string;
  description?: string;
}

export interface SkillRequest {
  name: string;
  description?: string;
}

@Injectable({
  providedIn: 'root',
})
export class SkillService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/skills`;

  getAll(): Observable<Skill[]> {
    return this.http.get<Skill[]>(this.baseUrl);
  }

  getById(id: number): Observable<Skill> {
    return this.http.get<Skill>(`${this.baseUrl}/${id}`);
  }

  create(request: SkillRequest): Observable<Skill> {
    return this.http.post<Skill>(this.baseUrl, request);
  }

  update(id: number, request: SkillRequest): Observable<Skill> {
    return this.http.put<Skill>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
