import { Observable } from 'rxjs';

export interface IGeneriqueService<T> {
  create(item: T): Observable<T>;
  update(item: T, id: number): Observable<T>;
  delete(id: number): Observable<void>;
  getOne(id: number): Observable<T>;
  getAll(): Observable<T[]>;
  getAllWithPagination(page: number, size: number): Observable<T[]>;
}
