import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import { AlertBoxModal, AlertBoxState } from '../components/AlertBoxModal';
import {
  Receipt,
  Copy,
  Trash2,
  Edit,
  Check,
  Search,
  Filter,
  X,
  AlertTriangle,
} from 'lucide-react';
import { Voucher, BetItem } from '../types';
import { soundManager } from '../util/soundManager';
import { useLongPress } from '../hooks/useLongPress';

interface VoucherBetRowProps {
  index: number;
  item: BetItem;
  onSelect: () => void;
}

const VoucherBetRow: React.FC<VoucherBetRowProps> = ({ index, item, onSelect }) => {
  const longPressHandlers = useLongPress(onSelect, onSelect);

  return (
    <div
      {...longPressHandlers}
      className={`grid grid-cols-12 items-center py-2 px-3 text-xs font-mono transition cursor-pointer ${
        index % 2 === 0 ? 'bg-white hover:bg-emerald-50/50' : 'bg-[#F8FAF9] hover:bg-emerald-50/50'
      }`}
      title="ပြင်ဆင်ရန် / ဖျက်ရန် နှိပ်ပါ (သို့) ဖိထားပါ"
    >
      <div className="col-span-2 text-center text-slate-400 font-sans tabular-nums">{index + 1}</div>
      <div className="col-span-4 text-center font-bold text-slate-900 text-sm tabular-nums">
        {item.n}
      </div>
      <div className="col-span-5 text-right font-bold text-[#235347] tabular-nums pr-1">
        {fmt(item.a)}
      </div>
      <div className="col-span-1 text-center text-slate-400 hover:text-[#235347]">
        <Edit className="w-3.5 h-3.5 mx-auto" />
      </div>
    </div>
  );
};

interface VoucherCardHeaderProps {
  voucher: Voucher;
  isCopied: boolean;
  onCopy: () => void;
  onDelete: () => void;
  onLongPress: () => void;
}

