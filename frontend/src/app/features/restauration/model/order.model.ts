// ========================================
// MODÈLES FRONTEND (order.model.ts)
// ========================================
export interface OrderDTO {
  id: number;
  userId: number;
  username: string;
  orderDate: string; // Format: YYYY-MM-DD

  // ✅ AJOUT: Nom du restaurant principal (du premier item)
  restaurantName?: string;

  // Montants
  subtotal: number;
  subsidyAmount: number;
  amountAfterSubsidy: number;
  setAmountAfterSubsidy?: number; // Pour calcul alternatif
  deliveryFeeShare: number;
  totalAmount: number;

  status: OrderStatus;
  items: OrderItemDTO[];
  createdAt: string;
}

export interface OrderItemDTO {
  id: number;
  mealId: number;
  mealName: string;
  restaurantId: number;
  restaurantName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export enum OrderStatus {
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
}

export type OrderFilterValue = 'all' | OrderStatus;

export interface OrderFilter {
  value: OrderFilterValue;
  label: string;
  icon: string;
}

export const ORDER_FILTERS: OrderFilter[] = [
  { value: 'all', label: 'Toutes', icon: 'list' },
  { value: OrderStatus.CONFIRMED, label: 'Confirmées', icon: 'check-circle' },
  { value: OrderStatus.CANCELLED, label: 'Annulées', icon: 'times-circle' },
];