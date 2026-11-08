/**
 * 💰 Récapitulatif de paiement pour un restaurant
 */
export interface RestaurantPaymentSummary {
  restaurantId: number;
  restaurantName: string;
  totalAmountWithSubsidy: number; // Montant avec subvention
  totalAmountWithoutSubsidy: number; // Montant réel à payer au restaurant
  subsidyAmount: number; // Subvention totale
  deliveryFees: number; // Frais de livraison
  orderCount: number;
  userCount: number;
}

/**
 * 📊 Récapitulatif global de paiement
 */
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

/**
 * ✅ Requête de validation de paiement
 */
export interface PaymentValidationRequest {
  startDate: string;
  endDate: string;
}

/**
 * 📝 Réponse de validation de paiement
 */
export interface PaymentValidationResponse {
  success: boolean;
  message: string;
  deliveryFeesCalculated: boolean;
  affectedOrders: number;
  affectedUsers: number;
  paymentDate: string;
}
