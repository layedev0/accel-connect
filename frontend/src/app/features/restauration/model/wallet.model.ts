export interface Wallet {
  id: number;
  userId: number;
  username: string;
  balance: number;
  totalDeposited: number;
  totalSpent: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface WalletBalance {
  balance: number;
  creditBalance: number;
  debitBalance: number;
  totalDeposited: number;
  totalSpent: number;
  message: string;
  hasCredit: boolean;     
  creditAmount: number;  
}

export interface Transaction {
  id: number;
  walletId: number;
  orderId?: number;
  orderReference?: string;
  type: TransactionType;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string;
  createdByUsername?: string;
  createdAt: Date;
}

export enum TransactionType {
  CREDIT = 'CREDIT',
  DEBIT = 'DEBIT',
  REFUND = 'REFUND',
}