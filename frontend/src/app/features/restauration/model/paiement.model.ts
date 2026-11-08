export interface RestaurantPaymentSummary {
  restaurantId: number;
  restaurantName: string;
  totalAmountWithSubsidy: number;
  totalAmountWithoutSubsidy: number;
  subsidyAmount: number;
  deliveryFees: number;
  orderCount: number;
  userCount: number;
}

export interface PaymentSummary {
  startDate: string;
  endDate: string;
  lastPaymentDate: string | null;
  restaurants: RestaurantPaymentSummary[];
  globalStats: {
    totalAmountWithSubsidy: number;
    totalAmountWithoutSubsidy: number;
    totalSubsidy: number;
    totalDeliveryFees: number;
    totalOrders: number;
    totalUsers: number;
  };
}

export interface PaymentValidationRequest {
  startDate: string;
  endDate: string;
}

export interface PaymentValidationResponse {
  success: boolean;
  message: string;
  deliveryFeesCalculated: boolean;
  affectedOrders: number;
  affectedUsers: number;
  paymentDate: string;
}
