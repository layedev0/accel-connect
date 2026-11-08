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

// DTO pour les statistiques utilisateur
export interface UserBalanceStatsDTO {
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
}

// Historique d'un depot
export interface DepositHistoryDTO {
  amount: number;
  date: string;
  description: string;
}

// Depenses d'une semaine
export interface WeeklySpendingDTO {
  weekLabel: string;
  totalSpent: number;
}

// DTO pour le solde utilisateur
export interface UserBalanceDTO {
  userId: number;
  username: string;
  totalDeposited: number;
  totalSpent: number;
  balance: number;
  hasDebt: boolean;
  debtAmount: number;
  refundableAmount: number;
  message: string;

  // Dernier depot
  lastDepositAmount: number;
  lastDepositDate: string;

  // Historique des 4 derniers depots
  recentDeposits: DepositHistoryDTO[];

  // Depenses semaine en cours
  currentWeekSpent: number;

  // Depenses des 4 dernieres semaines
  recentWeeksSpending: WeeklySpendingDTO[];

  // Avoir en caisse
  avoir: number;

  // Dette
  debt: number;
}
