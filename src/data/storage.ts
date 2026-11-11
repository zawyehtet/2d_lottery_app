import {
  Agent,
  AppSettings,
  DailySettlement,
  LayoffRecord,
  Session,
  SettledRecord,
  Voucher,
  WinRecord,
} from '../types';
import { toAsciiDigits } from '../logic/patternAlgorithms';

const KEY_AGENTS = 'lucky2d_agents';
const KEY_SESSIONS = 'lucky2d_sessions';
const KEY_VOUCHERS = 'lucky2d_vouchers';
const KEY_LAYOFFS = 'lucky2d_layoffs';
const KEY_BLOCKED = 'lucky2d_blocked';
const KEY_WINS = 'lucky2d_wins';
const KEY_SETTLED = 'lucky2d_settled';
const KEY_DAILY_SETTLED = 'lucky2d_daily_settled';
const KEY_SETTINGS = 'lucky2d_settings';
const PREF_LAST_SELECTED_SESSION = 'last_active_session';

export function loadSettings(): AppSettings {
  try {
    const raw = localStorage.getItem(KEY_SETTINGS);
    if (!raw) {
      const def: AppSettings = { password: '1234', brakeLimit: 0, soundEnabled: true };
      localStorage.setItem(KEY_SETTINGS, JSON.stringify(def));
      return def;
    }
    const parsed = JSON.parse(raw);
    return {
      password: parsed.password ?? '1234',
      brakeLimit: Number(parsed.brakeLimit) || 0,
      soundEnabled: parsed.soundEnabled ?? true,
    };
  } catch {
    return { password: '1234', brakeLimit: 0, soundEnabled: true };
  }
}

export function saveSettings(settings: AppSettings): void {
  try {
    localStorage.setItem(KEY_SETTINGS, JSON.stringify(settings));
  } catch {
    // Ignore
  }
}

export function getLastActiveSession(): string {
  return localStorage.getItem(PREF_LAST_SELECTED_SESSION) || '';
}

export function saveLastActiveSession(session: string): void {
  localStorage.setItem(PREF_LAST_SELECTED_SESSION, session.trim());
}

export function loadSessionsForDate(date: string): Session[] {
  const asciiDate = toAsciiDigits(date);
  try {
    const dateKey = `${KEY_SESSIONS}_${asciiDate}`;
    const raw = localStorage.getItem(dateKey);
    if (raw) {
      return JSON.parse(raw);
    }
    // Fallback: check global sessions
    const all = loadSessions();
    const filtered = all.filter((s) => toAsciiDigits(s.date) === asciiDate);
    if (filtered.length > 0) return filtered;

    // Default 2 sessions if none exist yet for this date
    const defaults: Session[] = [
      { id: Date.now(), name: '12.AM', rate: 15, mult: 80, date: asciiDate },
      { id: Date.now() + 1, name: '4.PM', rate: 15, mult: 80, date: asciiDate },
    ];
    saveSessionsForDate(asciiDate, defaults);
    return defaults;
  } catch {
    return [];
  }
}

export function saveSessionsForDate(date: string, sessions: Session[]): void {
  const asciiDate = toAsciiDigits(date);
  try {
    localStorage.setItem(`${KEY_SESSIONS}_${asciiDate}`, JSON.stringify(sessions));
    // also sync to global sessions
    const all = loadSessions().filter((s) => toAsciiDigits(s.date) !== asciiDate);
    localStorage.setItem(KEY_SESSIONS, JSON.stringify([...all, ...sessions]));
  } catch {
    // Ignore
  }
}

