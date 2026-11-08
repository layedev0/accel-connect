import { Observable } from 'rxjs';
import { Restaurant } from '../model/restaurant.model';

export interface IRestaurantService {
  getRestaurants(): Observable<Restaurant[]>;
  createRestaurant(restaurant: Restaurant): Observable<Restaurant>;
  updateRestaurant(id: number, restaurant: Restaurant): Observable<Restaurant>;
  deleteRestaurant(id: number): Observable<void>;
  addMealToRestaurant(restaurantId: number, mealId: number): Observable<Restaurant>;
  getRestaurantWithMeals(restaurantId: number): Observable<Restaurant>;
  toggleOrdersBlock(restaurantId: number, blockMessage?: string): Observable<Restaurant>; // 🔒 NOUVEAU
}
