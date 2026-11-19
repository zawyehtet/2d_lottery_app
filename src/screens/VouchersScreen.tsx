import React, { useState, useMemo } from 'react';
import { useLottery, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import {
  Receipt,
  Copy,
  Trash2,
  Check,
  Search,
  X,
  AlertTriangle,
} from 'lucide-react';
import { Voucher, BetItem } from '../types';
import { soundManager } from '../util/soundManager';

export const VouchersScreen: React.FC = () => {
  const {
    activeVouchers,
    agents,
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
  const [betItemToEdit, setBetItemToEdit] = useState<{
    voucherId: number;
    item: BetItem;
  } | null>(null);

  // Edit fields
  const [editNum, setEditNum] = useState('');
  const [editAmt, setEditAmt] = useState('');

  const dotDate = formatDotDate(selectedDate);

  // Filtered vouchers
  const filteredVouchers = useMemo(() => {
    return activeVouchers.filter((v) => {
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
    });
  }, [activeVouchers, voucherFilterAgent, searchQuery]);

  const totalVoucherAmount = useMemo(() => {
    return filteredVouchers.reduce(
      (sum, v) => sum + v.items.reduce((s, i) => s + i.a, 0),
      0
    );
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
      lines.push(`${item.n} = ${item.a}`);
    });

    lines.push(`-------------------`);
    lines.push(`စုစုပေါင်း: ${total}`);
    lines.push(`ကော်မရှင် (${rate}%): ${comm}`);
    lines.push(`ကျန်ငွေ: ${net}`);

    navigator.clipboard.writeText(lines.join('\n')).catch(() => {});
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
    if (num.length === 2 && !isNaN(amt) && amt > 0) {
      editNumber(betItemToEdit.item.id, num, amt);
      setBetItemToEdit(null);
    }
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

  const headerTitle =
    voucherFilterAgent !== 'ALL' ? `${voucherFilterAgent} ၏ ဘောင်ချာ` : 'ဘောင်ချာ';

  return (
    <div className="flex flex-col h-screen bg-[#F5F7F6] select-none">
      <Header title={headerTitle} showBack />

      <main className="flex-1 min-h-0 flex flex-col max-w-2xl w-full mx-auto">
        <div className="px-3 pt-3 pb-2 space-y-2 shrink-0">
          <RoundRequiredHelperBanner />

          {/* Clean Rounded Search Input */}
          <div className="relative">
            <Search className="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-600" />
            <input
              type="text"
              placeholder="ဘောင်ချာ ရှာရန်"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-12 pr-10 py-3.5 text-[15px] bg-white border border-[#CBD6D1] rounded-2xl text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-[#065240]"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-600"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {voucherFilterAgent !== 'ALL' && (
            <div className="flex items-center justify-between px-1 text-xs text-slate-600">
              <span>ထိုးသူ: {voucherFilterAgent}</span>
              <button
                type="button"
                onClick={() => setVoucherFilterAgent('ALL')}
                className="text-[#065240] font-semibold hover:underline"
              >
                အားလုံးကြည့်ရန်
              </button>
            </div>
          )}
        </div>

        {/* Vouchers List */}
        <div className="flex-1 overflow-y-auto px-3 pb-4 space-y-3">
          {filteredVouchers.length === 0 ? (
            <div className="bg-white rounded-2xl p-12 text-center border border-[#DCE5E1] shadow-sm flex flex-col items-center justify-center gap-2 mt-2">
              <Receipt className="w-8 h-8 text-slate-400" />
              <p className="text-slate-500 text-sm">ဘောင်ချာမှတ်တမ်း မရှိသေးပါ</p>
            </div>
          ) : (
            filteredVouchers.map((voucher) => {
              const isCopied = copiedVoucherId === voucher.id;
              const totalAmount = voucher.items.reduce((s, i) => s + i.a, 0);
              const initial = (voucher.agent || '?').trim().charAt(0).toUpperCase();

              return (
                <div
                  key={voucher.id}
                  className="bg-white rounded-2xl overflow-hidden shadow-sm border border-[#DCE5E1]"
                >
                  {/* Dark-Green Card Header */}
                  <div className="bg-[#065240] text-white px-4 py-3.5 flex items-center justify-between">
                    <span className="text-[18px] font-medium">အမှတ် {voucher.no}</span>
                    <span className="text-[13px] text-white/95">
                      အချိန် : {voucher.time}
                    </span>
                  </div>

                  {/* Agent ID & Name Strip */}
                  <div className="bg-white px-4 py-3 border-b border-[#E5ECE9] flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <div className="w-7 h-7 rounded-full bg-[#065240] text-white text-xs font-bold flex items-center justify-center shrink-0">
                        {initial}
                      </div>
                      <span className="text-[15px] text-slate-900 truncate">
                        အိုင်ဒီ : {voucher.agentId} , အမည် : {voucher.agent}
                      </span>
                    </div>
                    <span className="text-[15px] text-slate-600 shrink-0">
                      {voucher.items.length} ကွက်
                    </span>
                  </div>

                  {/* Bet Items Table */}
                  <div className="divide-y divide-[#EEF3F1]">
                    {voucher.items.map((item, idx) => (
                      <div
                        key={item.id}
                        onClick={() => handleOpenEditBet(voucher.id, item)}
                        className={`px-4 py-3 flex items-center justify-between cursor-pointer active:bg-emerald-50/40 ${
                          idx % 2 === 0 ? 'bg-white' : 'bg-[#F4F8F6]'
                        }`}
                        title="ပြင်ဆင်ရန် နှိပ်ပါ"
                      >
                        <span className="w-12 text-[15px] text-slate-600">
                          {idx + 1}.
                        </span>
                        <span className="flex-1 text-center text-[18px] font-medium text-[#065240]">
                          {item.n}
                        </span>
                        <span className="w-24 text-right text-[16px] text-slate-900">
                          {item.a}
                        </span>
                      </div>
                    ))}
                  </div>

                  {/* Compact Voucher Footer Actions */}
                  <div className="bg-[#F6FAF8] border-t border-[#E5ECE9] px-4 py-2 flex items-center justify-between">
                    <span className="text-[14px] font-medium text-[#065240]">
                      စုစုပေါင်း : {totalAmount}
                    </span>
                    <div className="flex items-center gap-1.5">
                      <button
                        type="button"
                        onClick={() => handleCopyVoucher(voucher)}
                        className={`p-2 rounded-lg transition ${
                          isCopied
                            ? 'bg-emerald-100 text-[#065240]'
                            : 'text-slate-600 hover:bg-emerald-50 hover:text-[#065240]'
                        }`}
                        title="ဘောင်ချာ စာသားကူးယူမည်"
                      >
                        {isCopied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          soundManager.playClick();
                          setVoucherToDelete(voucher);
                        }}
                        className="p-2 rounded-lg text-slate-500 hover:text-[#F42222] hover:bg-red-50 transition"
                        title="ဘောင်ချာဖျက်မည်"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Sticky Dark-Green Bottom Total Bar */}
        <div className="px-3 pb-3 pt-1 shrink-0 bg-[#F5F7F6]">
          <div className="bg-[#065240] text-white rounded-xl px-4 py-3.5 flex items-center justify-between shadow-md">
            <span className="text-[17px] font-medium">စုစုပေါင်း:</span>
            <span className="text-[19px] font-medium">{totalVoucherAmount}</span>
          </div>
        </div>
      </main>

      {/* Edit Single Bet Item Modal */}
      {betItemToEdit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSaveEditBet}
            className="bg-white rounded-2xl max-w-xs w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col"
          >
            <div className="bg-[#065240] text-white p-3.5 flex items-center justify-between">
              <h3 className="font-bold text-sm">ဂဏန်း ပြင်ဆင်/ဖျက်မည်</h3>
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
                <label className="text-xs font-semibold text-gray-600 block mb-1">
                  ဂဏန်း (၂ လုံး)
                </label>
                <input
                  type="text"
                  maxLength={2}
                  value={editNum}
                  onChange={(e) => setEditNum(e.target.value.replace(/\D/g, ''))}
                  className="w-full px-3 py-2 text-center text-xl font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">
                  ငွေပမာဏ
                </label>
                <input
                  type="number"
                  step="50"
                  min="50"
                  value={editAmt}
                  onChange={(e) => setEditAmt(e.target.value)}
                  className="w-full px-3 py-2 text-center text-lg font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
                />
              </div>
            </div>

            <div className="p-3 bg-gray-50 border-t border-gray-100 flex items-center justify-between gap-2">
              <button
                type="button"
                onClick={handleDeleteSingleBet}
                className="px-3 py-2 bg-red-100 hover:bg-red-200 text-[#F42222] rounded-xl text-xs font-bold transition flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>ဖျက်မည်</span>
              </button>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setBetItemToEdit(null)}
                  className="px-3 py-2 bg-gray-200 text-gray-700 rounded-xl text-xs font-semibold"
                >
                  ပိတ်မည်
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#065240] hover:bg-[#054435] text-white rounded-xl text-xs font-bold transition shadow"
                >
                  သိမ်းမည်
                </button>
              </div>
            </div>
          </form>
        </div>
      )}

      {/* Delete Voucher Confirmation Modal */}
      {voucherToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-gray-200 text-center flex flex-col gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-[#F42222] flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">
              ဘောင်ချာ ဖျက်ရန် သေချာပါသလား?
            </h4>
            <p className="text-xs text-gray-500">
              ဘောင်ချာ အမှတ် {voucherToDelete.no} ({voucherToDelete.agent}) အား ဖျက်ပါက
              ထိုးထားသော ဂဏန်းအားလုံး စာရင်းမှ ဖယ်ရှားသွားမည် ဖြစ်သည်။
            </p>
            <div className="flex gap-2 mt-2">
              <button
                type="button"
                onClick={() => setVoucherToDelete(null)}
                className="flex-1 py-2.5 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မဖျက်ပါ
              </button>
              <button
                type="button"
                onClick={handleConfirmDeleteVoucher}
                className="flex-1 py-2.5 bg-[#F42222] hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition"
              >
                ဖျက်မည်
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