export function loadSessions(): Session[] {
  try {
    const raw = localStorage.getItem(KEY_SESSIONS);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveSessions(sessions: Session[]): void {
  try {
    localStorage.setItem(KEY_SESSIONS, JSON.stringify(sessions));
  } catch {
    // Ignore
  }
}

export function loadAgents(): Agent[] {
  try {
    const raw = localStorage.getItem(KEY_AGENTS);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveAgents(agents: Agent[]): void {
  try {
    localStorage.setItem(KEY_AGENTS, JSON.stringify(agents));
  } catch {
    // Ignore
  }
}

export function loadAgentsForDateAndRound(date: string, round: string): Agent[] {
  const asciiDate = toAsciiDigits(date);
  const all = loadAgents();
  return all.filter((a) => toAsciiDigits(a.date) === asciiDate && a.roundNumber === round);
}

export function insertOrUpdateAgentForSession(date: string, round: string, agent: Agent): Agent[] {
  const asciiDate = toAsciiDigits(date);
  const all = loadAgents();
  const existingIdx = all.findIndex(
    (a) => toAsciiDigits(a.date) === asciiDate && a.roundNumber === round && a.id === agent.id
  );
  const updatedAgent = { ...agent, date: asciiDate, roundNumber: round };
  if (existingIdx !== -1) {
    all[existingIdx] = updatedAgent;
  } else {
    all.push(updatedAgent);
  }
  saveAgents(all);
  return all.filter((a) => toAsciiDigits(a.date) === asciiDate && a.roundNumber === round);
}

export function deleteAgentBySession(date: string, round: string, agentId: number): Agent[] {
  const asciiDate = toAsciiDigits(date);
  const all = loadAgents();
  const filtered = all.filter(
    (a) => !(toAsciiDigits(a.date) === asciiDate && a.roundNumber === round && a.id === agentId)
  );
  saveAgents(filtered);
  return filtered.filter((a) => toAsciiDigits(a.date) === asciiDate && a.roundNumber === round);
}

export function loadVouchers(): Voucher[] {
  try {
    const raw = localStorage.getItem(KEY_VOUCHERS);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveVouchers(vouchers: Voucher[]): void {
  try {
    localStorage.setItem(KEY_VOUCHERS, JSON.stringify(vouchers));
  } catch {
    // Ignore
  }
}

export function loadLayoffs(): LayoffRecord[] {
  try {
    const raw = localStorage.getItem(KEY_LAYOFFS);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveLayoffs(layoffs: LayoffRecord[]): void {
  try {
    localStorage.setItem(KEY_LAYOFFS, JSON.stringify(layoffs));
  } catch {
    // Ignore
  }
}

export function loadBlocked(): Set<string> {
  try {
    const raw = localStorage.getItem(KEY_BLOCKED);
    if (raw) {
      const arr = JSON.parse(raw);
      return new Set(arr);
    }
    return new Set();
  } catch {
    return new Set();
  }
}

export function saveBlocked(blocked: Set<string>): void {
  try {
    localStorage.setItem(KEY_BLOCKED, JSON.stringify(Array.from(blocked)));
  } catch {
    // Ignore
  }
}

export function loadWins(): WinRecord[] {
  try {
    const raw = localStorage.getItem(KEY_WINS);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveWins(wins: WinRecord[]): void {
  try {
    localStorage.setItem(KEY_WINS, JSON.stringify(wins));
  } catch {
    // Ignore
  }
}

export function loadSettled(): SettledRecord[] {
  try {
    const raw = localStorage.getItem(KEY_SETTLED);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveSettled(records: SettledRecord[]): void {
  try {
    localStorage.setItem(KEY_SETTLED, JSON.stringify(records));
    rebuildDailySettlements(records);
  } catch {
    // Ignore
  }
}

export function loadDailySettlements(): DailySettlement[] {
  try {
    const raw = localStorage.getItem(KEY_DAILY_SETTLED);
    if (raw) {
      return JSON.parse(raw);
    }
    return [];
  } catch {
    return [];
  }
}

export function saveDailySettlements(daily: DailySettlement[]): void {
  try {
    localStorage.setItem(KEY_DAILY_SETTLED, JSON.stringify(daily));
  } catch {
    // Ignore
  }
}

function rebuildDailySettlements(records: SettledRecord[]): void {
  const grouped: Record<string, SettledRecord[]> = {};
  for (const r of records) {
    if (!grouped[r.date]) grouped[r.date] = [];
    grouped[r.date].push(r);
  }

  const dailyList: DailySettlement[] = Object.entries(grouped).map(([date, items]) => {
    const ordered = [...items].reverse();
    return {
      date,
      totalSales: items.reduce((sum, x) => sum + x.totalRetained, 0),
      totalCommission: items.reduce((sum, x) => sum + x.commission, 0),
      netSales: items.reduce((sum, x) => sum + x.retainedNet, 0),
      winningPayout: items.reduce((sum, x) => sum + x.payout, 0),
      profitOrLoss: items.reduce((sum, x) => sum + x.profitLoss, 0),
      roundsSummary: ordered.map((x) => x.session).join(', '),
      winningNumbersSummary: ordered.map((x) => `${x.session}:${x.winningNumber || '-'}`).join(', '),
      roundCount: items.length,
      timestamp: items[0]?.timestamp || '',
    };
  });

  saveDailySettlements(dailyList);
}

export function resetAllData(): void {
  try {
    const keysToRemove = [
      KEY_AGENTS,
      KEY_SESSIONS,
      KEY_VOUCHERS,
      KEY_LAYOFFS,
      KEY_BLOCKED,
      KEY_WINS,
      KEY_SETTLED,
      KEY_DAILY_SETTLED,
      PREF_LAST_SELECTED_SESSION,
    ];
    for (const key of keysToRemove) {
      localStorage.removeItem(key);
    }
    // Also remove any date-specific keys like lucky2d_sessions_2026-11-11
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k && k.startsWith('lucky2d_sessions_')) {
        localStorage.removeItem(k);
      }
    }
  } catch {
    // Ignore
  }
}
