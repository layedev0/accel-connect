export interface WeeklyBudget {
  id: number;
  userId: number;
  username: string;
  weekStartDate: string;
  weekEndDate: string;
  depositedAmount: number;
  spentAmount: number;
  deliveryFees: number;
  balance: number;
  isInDebt: boolean;
  debtAmount: number;
  availableAmount: number;
  deliveryFeesCalculated: boolean;
  deliveryFeesCalculatedAt?: string;
}

export interface UserBudgetStats {
  weeklyDeposit: number;
  weeklySpent: number;
  remainingBalance: number;
  isInDebt: boolean;
  statusMessage: string;
}

export interface DepositRequest {
  userId: number;
  amount: number;
  description?: string;
}
