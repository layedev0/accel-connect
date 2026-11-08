import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface JobTitle {
  id: number;
  jobTitle: string;
  jobTitleCode: string;
  jobDescription?: string;
}

export interface JobTitleRequest {
  jobTitle: string;
  jobTitleCode: string;
  jobDescription?: string;
}

export interface JobCategory {
  id: number;
  name: string;
  code: string;
  categoryCode: string;
  salaireBase?: number;
  description?: string;
}

export interface JobCategoryRequest {
  name: string;
  code: string;
  categoryCode: string;
  salaireBase?: number;
  description?: string;
}

@Injectable({ providedIn: 'root' })
export class JobService {
  private readonly http = inject(HttpClient);
  private readonly titlesUrl = `${environment.apiUrl}/job-titles`;
  private readonly categoriesUrl = `${environment.apiUrl}/job-categories`;

  // ── Job Titles ──────────────────────────────────────────
  getAllTitles(): Observable<JobTitle[]> {
    return this.http.get<JobTitle[]>(this.titlesUrl);
  }

  createTitle(req: JobTitleRequest): Observable<JobTitle> {
    return this.http.post<JobTitle>(this.titlesUrl, req);
  }

  updateTitle(id: number, req: JobTitleRequest): Observable<JobTitle> {
    return this.http.put<JobTitle>(`${this.titlesUrl}/${id}`, req);
  }

  deleteTitle(id: number): Observable<void> {
    return this.http.delete<void>(this.titlesUrl, { params: { id: id.toString() } });
  }

  // ── Job Categories ───────────────────────────────────────
  getAllCategories(): Observable<JobCategory[]> {
    return this.http.get<JobCategory[]>(this.categoriesUrl);
  }

  createCategory(req: JobCategoryRequest): Observable<JobCategory> {
    return this.http.post<JobCategory>(this.categoriesUrl, req);
  }

  updateCategory(id: number, req: JobCategoryRequest): Observable<JobCategory> {
    return this.http.put<JobCategory>(`${this.categoriesUrl}/${id}`, req);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(this.categoriesUrl, { params: { id: id.toString() } });
  }
}
