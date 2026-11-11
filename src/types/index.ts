export type Screen =
  | 'HOME'
  | 'AGENTS'
  | 'NUMBER_ENTRY'
  | 'VOUCHERS'
  | 'NUMBERS'
  | 'BRAKE'
  | 'SETTLEMENT'
  | 'SETTINGS'
  | 'LAYOFF_HISTORY'
  | 'AGENT_SETTLEMENT';

export interface Agent {
  id: number;
  name: string;
  rate: number;
  mult: number;
  date: string;
  roundNumber: string;
}

export interface Session {
  id: number;
  name: string;
  rate: number;
  mult: number;
  date: string;
  winningNumber?: string | null;
  isClosed?: boolean;
  createdAt?: number;
}

export interface BetItem {
  id: number;
  n: string; // "00" to "99"
  a: number; // amount
}

export interface Voucher {
  id: number;
  no: number;
  agent: string;
  agentId: number;
  date: string;
  session: string;
  time: string;
  items: BetItem[];
}

export interface LayoffItem {
  n: string;
  a: number;
}

export interface LayoffRecord {
  id: number;
  date: string;
  session: string;
  time: string;
  limit: number;
  items: LayoffItem[];
  total: number;
  recordNo: number;
}

export interface WinRecord {
  date: string;
  session: string;
  n: string;
}

export interface SettledRecord {
  id: number;
  date: string;
  session: string;
  winningNumber: string;
  totalRetained: number;
  commission: number;
  retainedNet: number;
  payout: number;
  profitLoss: number;
  timestamp: string;
}

export interface DailySettlement {
  date: string;
  totalSales: number;
  totalCommission: number;
  netSales: number;
  winningPayout: number;
  profitOrLoss: number;
  roundsSummary: string;
  winningNumbersSummary: string;
  roundCount: number;
  timestamp: string;
}

export interface AppSettings {
  password: string;
  brakeLimit: number;
  soundEnabled: boolean;
}

export interface NumberSummary {
  n: string;
  entryCount: number;
  totalBet: number;
  layoffAmount: number;
  netAmount: number;
}

export interface NumberAgentBreakdownItem {
  agentId: number;
  agentName: string;
  rate: number;
  mult: number;
  entryCount: number;
  betOnNumber: number;
  totalAgentSales: number;
  commissionOnNumber: number;
  totalSalesCommission: number;
  winningPayout: number;
}

export interface AgentSummaryData {
  totalBet: number;
  commission: number;
  netAmount: number;
  voucherCount: number;
  payout: number;
}

export interface Agent2DSettlement {
  agentId: number;
  agentName: string;
  commissionRate: number;
  multiplier: number;
  totalSales: number;
  commissionAmount: number;
  netSales: number;
  winningBetAmount: number;
  payoutAmount: number;
  netBalance: number;
}

export const MAX_BET_AMOUNT = 999_999_999;

export type BetAmountValidationResult =
  | 'Valid'
  | 'Empty'
  | 'NonNumeric'
  | 'ZeroOrNegative'
  | 'TooLarge';
