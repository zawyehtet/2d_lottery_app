import React, { useState, useMemo } from 'react';
import { useLottery } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import { Download, SendHorizontal } from 'lucide-react';
import { soundManager } from '../util/soundManager';

export const BrakeScreen: React.FC = () => {
  const {
    selectedDate,
    selectedSession,
    currentBrakeInput,
    updateBrakeLimit,
    saveBrakeLimit,
    getAll100Numbers,
    sort2DNumbersWithWinning,
    executeLayoff,
    lockAndSaveBrakeSettlement,
    currentWinningNumber,
    navigateTo,
    showToast,
  } = useLottery();

  const [showLayoffModal, setShowLayoffModal] = useState(false);
  const [layoffMode, setLayoffMode] = useState<'all' | 'top30'>('all');

  const currentLimit = parseInt(currentBrakeInput, 10) || 0;

  // All 100 numbers sorted (winning first if any, then active amounts desc, then 00..99 asc)
  const all100Sorted = useMemo(() => {
    const all = getAll100Numbers();
    return sort2DNumbersWithWinning(all, currentWinningNumber || null);
  }, [getAll100Numbers, sort2DNumbersWithWinning, currentWinningNumber]);

  // Excess items over active brake limit (when brake limit > 0)
  const allExcessItems = useMemo(() => {
    if (currentLimit <= 0) return [];
    return all100Sorted
      .filter((item) => item.netAmount > currentLimit)
      .map((item) => ({
        n: item.n,
        excess: item.netAmount - currentLimit,
      }))
      .sort((a, b) => b.excess - a.excess || a.n.localeCompare(b.n));
  }, [all100Sorted, currentLimit]);

  const modalExcessItems = useMemo(() => {
    if (layoffMode === 'top30') {
      return allExcessItems.slice(0, 30);
    }
    return allExcessItems;
  }, [allExcessItems, layoffMode]);

  const totalRetainedDisplay = useMemo(() => {
    return all100Sorted.reduce((sum, item) => {
      const kept = currentLimit > 0 ? Math.min(item.netAmount, currentLimit) : item.netAmount;
      return sum + kept;
    }, 0);
  }, [all100Sorted, currentLimit]);

  const totalExcessDisplay = useMemo(() => {
    return allExcessItems.reduce((sum, item) => sum + item.excess, 0);
  }, [allExcessItems]);

  const totalModalExcess = useMemo(() => {
    return modalExcessItems.reduce((sum, item) => sum + item.excess, 0);
  }, [modalExcessItems]);

  const handleExport = () => {
    soundManager.playClick();
    const lines: string[] = [
      `Lucky2D တင်ကွက် (${selectedDate} - ${selectedSession || 'အကြိမ်မရှိ'})`,
      `ဘရိတ်: ${currentLimit}`,
    ];
    if (allExcessItems.length > 0) {
      lines.push('--- တင်ကွက် ---');
      allExcessItems.forEach((item) => {
        lines.push(`${item.n}=${item.excess}`);
      });
      lines.push(`စုစုပေါင်း = ${totalExcessDisplay}`);
    } else {
      const active = all100Sorted.filter((x) => x.netAmount > 0);
      lines.push('--- လက်ခံကွက် ---');
      active.forEach((item) => {
        lines.push(`${item.n}=${item.netAmount}`);
      });
      lines.push(`စုစုပေါင်း = ${totalRetainedDisplay}`);
    }
    navigator.clipboard
      .writeText(lines.join('\n'))
      .then(() => showToast('Export စာသား ကူးယူပြီးပါပြီ'))
      .catch(() => showToast('ကူးယူရန် မအောင်မြင်ပါ'));
  };

  const handleSaveBrakeAndSettlement = () => {
    saveBrakeLimit();
    lockAndSaveBrakeSettlement();
  };

  const handleConfirmLayoff = () => {
    executeLayoff(layoffMode === 'all');
    setShowLayoffModal(false);
  };

  return (
    <div className="flex flex-col h-screen bg-[#F5F7F6] select-none">
      <Header
        title="တင်ကွက်"
        showBack
        actions={
          <button
            type="button"
            onClick={handleExport}
            className="flex items-center gap-1.5 bg-white text-slate-900 px-3 py-1.5 rounded-xl text-xs font-bold shadow-sm active:scale-95 transition"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export</span>
          </button>
        }
      />

      <main className="flex-1 min-h-0 max-w-2xl w-full mx-auto px-3 pt-2.5 pb-3 flex flex-col gap-2.5">
        <RoundRequiredHelperBanner />

        {/* Brake Limit Input Row */}
        <div className="flex items-center gap-3 px-1 shrink-0">
          <label
            htmlFor="brake-limit-input"
            className="text-[18px] font-medium text-[#065240]"
          >
            ဘရိတ် :
          </label>
          <input
            id="brake-limit-input"
            type="text"
            inputMode="numeric"
            value={currentBrakeInput}
            placeholder="0"
            onChange={(e) => updateBrakeLimit(e.target.value)}
            className="w-28 h-10 px-3 bg-white border border-[#CBD6D1] rounded-xl text-center text-[17px] font-medium text-slate-900 focus:outline-none focus:border-[#065240]"
          />
        </div>

        {/* Main 2-Column Split Card */}
        <div className="flex-1 min-h-0 bg-white rounded-2xl border border-[#DCE5E1] shadow-sm overflow-hidden flex flex-col">
          {/* Orange Split Header */}
          <div className="bg-[#FA7400] text-white grid grid-cols-2 items-center py-3 text-[16px] font-medium shrink-0 relative">
            <div className="text-center">လက်ခံကွက် ({all100Sorted.length})</div>
            <div className="text-center">တင်ကွက် ({allExcessItems.length})</div>
            <div className="absolute left-1/2 top-2.5 bottom-2.5 w-px bg-white/50 -translate-x-1/2" />
          </div>

          {/* Dark-Green Sub-Header */}
          <div className="bg-[#065240] text-white grid grid-cols-2 text-[14px] font-medium shrink-0 relative">
            <div className="px-3.5 py-2.5 flex items-center justify-between">
              <span>ဂဏန်းများ</span>
              <span>ပမာဏ</span>
            </div>
            <div className="px-3.5 py-2.5 flex items-center justify-between">
              <span>ဂဏန်းများ</span>
              <span>ပမာဏ</span>
            </div>
            <div className="absolute left-1/2 top-2 bottom-2 w-px bg-white/30 -translate-x-1/2" />
          </div>

          {/* Scrollable 2-Column Content */}
          <div className="flex-1 min-h-0 grid grid-cols-2 divide-x divide-[#DCE5E1]">
            {/* Left Column: Retained 100 Numbers */}
            <div className="overflow-y-auto divide-y divide-[#EEF3F1]">
              {all100Sorted.map((item, idx) => {
                const kept =
                  currentLimit > 0 ? Math.min(item.netAmount, currentLimit) : item.netAmount;
                return (
                  <div
                    key={item.n}
                    className={`px-3.5 py-2.5 flex items-center justify-between text-[16px] ${
                      idx % 2 === 1 ? 'bg-[#EDF3F0]' : 'bg-white'
                    }`}
                  >
                    <span className="text-slate-900">{item.n}</span>
                    <span className="text-slate-900 font-medium">{kept}</span>
                  </div>
                );
              })}
            </div>

            {/* Right Column: Excess / Layoff Numbers */}
            <div className="overflow-y-auto divide-y divide-[#EEF3F1]">
              {allExcessItems.length === 0 ? (
                <div className="p-4 text-[15px] text-slate-600">
                  ဘရိတ်ကျော် မရှိပါ
                </div>
              ) : (
                allExcessItems.map((item, idx) => (
                  <div
                    key={item.n}
                    className={`px-3.5 py-2.5 flex items-center justify-between text-[16px] ${
                      idx % 2 === 1 ? 'bg-[#FDF4F4]' : 'bg-white'
                    }`}
                  >
                    <span className="text-slate-900 font-medium">{item.n}</span>
                    <span className="text-[#F42222] font-medium">{item.excess}</span>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Bottom Split Footer */}
          <div className="bg-[#EDF3F0] border-t border-[#DCE5E1] grid grid-cols-2 divide-x divide-[#DCE5E1] shrink-0">
            {/* Left Footer */}
            <div className="p-2.5 space-y-2">
              <div className="text-center text-[15px] font-medium text-slate-900 truncate">
                စုစုပေါင်း = {totalRetainedDisplay}
              </div>
              <div className="grid grid-cols-2 gap-1.5">
                <button
                  type="button"
                  onClick={handleSaveBrakeAndSettlement}
                  className="py-2.5 bg-[#065240] hover:bg-[#054435] text-white rounded-xl text-[13px] font-bold shadow-sm transition cursor-pointer"
                >
                  သိမ်းမည်
                </button>
                <button
                  type="button"
                  onClick={() => {
                    soundManager.playClick();
                    navigateTo('SETTLEMENT');
                  }}
                  className="py-2.5 bg-[#0A8819] hover:bg-[#087215] text-white rounded-xl text-[13px] font-bold shadow-sm transition cursor-pointer"
                >
                  ရက်ချုပ်
                </button>
              </div>
            </div>

            {/* Right Footer */}
            <div className="p-2.5 space-y-2">
              <div className="text-center text-[15px] font-medium text-[#F42222] truncate">
                စုစုပေါင်း = {totalExcessDisplay}
              </div>
              <div className="grid grid-cols-2 gap-1.5">
                <button
                  type="button"
                  onClick={() => {
                    soundManager.playClick();
                    setShowLayoffModal(true);
                  }}
                  className="py-2.5 bg-[#F42222] hover:bg-red-700 text-white rounded-xl text-[13px] font-bold shadow-sm transition cursor-pointer"
                >
                  + တင်မည်
                </button>
                <button
                  type="button"
                  onClick={() => {
                    soundManager.playClick();
                    navigateTo('LAYOFF_HISTORY');
                  }}
                  className="py-2.5 bg-[#065240] hover:bg-[#054435] text-white rounded-xl text-[13px] font-bold shadow-sm transition cursor-pointer"
                >
                  မှတ်တမ်းများ
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Layoff Confirmation Modal */}
      {showLayoffModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col max-h-[85vh]">
            <div className="bg-[#F42222] text-white p-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <SendHorizontal className="w-5 h-5" />
                <h3 className="font-bold text-base">တင်ကွက် ထုတ်ယူရန် စစ်ဆေးခြင်း</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowLayoffModal(false)}
                className="text-white/80 hover:text-white text-lg font-bold p-1"
              >
                ✕
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3 flex-1 overflow-y-auto">
              <div className="bg-amber-50 border border-amber-200 text-amber-900 p-2.5 rounded-xl text-xs">
                ဘရိတ်သတ်မှတ်ချက် {currentLimit} ကျော်လွန်သော ဂဏန်းများကို တင်ထုတ်မည်။
              </div>

              {/* Mode Selection */}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setLayoffMode('all')}
                  className={`flex-1 py-2 rounded-xl text-xs font-semibold border transition ${
                    layoffMode === 'all'
                      ? 'bg-[#065240] text-white border-[#065240]'
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
                      ? 'bg-[#065240] text-white border-[#065240]'
                      : 'bg-gray-50 text-gray-700 border-gray-200 hover:bg-gray-100'
                  }`}
                >
                  အကွက် ၃၀ တင်မည်
                </button>
              </div>

              {/* Layoff Items Preview List */}
              <div className="border border-gray-200 rounded-xl overflow-hidden">
                <div className="bg-gray-100 px-3 py-2 text-[11px] font-semibold text-gray-600 flex justify-between">
                  <span>ဂဏန်း (အရေအတွက်: {modalExcessItems.length})</span>
                  <span>တင်မည့်ငွေ</span>
                </div>
                <div className="divide-y divide-gray-100 max-h-48 overflow-y-auto">
                  {modalExcessItems.length === 0 ? (
                    <div className="py-6 text-center text-gray-400 text-xs">
                      ဘရိတ်ကျော်သော ဂဏန်း မရှိပါ
                    </div>
                  ) : (
                    modalExcessItems.map((item) => (
                      <div
                        key={item.n}
                        className="flex justify-between px-3 py-2 text-xs font-mono"
                      >
                        <span className="font-bold text-gray-800">{item.n}</span>
                        <span className="text-[#F42222] font-bold">{item.excess}</span>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="bg-red-50 p-3 rounded-xl border border-red-200 flex justify-between items-center text-xs">
                <span className="font-semibold text-red-900">စုစုပေါင်း တင်မည့်ငွေ:</span>
                <span className="font-bold font-mono text-base text-[#F42222]">
                  {totalModalExcess}
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
                disabled={modalExcessItems.length === 0}
                onClick={handleConfirmLayoff}
                className="px-4 py-2 bg-[#F42222] hover:bg-red-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition shadow"
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
