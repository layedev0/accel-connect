export interface CartItem {
  mealId: number;
  mealName: string;
  restaurantId: number;
  restaurantName: string;
  unitPrice: number;
  quantity: number;
  imageUrl?: string;
}

export interface CartSummary {
  totalItems: number;
  subtotal: number;
  subsidyAmount: number; // 1000 FCFA
  amountAfterSubsidy: number; // max(0, subtotal - 1000)
  deliveryFeesNote: string;
  items: CartItem[];
}

export interface OrderCreationPayload {
  items: {
    mealId: number;
    restaurantId: number;
    quantity: number;
    unitPrice: number;
  }[];
}