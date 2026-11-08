import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface UserLeaveBalance {
  userId: number;
  userName: string;
  userEmail: string;
  balanceDays: number;
  lastUpdated: string | null;
  leaveDue: number;
  leaveTaken: number;
  leaveBalance: number;
}

export interface EmployeeUser {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface EmployeeWithBalance extends UserLeaveBalance {
  hasBalance: boolean;
}

@Injectable({ providedIn: 'root' })
export class LeaveBalanceService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/leave-balances`;
  private readonly employeeBaseUrl = `${environment.apiUrl}/users`;

  /**
   * Get all employees with their leave balances
   * Returns all employees, with balance data if available
   */
  listAllWithBalances(): Observable<EmployeeWithBalance[]> {
    return forkJoin({
      balances: this.getBalances().pipe(
        map(balances => new Map(balances.map(b => [b.userId, b])))
      ),
      employees: this.getAllEmployees()
    }).pipe(
      map(({ balances, employees }) => {
        return employees.map(emp => {
          const balance = balances.get(emp.id);
          return {
            userId: emp.id,
            userName: `${emp.firstName} ${emp.lastName}`,
            userEmail: emp.email,
            balanceDays: balance?.balanceDays ?? 0,
            lastUpdated: balance?.lastUpdated ?? null,
            leaveDue: balance?.leaveDue ?? 0,
            leaveTaken: balance?.leaveTaken ?? 0,
            leaveBalance: balance?.leaveBalance ?? 0,
            hasBalance: !!balance
          };
        });
      })
    );
  }

  /**
   * Get only users that have leave balances
   */
  private getBalances(): Observable<UserLeaveBalance[]> {
    return this.http.get<UserLeaveBalance[]>(this.baseUrl);
  }

  /**
   * Get all employees from the users endpoint
   */
  private getAllEmployees(): Observable<EmployeeUser[]> {
    return this.http.get<EmployeeUser[]>(`${this.employeeBaseUrl}/all`);
  }


  listAll(): Observable<UserLeaveBalance[]> {
    return this.getBalances();
  }

  getBalanceForUser(): Observable<UserLeaveBalance | null> {
    return this.http.get<UserLeaveBalance>(`${this.baseUrl}/me`).pipe(
      catchError(() => of(null))
    );
  }

  addBalance(userId: number, days: number): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/${userId}/add-balance`,
      null,
      { params: { days: days.toString() } }
    );
  }

  deductBalance(userId: number, days: number): Observable<void> {
    return this.http.put<void>(
      `${this.baseUrl}/${userId}/deduct-balance`,
      null,
      { params: { days: days.toString() } }
    );
  }

  addLeaveDue(userId: number, days: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${userId}/add-leave-due`, null, { params: { days: days.toString() } });
  }

  deductLeaveDue(userId: number, days: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${userId}/deduct-leave-due`, null, { params: { days: days.toString() } });
  }

  addLeaveTaken(userId: number, days: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${userId}/add-leave-taken`, null, { params: { days: days.toString() } });
  }

  deductLeaveTaken(userId: number, days: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${userId}/deduct-leave-taken`, null, { params: { days: days.toString() } });
  }
}
