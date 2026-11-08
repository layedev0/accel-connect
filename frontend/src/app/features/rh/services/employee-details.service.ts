import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface EmployeInfoPersonnelDTO {
  id?: number;
  userId?: number;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  cni?: string;
  statut?: string;
  telephone?: string;
  dateNaissance?: string;
  dateAdhesion?: string;
  sexe?: string;
  maritalStatus?: string;
  departmentId?: number;
  departmentName?: string;
  jobTitleId?: number;
  jobTitleName?: string;
  jobCategoryId?: number;
  jobCategoryName?: string;
  jobCategoryCode?: string;
  jobCategorySalaireBase?: number;
}

/** DTO for employee profile display in the RH employee-profile section */
export interface EmployeeProfileDTO {
  id?: number;
  userId?: number;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  employeeNumber?: string;
  jobTitle?: string;
  department?: string;
  locationLabel?: string;
  status?: string;
  phone?: string;
  intern?: boolean;
  subsidyAmount?: number;
  joinDate?: string;
  birthday?: string;
  cni?: string;
  sexe?: string;
  maritalStatus?: string;
}

export interface EmployeeDetailsRequest {
  id?: number;
  userId?: number;
  birthday?: string;
  sexe?: string;
  maritalStatus?: string;
  cni?: string;
  jobTitleId?: number;
  jobCategoryId?: number;
  departmentId?: number;
  workTelephone?: string;
  joinedDate?: string;
  typeContrat?: string;
}

/** An upcoming employee birthday, derived from the stored day/month of birth. */
export interface UpcomingBirthday {
  userId?: number;
  firstName?: string;
  lastName?: string;
  departmentName?: string;
  /** Day of month (1-31). */
  day: number;
  /** Month (1-12). */
  month: number;
  /** Next calendar date the birthday falls on (yyyy-MM-dd). */
  nextBirthday: string;
  /** Days from today until the birthday (0 = today). */
  daysUntil: number;
  /** True when the birthday is today. */
  today: boolean;
  /** Age the employee is turning, when the birth year is known. */
  turningAge?: number;
}

@Injectable({
  providedIn: 'root',
})
export class EmployeeDetailsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/employee-details`;

  /**
   * Returns employee personnel info if it exists, or null when not found.
   * Use when loading the form: existing data → patch form; null → show empty form.
   */
  getByUserIdOptional(userId: number): Observable<EmployeInfoPersonnelDTO | null> {
    return this.http.get<EmployeInfoPersonnelDTO | null>(
      `${this.baseUrl}/by-user/${userId}/optional`
    );
  }

  /**
   * Create or update employee details in one call (HR_MANAGER only).
   * If request.id is provided and exists, updates; otherwise creates new.
   */
  save(request: EmployeeDetailsRequest): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/save`, request);
  }

  /**
   * Allows an authenticated employee to save their own personal info.
   * The backend enforces that userId = the authenticated user's id.
   */
  saveOwn(request: EmployeeDetailsRequest): Observable<unknown> {
    return this.http.put(`${this.baseUrl}/save-own`, request);
  }

  /**
   * Returns employee profile for display in the RH employee-profile section.
   * Combines User and EmployeeDetails (when available).
   */
  getProfileByUserId(userId: number): Observable<EmployeeProfileDTO> {
    return this.http.get<EmployeeProfileDTO>(
      `${this.baseUrl}/profile/by-user/${userId}`
    );
  }

  /**
   * Returns employees whose birthday falls within the next `days` days
   * (default 30), ordered by the soonest first.
   */
  getUpcomingBirthdays(days = 30): Observable<UpcomingBirthday[]> {
    return this.http.get<UpcomingBirthday[]>(
      `${this.baseUrl}/birthdays/upcoming`,
      { params: { days } }
    );
  }
}
