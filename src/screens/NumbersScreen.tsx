import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import { Search, Trophy, Users, X, Info } from 'lucide-react';
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

  const winningSummary = useMemo(() => {
    if (!currentWinningNumber) return null;
    return sortedNumbers.find((item) => item.n === currentWinningNumber) || null;
  }, [sortedNumbers, currentWinningNumber]);

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
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ဂဏန်းများ စာရင်း" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        <RoundRequiredHelperBanner />

        {/* Top Info Banner / Winning Number Banner */}
        {currentWinningNumber ? (
          <div className="bg-amber-100 border border-amber-300 rounded-xl p-3 flex items-center justify-between text-amber-900 shadow-sm">
            <div className="flex items-center gap-2">
              <Trophy className="w-5 h-5 text-amber-600" />
              <div>
                <span className="font-semibold text-sm">ပေါက်ဂဏန်း : </span>
                <span className="text-xl font-bold font-mono tracking-wider ml-1 text-red-600">
                  {currentWinningNumber}
                </span>
              </div>
            </div>
            {winningSummary && (
              <div className="text-right">
                <span className="text-xs text-amber-800">ထိုးငွေ : </span>
                <span className="text-sm font-bold text-red-700 ml-1">
                  {fmt(winningSummary.totalBet)} Ks
                </span>
              </div>
            )}
          </div>
        ) : null}

        {/* Search Bar & Summary Card */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              inputMode="numeric"
              placeholder="ဂဏန်းရှာဖွေရန် (ဥပမာ- 05)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 text-sm bg-gray-50 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] focus:bg-white"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 p-1"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <div className="flex items-center justify-between md:justify-end gap-4 text-xs md:text-sm font-medium text-gray-600">
            <div>
              <span>ထိုးထားသောဂဏန်း: </span>
              <span className="font-bold text-[#235347] font-mono text-base">
                {sortedNumbers.filter((x) => x.totalBet > 0).length}
              </span>
            </div>
            <div className="h-4 w-px bg-gray-300" />
            <div>
              <span>စုစုပေါင်းငွေ: </span>
              <span className="font-bold text-[#235347] font-mono text-base">
                {fmt(totalBetsAmount)} Ks
              </span>
            </div>
          </div>
        </div>

        {/* 3-Column Table */}
        <div className="bg-white rounded-2xl shadow-sm border border-emerald-900/10 overflow-hidden flex flex-col flex-1">
          <div className="grid grid-cols-12 bg-[#235347] text-white text-xs font-semibold py-3 px-4">
            <div className="col-span-2 text-center">စဉ်</div>
            <div className="col-span-5 text-center">ဂဏန်း</div>
            <div className="col-span-5 text-right pr-2">ထိုးငွေပမာဏ</div>
          </div>

          <div className="divide-y divide-gray-100 max-h-[60vh] overflow-y-auto">
            {filteredNumbers.length === 0 ? (
              <div className="py-12 text-center text-gray-400 text-sm">
                ဂဏန်း မရှိပါ
              </div>
            ) : (
              filteredNumbers.map((item, index) => {
                const isWinning = item.n === currentWinningNumber;

                return (
                  <div
                    key={item.n}
                    onClick={() => handleRowClick(item)}
                    className={`grid grid-cols-12 items-center py-2.5 px-4 cursor-pointer transition-colors text-sm font-mono ${
                      isWinning
                        ? 'bg-amber-100/90 hover:bg-amber-100 border-l-4 border-l-red-500'
                        : index % 2 === 1
                        ? 'bg-[#EAF0EE]/30 hover:bg-[#EAF0EE]/70'
                        : 'bg-white hover:bg-gray-50'
                    }`}
                  >
                    <div className="col-span-2 text-center text-xs text-gray-500 font-sans">
                      {index + 1}
                    </div>
                    <div className="col-span-5 text-center flex items-center justify-center gap-2">
                      <span className={`text-base font-bold ${isWinning ? 'text-red-600' : 'text-gray-900'}`}>
                        {item.n}
                      </span>
                      {isWinning && (
                        <span className="text-[10px] bg-red-600 text-white font-sans px-1.5 py-0.5 rounded font-bold">
                          ပေါက်သီး
                        </span>
                      )}
                    </div>
                    <div className="col-span-5 text-right pr-2 flex items-center justify-end gap-1.5">
                      <span className={`font-bold ${isWinning ? 'text-red-700' : 'text-[#235347]'}`}>
                        {fmt(item.totalBet)}
                      </span>
                      <span className="text-[11px] font-sans text-gray-500">Ks</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </main>

      {/* Agent Breakdown Modal for Selected Number */}
      {selectedNumber && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col max-h-[85vh]">
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Info className="w-5 h-5 text-emerald-300" />
                <div>
                  <h3 className="font-bold text-base">ဂဏန်း {selectedNumber} ၏ အသေးစိတ်</h3>
                  <p className="text-xs text-emerald-200">
                    {dotDate} ({selectedSession || 'အပိုင်းမရွေးရသေး'})
                  </p>
                </div>
              </div>
              <button
                onClick={() => setSelectedNumber(null)}
                className="text-white/80 hover:text-white p-1 rounded-full hover:bg-white/10"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex-1 overflow-y-auto">
              <h4 className="text-xs font-semibold text-gray-700 mb-2 flex items-center gap-1.5">
                <Users className="w-4 h-4 text-[#235347]" /> ကိုယ်စားလှယ်များအလိုက် ထိုးကြေး
              </h4>

              <div className="divide-y divide-gray-100 border border-gray-200 rounded-xl overflow-hidden">
                <div className="grid grid-cols-12 bg-gray-100 text-[11px] font-semibold text-gray-600 py-2 px-3">
                  <div className="col-span-7">ကိုယ်စားလှယ်</div>
                  <div className="col-span-5 text-right">ငွေပမာဏ</div>
                </div>
                {agentBreakdown.length === 0 ? (
                  <div className="p-4 text-center text-xs text-gray-400">အချက်အလက် မရှိပါ</div>
                ) : (
                  agentBreakdown.map((agentItem) => (
                    <div key={agentItem.agentId} className="grid grid-cols-12 py-2.5 px-3 text-xs items-center">
                      <div className="col-span-7 font-medium text-gray-800">
                        {agentItem.agentName}
                      </div>
                      <div className="col-span-5 text-right font-mono font-bold text-[#235347]">
                        {fmt(agentItem.betOnNumber)} Ks
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
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
