import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
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
      lines.push(`${item.n} = ${fmt(item.a)} Ks`);
    });

    lines.push(`-------------------`);
    lines.push(`စုစုပေါင်း: ${fmt(total)} Ks`);
    lines.push(`ကော်မရှင် (${rate}%): ${fmt(comm)} Ks`);
    lines.push(`ကျန်ငွေ: ${fmt(net)} Ks`);

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

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ဘောင်ချာများ" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        <RoundRequiredHelperBanner />

        {/* Filter & Search Bar */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
          <div className="flex flex-1 items-center gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                placeholder="ဘောင်ချာနံပါတ် သို့မဟုတ် ဂဏန်းရှာရန်..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347]"
              />
            </div>

            <div className="flex items-center gap-1.5 bg-gray-50 border border-gray-200 rounded-xl px-2.5 py-2">
              <Filter className="w-3.5 h-3.5 text-gray-500" />
              <select
                value={voucherFilterAgent}
                onChange={(e) => setVoucherFilterAgent(e.target.value)}
                className="bg-transparent text-xs text-gray-700 font-medium focus:outline-none"
              >
                <option value="ALL">ကိုယ်စားလှယ် အားလုံး</option>
                {agents.map((a) => (
                  <option key={a.id} value={a.name}>
                    {a.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="flex items-center justify-between md:justify-end gap-3 text-xs text-gray-600 font-medium pt-2 md:pt-0 border-t md:border-t-0 border-gray-100">
            <span>
              ဘောင်ချာ: <strong className="text-gray-900">{filteredVouchers.length}</strong> စောင်
            </span>
            <div className="h-3 w-px bg-gray-300" />
            <span>
              စုစုပေါင်း: <strong className="text-[#235347] font-mono">{fmt(totalVoucherAmount)}</strong> Ks
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
                  {/* Voucher Header */}
                  <div className="flex items-start justify-between border-b border-gray-100 pb-2.5">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-[#235347] text-sm">
                          {voucher.agent}
                        </span>
                        <span className="text-[11px] font-mono bg-gray-100 px-2 py-0.5 rounded text-gray-600 font-bold">
                          No.{voucher.no}
                        </span>
                      </div>
                      <span className="text-[11px] text-gray-400">
                        {voucher.time}
                      </span>
                    </div>

                    <div className="flex items-center gap-1.5">
                      <button
                        onClick={() => handleCopyVoucher(voucher)}
                        className={`p-1.5 rounded-lg text-xs flex items-center gap-1 transition ${
                          isCopied
                            ? 'bg-emerald-100 text-[#235347]'
                            : 'bg-gray-100 text-gray-600 hover:bg-emerald-50 hover:text-[#235347]'
                        }`}
                        title="ဘောင်ချာ စာသားကူးယူမည်"
                      >
                        {isCopied ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
                      </button>

                      <button
                        onClick={() => {
                          soundManager.playClick();
                          setVoucherToDelete(voucher);
                        }}
                        className="p-1.5 rounded-lg text-xs text-gray-400 hover:text-red-600 hover:bg-red-50 transition"
                        title="ဘောင်ချာဖျက်မည်"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Bet Items Table */}
                  <div className="divide-y divide-gray-100 border border-gray-100 rounded-xl overflow-hidden bg-gray-50/50">
                    <div className="grid grid-cols-12 bg-gray-100 text-[11px] font-semibold text-gray-600 py-1.5 px-3">
                      <div className="col-span-2 text-center">စဉ်</div>
                      <div className="col-span-4 text-center">ဂဏန်း</div>
                      <div className="col-span-5 text-right">ငွေပမာဏ</div>
                      <div className="col-span-1 text-center"></div>
                    </div>

                    {voucher.items.map((item, idx) => (
                      <div
                        key={item.id}
                        className="grid grid-cols-12 items-center py-1.5 px-3 text-xs font-mono hover:bg-emerald-50/50 transition cursor-pointer"
                        onClick={() => handleOpenEditBet(voucher.id, item)}
                        title="ပြင်ဆင်ရန် နှိပ်ပါ"
                      >
                        <div className="col-span-2 text-center text-gray-400 font-sans">{idx + 1}</div>
                        <div className="col-span-4 text-center font-bold text-gray-900 text-sm">
                          {item.n}
                        </div>
                        <div className="col-span-5 text-right font-bold text-[#235347]">
                          {fmt(item.a)} Ks
                        </div>
                        <div className="col-span-1 text-center text-gray-400 hover:text-gray-700">
                          <Edit className="w-3 h-3 mx-auto" />
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Voucher Footer Summary */}
                  <div className="flex items-center justify-between text-xs pt-1">
                    <div className="text-gray-500 space-x-3">
                      <span>
                        ကော်မရှင် ({rate}%): <strong className="text-amber-700 font-mono">{fmt(commissionAmount)}</strong> Ks
                      </span>
                      <span>
                        ကျန်ငွေ: <strong className="text-blue-700 font-mono">{fmt(netAmount)}</strong> Ks
                      </span>
                    </div>

                    <div className="text-right">
                      <span className="text-gray-600 mr-1.5 font-medium">စုစုပေါင်း:</span>
                      <span className="text-base font-bold font-mono text-[#235347]">
                        {fmt(totalAmount)} Ks
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Edit Single Bet Item Modal */}
      {betItemToEdit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSaveEditBet}
            className="bg-white rounded-2xl max-w-xs w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col"
          >
            <div className="bg-[#235347] text-white p-3.5 flex items-center justify-between">
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
                <label className="text-xs font-semibold text-gray-600 block mb-1">ဂဏန်း (၂ လုံး)</label>
                <input
                  type="text"
                  maxLength={2}
                  value={editNum}
                  onChange={(e) => setEditNum(e.target.value.replace(/\D/g, ''))}
                  className="w-full px-3 py-2 text-center text-xl font-bold font-mono border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#235347]"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">ငွေပမာဏ (Ks)</label>
                <input
                  type="number"
                  step="50"
                  min="50"
                  value={editAmt}
                  onChange={(e) => setEditAmt(e.target.value)}
                  className="w-full px-3 py-2 text-center text-lg font-bold font-mono border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#235347]"
                />
              </div>
            </div>

            <div className="p-3 bg-gray-50 border-t border-gray-100 flex items-center justify-between gap-2">
              <button
                type="button"
                onClick={handleDeleteSingleBet}
                className="px-3 py-2 bg-red-100 hover:bg-red-200 text-red-700 rounded-xl text-xs font-bold transition flex items-center gap-1"
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
                  className="px-4 py-2 bg-[#235347] hover:bg-[#1b4339] text-white rounded-xl text-xs font-bold transition shadow"
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
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">ဘောင်ချာ ဖျက်ရန် သေချာပါသလား?</h4>
            <p className="text-xs text-gray-500">
              ဘောင်ချာ No.{voucherToDelete.no} ({voucherToDelete.agent}) အား ဖျက်ပါက ထိုးထားသော ဂဏန်းအားလုံး စာရင်းမှ ဖယ်ရှားသွားမည် ဖြစ်သည်။
            </p>
            <div className="flex gap-2 mt-2">
              <button
                onClick={() => setVoucherToDelete(null)}
                className="flex-1 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မဖျက်ပါ
              </button>
              <button
                onClick={handleConfirmDeleteVoucher}
                className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition"
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
