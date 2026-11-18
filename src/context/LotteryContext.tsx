import React, { createContext, useContext, useState, useEffect, useMemo, useCallback } from 'react';
import {
  Agent,
  Agent2DSettlement,
  AgentSummaryData,
  AppSettings,
  BetItem,
  DailySettlement,
  LayoffItem,
  LayoffRecord,
  NumberAgentBreakdownItem,
  NumberSummary,
  Screen,
  Session,
  SettledRecord,
  Voucher,
  WinRecord,
} from '../types';
import * as storage from '../data/storage';
import { soundManager } from '../util/soundManager';
import { toAsciiDigits, reverse2D, validateBetAmount } from '../logic/patternAlgorithms';

function getTodayIso(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function getTodayFormattedTime(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const mins = String(d.getMinutes()).padStart(2, '0');
  const secs = String(d.getSeconds()).padStart(2, '0');
  return `${year}.${month}.${day}/${hours}:${mins}:${secs}`;
}

export function formatDotDate(dateStr: string): string {
  try {
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}.${parts[1]}.${parts[0]}`;
    }
    return dateStr;
  } catch {
    return dateStr;
  }
}

export function fmt(amount: number): string {
  return new Intl.NumberFormat('en-US').format(amount);
}

export function fmtThousands(amount: number): string {
  return new Intl.NumberFormat('en-US').format(amount);
}

export interface CommitVoucherResult {
  voucher: Voucher;
  agentSlipCount: number;
  totalAmount: number;
  agent: Agent;
}

interface LotteryContextValue {
  currentScreen: Screen;
  navigateTo: (screen: Screen) => void;
  goBack: () => boolean;
  selectedDate: string;
  setDate: (date: string) => void;
  selectedSession: string;
  setSession: (session: string) => void;
  sessions: Session[];
  availableRounds: string[];
  hasRound: boolean;
  agents: Agent[];
  addAgent: (name: string, rate: number, mult: number, phoneNumber?: string) => void;
  updateAgent: (agent: Agent) => void;
  deleteAgent: (agentId: number) => void;
  vouchers: Voucher[];
  activeVouchers: Voucher[];
  deleteVoucher: (id: number) => boolean;
  editNumber: (numberId: number, newNum: string, newAmt: number) => boolean;
  deleteNumber: (numberId: number) => boolean;
  layoffs: LayoffRecord[];
  deleteLayoffRecord: (id: number) => void;
  editLayoffItem: (recordId: number, itemIndex: number, newNum: string, newAmt: number) => boolean;
  deleteLayoffItem: (recordId: number, itemIndex: number) => void;
  blocked: Set<string>;
  toggleBlockedNumber: (num: string) => void;
  wins: WinRecord[];
  currentWinningNumber: string;
  setWinningNumber: (num: string) => void;
  updateWinRecord: (oldRecord: WinRecord, newRecord: WinRecord) => void;
  deleteWinRecord: (record: WinRecord) => void;
  settled: SettledRecord[];
  dailySettlements: DailySettlement[];
  settings: AppSettings;
  updateSettings: (newSettings: Partial<AppSettings>) => void;
  updateBrakeLimit: (limitStr: string) => void;
  saveBrakeLimit: () => void;
  currentBrakeInput: string;
  savedBrakeLimitText: string;
  entryAgent: Agent | null;
  setEntryAgent: (agent: Agent | null) => void;
  entryItems: BetItem[];
  addEntryItem: (item: BetItem) => boolean;
  addEntryItems: (items: BetItem[]) => boolean;
  editEntryItem: (index: number, newNum: string, newAmt: number) => boolean;
  deleteEntryItem: (index: number) => boolean;
  clearEntryItems: () => void;
  commitCurrentVoucher: () => CommitVoucherResult | null;
  startNumberEntry: (agent: Agent) => void;
  voucherFilterAgent: string;
  setVoucherFilterAgent: (name: string) => void;
  voucherFilterAgentId: number | null;
  openAgentVouchers: (agentId: number | null, agentName?: string) => void;
  clearVoucherFilter: () => void;
  executeLayoff: (isAll: boolean) => void;
  lockAndSaveBrakeSettlement: () => void;
  deleteSettledRecord: (recordId: number) => void;
  startEditingSettledRecord: (record: SettledRecord) => void;
  cancelEditingSettledRecord: () => void;
  editingSettledRecord: SettledRecord | null;
  saveEditedSettledRecord: (data: {
    date: string;
    session: string;
    winningNumber: string;
    totalRetained: number;
    commission: number;
    payout: number;
  }) => void;
  clearSettledLedger: () => void;
  addSession: (name: string, rate: number, mult: number) => boolean;
  updateSessionRecord: (targetId: number | null, oldName: string | null, updated: Session) => boolean;
  deleteSessionRecord: (targetId: number, name: string) => void;
  open2DSummaryFromWinningNumber: (winningNumber?: string, session?: string) => void;
  activeWinningNumberFor2DSummary: string | null;
  resetAllData: () => void;
  toastMessage: string | null;
  showToast: (msg: string) => void;
  getAll100Numbers: () => NumberSummary[];
  sort2DNumbersWithWinning: (all: NumberSummary[], winningNum: string | null) => NumberSummary[];
  calculateSettlementForSession: (date?: string, session?: string, winningNum?: string) => {
    totalRetained: number;
    commission: number;
    retainedNet: number;
    payout: number;
    profitLoss: number;
  };
  getAgentSummaryData: (agent: Agent, winNum?: string) => AgentSummaryData;
  getAgent2DSettlements: (winningNumber: string) => Agent2DSettlement[];
  getNumberAgentBreakdown: (targetNum: string) => NumberAgentBreakdownItem[];
  buildDailySettlementsTextSummary: () => string;
}

const LotteryContext = createContext<LotteryContextValue | null>(null);

export const LotteryProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const todayIso = useMemo(() => getTodayIso(), []);
  const [selectedDate, setSelectedDateState] = useState(todayIso);
  const [selectedSession, setSelectedSessionState] = useState('');
  const [screenHistory, setScreenHistory] = useState<Screen[]>(['HOME']);
  const currentScreen = screenHistory[screenHistory.length - 1] || 'HOME';

  const [sessions, setSessions] = useState<Session[]>([]);
  const [agents, setAgents] = useState<Agent[]>([]);
  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [layoffs, setLayoffs] = useState<LayoffRecord[]>([]);
  const [blocked, setBlocked] = useState<Set<string>>(new Set());
  const [wins, setWins] = useState<WinRecord[]>([]);
  const [settled, setSettled] = useState<SettledRecord[]>([]);
  const [dailySettlements, setDailySettlements] = useState<DailySettlement[]>([]);
  const [settings, setSettingsState] = useState<AppSettings>(() => storage.loadSettings());

  const [currentBrakeInput, setCurrentBrakeInput] = useState('');
  const [savedBrakeLimitText, setSavedBrakeLimitText] = useState('');

  // Number Entry Draft
  const [entryAgent, setEntryAgent] = useState<Agent | null>(null);
  const [entryItems, setEntryItems] = useState<BetItem[]>([]);

  // Filters & Subscreen Modals
  const [voucherFilterAgent, setVoucherFilterAgent] = useState('ALL');
  const [voucherFilterAgentId, setVoucherFilterAgentId] = useState<number | null>(null);
  const [activeWinningNumberFor2DSummary, setActiveWinningNumberFor2DSummary] = useState<string | null>(null);
  const [editingSettledRecord, setEditingSettledRecord] = useState<SettledRecord | null>(null);

  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage((cur) => (cur === msg ? null : cur));
    }, 2500);
  }, []);

  // Initial Load
  useEffect(() => {
    const s = storage.loadSettings();
    setSettingsState(s);
    soundManager.setSoundEnabled(s.soundEnabled);
    if (s.brakeLimit > 0) {
      setCurrentBrakeInput(String(s.brakeLimit));
      setSavedBrakeLimitText(String(s.brakeLimit));
    }

    const loadedSessions = storage.loadSessionsForDate(todayIso);
    setSessions(loadedSessions);

    const loadedAgents = storage.loadAgents();
    setAgents(loadedAgents);

    const loadedVouchers = storage.loadVouchers();
    setVouchers(loadedVouchers);

    const loadedLayoffs = storage.loadLayoffs();
    setLayoffs(loadedLayoffs);

    const loadedBlocked = storage.loadBlocked();
    setBlocked(loadedBlocked);

    const loadedWins = storage.loadWins();
    setWins(loadedWins);

    const loadedSettled = storage.loadSettled();
    setSettled(loadedSettled);

    const loadedDaily = storage.loadDailySettlements();
    setDailySettlements(loadedDaily);

    const lastSession = storage.getLastActiveSession();
    if (lastSession && loadedSessions.some((x) => x.name === lastSession)) {
      setSelectedSessionState(lastSession);
    } else if (loadedSessions.length > 0) {
      setSelectedSessionState(loadedSessions[0].name);
    }
  }, [todayIso]);

  // Available rounds
  const availableRounds = useMemo(() => {
    const cleanDate = toAsciiDigits(selectedDate);
    const configured = sessions.map((s) => s.name.trim()).filter(Boolean);
    const vRounds = vouchers.filter((v) => toAsciiDigits(v.date) === cleanDate).map((v) => v.session);
    const wRounds = wins.filter((w) => toAsciiDigits(w.date) === cleanDate).map((w) => w.session);
    const lRounds = layoffs.filter((l) => toAsciiDigits(l.date) === cleanDate).map((l) => l.session);
    const sRounds = settled.filter((s) => toAsciiDigits(s.date) === cleanDate).map((s) => s.session);
    return Array.from(new Set([...configured, ...vRounds, ...wRounds, ...lRounds, ...sRounds]));
  }, [selectedDate, sessions, vouchers, wins, layoffs, settled]);

  const hasRound = useMemo(() => {
    return Boolean(selectedSession && availableRounds.includes(selectedSession));
  }, [selectedSession, availableRounds]);

  const setDate = useCallback(
    (newDate: string) => {
      const clean = toAsciiDigits(newDate);
      setSelectedDateState(clean);
      const dateSessions = storage.loadSessionsForDate(clean);
      setSessions(dateSessions);
      if (dateSessions.length > 0 && (!selectedSession || !dateSessions.some((s) => s.name === selectedSession))) {
        setSelectedSessionState(dateSessions[0].name);
      }
    },
    [selectedSession]
  );

  const setSession = useCallback((sess: string) => {
    const trimmed = sess.trim();
    setSelectedSessionState(trimmed);
    storage.saveLastActiveSession(trimmed);
  }, []);

  const navigateTo = useCallback((screen: Screen) => {
    setScreenHistory((prev) => {
      if (prev[prev.length - 1] === screen) return prev;
      const existing = prev.indexOf(screen);
      if (existing !== -1) {
        return prev.slice(0, existing + 1);
      }
      return [...prev, screen];
    });
  }, []);

  const goBack = useCallback((): boolean => {
    if (editingSettledRecord && currentScreen === 'HOME') {
      setEditingSettledRecord(null);
      return true;
    }
    if (screenHistory.length > 1) {
      setScreenHistory((prev) => prev.slice(0, prev.length - 1));
      return true;
    } else if (currentScreen !== 'HOME') {
      setScreenHistory(['HOME']);
      return true;
    }
    return false;
  }, [editingSettledRecord, currentScreen, screenHistory]);

  const updateSettings = useCallback((newSettings: Partial<AppSettings>) => {
    setSettingsState((prev) => {
      const merged = { ...prev, ...newSettings };
      storage.saveSettings(merged);
      if (newSettings.soundEnabled !== undefined) {
        soundManager.setSoundEnabled(newSettings.soundEnabled);
      }
      return merged;
    });
  }, []);

  const updateBrakeLimit = useCallback((limitStr: string) => {
    setCurrentBrakeInput(limitStr);
  }, []);

  const saveBrakeLimit = useCallback(() => {
    const limit = parseInt(currentBrakeInput, 10) || 0;
    updateSettings({ brakeLimit: limit });
    setSavedBrakeLimitText(currentBrakeInput);
    soundManager.playSave();
    showToast('ဘရိတ် သတ်မှတ်ချက် သိမ်းဆည်းပြီးပါပြီ');
  }, [currentBrakeInput, updateSettings, showToast]);

  // Current winning number
  const currentWinningNumber = useMemo(() => {
    const cleanDate = toAsciiDigits(selectedDate);
    const win = wins.find((w) => toAsciiDigits(w.date) === cleanDate && w.session === selectedSession);
    return win?.n || '';
  }, [wins, selectedDate, selectedSession]);

  const setWinningNumber = useCallback(
    (num: string) => {
      const cleanNum = toAsciiDigits(num).trim();
      const cleanDate = toAsciiDigits(selectedDate);
      if (cleanNum.length !== 2) return;

      const newRecord: WinRecord = { date: cleanDate, session: selectedSession, n: cleanNum };
      setWins((prev) => {
        const filtered = prev.filter(
          (w) => !(toAsciiDigits(w.date) === cleanDate && w.session === selectedSession)
        );
        const updated = [...filtered, newRecord];
        storage.saveWins(updated);
        return updated;
      });
      soundManager.playSave();
      showToast(`ထွက်ဂဏန်း (${cleanNum}) သိမ်းဆည်းပြီးပါပြီ`);
    },
    [selectedDate, selectedSession, showToast]
  );

  const updateWinRecord = useCallback(
    (oldRecord: WinRecord, newRecord: WinRecord) => {
      setWins((prev) => {
        const updated = prev.map((w) =>
          toAsciiDigits(w.date) === toAsciiDigits(oldRecord.date) && w.session === oldRecord.session
            ? newRecord
            : w
        );
        storage.saveWins(updated);
        return updated;
      });
      soundManager.playEdit();
      showToast('ပြင်ဆင်ပြီးပါပြီ');
    },
    [showToast]
  );

  const deleteWinRecord = useCallback(
    (record: WinRecord) => {
      setWins((prev) => {
        const updated = prev.filter(
          (w) => !(toAsciiDigits(w.date) === toAsciiDigits(record.date) && w.session === record.session)
        );
        storage.saveWins(updated);
        return updated;
      });
      soundManager.playDelete();
      showToast('ဖျက်ပြီးပါပြီ');
    },
    [showToast]
  );

  // Agent CRUD
  const addAgent = useCallback(
    (name: string, rate: number, mult: number, phoneNumber?: string) => {
      const cleanName = name.trim();
      if (!cleanName) return;
      const cleanPhone = (phoneNumber || '').trim();
      const nextId = (agents.reduce((max, a) => Math.max(max, a.id), 0) || 0) + 1;
      const newAgent: Agent = {
        id: nextId,
        name: cleanName,
        rate,
        mult,
        phoneNumber: cleanPhone,
        date: toAsciiDigits(selectedDate),
        roundNumber: selectedSession,
      };
      storage.insertOrUpdateAgentForSession(selectedDate, selectedSession, newAgent);
      setAgents(storage.loadAgents());
      soundManager.playSave();
      showToast('ကော်မရှင် အသစ် ထည့်သွင်းပြီးပါပြီ');
    },
    [agents, selectedDate, selectedSession, showToast]
  );

  const updateAgent = useCallback(
    (agent: Agent) => {
      const normalizedAgent: Agent = {
        ...agent,
        phoneNumber: (agent.phoneNumber || '').trim(),
      };
      storage.insertOrUpdateAgentForSession(selectedDate, selectedSession, normalizedAgent);
      setAgents(storage.loadAgents());
      if (entryAgent?.id === normalizedAgent.id) {
        setEntryAgent(normalizedAgent);
      }
      soundManager.playEdit();
      showToast('ပြင်ဆင်ပြီးပါပြီ');
    },
    [selectedDate, selectedSession, entryAgent, showToast]
  );

  const deleteAgent = useCallback(
    (agentId: number) => {
      const target = agents.find((a) => a.id === agentId);
      if (!target) return;
      storage.deleteAgentBySession(selectedDate, selectedSession, agentId);
      setAgents(storage.loadAgents());
      if (entryAgent?.id === agentId) {
        setEntryAgent(null);
      }
      // Also delete vouchers for this agent in current date/session
      const cleanDate = toAsciiDigits(selectedDate);
      setVouchers((prev) => {
        const updated = prev.filter(
          (v) =>
            !(
              toAsciiDigits(v.date) === cleanDate &&
              v.session === selectedSession &&
              (v.agentId === agentId || v.agent === target.name)
            )
        );
        storage.saveVouchers(updated);
        return updated;
      });
      soundManager.playDelete();
      showToast('ဖျက်ပြီးပါပြီ');
    },
    [agents, selectedDate, selectedSession, entryAgent, showToast]
  );

  // Active Vouchers for currently selected Date & Session
  const activeVouchers = useMemo(() => {
    const cleanDate = toAsciiDigits(selectedDate);
    return vouchers.filter(
      (v) => toAsciiDigits(v.date) === cleanDate && v.session === selectedSession
    );
  }, [vouchers, selectedDate, selectedSession]);

  // Vouchers manipulation
  const startNumberEntry = useCallback(
    (agent: Agent) => {
      setEntryAgent(agent);
      setEntryItems([]);
      navigateTo('NUMBER_ENTRY');
    },
    [navigateTo]
  );

  const addEntryItem = useCallback(
    (item: BetItem): boolean => {
      if (!hasRound) {
        showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
        return false;
      }
      if (!entryAgent) {
        showToast('ထိုးသူ ရွေးချယ်ပါ');
        return false;
      }
      if (blocked.has(item.n)) {
        showToast(`${item.n} သည် မရဂဏန်း ဖြစ်ပါသည်`);
        return false;
      }
      setEntryItems((prev) => [...prev, item]);
      soundManager.playSave();
      return true;
    },
    [hasRound, entryAgent, blocked, showToast]
  );

  const addEntryItems = useCallback(
    (items: BetItem[]): boolean => {
      if (!hasRound) {
        showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
        return false;
      }
      if (!entryAgent) {
        showToast('ထိုးသူ ရွေးချယ်ပါ');
        return false;
      }
      const valid = items.filter((x) => !blocked.has(x.n) && validateBetAmount(x.a).isValid);
      if (valid.length === 0) return false;
      setEntryItems((prev) => [...prev, ...valid]);
      soundManager.playSave();
      return true;
    },
    [hasRound, entryAgent, blocked, showToast]
  );

  const editEntryItem = useCallback((index: number, newNum: string, newAmt: number): boolean => {
    setEntryItems((prev) => {
      if (index < 0 || index >= prev.length) return prev;
      const updated = [...prev];
      updated[index] = { ...updated[index], n: newNum, a: newAmt };
      return updated;
    });
    soundManager.playEdit();
    return true;
  }, []);

  const deleteEntryItem = useCallback((index: number): boolean => {
    setEntryItems((prev) => {
      if (index < 0 || index >= prev.length) return prev;
      const updated = [...prev];
      updated.splice(index, 1);
      return updated;
    });
    soundManager.playDelete();
    return true;
  }, []);

  const clearEntryItems = useCallback(() => {
    setEntryItems([]);
  }, []);

  const commitCurrentVoucher = useCallback((): CommitVoucherResult | null => {
    if (!hasRound) {
      showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
      return null;
    }
    if (!entryAgent) {
      showToast('ထိုးသူ ရွေးချယ်ပါ');
      return null;
    }
    if (entryItems.length === 0) {
      showToast('ထိုးမည့် ဂဏန်းများ ထည့်သွင်းပါ');
      return null;
    }

    const cleanDate = toAsciiDigits(selectedDate);
    const currentAgent = agents.find((a) => a.id === entryAgent.id) || entryAgent;
    const sessionVouchers = vouchers.filter(
      (v) => toAsciiDigits(v.date) === cleanDate && v.session === selectedSession
    );
    const nextNo = (sessionVouchers.reduce((max, v) => Math.max(max, v.no), 0) || 0) + 1;
    const agentSessionVouchers = sessionVouchers.filter(
      (v) => v.agentId === currentAgent.id || v.agent === currentAgent.name
    );
    const agentSlipCount = agentSessionVouchers.length + 1;

    const cleanItems = entryItems.map((item) => ({ ...item, n: toAsciiDigits(item.n) }));
    const totalAmount = cleanItems.reduce((sum, item) => sum + item.a, 0);

    const newVoucher: Voucher = {
      id: Date.now(),
      no: nextNo,
      agent: currentAgent.name,
      agentId: currentAgent.id,
      date: cleanDate,
      session: selectedSession,
      time: getTodayFormattedTime(),
      items: cleanItems,
    };

    setVouchers((prev) => {
      const updated = [...prev, newVoucher];
      storage.saveVouchers(updated);
      return updated;
    });

    setEntryItems([]);
    soundManager.playComplete();
    showToast(`ဘောင်ချာ (No.${nextNo}) သိမ်းဆည်းပြီးပါပြီ`);
    return {
      voucher: newVoucher,
      agentSlipCount,
      totalAmount,
      agent: currentAgent,
    };
  }, [hasRound, entryAgent, entryItems, selectedDate, selectedSession, vouchers, agents, showToast]);

  const deleteVoucher = useCallback(
    (voucherId: number): boolean => {
      setVouchers((prev) => {
        const updated = prev.filter((v) => v.id !== voucherId);
        storage.saveVouchers(updated);
        return updated;
      });
      soundManager.playDelete();
      showToast('ဘောင်ချာ ဖျက်ပြီးပါပြီ');
      return true;
    },
    [showToast]
  );

  const editNumber = useCallback(
    (numberId: number, newNum: string, newAmt: number): boolean => {
      let changed = false;
      setVouchers((prev) => {
        const updated = prev.map((v) => {
          if (v.items.some((i) => i.id === numberId)) {
            changed = true;
            return {
              ...v,
              items: v.items.map((i) => (i.id === numberId ? { ...i, n: newNum, a: newAmt } : i)),
            };
          }
          return v;
        });
        if (changed) {
          storage.saveVouchers(updated);
        }
        return updated;
      });
      if (changed) {
        soundManager.playEdit();
        showToast('ဂဏန်း ပြင်ဆင်ပြီးပါပြီ');
      }
      return changed;
    },
    [showToast]
  );

  const deleteNumber = useCallback(
    (numberId: number): boolean => {
      let changed = false;
      setVouchers((prev) => {
        const updated = prev.map((v) => {
          if (v.items.some((i) => i.id === numberId)) {
            changed = true;
            return {
              ...v,
              items: v.items.filter((i) => i.id !== numberId),
            };
          }
          return v;
        });
        if (changed) {
          storage.saveVouchers(updated);
        }
        return updated;
      });
      if (changed) {
        soundManager.playDelete();
        showToast('ဂဏန်း ဖျက်ပြီးပါပြီ');
      }
      return changed;
    },
    [showToast]
  );

  const openAgentVouchers = useCallback(
    (agentId: number | null, agentName?: string) => {
      setVoucherFilterAgentId(agentId);
      setVoucherFilterAgent(agentName || 'ALL');
      navigateTo('VOUCHERS');
    },
    [navigateTo]
  );

  const clearVoucherFilter = useCallback(() => {
    setVoucherFilterAgent('ALL');
    setVoucherFilterAgentId(null);
  }, []);

  // Blocked Numbers
  const toggleBlockedNumber = useCallback(
    (num: string) => {
      const cleanNum = toAsciiDigits(num).padStart(2, '0').slice(-2);
      setBlocked((prev) => {
        const next = new Set(prev);
        if (next.has(cleanNum)) {
          next.delete(cleanNum);
        } else {
          next.add(cleanNum);
        }
        storage.saveBlocked(next);
        return next;
      });
    },
    []
  );

  // Layoff Management
  const executeLayoff = useCallback(
    (isAll: boolean) => {
      const cleanDate = toAsciiDigits(selectedDate);
      const limit = parseInt(currentBrakeInput, 10) || settings.brakeLimit || 0;

      // Calculate overflow per number
      const full = getAll100NumbersHelper(vouchers, layoffs, cleanDate, selectedSession);
      const overflows: LayoffItem[] = [];
      for (const item of full) {
        if (item.netAmount > limit) {
          overflows.push({
            n: item.n,
            a: item.netAmount - limit,
          });
        }
      }

      if (overflows.length === 0) {
        showToast('ဘရိတ်ကျော် တင်ကွက် မရှိပါ');
        return;
      }

      // If not isAll, sort by amount descending and pick top 30
      const itemsToLayoff = isAll
        ? overflows
        : [...overflows].sort((a, b) => b.a - a.a).slice(0, 30);

      const sessionLayoffs = layoffs.filter(
        (l) => toAsciiDigits(l.date) === cleanDate && l.session === selectedSession
      );
      const nextNo = sessionLayoffs.length + 1;
      const total = itemsToLayoff.reduce((sum, x) => sum + x.a, 0);

      const newRecord: LayoffRecord = {
        id: Date.now(),
        date: cleanDate,
        session: selectedSession,
        time: getTodayFormattedTime(),
        limit,
        items: itemsToLayoff,
        total,
        recordNo: nextNo,
      };

      setLayoffs((prev) => {
        const updated = [...prev, newRecord];
        storage.saveLayoffs(updated);
        return updated;
      });

      soundManager.playSave();
      showToast(`တင်ကွက် No.${nextNo} (${itemsToLayoff.length} ကွက်) တင်ပြီးပါပြီ`);
    },
    [selectedDate, currentBrakeInput, settings.brakeLimit, vouchers, layoffs, selectedSession, showToast]
  );

  const deleteLayoffRecord = useCallback(
    (id: number) => {
      setLayoffs((prev) => {
        const updated = prev.filter((l) => l.id !== id);
        storage.saveLayoffs(updated);
        return updated;
      });
      soundManager.playDelete();
      showToast('တင်ကွက် မှတ်တမ်း ဖျက်ပြီးပါပြီ');
    },
    [showToast]
  );

  const editLayoffItem = useCallback(
    (recordId: number, itemIndex: number, newNum: string, newAmt: number): boolean => {
      let success = false;
      setLayoffs((prev) => {
        const updated = prev.map((rec) => {
          if (rec.id === recordId && itemIndex >= 0 && itemIndex < rec.items.length) {
            success = true;
            const newItems = [...rec.items];
            newItems[itemIndex] = { n: newNum, a: newAmt };
            const newTotal = newItems.reduce((sum, x) => sum + x.a, 0);
            return { ...rec, items: newItems, total: newTotal };
          }
          return rec;
        });
        if (success) {
          storage.saveLayoffs(updated);
        }
        return updated;
      });
      if (success) {
        soundManager.playEdit();
        showToast('ပြင်ဆင်ပြီးပါပြီ');
      }
      return success;
    },
    [showToast]
  );

  const deleteLayoffItem = useCallback(
    (recordId: number, itemIndex: number) => {
      setLayoffs((prev) => {
        const updated = prev.map((rec) => {
          if (rec.id === recordId && itemIndex >= 0 && itemIndex < rec.items.length) {
            const newItems = [...rec.items];
            newItems.splice(itemIndex, 1);
            const newTotal = newItems.reduce((sum, x) => sum + x.a, 0);
            return { ...rec, items: newItems, total: newTotal };
          }
          return rec;
        });
        storage.saveLayoffs(updated);
        return updated;
      });
      soundManager.playDelete();
      showToast('ဖျက်ပြီးပါပြီ');
    },
    [showToast]
  );

  // Settlement calculations
  const calculateSettlementForSession = useCallback(
    (
      dateStr = selectedDate,
      sessionName = selectedSession,
      winningNum = currentWinningNumber
    ) => {
      const cleanDate = toAsciiDigits(dateStr);
      const sessionVouchers = vouchers.filter(
        (v) => toAsciiDigits(v.date) === cleanDate && v.session === sessionName
      );
      const sessionLayoffs = layoffs.filter(
        (l) => toAsciiDigits(l.date) === cleanDate && l.session === sessionName
      );
      const sessionAgents = agents.filter(
        (a) => toAsciiDigits(a.date) === cleanDate && a.roundNumber === sessionName
      );

      // Total bet placed across vouchers
      const totalSales = sessionVouchers.reduce((sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0), 0);
      const totalPassed = sessionLayoffs.reduce((sum, l) => sum + l.total, 0);
      const totalRetained = totalSales - totalPassed;

      // Commission: sum of each agent's (totalBet * (rate / 100))
      let totalCommission = 0;
      sessionAgents.forEach((ag) => {
        const agVouchers = sessionVouchers.filter((v) => v.agentId === ag.id || v.agent === ag.name);
        const agTotal = agVouchers.reduce((sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0), 0);
        totalCommission += Math.round(agTotal * (ag.rate / 100));
      });

      const retainedNet = totalRetained - totalCommission;

      // Payout on winning number
      let payout = 0;
      if (winningNum && winningNum.length === 2) {
        sessionAgents.forEach((ag) => {
          const agVouchers = sessionVouchers.filter((v) => v.agentId === ag.id || v.agent === ag.name);
          let agWinBet = 0;
          agVouchers.forEach((v) => {
            v.items.forEach((item) => {
              if (item.n === winningNum) {
                agWinBet += item.a;
              }
            });
          });
          payout += agWinBet * ag.mult;
        });
      }

      const profitLoss = retainedNet - payout;

      return {
        totalRetained,
        commission: totalCommission,
        retainedNet,
        payout,
        profitLoss,
      };
    },
    [selectedDate, selectedSession, currentWinningNumber, vouchers, layoffs, agents]
  );

  const lockAndSaveBrakeSettlement = useCallback(() => {
    if (!hasRound) {
      showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
      return;
    }
    const cleanDate = toAsciiDigits(selectedDate);
    const calc = calculateSettlementForSession(cleanDate, selectedSession, currentWinningNumber);

    const newRecord: SettledRecord = {
      id: Date.now(),
      date: cleanDate,
      session: selectedSession,
      winningNumber: currentWinningNumber || '-',
      totalRetained: calc.totalRetained,
      commission: calc.commission,
      retainedNet: calc.retainedNet,
      payout: calc.payout,
      profitLoss: calc.profitLoss,
      timestamp: getTodayFormattedTime(),
    };

    setSettled((prev) => {
      // Replace existing settled for same date and round or append
      const filtered = prev.filter(
        (s) => !(toAsciiDigits(s.date) === cleanDate && s.session === selectedSession)
      );
      const updated = [newRecord, ...filtered];
      storage.saveSettled(updated);
      setDailySettlements(storage.loadDailySettlements());
      return updated;
    });

    soundManager.playSave();
    showToast(`ရက်ချုပ် စာရင်း (${selectedSession}) သိမ်းဆည်းပြီးပါပြီ`);
  }, [hasRound, selectedDate, selectedSession, currentWinningNumber, calculateSettlementForSession, showToast]);

  const deleteSettledRecord = useCallback(
    (recordId: number) => {
      setSettled((prev) => {
        const updated = prev.filter((r) => r.id !== recordId);
        storage.saveSettled(updated);
        setDailySettlements(storage.loadDailySettlements());
        return updated;
      });
      soundManager.playDelete();
      showToast('ဖျက်ပြီးပါပြီ');
    },
    [showToast]
  );

  const startEditingSettledRecord = useCallback((record: SettledRecord) => {
    setEditingSettledRecord(record);
  }, []);

  const cancelEditingSettledRecord = useCallback(() => {
    setEditingSettledRecord(null);
  }, []);

  const saveEditedSettledRecord = useCallback(
    (data: {
      date: string;
      session: string;
      winningNumber: string;
      totalRetained: number;
      commission: number;
      payout: number;
    }) => {
      if (!editingSettledRecord) return;
      const targetId = editingSettledRecord.id;
      const retainedNet = data.totalRetained - data.commission;
      const profitLoss = retainedNet - data.payout;

      const updatedRecord: SettledRecord = {
        ...editingSettledRecord,
        date: toAsciiDigits(data.date),
        session: data.session,
        winningNumber: data.winningNumber,
        totalRetained: data.totalRetained,
        commission: data.commission,
        retainedNet,
        payout: data.payout,
        profitLoss,
      };

      setSettled((prev) => {
        const updated = prev.map((r) => (r.id === targetId ? updatedRecord : r));
        storage.saveSettled(updated);
        setDailySettlements(storage.loadDailySettlements());
        return updated;
      });

      setEditingSettledRecord(null);
      soundManager.playEdit();
      showToast('ပြင်ဆင်ပြီးပါပြီ');
    },
    [editingSettledRecord, showToast]
  );

  const clearSettledLedger = useCallback(() => {
    setSettled([]);
    storage.saveSettled([]);
    setDailySettlements([]);
    soundManager.playDelete();
    showToast('ရက်ချုပ်အားလုံး ရှင်းပြီးပါပြီ');
  }, [showToast]);

  // Session CRUD
  const addSession = useCallback(
    (name: string, rate: number, mult: number): boolean => {
      const cleanName = name.trim();
      if (!cleanName) return false;
      const newSess: Session = {
        id: Date.now(),
        name: cleanName,
        rate,
        mult,
        date: toAsciiDigits(selectedDate),
      };
      setSessions((prev) => {
        const updated = [...prev, newSess];
        storage.saveSessionsForDate(selectedDate, updated);
        return updated;
      });
      soundManager.playSave();
      showToast(`အကြိမ်ရေ (${cleanName}) အသစ်ထည့်ပြီးပါပြီ`);
      return true;
    },
    [selectedDate, showToast]
  );

  const updateSessionRecord = useCallback(
    (targetId: number | null, oldName: string | null, updated: Session): boolean => {
      setSessions((prev) => {
        const next = prev.map((s) => {
          if ((targetId && s.id === targetId) || (oldName && s.name === oldName)) {
            return { ...s, ...updated };
          }
          return s;
        });
        storage.saveSessionsForDate(selectedDate, next);
        return next;
      });
      soundManager.playEdit();
      showToast('ပြင်ဆင်ပြီးပါပြီ');
      return true;
    },
    [selectedDate, showToast]
  );

  const deleteSessionRecord = useCallback(
    (targetId: number, name: string) => {
      setSessions((prev) => {
        const next = prev.filter((s) => s.id !== targetId && s.name !== name);
        storage.saveSessionsForDate(selectedDate, next);
        if (selectedSession === name) {
          setSelectedSessionState(next[0]?.name || '');
        }
        return next;
      });
      soundManager.playDelete();
      showToast('ဖျက်ပြီးပါပြီ');
    },
    [selectedDate, selectedSession, showToast]
  );

  const open2DSummaryFromWinningNumber = useCallback(
    (winNum?: string, sess?: string) => {
      if (sess) setSession(sess);
      setActiveWinningNumberFor2DSummary(winNum || currentWinningNumber || null);
      navigateTo('AGENT_SETTLEMENT');
    },
    [currentWinningNumber, setSession, navigateTo]
  );

  const resetAllData = useCallback(() => {
    storage.resetAllData();
    setSessions([]);
    setAgents([]);
    setVouchers([]);
    setLayoffs([]);
    setBlocked(new Set());
    setWins([]);
    setSettled([]);
    setDailySettlements([]);
    setSelectedSessionState('');
    showToast('အချက်အလက်အားလုံး အသစ်ပြန်စပြီးပါပြီ');
  }, [showToast]);

  // Summaries helpers
  const getAll100Numbers = useCallback(() => {
    return getAll100NumbersHelper(vouchers, layoffs, toAsciiDigits(selectedDate), selectedSession);
  }, [vouchers, layoffs, selectedDate, selectedSession]);

  const sort2DNumbersWithWinning = useCallback(
    (all: NumberSummary[], winningNum: string | null): NumberSummary[] => {
      const cleanWin = winningNum?.trim();
      const hasWin = cleanWin && cleanWin.length === 2 && /^\d+$/.test(cleanWin);

      const winItem = hasWin ? all.find((x) => x.n === cleanWin) || { n: cleanWin, entryCount: 0, totalBet: 0, layoffAmount: 0, netAmount: 0 } : null;
      const pool = hasWin ? all.filter((x) => x.n !== cleanWin) : all;

      const active = pool.filter((x) => x.totalBet > 0).sort((a, b) => {
        if (b.totalBet !== a.totalBet) return b.totalBet - a.totalBet;
        return a.n.localeCompare(b.n);
      });

      const zero = pool.filter((x) => x.totalBet <= 0).sort((a, b) => a.n.localeCompare(b.n));

      return winItem ? [winItem, ...active, ...zero] : [...active, ...zero];
    },
    []
  );

  const getAgentSummaryData = useCallback(
    (agent: Agent, winNum = currentWinningNumber): AgentSummaryData => {
      const cleanDate = toAsciiDigits(selectedDate);
      const agVouchers = vouchers.filter(
        (v) =>
          toAsciiDigits(v.date) === cleanDate &&
          v.session === selectedSession &&
          (v.agentId === agent.id || v.agent === agent.name)
      );

      const totalBet = agVouchers.reduce((sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0), 0);
      const commission = Math.round(totalBet * (agent.rate / 100));
      const netAmount = totalBet - commission;

      let payout = 0;
      if (winNum && winNum.length === 2) {
        let winBet = 0;
        agVouchers.forEach((v) => {
          v.items.forEach((item) => {
            if (item.n === winNum) winBet += item.a;
          });
        });
        payout = winBet * agent.mult;
      }

      return {
        totalBet,
        commission,
        netAmount,
        voucherCount: agVouchers.length,
        payout,
      };
    },
    [selectedDate, selectedSession, vouchers, currentWinningNumber]
  );

  const getAgent2DSettlements = useCallback(
    (winNum: string): Agent2DSettlement[] => {
      const cleanDate = toAsciiDigits(selectedDate);
      const sessionAgents = agents.filter(
        (a) => toAsciiDigits(a.date) === cleanDate && a.roundNumber === selectedSession
      );
      const sessionVouchers = vouchers.filter(
        (v) => toAsciiDigits(v.date) === cleanDate && v.session === selectedSession
      );

      return sessionAgents.map((ag) => {
        const agVouchers = sessionVouchers.filter((v) => v.agentId === ag.id || v.agent === ag.name);
        const totalSales = agVouchers.reduce((sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0), 0);
        const commissionAmount = Math.round(totalSales * (ag.rate / 100));
        const netSales = totalSales - commissionAmount;

        let winningBetAmount = 0;
        if (winNum && winNum.length === 2) {
          agVouchers.forEach((v) => {
            v.items.forEach((item) => {
              if (item.n === winNum) winningBetAmount += item.a;
            });
          });
        }
        const payoutAmount = winningBetAmount * ag.mult;
        const netBalance = netSales - payoutAmount;

        return {
          agentId: ag.id,
          agentName: ag.name,
          commissionRate: ag.rate,
          multiplier: ag.mult,
          totalSales,
          commissionAmount,
          netSales,
          winningBetAmount,
          payoutAmount,
          netBalance,
        };
      });
    },
    [selectedDate, selectedSession, agents, vouchers]
  );

  const getNumberAgentBreakdown = useCallback(
    (targetNum: string): NumberAgentBreakdownItem[] => {
      const cleanDate = toAsciiDigits(selectedDate);
      const sessionAgents = agents.filter(
        (a) => toAsciiDigits(a.date) === cleanDate && a.roundNumber === selectedSession
      );
      const sessionVouchers = vouchers.filter(
        (v) => toAsciiDigits(v.date) === cleanDate && v.session === selectedSession
      );

      return sessionAgents.map((ag) => {
        const agVouchers = sessionVouchers.filter((v) => v.agentId === ag.id || v.agent === ag.name);
        const totalAgentSales = agVouchers.reduce((sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0), 0);
        let betOnNumber = 0;
        let entryCount = 0;
        agVouchers.forEach((v) => {
          v.items.forEach((item) => {
            if (item.n === targetNum) {
              betOnNumber += item.a;
              entryCount++;
            }
          });
        });

        const commissionOnNumber = Math.round(betOnNumber * (ag.rate / 100));
        const totalSalesCommission = Math.round(totalAgentSales * (ag.rate / 100));
        const winningPayout = betOnNumber * ag.mult;

        return {
          agentId: ag.id,
          agentName: ag.name,
          rate: ag.rate,
          mult: ag.mult,
          entryCount,
          betOnNumber,
          totalAgentSales,
          commissionOnNumber,
          totalSalesCommission,
          winningPayout,
        };
      }).filter((x) => x.betOnNumber > 0 || x.totalAgentSales > 0);
    },
    [selectedDate, selectedSession, agents, vouchers]
  );

  const buildDailySettlementsTextSummary = useCallback((): string => {
    let out = '----------------------------\n';
    out += '🍀 Lucky2D - ရက်ချုပ်အားလုံး မှတ်တမ်း\n';
    out += '----------------------------\n';
    settled.forEach((r, idx) => {
      const sign = r.profitLoss > 0 ? '+' : r.profitLoss < 0 ? '-' : '';
      out += `${idx + 1}. ${r.date} (${r.session}) | ပေါက်သီး: ${r.winningNumber} | အမြတ်ငွေ: ${sign}${fmt(Math.abs(r.profitLoss))}\n`;
    });
    out += '----------------------------\n';
    const totalPL = settled.reduce((sum, x) => sum + x.profitLoss, 0);
    const sign = totalPL > 0 ? '+' : totalPL < 0 ? '-' : '';
    out += `စုစုပေါင်း အမြတ်ငွေ : ${sign}${fmt(Math.abs(totalPL))} ကျပ်\n`;
    out += '----------------------------';
    return out;
  }, [settled]);

  const value: LotteryContextValue = {
    currentScreen,
    navigateTo,
    goBack,
    selectedDate,
    setDate,
    selectedSession,
    setSession,
    sessions,
    availableRounds,
    hasRound,
    agents,
    addAgent,
    updateAgent,
    deleteAgent,
    vouchers,
    activeVouchers,
    deleteVoucher,
    editNumber,
    deleteNumber,
    layoffs,
    deleteLayoffRecord,
    editLayoffItem,
    deleteLayoffItem,
    blocked,
    toggleBlockedNumber,
    wins,
    currentWinningNumber,
    setWinningNumber,
    updateWinRecord,
    deleteWinRecord,
    settled,
    dailySettlements,
    settings,
    updateSettings,
    updateBrakeLimit,
    saveBrakeLimit,
    currentBrakeInput,
    savedBrakeLimitText,
    entryAgent,
    setEntryAgent,
    entryItems,
    addEntryItem,
    addEntryItems,
    editEntryItem,
    deleteEntryItem,
    clearEntryItems,
    commitCurrentVoucher,
    startNumberEntry,
    voucherFilterAgent,
    setVoucherFilterAgent,
    voucherFilterAgentId,
    openAgentVouchers,
    clearVoucherFilter,
    executeLayoff,
    lockAndSaveBrakeSettlement,
    deleteSettledRecord,
    startEditingSettledRecord,
    cancelEditingSettledRecord,
    editingSettledRecord,
    saveEditedSettledRecord,
    clearSettledLedger,
    addSession,
    updateSessionRecord,
    deleteSessionRecord,
    open2DSummaryFromWinningNumber,
    activeWinningNumberFor2DSummary,
    resetAllData,
    toastMessage,
    showToast,
    getAll100Numbers,
    sort2DNumbersWithWinning,
    calculateSettlementForSession,
    getAgentSummaryData,
    getAgent2DSettlements,
    getNumberAgentBreakdown,
    buildDailySettlementsTextSummary,
  };

  return <LotteryContext.Provider value={value}>{children}</LotteryContext.Provider>;
};

export function useLottery(): LotteryContextValue {
  const context = useContext(LotteryContext);
  if (!context) {
    throw new Error('useLottery must be used within a LotteryProvider');
  }
  return context;
}

function getAll100NumbersHelper(
  vouchers: Voucher[],
  layoffs: LayoffRecord[],
  cleanDate: string,
  sessionName: string
): NumberSummary[] {
  const sessionVouchers = vouchers.filter(
    (v) => toAsciiDigits(v.date) === cleanDate && v.session === sessionName
  );
  const sessionLayoffs = layoffs.filter(
    (l) => toAsciiDigits(l.date) === cleanDate && l.session === sessionName
  );

  const salesMap: Record<string, number> = {};
  const countMap: Record<string, number> = {};
  const layoffMap: Record<string, number> = {};

  for (let i = 0; i <= 99; i++) {
    const k = i.toString().padStart(2, '0');
    salesMap[k] = 0;
    countMap[k] = 0;
    layoffMap[k] = 0;
  }

  sessionVouchers.forEach((v) => {
    v.items.forEach((item) => {
      const num = toAsciiDigits(item.n).padStart(2, '0').slice(-2);
      salesMap[num] = (salesMap[num] || 0) + item.a;
      countMap[num] = (countMap[num] || 0) + 1;
    });
  });

  sessionLayoffs.forEach((l) => {
    l.items.forEach((item) => {
      const num = toAsciiDigits(item.n).padStart(2, '0').slice(-2);
      layoffMap[num] = (layoffMap[num] || 0) + item.a;
    });
  });

  const res: NumberSummary[] = [];
  for (let i = 0; i <= 99; i++) {
    const num = i.toString().padStart(2, '0');
    const totalBet = salesMap[num] || 0;
    const layoffAmount = layoffMap[num] || 0;
    res.push({
      n: num,
      entryCount: countMap[num] || 0,
      totalBet,
      layoffAmount,
      netAmount: totalBet - layoffAmount,
    });
  }
  return res;
}
