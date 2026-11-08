export interface UserWalletStats {
  userId: number;
  username: string;
  email: string;
  totalDeposited: number;
  totalSpent: number;
  balance: number;
  orderCount: number;
  status: string;
  hasDebt: boolean;
  debtAmount: number;
  refundableAmount: number;
  totalOrders: number;
}

// DTO pour le dépôt
export interface DepositRequest {
  userId: number;
  amount: number;
  description?: string;
}

// DTO pour la mise à jour du solde
export interface UpdateBalanceRequest {
  userId: number;
  newBalance: number;
  reason: string;
}

export interface DepositResponse {
  userId: number;
  amount: number;
  newBalance: number;
  timestamp: string;
}

export interface AddDebtRequest {
  userId: number;
  debtAmount: number;
  reason: string;
}
