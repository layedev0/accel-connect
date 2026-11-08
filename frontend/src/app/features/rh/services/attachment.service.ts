import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface AttachmentDTO {
  userId: number;
  attachId: number;
  description: string;
  filename: string;
  size: number;
  fileType: string;
  screen: string;
  attachedByName: string;
  createdAt: string;
}

export const DOCUMENT_TYPES = [
  { value: 'bulletin-salaire', label: 'Bulletin de salaire' },
  { value: 'contrat', label: 'Contrat de travail' },
  { value: 'attestation-travail', label: 'Attestation de travail' },
  { value: 'attestation-presence', label: 'Attestation de présence' },
  { value: 'avenant-contrat', label: 'Avenant au contrat' },
  { value: 'avertissement', label: 'Avertissement' },
  { value: 'autre', label: 'Autre' },
] as const;

@Injectable({ providedIn: 'root' })
export class AttachmentService {
  private readonly http = inject(HttpClient);

  private url(userId: number) {
    return `${environment.apiUrl}/employees/${userId}/attachments`;
  }

  list(userId: number): Observable<AttachmentDTO[]> {
    return this.http.get<AttachmentDTO[]>(this.url(userId));
  }

  upload(userId: number, file: File, description: string, screen: string): Observable<AttachmentDTO> {
    const form = new FormData();
    form.append('file', file);
    form.append('description', description);
    form.append('screen', screen);
    return this.http.post<AttachmentDTO>(this.url(userId), form);
  }

  download(userId: number, attachId: number): Observable<Blob> {
    return this.http.get(`${this.url(userId)}/${attachId}/download`, { responseType: 'blob' });
  }

  delete(userId: number, attachId: number): Observable<void> {
    return this.http.delete<void>(`${this.url(userId)}/${attachId}`);
  }

  // ── Employee self-service endpoints (/api/v1/my/attachments) ─────────────

  private readonly myUrl = `${environment.apiUrl}/my/attachments`;

  listOwn(): Observable<AttachmentDTO[]> {
    return this.http.get<AttachmentDTO[]>(this.myUrl);
  }

  downloadOwn(attachId: number): Observable<Blob> {
    return this.http.get(`${this.myUrl}/${attachId}/download`, { responseType: 'blob' });
  }

  /** List supporting documents attached to a leave request (accessible to approvers). */
  listLeaveRequestAttachments(leaveRequestId: number): Observable<AttachmentDTO[]> {
    return this.http.get<AttachmentDTO[]>(
      `${environment.apiUrl}/leaves/leave-requests/${leaveRequestId}/attachments`
    );
  }

  /** Download a supporting document of a leave request (accessible to approvers). */
  downloadLeaveRequestAttachment(leaveRequestId: number, attachId: number): Observable<Blob> {
    return this.http.get(
      `${environment.apiUrl}/leaves/leave-requests/${leaveRequestId}/attachments/${attachId}/download`,
      { responseType: 'blob' }
    );
  }

  /** Upload a supporting document for a specific leave request (owner only). */
  uploadLeaveRequestAttachment(leaveRequestId: number, file: File, description?: string): Observable<AttachmentDTO> {
    const form = new FormData();
    form.append('file', file);
    if (description) form.append('description', description);
    return this.http.post<AttachmentDTO>(
      `${environment.apiUrl}/leaves/leave-requests/${leaveRequestId}/attachments`,
      form
    );
  }

  /** Delete a supporting document from a leave request (owner only). */
  deleteLeaveRequestAttachment(leaveRequestId: number, attachId: number): Observable<void> {
    return this.http.delete<void>(
      `${environment.apiUrl}/leaves/leave-requests/${leaveRequestId}/attachments/${attachId}`
    );
  }

  /** Re-tag an existing attachment onto a different leave request (used when edit recreates the request). */
  reassignLeaveRequestAttachment(newLeaveRequestId: number, attachId: number): Observable<AttachmentDTO> {
    return this.http.put<AttachmentDTO>(
      `${environment.apiUrl}/leaves/leave-requests/${newLeaveRequestId}/attachments/${attachId}/reassign`,
      {}
    );
  }
}
