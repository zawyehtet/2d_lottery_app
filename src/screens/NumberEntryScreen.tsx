import React, { useState, useEffect, useRef } from 'react';
import {
  X,
  Zap,
  Trash2,
  Edit2,
  Check,
  AlertCircle,
  HelpCircle,
  Users,
} from 'lucide-react';
import { useLottery, fmt } from '../context/LotteryContext';
import {
  generateNumbersForPattern,
  parseBetInput,
  parseQuickEntryMode1,
  parseQuickEntryMode2,
  reverse2D,
  validateBetAmount,
} from '../logic/patternAlgorithms';
import { BetItem } from '../types';
import { soundManager } from '../util/soundManager';

export const NumberEntryScreen: React.FC = () => {
  const {
    selectedDate,
    selectedSession,
    hasRound,
    agents,
    entryAgent,
    setEntryAgent,
    entryItems,
    addEntryItem,
    addEntryItems,
    editEntryItem,
    deleteEntryItem,
    clearEntryItems,
    commitCurrentVoucher,
    blocked,
    goBack,
    openAgentVouchers,
    showToast,
  } = useLottery();

  // Three-stage sequential focus flow:
  // "NUMBER" -> "PATTERN" -> "AMOUNT"
  const [activeField, setActiveField] = useState<'NUMBER' | 'PATTERN' | 'AMOUNT'>('NUMBER');
  const [inputNumber, setInputNumber] = useState('');
  const [inputPattern, setInputPattern] = useState('ဒဲ့');
  const [inputAmount, setInputAmount] = useState('');
  const [isReverseOn, setIsReverseOn] = useState(false);
  const [shouldOverwriteAmount, setShouldOverwriteAmount] = useState(false);
  const [amountValidationError, setAmountValidationError] = useState(false);

  // Modals
  const [quickEntryMode, setQuickEntryMode] = useState<1 | 2 | null>(null);
  const [quickEntryText, setQuickEntryText] = useState('');
  const [showAgentPicker, setShowAgentPicker] = useState(!entryAgent);
  const [showConfirmBetDialog, setShowConfirmBetDialog] = useState(false);
  const [showKwayDialog, setShowKwayDialog] = useState(false);
  const [editingItemIndex, setEditingItemIndex] = useState<number | null>(null);
  const [editNumInput, setEditNumInput] = useState('');
  const [editAmtInput, setEditAmtInput] = useState('');
  const [deletingItemIndex, setDeletingItemIndex] = useState<number | null>(null);

  const tableEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll on new items
  useEffect(() => {
    tableEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [entryItems.length]);

  // Validation debounce on inputAmount
  useEffect(() => {
    if (!inputAmount) {
      setAmountValidationError(false);
      return;
    }
    const timer = setTimeout(() => {
      setAmountValidationError(!validateBetAmount(inputAmount).isValid);
    }, 180);
    return () => clearTimeout(timer);
  }, [inputAmount]);

  const advanceToAmount = () => {
    setActiveField('AMOUNT');
    setShouldOverwriteAmount(inputAmount.length > 0);
  };

  const onKeypadNumPress = (num: string) => {
    if (!hasRound) {
      showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
      return;
    }
    soundManager.playTap();

    if (activeField === 'NUMBER') {
      if (inputPattern === 'ခွေ') {
        if (inputNumber.length < 9) {
          setInputNumber((prev) => (prev + num).slice(0, 9));
        }
      } else if (['ထိပ်', 'နောက်', 'အပါ', 'ဘရိတ်'].includes(inputPattern)) {
        setInputNumber(num.slice(0, 1));
        setActiveField('PATTERN');
        setShouldOverwriteAmount(false);
      } else {
        if (inputNumber.length < 2) {
          const next = (inputNumber + num).slice(0, 2);
          setInputNumber(next);
          if (next.length === 2) {
            setActiveField('PATTERN');
            setShouldOverwriteAmount(false);
          }
        }
      }
    } else if (activeField === 'PATTERN') {
      if (['ထိပ်', 'နောက်', 'အပါ', 'ဘရိတ်'].includes(inputPattern) && inputNumber.length === 0) {
        setInputNumber(num.slice(0, 1));
        advanceToAmount();
      } else if (inputPattern === 'ခွေ' && inputNumber.length < 2) {
        setInputNumber((prev) => (prev + num).slice(0, 9));
        setActiveField('NUMBER');
      } else {
        if (!inputPattern) setInputPattern('ဒဲ့');
        setInputAmount(num.replace(/\D/g, ''));
        setActiveField('AMOUNT');
        setShouldOverwriteAmount(false);
      }
    } else {
      // AMOUNT
      const allowed = num.replace(/[^\d/]/g, '');
      if (shouldOverwriteAmount) {
        setInputAmount(allowed);
        setShouldOverwriteAmount(false);
      } else {
        if (allowed.includes('/') && inputAmount.includes('/')) {
          setInputAmount((prev) => prev + allowed.replace(/\//g, ''));
        } else {
          setInputAmount((prev) => prev + allowed);
        }
      }
    }
  };

  const onKeypadSlashPress = () => {
    if (!hasRound) {
      showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
      return;
    }
    soundManager.playClick();
    if (activeField === 'NUMBER' || activeField === 'PATTERN') {
      if (!inputPattern) setInputPattern('ဒဲ့');
      advanceToAmount();
      if (inputAmount && !inputAmount.includes('/')) {
        setInputAmount((prev) => prev + '/');
        setShouldOverwriteAmount(false);
      }
    } else {
      setShouldOverwriteAmount(false);
      if (!inputAmount.includes('/')) {
        setInputAmount((prev) => (prev ? `${prev}/` : '/'));
      }
    }
  };

  const onPatternSelected = (pattern: string) => {
    if (!hasRound) {
      showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
      return;
    }
    soundManager.playClick();
    setInputPattern(pattern);

    if (['ထိပ်', 'နောက်', 'အပါ', 'ဘရိတ်'].includes(pattern) && inputNumber.length > 1) {
      setInputNumber(inputNumber.slice(0, 1));
    }

    if (activeField === 'PATTERN') {
      advanceToAmount();
    } else if (['ပါဝါ', 'ညီအစ်ကို', 'အပူး', 'စုံစုံ', 'မမ', 'စုံမ', 'မစုံ', 'နက္ခတ်'].includes(pattern)) {
      advanceToAmount();
    } else if (['ထိပ်', 'နောက်', 'အပါ', 'ဘရိတ်'].includes(pattern) && inputNumber.length === 1) {
      advanceToAmount();
    } else if (pattern === 'ဒဲ့' && inputNumber.length === 2) {
      advanceToAmount();
    } else {
      setActiveField('NUMBER');
      setShouldOverwriteAmount(false);
    }
  };

  const onAdvanceFocusPress = () => {
    soundManager.playClick();
    if (activeField === 'NUMBER') {
      setActiveField('PATTERN');
      setShouldOverwriteAmount(false);
    } else if (activeField === 'PATTERN') {
      if (!inputPattern) setInputPattern('ဒဲ့');
      advanceToAmount();
    } else {
      setActiveField('NUMBER');
      setShouldOverwriteAmount(false);
    }
  };

  const handleAddBet = (withDoublesForKway = false) => {
    if (!hasRound) {
      showToast('အကြိမ်ရေ အရင်ထည့်ပါ');
      return;
    }
    if (!entryAgent) {
      showToast('ထိုးသူ အရင်ရွေးပါ');
      setShowAgentPicker(true);
      return;
    }

    // Check full expression e.g. 58=3000/2000
    const fullInput = inputNumber.includes('=') ? inputNumber : inputAmount.includes('=') ? inputAmount : null;
    if (fullInput) {
      const parsed = parseBetInput(fullInput);
      if (parsed.length > 0) {
        const betList: BetItem[] = [];
        const blockedFound: string[] = [];
        for (const item of parsed) {
          if (blocked.has(item.n)) {
            blockedFound.push(item.n);
          } else {
            betList.push({ id: Date.now() + Math.random(), n: item.n, a: item.a });
          }
        }
        if (blockedFound.length > 0) {
          showToast(`မရဂဏန်း (${blockedFound.join(', ')}) ပါဝင်နေပါသည်`);
        }
        if (betList.length > 0) {
          addEntryItems(betList);
          setInputNumber('');
          setInputPattern('ဒဲ့');
          setInputAmount('');
          setIsReverseOn(false);
          setActiveField('NUMBER');
          setShouldOverwriteAmount(false);
        }
        return;
      }
    }

    const amtValidation = validateBetAmount(inputAmount);
    if (!amtValidation.isValid) {
      setAmountValidationError(true);
      showToast('ထိုးကြေး ပမာဏ လက်မခံပါ');
      setActiveField('AMOUNT');
      return;
    }

    const isSplit = inputAmount.includes('/');
    let directAmount = 0;
    let reverseAmount = 0;
    if (isSplit) {
      const parts = inputAmount.split('/');
      directAmount = parseInt(parts[0]?.trim() || '0', 10) || 0;
      reverseAmount = parseInt(parts[1]?.trim() || '0', 10) || 0;
    } else {
      directAmount = parseInt(inputAmount.trim(), 10) || 0;
    }

    const effectivePattern = inputPattern || 'ဒဲ့';

    if (effectivePattern === 'ခွေ') {
      const distinct = new Set(inputNumber.split('').filter((c) => /\d/.test(c)));
      if (distinct.size < 2) {
        showToast('ခွေရန် အနည်းဆုံး ဂဏန်း ၂ လုံး ရိုက်ပါ');
        setActiveField('NUMBER');
        return;
      }
    } else if (['ထိပ်', 'နောက်', 'အပါ', 'ဘရိတ်'].includes(effectivePattern)) {
      if (!inputNumber) {
        showToast('ဂဏန်း ၁ လုံး ရိုက်ထည့်ပါ');
        setActiveField('NUMBER');
        return;
      }
    } else if (effectivePattern === 'ဒဲ့') {
      if (inputNumber.length !== 2) {
        showToast('၂ လုံးဂဏန်း အပြည့်ရိုက်ပါ');
        setActiveField('NUMBER');
        return;
      }
    }

    const generated = generateNumbersForPattern(
      effectivePattern,
      inputNumber,
      isSplit ? false : isReverseOn,
      withDoublesForKway
    );

    if (generated.length === 0) {
      showToast('ဂဏန်း မထွက်ရှိပါ');
      return;
    }

    const betList: BetItem[] = [];
    const blockedFound: string[] = [];

    if (isSplit) {
      for (const num of generated) {
        const rev = reverse2D(num);
        if (rev === num) {
          // Twin number
          const comb = directAmount + reverseAmount;
          if (blocked.has(num)) blockedFound.push(num);
          else betList.push({ id: Date.now() + Math.random(), n: num, a: comb });
        } else {
          if (blocked.has(num)) blockedFound.push(num);
          else betList.push({ id: Date.now() + Math.random(), n: num, a: directAmount });

          if (reverseAmount > 0) {
            if (blocked.has(rev)) blockedFound.push(rev);
            else betList.push({ id: Date.now() + Math.random() + 0.1, n: rev, a: reverseAmount });
          }
        }
      }
    } else {
      for (const num of generated) {
        if (blocked.has(num)) blockedFound.push(num);
        else betList.push({ id: Date.now() + Math.random(), n: num, a: directAmount });
      }
    }

    if (blockedFound.length > 0) {
      showToast(`မရဂဏန်း (${Array.from(new Set(blockedFound)).join(', ')}) ပါဝင်နေပါသည်`);
    }

    if (betList.length > 0) {
      addEntryItems(betList);
      setInputNumber('');
      setInputPattern('ဒဲ့');
      setIsReverseOn(false);
      setActiveField('NUMBER');
      setShouldOverwriteAmount(false);
    }
  };

  const handleQuickEntrySubmit = () => {
    if (!quickEntryText.trim()) return;
    const res =
      quickEntryMode === 1
        ? parseQuickEntryMode1(quickEntryText, blocked)
        : parseQuickEntryMode2(quickEntryText, blocked);

    if (res.items.length === 0) {
      showToast('ထိုးမည့် ဂဏန်း မတွေ့ပါ');
      return;
    }

    const newItems: BetItem[] = res.items.map((x) => ({
      id: Date.now() + Math.random(),
      n: x.n,
      a: x.a,
    }));

    addEntryItems(newItems);
    setQuickEntryMode(null);
    setQuickEntryText('');
    showToast(`${newItems.length} ကွက် ထည့်သွင်းပြီးပါပြီ`);
  };

  const totalAmount = entryItems.reduce((sum, item) => sum + item.a, 0);

  return (
    <div className="flex flex-col h-screen bg-[#F4F7F6] select-none">
      {/* Top Header */}
      <header className="bg-[#235347] text-white px-3 py-2.5 shadow-md flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2 flex-1 truncate">
          <div className="w-7 h-7 rounded-full bg-[#0C4E3E] border border-[#FEC72E] flex items-center justify-center shrink-0 overflow-hidden">
            <img src="/ic_lucky2d_logo.png" alt="Lucky2D" className="w-full h-full object-cover" />
          </div>
          <span className="font-bold text-sm truncate">
            နေ့စွဲ : {selectedDate} , အကြိမ် : {hasRound ? selectedSession : 'အကြိမ်မရှိ'}
          </span>
        </div>
        <button
          type="button"
          onClick={goBack}
          className="p-1.5 rounded-full hover:bg-white/10 active:bg-white/20"
          title="ပိတ်မည်"
        >
          <X className="w-5 h-5 text-white" />
        </button>
      </header>

      {/* Agent Info Strip */}
      <div className="bg-white border-b border-slate-200 px-3 py-2 flex items-center justify-between shrink-0 text-xs">
        <div
          onClick={() => setShowAgentPicker(true)}
          className="flex items-center gap-1.5 cursor-pointer hover:opacity-80 font-bold"
        >
          <Users className="w-4 h-4 text-[#235347]" />
          <span className="text-slate-600">ထိုးသူ :</span>
          <span className="text-[#235347] text-sm">
            {entryAgent ? entryAgent.name : 'ရွေးချယ်ပါ'}
          </span>
        </div>

        <button
          type="button"
          onClick={() => openAgentVouchers(entryAgent ? entryAgent.id : null, entryAgent?.name)}
          className="text-[#235347] font-semibold hover:underline"
        >
          ဘောင်ချာများ ကြည့်ရန် နှိပ်ပါ
        </button>
      </div>

      {/* Main Entered Bets Table */}
      <div className="flex-1 flex flex-col min-h-0 bg-white">
        {/* Table Header */}
        <div className="bg-[#EAF0EE] border-b border-slate-200 px-4 py-2 flex items-center justify-between text-xs font-bold text-slate-800 shrink-0">
          <span className="w-12">စဉ်</span>
          <span className="flex-1 text-center">ဂဏန်း</span>
          <span className="w-24 text-right">ပမာဏ</span>
        </div>

        {/* Bets List */}
        <div className="flex-1 overflow-y-auto">
          {entryItems.length === 0 ? (
            <div className="h-full flex items-center justify-center p-6 text-sm text-slate-400 font-medium text-center">
              ဂဏန်းနှင့် ပမာဏ ရိုက်ထည့်ပါ (သို့မဟုတ် အမြန်ထိုး အသုံးပြုပါ)
            </div>
          ) : (
            <div className="divide-y divide-slate-100">
              {entryItems.map((item, index) => (
                <div
                  key={item.id}
                  onClick={() => {
                    setEditingItemIndex(index);
                    setEditNumInput(item.n);
                    setEditAmtInput(String(item.a));
                  }}
                  className={`px-4 py-2.5 flex items-center justify-between text-sm cursor-pointer hover:bg-slate-50 ${
                    index % 2 === 0 ? 'bg-white' : 'bg-[#F9FBFA]'
                  }`}
                >
                  <span className="w-12 text-xs font-semibold text-slate-500">
                    {index + 1}.
                  </span>
                  <span className="flex-1 text-center font-bold text-base text-[#235347] font-mono">
                    {item.n}
                  </span>
                  <div className="w-24 text-right font-bold text-slate-900 font-mono">
                    {fmt(item.a)}
                  </div>
                </div>
              ))}
              <div ref={tableEndRef} />
            </div>
          )}
        </div>

        {/* Action Summary Bar under the table */}
        <div className="bg-[#EAF0EE] border-t border-slate-300 px-3 py-2 flex items-center justify-between gap-2 shrink-0">
          <div className="relative">
            <button
              type="button"
              onClick={() => setQuickEntryMode(1)}
              className="h-9 px-3 bg-[#235347] hover:bg-[#183B32] text-white rounded-lg font-bold text-xs flex items-center gap-1 shadow-sm"
            >
              <Zap className="w-3.5 h-3.5" />
              <span>အမြန်ထိုး</span>
            </button>
          </div>

          <div className="flex-1 text-center text-xs font-bold text-slate-800 truncate px-1">
            စုစုပေါင်း = {fmt(totalAmount)} ({entryItems.length} ကွက်)
          </div>

          <button
            type="button"
            disabled={entryItems.length === 0}
            onClick={() => setShowConfirmBetDialog(true)}
            className="h-9 px-4 bg-[#2E7D32] hover:bg-emerald-800 disabled:bg-slate-300 text-white rounded-lg font-bold text-xs shadow-sm flex items-center gap-1 cursor-pointer disabled:cursor-not-allowed"
          >
            <span>ထိုးရန်</span>
          </button>
        </div>
      </div>

      {/* Pinned Bottom Keypad */}
      <div className="bg-white border-t border-slate-300 p-2 space-y-1.5 shrink-0">
        {/* 3 Preview Cells (ဂဏန်း | အမျိုးအစား | ပမာဏ) */}
        <div className="grid grid-cols-3 gap-1.5">
          {/* Cell 1: Number */}
          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              setActiveField('NUMBER');
              setShouldOverwriteAmount(false);
            }}
            className={`p-2 rounded-xl text-center border transition-all ${
              activeField === 'NUMBER'
                ? 'bg-emerald-50 border-[#235347] shadow-sm'
                : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="text-[10px] font-bold text-slate-500">ဂဏန်း</div>
            <div className="text-base font-extrabold text-[#235347] font-mono h-6 flex items-center justify-center">
              {inputNumber || '-'}
            </div>
          </button>

          {/* Cell 2: Pattern */}
          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              setActiveField('PATTERN');
              setShouldOverwriteAmount(false);
            }}
            className={`p-2 rounded-xl text-center border transition-all ${
              activeField === 'PATTERN'
                ? 'bg-emerald-50 border-[#235347] shadow-sm'
                : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="text-[10px] font-bold text-slate-500">အမျိုးအစား</div>
            <div className="text-xs font-bold text-slate-800 h-6 flex items-center justify-center truncate">
              {inputPattern || 'ဒဲ့'}
              {isReverseOn ? ' (အာ)' : ''}
            </div>
          </button>

          {/* Cell 3: Amount */}
          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              setActiveField('AMOUNT');
              setShouldOverwriteAmount(inputAmount.length > 0);
            }}
            className={`p-2 rounded-xl text-center border transition-all ${
              amountValidationError
                ? 'bg-red-50 border-red-500'
                : activeField === 'AMOUNT'
                ? 'bg-emerald-50 border-[#235347] shadow-sm'
                : 'bg-slate-50 border-slate-200'
            }`}
          >
            <div className="text-[10px] font-bold text-slate-500">ပမာဏ</div>
            <div className="text-base font-extrabold text-slate-900 font-mono h-6 flex items-center justify-center">
              {inputAmount || '-'}
            </div>
          </button>
        </div>

        {/* Scrollable Pattern Chips Row */}
        <div className="flex gap-1 overflow-x-auto pb-0.5 no-scrollbar text-xs">
          {['ဒဲ့', 'ပါဝါ', 'ညီအစ်ကို', 'ထိပ်', 'နောက်', 'အပူး', 'စုံစုံ', 'မမ', 'စုံမ', 'မစုံ', 'ဘရိတ်'].map(
            (p) => (
              <button
                key={p}
                type="button"
                onClick={() => onPatternSelected(p)}
                className={`px-3 py-1.5 rounded-lg shrink-0 font-bold transition-all ${
                  inputPattern === p
                    ? 'bg-[#2E7D32] text-white shadow-sm'
                    : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                }`}
              >
                {p}
              </button>
            )
          )}
        </div>

        {/* 5-Column Keypad */}
        <div className="space-y-1">
          {/* Row 1: နက္ခတ် | 1 | 2 | 3 | R */}
          <div className="grid grid-cols-5 gap-1 text-center font-bold">
            <button
              type="button"
              onClick={() => onPatternSelected('နက္ခတ်')}
              className={`h-11 rounded-xl text-xs flex items-center justify-center text-white ${
                inputPattern === 'နက္ခတ်' ? 'bg-[#2E7D32]' : 'bg-[#235347]'
              }`}
            >
              နက္ခတ်
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('1')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              1
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('2')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              2
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('3')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              3
            </button>
            <button
              type="button"
              onClick={() => {
                soundManager.playClick();
                setIsReverseOn(!isReverseOn);
              }}
              className={`h-11 rounded-xl text-sm flex items-center justify-center font-black ${
                isReverseOn ? 'bg-[#2E7D32] text-white' : 'bg-[#235347] text-white'
              }`}
            >
              R
            </button>
          </div>

          {/* Row 2: ခွေ | 4 | 5 | 6 | / */}
          <div className="grid grid-cols-5 gap-1 text-center font-bold">
            <button
              type="button"
              onClick={() => {
                onPatternSelected('ခွေ');
                setShowKwayDialog(true);
              }}
              className={`h-11 rounded-xl text-xs flex items-center justify-center text-white ${
                inputPattern === 'ခွေ' ? 'bg-[#2E7D32]' : 'bg-[#235347]'
              }`}
            >
              ခွေ
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('4')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              4
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('5')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              5
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('6')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              6
            </button>
            <button
              type="button"
              onClick={onKeypadSlashPress}
              className="h-11 rounded-xl bg-[#235347] text-white text-xl flex items-center justify-center font-black"
            >
              /
            </button>
          </div>

          {/* Row 3: အပါ | 7 | 8 | 9 | ENTER */}
          <div className="grid grid-cols-5 gap-1 text-center font-bold">
            <button
              type="button"
              onClick={() => onPatternSelected('အပါ')}
              className={`h-11 rounded-xl text-xs flex items-center justify-center text-white ${
                inputPattern === 'အပါ' ? 'bg-[#2E7D32]' : 'bg-[#235347]'
              }`}
            >
              အပါ
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('7')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              7
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('8')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              8
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('9')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              9
            </button>
            <button
              type="button"
              onClick={onAdvanceFocusPress}
              className="h-11 rounded-xl bg-[#235347] text-white text-xs flex items-center justify-center font-black"
            >
              ENTER
            </button>
          </div>

          {/* Row 4: ဖျက် | 0 | 00 | 000 | OK */}
          <div className="grid grid-cols-5 gap-1 text-center font-bold">
            <button
              type="button"
              onClick={() => {
                soundManager.playDelete();
                if (activeField === 'NUMBER') {
                  setInputNumber((prev) => prev.slice(0, -1));
                } else if (activeField === 'PATTERN') {
                  setInputPattern('ဒဲ့');
                  setIsReverseOn(false);
                } else {
                  setInputAmount((prev) => prev.slice(0, -1));
                }
              }}
              className="h-11 rounded-xl bg-[#D32F2F] hover:bg-red-700 text-white text-xs flex items-center justify-center font-bold"
            >
              ဖျက်
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('0')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-lg flex items-center justify-center"
            >
              0
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('00')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-sm flex items-center justify-center"
            >
              00
            </button>
            <button
              type="button"
              onClick={() => onKeypadNumPress('000')}
              className="h-11 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-900 text-xs flex items-center justify-center"
            >
              000
            </button>
            <button
              type="button"
              onClick={() => handleAddBet(false)}
              className="h-11 rounded-xl bg-[#2E7D32] hover:bg-emerald-800 text-white text-sm flex items-center justify-center font-black shadow-md cursor-pointer"
            >
              OK
            </button>
          </div>
        </div>
      </div>

      {/* MODAL 1: Quick Entry Modal (အမြန်ထိုး 1 & 2) */}
      {quickEntryMode && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-5 space-y-4 shadow-2xl border border-slate-200">
            <div className="flex items-center justify-between border-b pb-3">
              <div className="flex items-center gap-2">
                <Zap className="w-5 h-5 text-[#235347]" />
                <h3 className="font-bold text-base text-slate-900">
                  2D ဂဏန်းများ ( အမြန်ထိုး {quickEntryMode} )
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setQuickEntryMode(null)}
                className="p-1 rounded-full hover:bg-slate-100"
              >
                <X className="w-5 h-5 text-slate-500" />
              </button>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setQuickEntryMode(1)}
                className={`flex-1 py-1.5 text-xs font-bold rounded-lg border ${
                  quickEntryMode === 1
                    ? 'bg-[#235347] text-white border-[#235347]'
                    : 'bg-slate-50 text-slate-700 border-slate-200'
                }`}
              >
                အမြန်ထိုး 1
              </button>
              <button
                type="button"
                onClick={() => setQuickEntryMode(2)}
                className={`flex-1 py-1.5 text-xs font-bold rounded-lg border ${
                  quickEntryMode === 2
                    ? 'bg-[#235347] text-white border-[#235347]'
                    : 'bg-slate-50 text-slate-700 border-slate-200'
                }`}
              >
                အမြန်ထိုး 2
              </button>
            </div>

            <textarea
              rows={6}
              value={quickEntryText}
              onChange={(e) => setQuickEntryText(e.target.value)}
              placeholder={
                quickEntryMode === 1
                  ? 'ကြေးတူရင်\n01.78=500\n\nကြေးတူ R ရင်\n01.78R500\n\nကြေးမတူရင်\n01.78=500/100'
                  : '5a500 (5 အပါ)\n6b600 (6 ဘရိတ်)\n1t500 (1 ထိပ်)\n1w500 (1 နောက်)\np3500 (ပါဝါ)\nn7500 (နက္ခတ်)\ns500 (အပူး)\nx200 (ညီကို)\n123k500 (ခွေ)'
              }
              className="w-full p-3 font-mono text-sm border border-slate-300 rounded-xl focus:outline-none focus:border-[#235347]"
            />

            <div className="flex justify-between items-center pt-2">
              <button
                type="button"
                onClick={async () => {
                  try {
                    const clip = await navigator.clipboard.readText();
                    if (clip) setQuickEntryText((prev) => (prev ? `${prev}\n${clip}` : clip));
                  } catch {
                    // Ignore
                  }
                }}
                className="px-3 py-1.5 text-xs font-bold text-[#235347] bg-emerald-50 hover:bg-emerald-100 rounded-lg"
              >
                Clipboard မှ ကူးထည့်မည်
              </button>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setQuickEntryMode(null)}
                  className="px-3 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-lg"
                >
                  မလုပ်တော့ပါ
                </button>
                <button
                  type="button"
                  onClick={handleQuickEntrySubmit}
                  className="px-4 py-1.5 text-xs font-bold text-white bg-[#235347] hover:bg-[#183B32] rounded-lg shadow-sm"
                >
                  ထည့်သွင်းမည်
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: Agent Picker Modal */}
      {showAgentPicker && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b pb-3">
              <h3 className="font-bold text-base text-slate-900">ထိုးသူ (ကော်မရှင်) ရွေးချယ်ပါ</h3>
              <button
                type="button"
                onClick={() => setShowAgentPicker(false)}
                className="p-1 rounded-full hover:bg-slate-100"
              >
                <X className="w-5 h-5 text-slate-500" />
              </button>
            </div>

            <div className="max-h-60 overflow-y-auto divide-y divide-slate-100">
              {agents.length === 0 ? (
                <div className="py-6 text-center text-xs text-slate-400 font-medium">
                  ကော်မရှင် မရှိသေးပါ။ ကော်မရှင် အရင်ထည့်ပါ။
                </div>
              ) : (
                agents.map((ag) => (
                  <button
                    key={ag.id}
                    type="button"
                    onClick={() => {
                      setEntryAgent(ag);
                      setShowAgentPicker(false);
                      soundManager.playClick();
                    }}
                    className={`w-full text-left px-3 py-2.5 flex items-center justify-between hover:bg-slate-50 text-sm ${
                      entryAgent?.id === ag.id ? 'bg-emerald-50 text-[#235347] font-bold' : 'text-slate-800'
                    }`}
                  >
                    <span>
                      {ag.id}. {ag.name}
                    </span>
                    <span className="text-xs text-slate-500 font-medium">
                      ကော် {ag.rate}% | အဆ {ag.mult}
                    </span>
                  </button>
                ))
              )}
            </div>

            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setShowAgentPicker(false)}
                className="px-4 py-2 bg-slate-100 text-slate-700 text-xs font-bold rounded-lg hover:bg-slate-200"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 3: Kway Doubles Dialog */}
      {showKwayDialog && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 space-y-4 shadow-2xl">
            <h3 className="font-bold text-base text-slate-900 text-center">
              ခွေဂဏန်းများ အပူး ပါမည်လား?
            </h3>
            <p className="text-xs text-slate-500 text-center">
              ( ဥပမာ: 123 ခွေရာတွင် 11, 22, 33 အပူးများ ထည့်သွင်းမည်လား )
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => {
                  setShowKwayDialog(false);
                  handleAddBet(true);
                }}
                className="flex-1 py-2.5 bg-[#235347] text-white rounded-xl font-bold text-xs shadow-sm hover:bg-[#183B32]"
              >
                အပူးပါမည်
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowKwayDialog(false);
                  handleAddBet(false);
                }}
                className="flex-1 py-2.5 bg-slate-100 text-slate-700 rounded-xl font-bold text-xs hover:bg-slate-200"
              >
                အပူးမပါပါ
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 4: Confirm Bet Dialog (ထိုးမည်လား?) */}
      {showConfirmBetDialog && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 space-y-4 shadow-2xl">
            <h3 className="font-bold text-lg text-slate-900 text-center">
              ဘောင်ချာ အတည်ပြုပါ
            </h3>
            <div className="bg-slate-50 p-4 rounded-xl border border-slate-200 space-y-1.5 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-500">ထိုးသူ :</span>
                <span className="font-bold text-slate-800">{entryAgent?.name}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">ရက်စွဲ/အကြိမ် :</span>
                <span className="font-semibold text-slate-800">
                  {selectedDate} ({selectedSession})
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">အရေအတွက် :</span>
                <span className="font-bold text-slate-800">{entryItems.length} ကွက်</span>
              </div>
              <hr className="border-slate-200 my-1" />
              <div className="flex justify-between text-base">
                <span className="font-bold text-slate-900">စုစုပေါင်းငွေ :</span>
                <span className="font-black text-[#235347] font-mono">{fmt(totalAmount)} ကျပ်</span>
              </div>
            </div>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setShowConfirmBetDialog(false)}
                className="flex-1 py-2.5 bg-slate-100 text-slate-700 rounded-xl font-bold text-xs hover:bg-slate-200"
              >
                မလုပ်တော့ပါ
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowConfirmBetDialog(false);
                  commitCurrentVoucher();
                }}
                className="flex-1 py-2.5 bg-[#2E7D32] hover:bg-emerald-800 text-white rounded-xl font-bold text-xs shadow-md"
              >
                အတည်ပြုသိမ်းမည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 5: Edit Single Bet Item in Draft */}
      {editingItemIndex !== null && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 space-y-4 shadow-2xl">
            <h3 className="font-bold text-base text-slate-900">ဂဏန်း ပြင်ဆင်ရန် / ဖျက်ရန်</h3>
            <div className="space-y-3">
              <div>
                <label className="text-xs font-bold text-slate-600 block mb-1">ဂဏန်း</label>
                <input
                  type="text"
                  maxLength={2}
                  value={editNumInput}
                  onChange={(e) => setEditNumInput(e.target.value.replace(/\D/g, '').slice(0, 2))}
                  className="w-full px-3 py-2 border rounded-lg text-center font-mono font-bold text-lg"
                />
              </div>
              <div>
                <label className="text-xs font-bold text-slate-600 block mb-1">ပမာဏ</label>
                <input
                  type="text"
                  value={editAmtInput}
                  onChange={(e) => setEditAmtInput(e.target.value.replace(/\D/g, ''))}
                  className="w-full px-3 py-2 border rounded-lg text-center font-mono font-bold text-lg"
                />
              </div>
            </div>

            <div className="flex justify-between items-center gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  deleteEntryItem(editingItemIndex);
                  setEditingItemIndex(null);
                }}
                className="px-3 py-2 bg-red-100 hover:bg-red-200 text-red-700 font-bold text-xs rounded-lg flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>ဖျက်မည်</span>
              </button>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setEditingItemIndex(null)}
                  className="px-3 py-2 text-slate-600 text-xs font-bold hover:bg-slate-100 rounded-lg"
                >
                  မလုပ်တော့ပါ
                </button>
                <button
                  type="button"
                  onClick={() => {
                    const amt = parseInt(editAmtInput, 10);
                    if (editNumInput.length === 2 && amt > 0) {
                      editEntryItem(editingItemIndex, editNumInput, amt);
                      setEditingItemIndex(null);
                    }
                  }}
                  className="px-4 py-2 bg-[#235347] text-white text-xs font-bold rounded-lg shadow-sm"
                >
                  သိမ်းမည်
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
