import { Menu } from './menu.model';
import { Restaurant } from './restaurant.model';

export interface MealDisplay {
  id?: number;
  mealName: string;
  unitPrice: number;
  imageUrl?: string;
  quantity: number;
  // Pour le style UI
  gradientFrom?: string;
  gradientTo?: string;
  hoverBorder?: string;
}

export interface RestaurantDisplay {
  id: number;
  name: string;
  subtitle?: string;
  logoUrl?: string;
  icon: string;
  ordersBlocked?: boolean;
  blockMessage?: string;
  gradientFrom: string;
  gradientTo: string;
  hasMenu: boolean;
  items: MealDisplay[];
}

export interface RestaurantWithMenu {
  restaurant: Restaurant;
  menu: Menu | null;
  hasMenu: boolean;
  itemCount: number;
}