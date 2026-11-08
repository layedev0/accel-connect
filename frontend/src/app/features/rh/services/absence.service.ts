import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface AbsenceComment {
  text: string;
  author: string;
  createdAt: string;
}

export interface AbsenceApprover {
  id: number;
  displayName: string;
}

export interface AbsenceType {
  code: string;
  name: string;
}

export interface AbsenceSummary {
  id: number;
  employeeName?: string;
  employeeEmail?: string;
  employeeUserId?: number;
  absenceTypeName?: string;
  date: string;
  startTime: string;
  endTime: string;
  durationHours: number;
  reason?: string;
  status: string;
  dateApplied: string;
  comments: AbsenceComment[];
  approvers?: AbsenceApprover[];
  decidedByName?: string;
}

export interface AbsenceRequest {
  absenceType: string;
  date: string;
  startTime: string;
  endTime: string;
  reason: string;
  approverUserIds?: number[];
}

@Injectable({ providedIn: 'root' })
export class AbsenceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/absences`;
  private readonly typeUrl = `${environment.apiUrl}/absence-types`;

  getAbsenceTypes(): Observable<AbsenceType[]> {
    return this.http.get<AbsenceType[]>(this.typeUrl);
  }

  submitAbsence(userId: number, request: AbsenceRequest): Observable<AbsenceSummary> {
    return this.http.post<AbsenceSummary>(`${this.baseUrl}/users/${userId}/submit`, request);
  }

  getUserAbsences(userId: number): Observable<AbsenceSummary[]> {
    return this.http.get<AbsenceSummary[]>(`${this.baseUrl}/users/${userId}/requests`);
  }

  getPendingAbsences(): Observable<AbsenceSummary[]> {
    return this.http.get<AbsenceSummary[]>(`${this.baseUrl}/pending-requests`);
  }

  getApprovedAbsences(): Observable<AbsenceSummary[]> {
    return this.http.get<AbsenceSummary[]>(`${this.baseUrl}/approved-requests`);
  }

  getRejectedAbsences(): Observable<AbsenceSummary[]> {
    return this.http.get<AbsenceSummary[]>(`${this.baseUrl}/rejected-requests`);
  }

  approveAbsence(id: number, comment?: string): Observable<AbsenceSummary> {
    return this.http.patch<AbsenceSummary>(
      `${this.baseUrl}/requests/${id}/approve`,
      comment ? { comment } : {}
    );
  }

  rejectAbsence(id: number, comment?: string): Observable<AbsenceSummary> {
    return this.http.patch<AbsenceSummary>(
      `${this.baseUrl}/requests/${id}/reject`,
      comment ? { comment } : {}
    );
  }

  cancelApprovedAbsence(id: number, comment?: string): Observable<AbsenceSummary> {
    return this.http.patch<AbsenceSummary>(
      `${this.baseUrl}/requests/${id}/cancel-approved`,
      comment ? { comment } : {}
    );
  }

  cancelRejectedAbsence(id: number, comment?: string): Observable<AbsenceSummary> {
    return this.http.patch<AbsenceSummary>(
      `${this.baseUrl}/requests/${id}/cancel-rejection`,
      comment ? { comment } : {}
    );
  }

  updateAbsence(id: number, userId: number, request: AbsenceRequest): Observable<AbsenceSummary> {
    return this.http.put<AbsenceSummary>(
      `${this.baseUrl}/requests/${id}`,
      request,
      { params: { userId: userId.toString() } }
    );
  }

  deleteAbsence(id: number, userId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/requests/${id}`,
      { params: { userId: userId.toString() } }
    );
  }
}
