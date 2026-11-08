import { Observable } from "rxjs";
import { CreateMenuRequest, Menu } from "../model/menu.model";

export interface IMenuService {
  createOrUpdateMenu(request: CreateMenuRequest): Observable<Menu>;
  getTodayMenu(restaurantId: number): Observable<Menu>;
  getMenuByDate(restaurantId: number, date: string): Observable<Menu>;
  getAllMenusByRestaurant(restaurantId: number): Observable<Menu[]>;
  getActiveMenus(date: string): Observable<Menu[]>;
  deleteMenu(menuId: number): Observable<void>;
}
