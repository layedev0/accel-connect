import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface Holiday {
  /** ISO date (yyyy-MM-dd) */
  date: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class HolidayService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/holidays`;

  /** Named public holidays overlapping the inclusive range [start, end] (ISO yyyy-MM-dd). */
  getHolidays(start: string, end: string): Observable<Holiday[]> {
    const params = new HttpParams().set('start', start).set('end', end);
    return this.http.get<Holiday[]>(this.baseUrl, { params });
  }
}
