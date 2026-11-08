import { Injectable } from '@angular/core';
import { IRestaurantService } from '../IRestaurantService';
import { Observable } from 'rxjs';
import { Restaurant } from '../../model/restaurant.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class RestaurantService implements IRestaurantService {
  constructor(private readonly httpClient: HttpClient) {}

  getRestaurants(): Observable<Restaurant[]> {
    return this.httpClient.get<Restaurant[]>(`${environment.apiUrl}/restaurants/all`);
  }

  createRestaurant(restaurant: Restaurant): Observable<Restaurant> {
    return this.httpClient.post<Restaurant>(`${environment.apiUrl}/restaurants`, restaurant);
  }

  updateRestaurant(id: number, restaurant: Restaurant): Observable<Restaurant> {
    return this.httpClient.put<Restaurant>(`${environment.apiUrl}/restaurants/${id}`, restaurant);
  }

  deleteRestaurant(id: number): Observable<void> {
    return this.httpClient.delete<void>(`${environment.apiUrl}/restaurants/${id}`);
  }

  addMealToRestaurant(restaurantId: number, mealId: number): Observable<Restaurant> {
    return this.httpClient.post<Restaurant>(
      `${environment.apiUrl}/restaurants/${restaurantId}/meals/${mealId}`,
      {}
    );
  }

  getRestaurantWithMeals(restaurantId: number): Observable<Restaurant> {
    return this.httpClient.get<Restaurant>(`${environment.apiUrl}/restaurants/${restaurantId}`);
  }

  toggleOrdersBlock(restaurantId: number, blockMessage?: string): Observable<Restaurant> {
    return this.httpClient.post<Restaurant>(
      `${environment.apiUrl}/restaurants/${restaurantId}/toggle-block`,
      blockMessage ? { blockMessage } : {}
    );
  }
}
