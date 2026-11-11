import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import {
  ShieldAlert,
  SendHorizontal,
  History,
  FileCheck2,
  Users,
} from 'lucide-react';
import { soundManager } from '../util/soundManager';

export const BrakeScreen: React.FC = () => {
  const {
    selectedDate,
    selectedSession,
    sessions,
    savedBrakeLimitText,
    currentBrakeInput,
    updateBrakeLimit,
    saveBrakeLimit,
    getAll100Numbers,
    executeLayoff,
    lockAndSaveBrakeSettlement,
    calculateSettlementForSession,
    currentWinningNumber,
    navigateTo,
  } = useLottery();

  const [showLayoffModal, setShowLayoffModal] = useState(false);
  const [layoffMode, setLayoffMode] = useState<'all' | 'top30'>('all');

  const dotDate = formatDotDate(selectedDate);
  const currentLimit = parseInt(currentBrakeInput, 10) || 0;

  // 100 numbers summary
  const numbersData = useMemo(() => {
    return getAll100Numbers();
  }, [getAll100Numbers]);

  // Retained numbers (netAmount > 0)
  const retainedList = useMemo(() => {
    return numbersData
      .filter((item) => item.netAmount > 0)
      .sort((a, b) => b.netAmount - a.netAmount);
  }, [numbersData]);

  // Layoff numbers (layoffAmount > 0)
  const laidOffList = useMemo(() => {
    return numbersData
      .filter((item) => item.layoffAmount > 0)
      .sort((a, b) => b.layoffAmount - a.layoffAmount);
  }, [numbersData]);

  // Excess items over limit
  const excessItems = useMemo(() => {
    const list = numbersData
      .filter((item) => item.netAmount > currentLimit)
      .map((item) => ({
        n: item.n,
        excess: item.netAmount - currentLimit,
      }))
      .sort((a, b) => b.excess - a.excess);

    if (layoffMode === 'top30') {
      return list.slice(0, 30);
    }
    return list;
  }, [numbersData, currentLimit, layoffMode]);

  const totalExcessAmount = useMemo(() => {
    return excessItems.reduce((sum, item) => sum + item.excess, 0);
  }, [excessItems]);

  // Session settlement ledger calculation
  const settlementCalc = useMemo(() => {
    return calculateSettlementForSession(selectedDate, selectedSession, currentWinningNumber);
  }, [calculateSettlementForSession, selectedDate, selectedSession, currentWinningNumber]);

  const totalGrossSales = useMemo(() => {
    return numbersData.reduce((sum, i) => sum + i.totalBet, 0);
  }, [numbersData]);

  const totalLaidOffAmount = useMemo(() => {
    return numbersData.reduce((sum, i) => sum + i.layoffAmount, 0);
  }, [numbersData]);

  const handleConfirmLayoff = () => {
    executeLayoff(layoffMode === 'all');
    setShowLayoffModal(false);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ဘရိတ်နှင့် တင်ကွက်" showBack />

      <main className="flex-1 max-w-5xl w-full mx-auto p-4 flex flex-col gap-4">
        <RoundRequiredHelperBanner />

        {/* Top Action Bar: Brake Limit Setting & Action Buttons */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-emerald-50 rounded-xl text-[#235347]">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold block">
                လက်ရှိ ဘရိတ်သတ်မှတ်ချက်: <strong className="text-gray-800">{savedBrakeLimitText}</strong>
              </span>
              <div className="flex items-center gap-2 mt-1">
                <input
                  type="text"
                  inputMode="numeric"
                  value={currentBrakeInput}
                  onChange={(e) => updateBrakeLimit(e.target.value)}
                  className="w-32 px-3 py-1.5 text-base font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347]"
                />
                <span className="text-xs text-gray-500 font-medium">Ks</span>
                <button
                  onClick={saveBrakeLimit}
                  className="px-3 py-1.5 bg-[#235347] hover:bg-[#1b4339] text-white text-xs font-semibold rounded-xl transition"
                >
                  မှတ်မည်
                </button>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <button
              onClick={() => {
                soundManager.playClick();
                setShowLayoffModal(true);
              }}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-4 py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-semibold rounded-xl shadow-sm transition"
            >
              <SendHorizontal className="w-4 h-4" />
              <span>တင်ကွက် ထုတ်မည်</span>
            </button>

            <button
              onClick={() => {
                soundManager.playClick();
                navigateTo('LAYOFF_HISTORY');
              }}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-800 text-xs font-semibold rounded-xl transition"
            >
              <History className="w-4 h-4 text-[#235347]" />
              <span>တင်ကွက်မှတ်တမ်း</span>
            </button>

            <button
              onClick={() => {
                soundManager.playClick();
                navigateTo('AGENT_SETTLEMENT');
              }}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-2.5 bg-emerald-50 hover:bg-emerald-100 text-[#235347] border border-emerald-200 text-xs font-semibold rounded-xl transition"
            >
              <Users className="w-4 h-4" />
              <span>ကိုယ်စားလှယ်ရှင်းတမ်း</span>
            </button>
          </div>
        </div>

        {/* 2-Column Tables: လက်ခံကွက် vs တင်ကွက် */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Column 1: လက်ခံကွက် (Retained Bets) */}
          <div className="bg-white rounded-2xl shadow-sm border border-emerald-900/10 overflow-hidden flex flex-col">
            <div className="bg-[#235347] text-white px-4 py-3 flex items-center justify-between">
              <span className="font-bold text-sm">လက်ခံကွက်များ</span>
              <span className="text-xs text-emerald-200 font-mono">
                စုစုပေါင်း: {fmt(settlementCalc.totalRetained)} Ks
              </span>
            </div>

            <div className="grid grid-cols-12 bg-emerald-50/70 text-[11px] font-semibold text-[#235347] py-2 px-3 border-b border-emerald-100">
              <div className="col-span-2 text-center">စဉ်</div>
              <div className="col-span-4 text-center">ဂဏန်း</div>
              <div className="col-span-6 text-right pr-2">လက်ခံငွေ (Ks)</div>
            </div>

            <div className="divide-y divide-gray-100 max-h-80 overflow-y-auto">
              {retainedList.length === 0 ? (
                <div className="py-8 text-center text-gray-400 text-xs">
                  လက်ခံထားသောဂဏန်း မရှိသေးပါ
                </div>
              ) : (
                retainedList.map((item, idx) => (
                  <div
                    key={item.n}
                    className={`grid grid-cols-12 items-center py-2 px-3 text-xs font-mono ${
                      item.netAmount > currentLimit ? 'bg-red-50 text-red-700 font-bold' : 'text-gray-800'
                    }`}
                  >
                    <div className="col-span-2 text-center text-gray-400 font-sans">{idx + 1}</div>
                    <div className="col-span-4 text-center font-bold text-sm">{item.n}</div>
                    <div className="col-span-6 text-right pr-2 font-bold">
                      {fmt(item.netAmount)}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Column 2: တင်ကွက် (Layoff Bets) */}
          <div className="bg-white rounded-2xl shadow-sm border border-emerald-900/10 overflow-hidden flex flex-col">
            <div className="bg-red-700 text-white px-4 py-3 flex items-center justify-between">
              <span className="font-bold text-sm">တင်ကွက်များ (အပြင်လွှဲ)</span>
              <span className="text-xs text-red-100 font-mono">
                စုစုပေါင်း: {fmt(totalLaidOffAmount)} Ks
              </span>
            </div>

            <div className="grid grid-cols-12 bg-red-50/70 text-[11px] font-semibold text-red-800 py-2 px-3 border-b border-red-100">
              <div className="col-span-2 text-center">စဉ်</div>
              <div className="col-span-4 text-center">ဂဏန်း</div>
              <div className="col-span-6 text-right pr-2">တင်ငွေ (Ks)</div>
            </div>

            <div className="divide-y divide-gray-100 max-h-80 overflow-y-auto">
              {laidOffList.length === 0 ? (
                <div className="py-8 text-center text-gray-400 text-xs">
                  တင်ကွက် မရှိသေးပါ
                </div>
              ) : (
                laidOffList.map((item, idx) => (
                  <div
                    key={item.n}
                    className="grid grid-cols-12 items-center py-2 px-3 text-xs font-mono text-red-700"
                  >
                    <div className="col-span-2 text-center text-gray-400 font-sans">{idx + 1}</div>
                    <div className="col-span-4 text-center font-bold text-sm">{item.n}</div>
                    <div className="col-span-6 text-right pr-2 font-bold">
                      {fmt(item.layoffAmount)}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Post-Draw Single-Round Ledger Summary */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10">
          <div className="flex items-center justify-between pb-3 border-b border-gray-100 mb-3">
            <div className="flex items-center gap-2">
              <FileCheck2 className="w-5 h-5 text-[#235347]" />
              <h3 className="font-bold text-sm text-gray-800">
                ဤအချိန်ပိုင်း စာရင်းချုပ် ({selectedSession || 'အပိုင်းမရွေးရသေး'})
              </h3>
            </div>
            {currentWinningNumber ? (
              <span className="text-xs font-bold text-red-600 bg-red-50 border border-red-200 px-2 py-0.5 rounded-full font-mono">
                ပေါက်ဂဏန်း: {currentWinningNumber}
              </span>
            ) : (
              <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded-full">
                ပေါက်သီး မထွက်သေးပါ
              </span>
            )}
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
            <div className="bg-gray-50 p-2.5 rounded-xl border border-gray-100">
              <span className="text-gray-500 block text-[11px]">ရောင်းရငွေ (Gross)</span>
              <span className="font-bold font-mono text-gray-800 text-sm">
                {fmt(totalGrossSales)} Ks
              </span>
            </div>

            <div className="bg-red-50/50 p-2.5 rounded-xl border border-red-100">
              <span className="text-red-700 block text-[11px]">တင်ကွက်ငွေ (Layoff)</span>
              <span className="font-bold font-mono text-red-700 text-sm">
                {fmt(totalLaidOffAmount)} Ks
              </span>
            </div>

            <div className="bg-emerald-50/50 p-2.5 rounded-xl border border-emerald-100">
              <span className="text-emerald-700 block text-[11px]">လက်ခံငွေ (Retained)</span>
              <span className="font-bold font-mono text-[#235347] text-sm">
                {fmt(settlementCalc.totalRetained)} Ks
              </span>
            </div>

            <div className="bg-amber-50/50 p-2.5 rounded-xl border border-amber-100">
              <span className="text-amber-800 block text-[11px]">ကော်မရှင် (Commission)</span>
              <span className="font-bold font-mono text-amber-800 text-sm">
                {fmt(settlementCalc.commission)} Ks
              </span>
            </div>

            <div className="bg-blue-50/50 p-2.5 rounded-xl border border-blue-100">
              <span className="text-blue-800 block text-[11px]">ကျန်ငွေ (Retained Net)</span>
              <span className="font-bold font-mono text-blue-800 text-sm">
                {fmt(settlementCalc.retainedNet)} Ks
              </span>
            </div>

            <div className="bg-red-50 p-2.5 rounded-xl border border-red-200">
              <span className="text-red-800 block text-[11px]">လျော်ကြေး (Payout)</span>
              <span className="font-bold font-mono text-red-700 text-sm">
                {fmt(settlementCalc.payout)} Ks
              </span>
            </div>

            <div
              className={`col-span-2 p-2.5 rounded-xl border flex items-center justify-between ${
                settlementCalc.profitLoss >= 0
                  ? 'bg-emerald-100/70 border-emerald-300 text-emerald-900'
                  : 'bg-red-100/70 border-red-300 text-red-900'
              }`}
            >
              <div>
                <span className="block text-[11px] font-semibold">
                  {settlementCalc.profitLoss >= 0 ? 'အမြတ်ငွေ (Profit)' : 'အရှုံးငွေ (Loss)'}
                </span>
                <span className="text-base font-bold font-mono">
                  {settlementCalc.profitLoss >= 0 ? '+' : ''}
                  {fmt(settlementCalc.profitLoss)} Ks
                </span>
              </div>
              <button
                onClick={lockAndSaveBrakeSettlement}
                className="px-3.5 py-2 bg-[#235347] hover:bg-[#1b4339] text-white font-bold rounded-xl text-xs transition shadow"
              >
                ရက်ချုပ် သိမ်းဆည်းမည်
              </button>
            </div>
          </div>
        </div>
      </main>

      {/* Layoff Confirmation Modal */}
      {showLayoffModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col max-h-[85vh]">
            <div className="bg-red-600 text-white p-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <SendHorizontal className="w-5 h-5" />
                <h3 className="font-bold text-base">တင်ကွက် ထုတ်ယူရန် စစ်ဆေးခြင်း</h3>
              </div>
              <button
                onClick={() => setShowLayoffModal(false)}
                className="text-white/80 hover:text-white text-lg font-bold p-1"
              >
                ✕
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3 flex-1 overflow-y-auto">
              <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 text-amber-900 p-2.5 rounded-xl text-xs">
                <span>ဘရိတ်သတ်မှတ်ချက် {fmt(currentLimit)} Ks ကျော်လွန်သော ဂဏန်းများကို တင်ထုတ်မည်။</span>
              </div>

              {/* Mode Selection */}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setLayoffMode('all')}
                  className={`flex-1 py-2 rounded-xl text-xs font-semibold border transition ${
                    layoffMode === 'all'
                      ? 'bg-[#235347] text-white border-[#235347]'
                      : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  အားလုံးတင်မည်
                </button>
                <button
                  type="button"
                  onClick={() => setLayoffMode('top30')}
                  className={`flex-1 py-2 rounded-xl text-xs font-semibold border transition ${
                    layoffMode === 'top30'
                      ? 'bg-[#235347] text-white border-[#235347]'
                      : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  အကွက် ၃၀ တင်မည်
                </button>
              </div>

              {/* Layoff Items Preview List */}
              <div className="border border-gray-200 rounded-xl overflow-hidden">
                <div className="bg-gray-100 px-3 py-2 text-[11px] font-semibold text-gray-600 flex justify-between">
                  <span>ဂဏန်း (အရေအတွက်: {excessItems.length})</span>
                  <span>တင်မည့်ငွေ</span>
                </div>
                <div className="divide-y divide-gray-100 max-h-48 overflow-y-auto">
                  {excessItems.length === 0 ? (
                    <div className="py-6 text-center text-gray-400 text-xs">
                      ဘရိတ်ကျော်သော ဂဏန်း မရှိပါ
                    </div>
                  ) : (
                    excessItems.map((item) => (
                      <div key={item.n} className="flex justify-between px-3 py-2 text-xs font-mono">
                        <span className="font-bold text-gray-800">{item.n}</span>
                        <span className="text-red-600 font-bold">{fmt(item.excess)} Ks</span>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="bg-red-50 p-3 rounded-xl border border-red-200 flex justify-between items-center text-xs">
                <span className="font-semibold text-red-900">စုစုပေါင်း တင်မည့်ငွေ:</span>
                <span className="font-bold font-mono text-base text-red-700">
                  {fmt(totalExcessAmount)} Ks
                </span>
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => setShowLayoffModal(false)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-xl text-xs font-semibold"
              >
                မလုပ်တော့ပါ
              </button>
              <button
                type="button"
                disabled={excessItems.length === 0}
                onClick={handleConfirmLayoff}
                className="px-4 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition shadow"
              >
                တင်ကွက် အတည်ပြုမည်
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
