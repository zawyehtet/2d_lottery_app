import React, { useState, useMemo } from 'react';
import { useLottery, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import { Search, Users, X, Info } from 'lucide-react';
import { NumberSummary, NumberAgentBreakdownItem } from '../types';
import { soundManager } from '../util/soundManager';

export const NumbersScreen: React.FC = () => {
  const {
    selectedDate,
    selectedSession,
    getAll100Numbers,
    sort2DNumbersWithWinning,
    currentWinningNumber,
    getNumberAgentBreakdown,
  } = useLottery();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedNumber, setSelectedNumber] = useState<string | null>(null);

  const dotDate = formatDotDate(selectedDate);

  // Compute all 100 numbers and sort by winning number then bet amount
  const sortedNumbers = useMemo(() => {
    const all = getAll100Numbers();
    return sort2DNumbersWithWinning(all, currentWinningNumber || null);
  }, [getAll100Numbers, sort2DNumbersWithWinning, currentWinningNumber]);

  const filteredNumbers = useMemo(() => {
    if (!searchQuery.trim()) return sortedNumbers;
    const q = searchQuery.trim();
    return sortedNumbers.filter((item) => item.n.includes(q));
  }, [sortedNumbers, searchQuery]);

  const totalBetsAmount = useMemo(() => {
    return sortedNumbers.reduce((sum, item) => sum + item.totalBet, 0);
  }, [sortedNumbers]);

  // Breakdown for modal
  const agentBreakdown = useMemo((): NumberAgentBreakdownItem[] => {
    if (!selectedNumber) return [];
    return getNumberAgentBreakdown(selectedNumber);
  }, [selectedNumber, getNumberAgentBreakdown]);

  const handleRowClick = (item: NumberSummary) => {
    soundManager.playClick();
    setSelectedNumber(item.n);
  };

  return (
    <div className="flex flex-col h-screen bg-[#F5F7F6] select-none">
      <Header title="ဂဏန်းများ" showBack />

      <main className="flex-1 min-h-0 max-w-2xl w-full mx-auto px-3 pt-3 pb-3 flex flex-col gap-3">
        <RoundRequiredHelperBanner />

        {/* Search Input */}
        <div className="relative shrink-0">
          <Search className="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-600" />
          <input
            type="text"
            inputMode="numeric"
            placeholder="ဂဏန်း ရှာရန်"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-10 py-3.5 text-[15px] bg-white border border-[#CBD6D1] rounded-2xl text-slate-900 placeholder:text-slate-500 focus:outline-none focus:border-[#065240]"
          />
          {searchQuery && (
            <button
              type="button"
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 p-1"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* 3-Column Numbers Table Card with Sticky Bottom Total Bar */}
        <div className="bg-white rounded-2xl shadow-sm border border-[#DCE5E1] overflow-hidden flex flex-col flex-1 min-h-0">
          {/* Dark-Green Table Header */}
          <div className="bg-[#065240] text-white text-[15px] font-medium py-3.5 px-4 flex items-center justify-between shrink-0">
            <div className="w-16">အမှတ်စဉ်</div>
            <div className="flex-1 text-center">ဂဏန်း</div>
            <div className="w-24 text-right">ပမာဏ</div>
          </div>

          {/* Scrollable 100 Rows */}
          <div className="flex-1 overflow-y-auto divide-y divide-[#EEF3F1]">
            {filteredNumbers.length === 0 ? (
              <div className="py-12 text-center text-slate-400 text-sm">
                ဂဏန်း မရှိပါ
              </div>
            ) : (
              filteredNumbers.map((item, index) => {
                const isWinning = item.n === currentWinningNumber;

                return (
                  <div
                    key={item.n}
                    onClick={() => handleRowClick(item)}
                    className={`px-4 py-3.5 flex items-center justify-between cursor-pointer transition-colors ${
                      isWinning
                        ? 'bg-amber-100/90 border-l-4 border-l-[#F42222]'
                        : index % 2 === 1
                        ? 'bg-[#EDF3F0]'
                        : 'bg-white'
                    }`}
                  >
                    <div className="w-16 text-[15px] text-slate-800">
                      {index + 1}.
                    </div>
                    <div className="flex-1 text-center flex items-center justify-center gap-2">
                      <span
                        className={`text-[18px] font-medium ${
                          isWinning ? 'text-[#F42222] font-bold' : 'text-[#065240]'
                        }`}
                      >
                        {item.n}
                      </span>
                      {isWinning && (
                        <span className="text-[10px] bg-[#F42222] text-white px-1.5 py-0.5 rounded font-bold">
                          ပေါက်သီး
                        </span>
                      )}
                    </div>
                    <div
                      className={`w-24 text-right text-[17px] font-medium ${
                        isWinning ? 'text-[#F42222] font-bold' : 'text-slate-900'
                      }`}
                    >
                      {item.totalBet}
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Dark-Green Bottom Total Bar */}
          <div className="bg-[#065240] text-white px-4 py-3.5 flex items-center justify-between shrink-0">
            <span className="text-[17px] font-medium">စုစုပေါင်း:</span>
            <span className="text-[19px] font-medium">{totalBetsAmount}</span>
          </div>
        </div>
      </main>

      {/* Agent Breakdown Modal for Selected Number */}
      {selectedNumber && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col max-h-[85vh]">
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Info className="w-5 h-5 text-emerald-200" />
                <div>
                  <h3 className="font-bold text-base">ဂဏန်း {selectedNumber} ၏ အသေးစိတ်</h3>
                  <p className="text-xs text-white/80">
                    {dotDate} ({selectedSession || 'အကြိမ်မရှိ'})
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setSelectedNumber(null)}
                className="text-white/80 hover:text-white p-1 rounded-full hover:bg-white/10"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex-1 overflow-y-auto">
              <h4 className="text-xs font-semibold text-gray-700 mb-2 flex items-center gap-1.5">
                <Users className="w-4 h-4 text-[#065240]" /> ကော်မရှင်များအလိုက် ထိုးကြေး
              </h4>

              <div className="divide-y divide-gray-100 border border-gray-200 rounded-xl overflow-hidden">
                <div className="grid grid-cols-12 bg-gray-100 text-[11px] font-semibold text-gray-600 py-2 px-3">
                  <div className="col-span-7">အမည်</div>
                  <div className="col-span-5 text-right">ပမာဏ</div>
                </div>
                {agentBreakdown.length === 0 ? (
                  <div className="p-4 text-center text-xs text-gray-400">
                    အချက်အလက် မရှိပါ
                  </div>
                ) : (
                  agentBreakdown.map((agentItem) => (
                    <div
                      key={agentItem.agentId}
                      className="grid grid-cols-12 py-2.5 px-3 text-xs items-center"
                    >
                      <div className="col-span-7 font-medium text-gray-800">
                        {agentItem.agentName}
                      </div>
                      <div className="col-span-5 text-right font-mono font-bold text-[#065240]">
                        {agentItem.betOnNumber}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
                type="button"
                onClick={() => setSelectedNumber(null)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-800 text-xs font-semibold rounded-xl transition"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
