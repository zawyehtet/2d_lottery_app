import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import {
  Trophy,
  Copy,
  Check,
  Users,
  TrendingUp,
  TrendingDown,
} from 'lucide-react';
import { Agent2DSettlement } from '../types';
import { soundManager } from '../util/soundManager';

export const AgentSettlementScreen: React.FC = () => {
  const {
    selectedDate,
    selectedSession,
    currentWinningNumber,
    getAgent2DSettlements,
  } = useLottery();

  const [copiedAgentId, setCopiedAgentId] = useState<number | null>(null);

  const dotDate = formatDotDate(selectedDate);

  const agentSettlements = useMemo(() => {
    return getAgent2DSettlements(currentWinningNumber || '');
  }, [getAgent2DSettlements, currentWinningNumber]);

  // Totals across all agents
  const totalSales = useMemo(() => {
    return agentSettlements.reduce((sum, a) => sum + a.totalSales, 0);
  }, [agentSettlements]);

  const totalCommission = useMemo(() => {
    return agentSettlements.reduce((sum, a) => sum + a.commissionAmount, 0);
  }, [agentSettlements]);

  const totalPayout = useMemo(() => {
    return agentSettlements.reduce((sum, a) => sum + a.payoutAmount, 0);
  }, [agentSettlements]);

  const totalMasterNet = useMemo(() => {
    return agentSettlements.reduce((sum, a) => sum + a.netBalance, 0);
  }, [agentSettlements]);

  const handleCopyAgentReport = (a: Agent2DSettlement) => {
    soundManager.playClick();
    const isGain = a.netBalance >= 0;
    const lines = [
      `Lucky2D ကိုယ်စားလှယ် စာရင်းရှင်းတမ်း`,
      `ကိုယ်စားလှယ်: ${a.agentName}`,
      `ရက်စွဲ: ${dotDate} (${selectedSession || ''})`,
      `ပေါက်ဂဏန်း: ${currentWinningNumber || 'မထွက်သေး'}`,
      `-----------------------`,
      `ရောင်းရငွေ: ${fmt(a.totalSales)} Ks`,
      `ကော်မရှင် (${a.commissionRate}%): ${fmt(a.commissionAmount)} Ks`,
      `ကျန်ငွေ: ${fmt(a.netSales)} Ks`,
      `ပေါက်ငွေ: ${a.winningBetAmount > 0 ? `${fmt(a.winningBetAmount)} Ks (x${a.multiplier})` : '0 Ks'}`,
      `ပေါက်ကြေး: ${fmt(a.payoutAmount)} Ks`,
      `-----------------------`,
      isGain
        ? `ဒိုင်မှ ရရန်ကျန်ငွေ: ${fmt(a.netBalance)} Ks`
        : `ကိုယ်စားလှယ်သို့ ပေးရန်ငွေ: ${fmt(Math.abs(a.netBalance))} Ks`,
    ];

    navigator.clipboard.writeText(lines.join('\n'));
    setCopiedAgentId(a.agentId);
    setTimeout(() => setCopiedAgentId(null), 2500);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ကိုယ်စားလှယ် စာရင်းရှင်းတမ်း" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        <RoundRequiredHelperBanner />

        {/* Top Winning Banner */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-amber-100 rounded-xl text-amber-700">
              <Trophy className="w-6 h-6" />
            </div>
            <div>
              <span className="text-xs text-gray-500 font-semibold block">ထွက်ဂဏန်း (ပေါက်သီး)</span>
              <span className="text-2xl font-bold font-mono tracking-wider text-red-600">
                {currentWinningNumber || 'မသတ်မှတ်ရသေး'}
              </span>
            </div>
          </div>

          <div className="text-right text-xs">
            <span className="text-gray-500 block">ရက်စွဲ</span>
            <span className="font-bold text-gray-800 text-sm">{dotDate}</span>
          </div>
        </div>

        {/* Aggregate Banner */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div className="bg-white p-3 rounded-2xl border border-emerald-900/10 shadow-sm text-xs">
            <span className="text-gray-500 block text-[11px]">စုစုပေါင်း ရောင်းရငွေ</span>
            <span className="font-bold font-mono text-[#235347] text-sm">
              {fmt(totalSales)} Ks
            </span>
          </div>
          <div className="bg-white p-3 rounded-2xl border border-emerald-900/10 shadow-sm text-xs">
            <span className="text-gray-500 block text-[11px]">စုစုပေါင်း ကော်မရှင်</span>
            <span className="font-bold font-mono text-amber-700 text-sm">
              {fmt(totalCommission)} Ks
            </span>
          </div>
          <div className="bg-white p-3 rounded-2xl border border-emerald-900/10 shadow-sm text-xs">
            <span className="text-gray-500 block text-[11px]">စုစုပေါင်း ပေါက်ကြေး</span>
            <span className="font-bold font-mono text-red-600 text-sm">
              {fmt(totalPayout)} Ks
            </span>
          </div>
          <div
            className={`p-3 rounded-2xl border shadow-sm text-xs ${
              totalMasterNet >= 0
                ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                : 'bg-red-50 border-red-300 text-red-900'
            }`}
          >
            <span className="block text-[11px] font-semibold">
              {totalMasterNet >= 0 ? 'ဒိုင် အသားတင်အမြတ်' : 'ဒိုင် အသားတင်အရှုံး'}
            </span>
            <span className="font-bold font-mono text-sm">
              {totalMasterNet >= 0 ? '+' : ''}{fmt(totalMasterNet)} Ks
            </span>
          </div>
        </div>

        {/* Agents Breakdown Cards */}
        {agentSettlements.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center border border-gray-200 shadow-sm flex flex-col items-center justify-center gap-2">
            <Users className="w-8 h-8 text-gray-400" />
            <p className="text-gray-500 text-sm">ထိုးထားသော ကိုယ်စားလှယ် စာရင်းမရှိသေးပါ</p>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {agentSettlements.map((item) => {
              const isCopied = copiedAgentId === item.agentId;
              const isGain = item.netBalance >= 0;

              return (
                <div
                  key={item.agentId}
                  className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col gap-3"
                >
                  <div className="flex items-center justify-between border-b border-gray-100 pb-2.5">
                    <div className="flex items-center gap-2.5">
                      <div className="w-9 h-9 rounded-full bg-emerald-100 text-[#235347] font-bold flex items-center justify-center text-xs">
                        {item.agentName.slice(0, 1).toUpperCase()}
                      </div>
                      <div>
                        <h4 className="font-bold text-gray-900 text-sm">{item.agentName}</h4>
                        <span className="text-[11px] text-gray-500">
                          ကော်မရှင်နှုန်း: {item.commissionRate}% • အမြှောက်နှုန်း: x{item.multiplier} ဆ
                        </span>
                      </div>
                    </div>

                    <button
                      onClick={() => handleCopyAgentReport(item)}
                      className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition ${
                        isCopied
                          ? 'bg-emerald-100 text-[#235347]'
                          : 'bg-gray-100 text-gray-700 hover:bg-emerald-50 hover:text-[#235347]'
                      }`}
                    >
                      {isCopied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{isCopied ? 'ကူးယူပြီး' : 'စာရင်းကူးမည်'}</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                    <div className="bg-gray-50 p-2 rounded-xl">
                      <span className="text-gray-500 text-[11px] block">ရောင်းရငွေ</span>
                      <span className="font-bold font-mono text-gray-900">
                        {fmt(item.totalSales)} Ks
                      </span>
                    </div>

                    <div className="bg-gray-50 p-2 rounded-xl">
                      <span className="text-gray-500 text-[11px] block">ကော်မရှင် ({item.commissionRate}%)</span>
                      <span className="font-bold font-mono text-amber-700">
                        {fmt(item.commissionAmount)} Ks
                      </span>
                    </div>

                    <div className="bg-gray-50 p-2 rounded-xl">
                      <span className="text-gray-500 text-[11px] block">ကျန်ငွေ (Net)</span>
                      <span className="font-bold font-mono text-blue-700">
                        {fmt(item.netSales)} Ks
                      </span>
                    </div>

                    <div className="bg-gray-50 p-2 rounded-xl">
                      <span className="text-gray-500 text-[11px] block">ပေါက်ကြေးလျော်ငွေ</span>
                      <span className="font-bold font-mono text-red-600">
                        {fmt(item.payoutAmount)} Ks
                      </span>
                    </div>
                  </div>

                  {/* Net Due Box */}
                  <div
                    className={`p-3 rounded-xl border flex items-center justify-between text-xs font-medium ${
                      isGain
                        ? 'bg-emerald-50 border-emerald-200 text-emerald-900'
                        : 'bg-red-50 border-red-200 text-red-900'
                    }`}
                  >
                    <div className="flex items-center gap-1.5">
                      {isGain ? (
                        <TrendingUp className="w-4 h-4 text-emerald-600" />
                      ) : (
                        <TrendingDown className="w-4 h-4 text-red-600" />
                      )}
                      <span>
                        {isGain
                          ? 'ကိုယ်စားလှယ်မှ ဒိုင်သို့ ပေးသွင်းရန်ငွေ :'
                          : 'ဒိုင်မှ ကိုယ်စားလှယ်သို့ ပြန်လည်ပေးလျော်ရန်ငွေ :'}
                      </span>
                    </div>

                    <span className="text-sm font-bold font-mono">
                      {fmt(Math.abs(item.netBalance))} Ks
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>
    </div>
  );
};
