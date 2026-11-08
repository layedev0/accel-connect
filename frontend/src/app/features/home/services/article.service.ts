import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export type ArticleType = 'FORMATION' | 'RECHERCHE' | 'BUSINESS' | 'PARTENARIAT';

export interface ArticleDTO {
  id: number;
  title: string;
  content: string;
  articleType: ArticleType;
  documentName: string;
  documentType: string;
  documentSize: number;
  hasCoverImage: boolean;
  /** ISO publication date. Absent on articles created before the column existed. */
  createdAt?: string;
  /** Who published the article. Absent on articles created before the column existed. */
  authorName?: string;
}

export interface ArticleCommentDTO {
  id: number;
  content: string;
  createdAt: string;
  authorId: number;
  authorName: string;
  /** Computed server-side: the comment's author, or a super admin. */
  canDelete: boolean;
}

/** Gallery photo metadata. The bytes come from a separate endpoint. */
export interface ArticlePhotoDTO {
  id: number;
  fileName: string;
  contentType: string;
  size: number;
  position: number;
}

export interface ArticleLikeStatusDTO {
  likeCount: number;
  likedByCurrentUser: boolean;
}

export const ARTICLE_TYPES: { value: ArticleType; label: string }[] = [
  { value: 'FORMATION', label: 'Formation' },
  { value: 'RECHERCHE', label: 'Recherche' },
  { value: 'BUSINESS', label: 'Business' },
  { value: 'PARTENARIAT', label: 'Partenariat' },
];

@Injectable({ providedIn: 'root' })
export class ArticleService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/articles`;

  list(): Observable<ArticleDTO[]> {
    return this.http.get<ArticleDTO[]>(this.url);
  }

  create(
    title: string,
    content: string,
    articleType: ArticleType,
    coverImage?: File | null,
  ): Observable<ArticleDTO> {
    const form = new FormData();
    form.append('title', title);
    form.append('content', content);
    form.append('articleType', articleType);
    if (coverImage) form.append('coverImage', coverImage);
    return this.http.post<ArticleDTO>(this.url, form);
  }

  /**
   * Update an article; the cover is replaced only when provided.
   * No `file` is ever sent, so a document attached before the feature was retired stays put.
   */
  update(
    id: number,
    title: string,
    content: string,
    articleType: ArticleType,
    coverImage?: File | null,
  ): Observable<ArticleDTO> {
    const form = new FormData();
    form.append('title', title);
    form.append('content', content);
    form.append('articleType', articleType);
    if (coverImage) form.append('coverImage', coverImage);
    return this.http.put<ArticleDTO>(`${this.url}/${id}`, form);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** Fetch the cover image as a Blob (auth header is added by the interceptor). */
  downloadCover(id: number): Observable<Blob> {
    return this.http.get(`${this.url}/${id}/cover`, { responseType: 'blob' });
  }

  /** Gallery metadata in display order — carries no image bytes. */
  getPhotos(articleId: number): Observable<ArticlePhotoDTO[]> {
    return this.http.get<ArticlePhotoDTO[]>(`${this.url}/${articleId}/photos`);
  }

  /** Fetch one photo as a Blob (auth header is added by the interceptor). */
  downloadPhoto(articleId: number, photoId: number): Observable<Blob> {
    return this.http.get(`${this.url}/${articleId}/photos/${photoId}`, { responseType: 'blob' });
  }

  /** One photo per request: the server caps a whole multipart request at 10 MB. */
  uploadPhoto(articleId: number, file: File): Observable<ArticlePhotoDTO> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ArticlePhotoDTO>(`${this.url}/${articleId}/photos`, form);
  }

  deletePhoto(articleId: number, photoId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${articleId}/photos/${photoId}`);
  }

  /** An article's comments, newest first. */
  getComments(articleId: number): Observable<ArticleCommentDTO[]> {
    return this.http.get<ArticleCommentDTO[]>(`${this.url}/${articleId}/comments`);
  }

  addComment(articleId: number, content: string): Observable<ArticleCommentDTO> {
    return this.http.post<ArticleCommentDTO>(`${this.url}/${articleId}/comments`, { content });
  }

  deleteComment(articleId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${articleId}/comments/${commentId}`);
  }

  getLikeStatus(articleId: number): Observable<ArticleLikeStatusDTO> {
    return this.http.get<ArticleLikeStatusDTO>(`${this.url}/${articleId}/like`);
  }

  /** Idempotent: liking twice leaves a single like. Returns the refreshed status. */
  like(articleId: number): Observable<ArticleLikeStatusDTO> {
    return this.http.post<ArticleLikeStatusDTO>(`${this.url}/${articleId}/like`, {});
  }

  unlike(articleId: number): Observable<ArticleLikeStatusDTO> {
    return this.http.delete<ArticleLikeStatusDTO>(`${this.url}/${articleId}/like`);
  }
}
