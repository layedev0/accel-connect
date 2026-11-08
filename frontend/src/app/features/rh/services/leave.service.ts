import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface LeaveType {
  code: string;
  name: string;
  allocatedDays: number;
  deductionRule: 'FULL' | 'EXCESS_ONLY' | 'NONE';
}

export interface LeaveBalanceInfo {
  leaveTypeCode: string;
  leaveTypeName: string;
  entitled: number;
  taken: number;
  scheduled: number;
  pending: number;
  available: number;
  displayValue: string;
}

export interface LeaveDayDetail {
  date: string;
  dayOfWeek: string;
  lengthDays: number;
  lengthHours: number;
  status: string;
  durationType: string;
  isWeekend: boolean;
  isHoliday: boolean;
}

export interface LeaveRequestComment {
  text: string;
  author: string;
  createdAt: string;
}

export interface LeaveRequestSummary {
  id: number;
  employeeName?: string;
  employeeEmail?: string;
  leaveTypeCode: string;
  leaveTypeName: string;
  dateApplied: string;
  startDate: string;
  endDate: string;
  totalDays: number;
  overallStatus: string;
  days: LeaveDayDetail[];
  comments: LeaveRequestComment[];
  approvers?: LeaveApprover[];
  decidedByName?: string;
}

export interface ApproverOption {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface LeaveApprover {
  id: number;
  displayName: string;
}

export interface LeaveApplicationRequest {
  leaveTypeCode: string;
  startDate: string;
  endDate: string;
  comment?: string;
  approverUserIds?: number[];
  notifyUserIds?: number[];
  startTime?: string;
  endTime?: string;
}

export interface LeaveApplicationResponse {
  leaveRequestId: number;
  status: string;
  message: string;
  daysRequested: number;
  balanceDeductibleDays: number;
  availableBalance: number;
  balanceAfter: number;
  leaveDays: LeaveDayDetail[];
  warnings: string[];
}

@Injectable({ providedIn: 'root' })
export class LeaveService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/leaves`;
  private readonly leaveTypeUrl = `${environment.apiUrl}/leave-types`;
  private readonly userUrl = `${environment.apiUrl}/users`;

  getLeaveTypes(): Observable<LeaveType[]> {
    return this.http.get<LeaveType[]>(this.leaveTypeUrl);
  }

  getUserLeaveRequests(userId: number): Observable<LeaveRequestSummary[]> {
    return this.http.get<LeaveRequestSummary[]>(`${this.baseUrl}/users/${userId}/requests`);
  }

  getUserLeaveBalance(userId: number): Observable<LeaveBalanceInfo[]> {
    return this.http.get<LeaveBalanceInfo[]>(`${this.baseUrl}/users/${userId}/balance`);
  }

  applyForLeave(userId: number, request: LeaveApplicationRequest): Observable<LeaveApplicationResponse> {
    return this.http.post<LeaveApplicationResponse>(`${this.baseUrl}/users/${userId}/apply`, request);
  }

  cancelLeaveRequest(leaveRequestId: number, userId: number): Observable<void> {
    return this.http.patch<void>(
      `${this.baseUrl}/leave-requests/${leaveRequestId}/cancel`,
      null,
      { params: { userId: userId.toString() } }
    );
  }

  deleteLeaveRequest(leaveRequestId: number, userId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseUrl}/leave-requests/${leaveRequestId}`,
      { params: { userId: userId.toString() } }
    );
  }

  getColleagues(): Observable<ApproverOption[]> {
    return this.http.get<ApproverOption[]>(`${this.userUrl}/colleagues`);
  }

  getAllPendingRequests(): Observable<LeaveRequestSummary[]> {
    return this.http.get<LeaveRequestSummary[]>(`${this.baseUrl}/pending-requests`);
  }

  getApprovedRequests(): Observable<LeaveRequestSummary[]> {
    return this.http.get<LeaveRequestSummary[]>(`${this.baseUrl}/approved-requests`);
  }

  getRejectedRequests(): Observable<LeaveRequestSummary[]> {
    return this.http.get<LeaveRequestSummary[]>(`${this.baseUrl}/rejected-requests`);
  }

  approveLeaveRequest(id: number, comment?: string): Observable<void> {
    return this.http.patch<void>(
      `${this.baseUrl}/leave-requests/${id}/approve`,
      comment ? { comment } : {}
    );
  }

  rejectLeaveRequest(id: number, comment?: string): Observable<void> {
    return this.http.patch<void>(
      `${this.baseUrl}/leave-requests/${id}/reject`,
      comment ? { comment } : {}
    );
  }

  cancelApprovedLeaveRequest(id: number, comment?: string): Observable<void> {
    return this.http.patch<void>(
      `${this.baseUrl}/leave-requests/${id}/cancel-approved`,
      comment ? { comment } : {}
    );
  }

  cancelRejectedLeaveRequest(id: number, comment?: string): Observable<void> {
    return this.http.patch<void>(
      `${this.baseUrl}/leave-requests/${id}/cancel-rejection`,
      comment ? { comment } : {}
    );
  }
}
