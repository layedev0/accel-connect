import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface EmployeeSkillDto {
  employeeId?: number;
  skillId: number;
  skillName?: string;
  yearsOfExperience?: number;
  comments?: string | null;
}

export interface EmployeeSkillRequest {
  skillId: number;
  yearsOfExperience?: number;
  comments?: string | null;
}

@Injectable({
  providedIn: 'root',
})
export class EmployeeSkillService {
  private readonly http = inject(HttpClient);

  // ── HR admin endpoints (require HR_MANAGER role) ─────────────────────────

  private getEmployeeSkillsUrl(employeeDetailsId: number): string {
    return `${environment.apiUrl}/employees/${employeeDetailsId}/skills`;
  }

  getAll(employeeDetailsId: number): Observable<EmployeeSkillDto[]> {
    return this.http.get<EmployeeSkillDto[]>(this.getEmployeeSkillsUrl(employeeDetailsId));
  }

  create(employeeDetailsId: number, request: EmployeeSkillRequest): Observable<EmployeeSkillDto> {
    return this.http.post<EmployeeSkillDto>(this.getEmployeeSkillsUrl(employeeDetailsId), request);
  }

  update(employeeDetailsId: number, skillId: number, request: EmployeeSkillRequest): Observable<EmployeeSkillDto> {
    return this.http.put<EmployeeSkillDto>(`${this.getEmployeeSkillsUrl(employeeDetailsId)}/${skillId}`, request);
  }

  delete(employeeDetailsId: number, skillId: number): Observable<void> {
    return this.http.delete<void>(`${this.getEmployeeSkillsUrl(employeeDetailsId)}/${skillId}`);
  }

  // ── Employee self-service endpoints (/api/v1/my/skills) ──────────────────

  private readonly myUrl = `${environment.apiUrl}/my/skills`;

  getOwn(): Observable<EmployeeSkillDto[]> {
    return this.http.get<EmployeeSkillDto[]>(this.myUrl);
  }

  createOwn(request: EmployeeSkillRequest): Observable<EmployeeSkillDto> {
    return this.http.post<EmployeeSkillDto>(this.myUrl, request);
  }

  updateOwn(skillId: number, request: EmployeeSkillRequest): Observable<EmployeeSkillDto> {
    return this.http.put<EmployeeSkillDto>(`${this.myUrl}/${skillId}`, request);
  }

  deleteOwn(skillId: number): Observable<void> {
    return this.http.delete<void>(`${this.myUrl}/${skillId}`);
  }
}
