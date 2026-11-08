import { Menu } from "./menu.model";
import { Restaurant } from "./restaurant.model";

export interface MealDisplay {
  id?: number;
  mealName: string;
  unitPrice: number;
  quantity: number;
  imageUrl?: string | null;  // URL de l'image du plat
  gradientFrom: string;
  gradientTo: string;
  hoverBorder: string;
}

export interface RestaurantWithMenu {
  restaurant: Restaurant;
  menu: Menu | null;
  hasMenu: boolean;
  itemCount: number;
}

export interface RestaurantDisplay {
  id: number;
  name: string;
  subtitle: string;
  logoUrl?: string | null;  // URL du logo du restaurant
  icon: string;
  gradientFrom: string;
  gradientTo: string;
  hasMenu: boolean;
  items: MealDisplay[];
}

export interface Stat {
  id: string;
  label: string;
  value: string;
  icon: string;
  gradientFrom: string;
  gradientTo: string;
}