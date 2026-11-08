import { OrderDTO } from '../../restauration/model/order.model';
/**
 * 🏪 Statistiques par restaurant
 */
export interface RestaurantStats {
  restaurantId: number;
  restaurantName: string;
  totalOrders: number;
  totalAmount: number;
  confirmedCount: number;
  cancelledCount: number;
}

/**
 * 👤 Commande utilisateur enrichie
 */
export interface UserOrderSummary {
  userId: number;
  username: string;
  email: string;
  orders: OrderDTO[];
  totalAmount: number;
  restaurants: string[]; // Liste des restaurants
  meals: { name: string; quantity: number; restaurant: string }[];
  balance: number;
  hasDebt: boolean;
}

/**
 * 📊 Vue globale du dashboard
 */
export interface DashboardView {
  date: string;
  restaurantStats: RestaurantStats[];
  userOrders: UserOrderSummary[];
  globalStats: {
    totalOrders: number;
    totalAmount: number;
    totalUsers: number;
    totalRestaurants: number;
  };
}

/**
 * 🔍 Filtres dashboard
 */
export interface DashboardFilters {
  selectedDate: string;
  selectedRestaurantId: number | null;
  searchTerm: string;
  showCancelled: boolean;
}
