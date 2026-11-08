import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Menu } from '../../model/menu.model';
import { Restaurant } from '../../model/restaurant.model';
import { RestaurantWithMenu } from '../../model/menuMealRestau.model';



@Injectable({
  providedIn: 'root',
})
export class MenuClientService {
  constructor(private readonly httpClient: HttpClient) {}

  // Récupérer tous les menus actifs pour une date
  getMenusByDate(date: string): Observable<Menu[]> {
    return this.httpClient.get<Menu[]>(`${environment.apiUrl}/menus/active?date=${date}`);
  }

  // Récupérer le menu du jour d'un restaurant spécifique
  getTodayMenuForRestaurant(restaurantId: number): Observable<Menu> {
    return this.httpClient.get<Menu>(`${environment.apiUrl}/menus/today/${restaurantId}`);
  }

  // Récupérer tous les restaurants avec leurs menus du jour
  getRestaurantsWithTodayMenus(restaurants: Restaurant[]): Observable<RestaurantWithMenu[]> {
    const today = new Date().toISOString().split('T')[0];

    return this.getMenusByDate(today).pipe(
      map((menus) => {
        return restaurants.map((restaurant) => {
          const menu = menus.find((m) => m.restaurantId === restaurant.id);
          return {
            restaurant,
            menu: menu || null,
            hasMenu: !!menu,
            itemCount: menu?.meals?.length || 0,
          };
        });
      })
    );
  }

  // Récupérer les menus pour une date spécifique avec les restaurants
  getRestaurantsWithMenusByDate(
    restaurants: Restaurant[],
    date: string
  ): Observable<RestaurantWithMenu[]> {
    return this.getMenusByDate(date).pipe(
      map((menus) => {
        return restaurants.map((restaurant) => {
          const menu = menus.find((m) => m.restaurantId === restaurant.id);
          return {
            restaurant,
            menu: menu || null,
            hasMenu: !!menu,
            itemCount: menu?.meals?.length || 0,
          };
        });
      })
    );
  }

  // Récupérer tous les menus de la semaine pour un restaurant
  getWeeklyMenusForRestaurant(restaurantId: number): Observable<Menu[]> {
    return this.httpClient.get<Menu[]>(`${environment.apiUrl}/menus/restaurant/${restaurantId}`);
  }
}