const VoucherCardHeader: React.FC<VoucherCardHeaderProps> = ({
  voucher,
  isCopied,
  onCopy,
  onDelete,
  onLongPress,
}) => {
  const longPressHandlers = useLongPress(() => {
    soundManager.playClick();
    onLongPress();
  });

  return (
    <div
      {...longPressHandlers}
      className="flex items-start justify-between gap-2 border-b border-slate-100 pb-2.5"
    >
      <div className="min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-bold text-[#235347] text-sm truncate">
            {voucher.agent}
          </span>
          <span className="text-[11px] font-mono bg-[#EAF0EE] px-2 py-0.5 rounded-md text-[#235347] font-bold whitespace-nowrap">
            No.{voucher.no}
          </span>
        </div>
        <span className="text-[11px] text-slate-400 font-mono block mt-0.5">
          {voucher.time}
        </span>
      </div>

      <div
        className="flex items-center gap-1.5 shrink-0"
        onTouchStart={(e) => e.stopPropagation()}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={onCopy}
          className={`p-1.5 rounded-lg text-xs flex items-center gap-1 transition cursor-pointer ${
            isCopied
              ? 'bg-emerald-100 text-[#235347]'
              : 'bg-slate-100 text-slate-600 hover:bg-emerald-50 hover:text-[#235347]'
          }`}
          title="ဘောင်ချာ စာသားကူးယူမည်"
        >
          {isCopied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
        </button>

        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            onDelete();
          }}
          className="p-1.5 rounded-lg text-xs text-slate-400 hover:text-red-600 hover:bg-red-50 transition cursor-pointer"
          title="ဘောင်ချာဖျက်မည်"
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

export const VouchersScreen: React.FC = () => {
  const {
    activeVouchers,
    agents,
    blocked,
    selectedDate,
    selectedSession,
    deleteVoucher,
    deleteNumber,
    editNumber,
    voucherFilterAgent,
    setVoucherFilterAgent,
  } = useLottery();

  const [searchQuery, setSearchQuery] = useState('');
  const [copiedVoucherId, setCopiedVoucherId] = useState<number | null>(null);

  // Modals
  const [voucherToDelete, setVoucherToDelete] = useState<Voucher | null>(null);
  const [voucherActionTarget, setVoucherActionTarget] = useState<Voucher | null>(null);
  const [betItemToEdit, setBetItemToEdit] = useState<{
    voucherId: number;
    item: BetItem;
  } | null>(null);
  const [alertBox, setAlertBox] = useState<AlertBoxState | null>(null);

  // Edit fields
  const [editNum, setEditNum] = useState('');
  const [editAmt, setEditAmt] = useState('');

  const dotDate = formatDotDate(selectedDate);

  // Filtered vouchers sorted NEWEST FIRST (latest voucher number at top)
  const filteredVouchers = useMemo(() => {
    return activeVouchers
      .filter((v) => {
        if (voucherFilterAgent !== 'ALL' && v.agent !== voucherFilterAgent) {
          return false;
        }
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase().trim();
          const matchesNo = v.no.toString().includes(q);
          const matchesAgent = v.agent.toLowerCase().includes(q);
          const matchesNumber = v.items.some((i) => i.n.includes(q));
          if (!matchesNo && !matchesAgent && !matchesNumber) return false;
        }
        return true;
      })
      .sort((a, b) => {
        if (b.no !== a.no) return b.no - a.no;
        return b.id - a.id;
      });
  }, [activeVouchers, voucherFilterAgent, searchQuery]);

  const totalVoucherAmount = useMemo(() => {
    return filteredVouchers.reduce((sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0), 0);
  }, [filteredVouchers]);

  // Copy formatted voucher for Viber / Telegram
  const handleCopyVoucher = (v: Voucher) => {
    soundManager.playClick();
    const ag = agents.find((a) => a.id === v.agentId || a.name === v.agent);
    const rate = ag ? ag.rate : 15;
    const total = v.items.reduce((s, i) => s + i.a, 0);
    const comm = Math.round(total * (rate / 100));
    const net = total - comm;

    const lines = [
      `Lucky2D ဘောင်ချာ`,
      `အမှတ်: No.${v.no}`,
      `ကိုယ်စားလှယ်: ${v.agent}`,
      `ရက်စွဲ: ${dotDate} (${selectedSession || ''})`,
      `-------------------`,
    ];

    v.items.forEach((item) => {
      lines.push(`${item.n} = ${fmt(item.a)}`);
    });

    lines.push(`-------------------`);
    lines.push(`စုစုပေါင်း: ${fmt(total)}`);
    lines.push(`ကော်မရှင် (${rate}%): ${fmt(comm)}`);
    lines.push(`ကျန်ငွေ: ${fmt(net)}`);

    navigator.clipboard.writeText(lines.join('\n'));
    setCopiedVoucherId(v.id);
    setTimeout(() => setCopiedVoucherId(null), 2500);
  };

  const handleOpenEditBet = (voucherId: number, item: BetItem) => {
    soundManager.playClick();
    setBetItemToEdit({ voucherId, item });
    setEditNum(item.n);
    setEditAmt(item.a.toString());
  };

  const handleSaveEditBet = (e: React.FormEvent) => {
    e.preventDefault();
    if (!betItemToEdit) return;

    const num = editNum.trim().padStart(2, '0');
    const amt = parseInt(editAmt, 10);

    if (num.length !== 2 || !/^\d{2}$/.test(num) || isNaN(amt) || amt <= 0) {
      soundManager.playDelete();
      setAlertBox({
        title: 'ပုံစံမမှန်သော အချက်အလက်',
        message: '၂ လုံးဂဏန်း (00-99) နှင့် မှန်ကန်သော ငွေပမာဏ ထည့်သွင်းပါ။',
      });
      return;
    }

    if (blocked.has(num)) {
      soundManager.playDelete();
      setAlertBox({
        title: 'မရဂဏန်း သတိပေးချက်',
        sections: [
          {
            label: 'မရဂဏန်း (ပိတ်ဂဏန်း)',
            reason: 'ဤဂဏန်းသည် ပိတ်ဂဏန်းစာရင်းတွင် ပါဝင်နေသောကြောင့် ပြင်ဆင်၍ မရပါ။',
            items: [num],
            tone: 'danger',
          },
        ],
      });
      return;
    }

    editNumber(betItemToEdit.item.id, num, amt);
    setBetItemToEdit(null);
  };

  const handleDeleteSingleBet = () => {
    if (!betItemToEdit) return;
    deleteNumber(betItemToEdit.item.id);
    setBetItemToEdit(null);
  };

  const handleConfirmDeleteVoucher = () => {
    if (!voucherToDelete) return;
    deleteVoucher(voucherToDelete.id);
    setVoucherToDelete(null);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ဘောင်ချာများ" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        <RoundRequiredHelperBanner />

        {/* Filter & Search Bar */}
        <div className="bg-white rounded-2xl p-3.5 shadow-sm border border-emerald-900/10 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          <div className="flex flex-1 items-center gap-2 min-w-0">
            <div className="relative flex-1 min-w-0">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="ဘောင်ချာနံပါတ် / ဂဏန်းရှာရန်..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
              />
            </div>

            <div className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 rounded-xl px-2.5 py-2 shrink-0 max-w-[145px] sm:max-w-[180px]">
              <Filter className="w-3.5 h-3.5 text-gray-500 shrink-0" />
              <select
                value={voucherFilterAgent}
                onChange={(e) => setVoucherFilterAgent(e.target.value)}
                className="bg-transparent text-xs text-gray-700 font-medium focus:outline-none truncate w-full"
              >
                <option value="ALL">အားလုံး</option>
                {agents.map((a) => (
                  <option key={a.id} value={a.name}>
                    {a.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between md:justify-end gap-3 text-xs text-gray-600 font-medium pt-2 md:pt-0 border-t md:border-t-0 border-gray-100 whitespace-nowrap">
            <span>
              ဘောင်ချာ: <strong className="text-gray-900 tabular-nums">{filteredVouchers.length}</strong> စောင်
            </span>
            <div className="h-3 w-px bg-gray-300" />
            <span>
              စုစုပေါင်း: <strong className="text-[#235347] font-mono tabular-nums">{fmt(totalVoucherAmount)}</strong>
            </span>
          </div>
        </div>

        {/* Vouchers List */}
        {filteredVouchers.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center border border-gray-200 shadow-sm flex flex-col items-center justify-center gap-2">
            <Receipt className="w-8 h-8 text-gray-400" />
            <p className="text-gray-500 text-sm">ဘောင်ချာမှတ်တမ်း မရှိသေးပါ</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {filteredVouchers.map((voucher) => {
              const isCopied = copiedVoucherId === voucher.id;
              const ag = agents.find((a) => a.id === voucher.agentId || a.name === voucher.agent);
              const rate = ag ? ag.rate : 15;
              const totalAmount = voucher.items.reduce((s, i) => s + i.a, 0);
              const commissionAmount = Math.round(totalAmount * (rate / 100));
              const netAmount = totalAmount - commissionAmount;

              return (
                <div
                  key={voucher.id}
                  className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col gap-3"
                >
                  {/* Voucher Header (supports long-press action menu) */}
                  <VoucherCardHeader
                    voucher={voucher}
                    isCopied={isCopied}
                    onCopy={() => handleCopyVoucher(voucher)}
                    onDelete={() => setVoucherToDelete(voucher)}
                    onLongPress={() => setVoucherActionTarget(voucher)}
                  />

                  {/* Bet Items Table */}
                  <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl overflow-hidden bg-white">
                    <div className="grid grid-cols-12 bg-[#EAF0EE] text-[11px] font-bold text-[#235347] py-2 px-3">
                      <div className="col-span-2 text-center">စဉ်</div>
                      <div className="col-span-4 text-center">ဂဏန်း</div>
                      <div className="col-span-5 text-right pr-1">ပမာဏ</div>
                      <div className="col-span-1 text-center"></div>
                    </div>

                    {voucher.items.map((item, idx) => (
                      <VoucherBetRow
                        key={item.id}
                        index={idx}
                        item={item}
                        onSelect={() => handleOpenEditBet(voucher.id, item)}
                      />
                    ))}
                  </div>

                  {/* Voucher Footer Summary */}
                  <div className="flex flex-wrap items-center justify-between gap-2 text-xs pt-1">
                    <div className="text-slate-500 flex flex-wrap items-center gap-x-3 gap-y-1">
                      <span className="whitespace-nowrap">
                        ကော်မရှင် ({rate}%): <strong className="text-amber-700 font-mono tabular-nums">{fmt(commissionAmount)}</strong>
                      </span>
                      <span className="whitespace-nowrap">
                        ကျန်ငွေ: <strong className="text-blue-700 font-mono tabular-nums">{fmt(netAmount)}</strong>
                      </span>
                    </div>

                    <div className="text-right whitespace-nowrap ml-auto">
                      <span className="text-slate-600 mr-1.5 font-medium">စုစုပေါင်း:</span>
                      <span className="text-base font-bold font-mono tabular-nums text-[#235347]">
                        {fmt(totalAmount)}
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Voucher Long-Press Action Dialog Box */}
      {voucherActionTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-xs w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col">
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center justify-between gap-2">
              <div className="min-w-0">
                <h3 className="font-bold text-sm truncate">
                  {voucherActionTarget.agent} (No.{voucherActionTarget.no})
                </h3>
                <p className="text-[11px] text-emerald-200 font-mono">{voucherActionTarget.time}</p>
              </div>
              <button
                type="button"
                onClick={() => setVoucherActionTarget(null)}
                className="text-white/80 hover:text-white p-1 shrink-0"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-2.5">
              <button
                type="button"
                onClick={() => {
                  const target = voucherActionTarget;
                  setVoucherActionTarget(null);
                  handleCopyVoucher(target);
                }}
                className="w-full py-2.5 px-4 bg-[#235347] hover:bg-[#183B32] text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition cursor-pointer"
              >
                <Copy className="w-4 h-4" />
                <span>ဘောင်ချာ ကူးယူမည်</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  const target = voucherActionTarget;
                  setVoucherActionTarget(null);
                  soundManager.playClick();
                  setVoucherToDelete(target);
                }}
                className="w-full py-2.5 px-4 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
                <span>ဘောင်ချာ ဖျက်မည်</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  soundManager.playClick();
                  setVoucherActionTarget(null);
                }}
                className="w-full py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                မလုပ်ပါ
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Single Bet Item Modal */}
      {betItemToEdit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 animate-in fade-in">
          <form
            onSubmit={handleSaveEditBet}
            className="bg-white rounded-2xl max-w-xs w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col"
          >
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center justify-between">
              <h3 className="font-bold text-sm">ဂဏန်း ပြင်ဆင် / ဖျက်မည်</h3>
              <button
                type="button"
                onClick={() => setBetItemToEdit(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3">
              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">ဂဏန်း (၂ လုံး)</label>
                <input
                  type="text"
                  maxLength={2}
                  value={editNum}
                  onChange={(e) => setEditNum(e.target.value.replace(/\D/g, ''))}
                  className="w-full px-3 py-2 text-center text-xl font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">ငွေပမာဏ</label>
                <input
                  type="number"
                  min="1"
                  value={editAmt}
                  onChange={(e) => setEditAmt(e.target.value)}
                  className="w-full px-3 py-2 text-center text-lg font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
                />
              </div>
            </div>

            <div className="p-3.5 bg-gray-50 border-t border-gray-100 flex items-center justify-between gap-2">
              <button
                type="button"
                onClick={handleDeleteSingleBet}
                className="px-3 py-2 bg-red-100 hover:bg-red-200 text-red-700 rounded-xl text-xs font-bold transition flex items-center gap-1 whitespace-nowrap cursor-pointer"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>ဖျက်မည်</span>
              </button>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setBetItemToEdit(null)}
                  className="px-3 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-xl text-xs font-semibold whitespace-nowrap cursor-pointer"
                >
                  ပိတ်မည်
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#235347] hover:bg-[#1b4339] text-white rounded-xl text-xs font-bold transition shadow whitespace-nowrap cursor-pointer"
                >
                  သိမ်းမည်
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* Delete Voucher Confirmation Alert Box */}
      {voucherToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col">
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center gap-2.5">
              <div className="w-7 h-7 rounded-full bg-red-500/25 text-red-200 flex items-center justify-center shrink-0">
                <AlertTriangle className="w-4 h-4" />
              </div>
              <h4 className="font-bold text-sm">ဘောင်ချာ ဖျက်ရန် သေချာပါသလား?</h4>
            </div>
            <div className="p-4 space-y-4">
              <p className="text-xs text-slate-600 leading-relaxed">
                ဘောင်ချာ <strong className="text-slate-900">No.{voucherToDelete.no} ({voucherToDelete.agent})</strong> အား ဖျက်ပါက ထိုးထားသော ဂဏန်းအားလုံး စာရင်းမှ ဖယ်ရှားသွားမည် ဖြစ်သည်။
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setVoucherToDelete(null)}
                  className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl whitespace-nowrap cursor-pointer"
                >
                  မဖျက်ပါ
                </button>
                <button
                  type="button"
                  onClick={handleConfirmDeleteVoucher}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition whitespace-nowrap cursor-pointer"
                >
                  ဖျက်မည်
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Blocked / Rejected Alert Box */}
      <AlertBoxModal alert={alertBox} onClose={() => setAlertBox(null)} />
    </div>
  );
};
