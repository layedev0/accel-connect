import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface EmployeeDependentSummary {
  seqNo: number;
  name: string;
  relationship: string;
  contactNumber?: string;
  dateOfBirth?: string;
  age?: number;
}

/** Request body for create/update - matches EmployeeDependentDto without employeeId, seqNo */
export interface EmployeeDependentRequest {
  name: string;
  relationship: string;
  dateOfBirth?: string;
  contactNumber?: string;
}

@Injectable({
  providedIn: 'root',
})
export class EmployeeDependentService {
  private readonly http = inject(HttpClient);

  // ── HR admin endpoints (require HR_MANAGER role) ─────────────────────────

  private getDependentsUrl(employeeDetailsId: number): string {
    return `${environment.apiUrl}/employees/${employeeDetailsId}/dependents`;
  }

  getAll(employeeDetailsId: number): Observable<EmployeeDependentSummary[]> {
    return this.http.get<EmployeeDependentSummary[]>(this.getDependentsUrl(employeeDetailsId));
  }

  create(employeeDetailsId: number, request: EmployeeDependentRequest): Observable<EmployeeDependentSummary> {
    return this.http.post<EmployeeDependentSummary>(this.getDependentsUrl(employeeDetailsId), this.buildBody(request));
  }

  update(employeeDetailsId: number, seqNo: number, request: EmployeeDependentRequest): Observable<EmployeeDependentSummary> {
    return this.http.put<EmployeeDependentSummary>(`${this.getDependentsUrl(employeeDetailsId)}/${seqNo}`, this.buildBody(request));
  }

  delete(employeeDetailsId: number, seqNo: number): Observable<void> {
    return this.http.delete<void>(`${this.getDependentsUrl(employeeDetailsId)}/${seqNo}`);
  }

  // ── Employee self-service endpoints (/api/v1/my/dependents) ───────────────

  private readonly myUrl = `${environment.apiUrl}/my/dependents`;

  getOwn(): Observable<EmployeeDependentSummary[]> {
    return this.http.get<EmployeeDependentSummary[]>(this.myUrl);
  }

  createOwn(request: EmployeeDependentRequest): Observable<EmployeeDependentSummary> {
    return this.http.post<EmployeeDependentSummary>(this.myUrl, this.buildBody(request));
  }

  updateOwn(seqNo: number, request: EmployeeDependentRequest): Observable<EmployeeDependentSummary> {
    return this.http.put<EmployeeDependentSummary>(`${this.myUrl}/${seqNo}`, this.buildBody(request));
  }

  deleteOwn(seqNo: number): Observable<void> {
    return this.http.delete<void>(`${this.myUrl}/${seqNo}`);
  }

  private buildBody(req: EmployeeDependentRequest): Record<string, unknown> {
    const body: Record<string, unknown> = {
      name: req.name.trim(),
      relationship: req.relationship.trim(),
    };
    if (req.dateOfBirth) body['dateOfBirth'] = req.dateOfBirth;
    if (req.contactNumber?.trim()) body['contactNumber'] = req.contactNumber.trim();
    return body;
  }
}
