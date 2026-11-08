import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Menu, CreateMenuRequest } from '../../model/menu.model';
import { IMenuService } from '../IMenuService';

@Injectable({
  providedIn: 'root',
})
export class MenuService implements IMenuService {
  constructor(private readonly httpClient: HttpClient) {}

  // Créer ou mettre à jour un menu
  createOrUpdateMenu(request: CreateMenuRequest): Observable<Menu> {
    return this.httpClient.post<Menu>(`${environment.apiUrl}/menus`, request);
  }

  // Récupérer le menu du jour pour un restaurant
  getTodayMenu(restaurantId: number): Observable<Menu> {
    return this.httpClient.get<Menu>(`${environment.apiUrl}/menus/today/${restaurantId}`);
  }

  // Récupérer un menu pour une date spécifique
  getMenuByDate(restaurantId: number, date: string): Observable<Menu> {
    return this.httpClient.get<Menu>(`${environment.apiUrl}/menus/${restaurantId}?date=${date}`);
  }

  // Récupérer tous les menus d'un restaurant
  getAllMenusByRestaurant(restaurantId: number): Observable<Menu[]> {
    return this.httpClient.get<Menu[]>(`${environment.apiUrl}/menus/restaurant/${restaurantId}`);
  }

  // Récupérer tous les menus actifs pour une date
  getActiveMenus(date: string): Observable<Menu[]> {
    return this.httpClient.get<Menu[]>(`${environment.apiUrl}/menus/active?date=${date}`);
  }

  // Désactiver un menu
  deleteMenu(menuId: number): Observable<void> {
    return this.httpClient.delete<void>(`${environment.apiUrl}/menus/${menuId}`);
  }
}
