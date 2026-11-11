import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import {
  FileSpreadsheet,
  Download,
  Trash2,
  Calendar,
  Trophy,
  ChevronRight,
  X,
  AlertTriangle,
  Copy,
  Check,
} from 'lucide-react';
import { SettledRecord, DailySettlement } from '../types';
import { soundManager } from '../util/soundManager';

export const SettlementScreen: React.FC = () => {
  const {
    settled,
    dailySettlements,
    deleteSettledRecord,
    clearSettledLedger,
    buildDailySettlementsTextSummary,
    showToast,
  } = useLottery();

  const [activeTab, setActiveTab] = useState<'rounds' | 'daily'>('rounds');
  const [selectedRecord, setSelectedRecord] = useState<SettledRecord | null>(null);
  const [showClearAllModal, setShowClearAllModal] = useState(false);
  const [copiedSummary, setCopiedSummary] = useState(false);

  // Totals for rounds
  const totalRoundsProfitLoss = useMemo(() => {
    return settled.reduce((sum, s) => sum + s.profitLoss, 0);
  }, [settled]);

  const totalRoundsRetained = useMemo(() => {
    return settled.reduce((sum, s) => sum + s.totalRetained, 0);
  }, [settled]);

  const totalRoundsPayout = useMemo(() => {
    return settled.reduce((sum, s) => sum + s.payout, 0);
  }, [settled]);

  // Export CSV
  const handleExportCSV = () => {
    soundManager.playClick();
    if (settled.length === 0) return;

    const headers = [
      'စဉ်',
      'ရက်စွဲ',
      'အပိုင်း',
      'လက်ခံငွေ',
      'ကော်မရှင်',
      'ကျန်ငွေ',
      'ပေါက်ဂဏန်း',
      'ပေါက်ကြေး',
      'အမြတ်/အရှုံး',
      'အချိန်',
    ];

    const rows = settled.map((s, idx) => {
      return [
        idx + 1,
        `"${formatDotDate(s.date)}"`,
        `"${s.session}"`,
        s.totalRetained,
        s.commission,
        s.retainedNet,
        `"${s.winningNumber}"`,
        s.payout,
        s.profitLoss,
        `"${s.timestamp}"`,
      ].join(',');
    });

    const csvContent = '\uFEFF' + [headers.join(','), ...rows].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `lucky2d_settlements_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleCopyTextSummary = () => {
    soundManager.playClick();
    const text = buildDailySettlementsTextSummary();
    navigator.clipboard.writeText(text);
    setCopiedSummary(true);
    showToast('စာရင်းချုပ် အကျဉ်း ကူးယူပြီးပါပြီ');
    setTimeout(() => setCopiedSummary(false), 2500);
  };

  const handleDeleteOne = (id: number) => {
    deleteSettledRecord(id);
    if (selectedRecord?.id === id) {
      setSelectedRecord(null);
    }
  };

  const handleConfirmClearAll = () => {
    clearSettledLedger();
    setShowClearAllModal(false);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ရက်ချုပ် မှတ်တမ်းများ" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        {/* Action Header Card */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-emerald-50 rounded-xl text-[#235347]">
              <FileSpreadsheet className="w-6 h-6" />
            </div>
            <div>
              <h2 className="font-bold text-gray-900 text-sm">ရက်ချုပ် စာရင်းပေါင်းချုပ်</h2>
              <p className="text-xs text-gray-500">
                စုစုပေါင်း {settled.length} ကြိမ် မှတ်တမ်းတင်ထားသည်
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleCopyTextSummary}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 text-[#235347] border border-emerald-200 text-xs font-semibold rounded-xl transition"
            >
              {copiedSummary ? <Check className="w-4 h-4" /> : <Copy className="w-4 h-4" />}
              <span>{copiedSummary ? 'ကူးပြီး' : 'စာသားကူးမည်'}</span>
            </button>

            <button
              onClick={handleExportCSV}
              disabled={settled.length === 0}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-2 bg-emerald-50 hover:bg-emerald-100 disabled:opacity-40 text-[#235347] border border-emerald-200 text-xs font-semibold rounded-xl transition"
            >
              <Download className="w-4 h-4" />
              <span>CSV ထုတ်မည်</span>
            </button>

            <button
              onClick={() => {
                soundManager.playClick();
                setShowClearAllModal(true);
              }}
              disabled={settled.length === 0}
              className="flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-3 py-2 bg-red-50 hover:bg-red-100 disabled:opacity-40 text-red-700 border border-red-200 text-xs font-semibold rounded-xl transition"
            >
              <Trash2 className="w-4 h-4" />
              <span>အားလုံးဖျက်မည်</span>
            </button>
          </div>
        </div>

        {/* Tab Selection */}
        <div className="flex gap-2 bg-gray-200/70 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('rounds')}
            className={`flex-1 py-1.5 rounded-lg text-xs font-bold transition ${
              activeTab === 'rounds' ? 'bg-white text-[#235347] shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            အပိုင်းအလိုက် ရက်ချုပ် ({settled.length})
          </button>
          <button
            onClick={() => setActiveTab('daily')}
            className={`flex-1 py-1.5 rounded-lg text-xs font-bold transition ${
              activeTab === 'daily' ? 'bg-white text-[#235347] shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            ရက်အလိုက် စာရင်းချုပ် ({dailySettlements.length})
          </button>
        </div>

        {activeTab === 'rounds' ? (
          /* 4-Column Table for Rounds */
          <div className="bg-white rounded-2xl shadow-sm border border-emerald-900/10 overflow-hidden flex flex-col flex-1">
            <div className="grid grid-cols-12 bg-[#235347] text-white text-xs font-semibold py-3 px-4">
              <div className="col-span-2 text-center">စဉ်</div>
              <div className="col-span-4">ရက်စွဲ</div>
              <div className="col-span-3 text-center">အပိုင်း</div>
              <div className="col-span-3 text-right pr-2">အမြတ်/အရှုံး</div>
            </div>

            <div className="divide-y divide-gray-100 max-h-[60vh] overflow-y-auto">
              {settled.length === 0 ? (
                <div className="py-12 text-center text-gray-400 text-xs">
                  ရက်ချုပ်မှတ်တမ်း မရှိသေးပါ
                </div>
              ) : (
                settled.map((item, index) => {
                  const isProfit = item.profitLoss >= 0;

                  return (
                    <div
                      key={item.id}
                      onClick={() => {
                        soundManager.playClick();
                        setSelectedRecord(item);
                      }}
                      className={`grid grid-cols-12 items-center py-3 px-4 cursor-pointer text-xs font-mono transition-colors ${
                        index % 2 === 1 ? 'bg-[#EAF0EE]/30 hover:bg-[#EAF0EE]/70' : 'bg-white hover:bg-gray-50'
                      }`}
                    >
                      <div className="col-span-2 text-center text-gray-400 font-sans">{index + 1}</div>
                      <div className="col-span-4 font-semibold text-gray-800 flex items-center gap-1.5 font-sans">
                        <Calendar className="w-3.5 h-3.5 text-gray-400" />
                        <span>{formatDotDate(item.date)}</span>
                      </div>
                      <div className="col-span-3 text-center font-sans text-gray-600">
                        {item.session}
                      </div>
                      <div className="col-span-3 text-right pr-2 font-bold flex items-center justify-end gap-1">
                        <span className={isProfit ? 'text-emerald-700' : 'text-red-600'}>
                          {isProfit ? '+' : ''}{fmt(item.profitLoss)} Ks
                        </span>
                        <ChevronRight className="w-3.5 h-3.5 text-gray-400" />
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Bottom Summary Bar */}
            <div className="bg-gray-50 border-t border-gray-200 p-4 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-4 text-gray-600">
                <div>
                  လက်ခံငွေပေါင်း: <strong className="font-mono text-gray-900">{fmt(totalRoundsRetained)}</strong> Ks
                </div>
                <div className="h-3 w-px bg-gray-300" />
                <div>
                  လျော်ကြေးပေါင်း: <strong className="font-mono text-red-600">{fmt(totalRoundsPayout)}</strong> Ks
                </div>
              </div>

              <div className="flex items-center gap-2">
                <span className="font-semibold text-gray-700">အသားတင် အမြတ်ငွေ :</span>
                <span
                  className={`text-base font-bold font-mono px-3 py-1 rounded-xl ${
                    totalRoundsProfitLoss >= 0
                      ? 'bg-emerald-100 text-emerald-900 border border-emerald-300'
                      : 'bg-red-100 text-red-900 border border-red-300'
                  }`}
                >
                  {totalRoundsProfitLoss >= 0 ? '+' : ''}{fmt(totalRoundsProfitLoss)} Ks
                </span>
              </div>
            </div>
          </div>
        ) : (
          /* Daily Settlements View */
          <div className="flex flex-col gap-3">
            {dailySettlements.length === 0 ? (
              <div className="bg-white rounded-2xl p-12 text-center border border-gray-200 shadow-sm text-gray-400 text-xs">
                ရက်အလိုက် စာရင်းချုပ် မရှိသေးပါ
              </div>
            ) : (
              dailySettlements.map((ds) => {
                const isProfit = ds.profitOrLoss >= 0;
                return (
                  <div key={ds.date} className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col gap-2.5">
                    <div className="flex items-center justify-between border-b border-gray-100 pb-2">
                      <div className="flex items-center gap-2">
                        <Calendar className="w-4 h-4 text-[#235347]" />
                        <span className="font-bold text-gray-900 text-sm">{formatDotDate(ds.date)}</span>
                        <span className="text-[11px] bg-emerald-50 text-[#235347] font-semibold px-2 py-0.5 rounded-full">
                          {ds.roundCount} ကြိမ်
                        </span>
                      </div>
                      <span className="text-xs text-gray-500 font-mono">
                        ပေါက်ဂဏန်းများ: <strong className="text-red-600">{ds.winningNumbersSummary}</strong>
                      </span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
                      <div className="bg-gray-50 p-2 rounded-xl">
                        <span className="text-gray-500 text-[11px] block">စုစုပေါင်းရောင်းရငွေ</span>
                        <span className="font-bold font-mono text-gray-900">{fmt(ds.totalSales)} Ks</span>
                      </div>
                      <div className="bg-gray-50 p-2 rounded-xl">
                        <span className="text-gray-500 text-[11px] block">ကော်မရှင်စုစုပေါင်း</span>
                        <span className="font-bold font-mono text-amber-700">{fmt(ds.totalCommission)} Ks</span>
                      </div>
                      <div className="bg-gray-50 p-2 rounded-xl">
                        <span className="text-gray-500 text-[11px] block">ပေါက်ကြေးလျော်ငွေ</span>
                        <span className="font-bold font-mono text-red-600">{fmt(ds.winningPayout)} Ks</span>
                      </div>
                      <div className={`p-2 rounded-xl border ${isProfit ? 'bg-emerald-50 border-emerald-200 text-emerald-900' : 'bg-red-50 border-red-200 text-red-900'}`}>
                        <span className="text-[11px] block font-semibold">{isProfit ? 'အမြတ်ငွေ' : 'အရှုံးငွေ'}</span>
                        <span className="font-bold font-mono">{isProfit ? '+' : ''}{fmt(ds.profitOrLoss)} Ks</span>
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        )}
      </main>

      {/* Settlement Detail Modal */}
      {selectedRecord && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col max-h-[85vh]">
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base">ရက်ချုပ် စာရင်းအသေးစိတ်</h3>
                <p className="text-xs text-emerald-200">
                  {formatDotDate(selectedRecord.date)} ({selectedRecord.session})
                </p>
              </div>
              <button
                onClick={() => setSelectedRecord(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3 text-xs overflow-y-auto">
              <div className="grid grid-cols-2 gap-2">
                <div className="bg-emerald-50/50 p-2.5 rounded-xl border border-emerald-100">
                  <span className="text-emerald-700 block text-[11px]">လက်ခံငွေ (Retained)</span>
                  <span className="font-bold font-mono text-[#235347] text-sm">
                    {fmt(selectedRecord.totalRetained)} Ks
                  </span>
                </div>
                <div className="bg-amber-50/50 p-2.5 rounded-xl border border-amber-100">
                  <span className="text-amber-800 block text-[11px]">ကော်မရှင် (Commission)</span>
                  <span className="font-bold font-mono text-amber-800 text-sm">
                    {fmt(selectedRecord.commission)} Ks
                  </span>
                </div>
                <div className="bg-blue-50/50 p-2.5 rounded-xl border border-blue-100">
                  <span className="text-blue-800 block text-[11px]">ကျန်ငွေ (Retained Net)</span>
                  <span className="font-bold font-mono text-blue-800 text-sm">
                    {fmt(selectedRecord.retainedNet)} Ks
                  </span>
                </div>
                <div className="bg-red-50/50 p-2.5 rounded-xl border border-red-100">
                  <span className="text-red-800 block text-[11px]">ပေါက်ကြေးလျော်ငွေ (Payout)</span>
                  <span className="font-bold font-mono text-red-700 text-sm">
                    {fmt(selectedRecord.payout)} Ks
                  </span>
                </div>
              </div>

              <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Trophy className="w-5 h-5 text-amber-600" />
                  <div>
                    <span className="text-gray-600 block text-[11px]">ပေါက်ဂဏန်း</span>
                    <span className="text-lg font-bold font-mono text-red-600">
                      {selectedRecord.winningNumber || '-'}
                    </span>
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-gray-600 block text-[11px]">အချိန်</span>
                  <span className="text-xs font-mono text-gray-500">
                    {selectedRecord.timestamp}
                  </span>
                </div>
              </div>

              <div
                className={`p-3.5 rounded-xl border flex items-center justify-between ${
                  selectedRecord.profitLoss >= 0
                    ? 'bg-emerald-100/80 border-emerald-300 text-emerald-900'
                    : 'bg-red-100/80 border-red-300 text-red-900'
                }`}
              >
                <span className="font-semibold text-sm">
                  {selectedRecord.profitLoss >= 0 ? 'အမြတ်ငွေ (Profit)' : 'အရှုံးငွေ (Loss)'}
                </span>
                <span className="text-lg font-bold font-mono">
                  {selectedRecord.profitLoss >= 0 ? '+' : ''}
                  {fmt(selectedRecord.profitLoss)} Ks
                </span>
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between">
              <button
                onClick={() => handleDeleteOne(selectedRecord.id)}
                className="px-3 py-2 bg-red-100 hover:bg-red-200 text-red-700 text-xs font-bold rounded-xl transition flex items-center gap-1"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>ဤမှတ်တမ်းဖျက်မည်</span>
              </button>

              <button
                onClick={() => setSelectedRecord(null)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Clear All Confirmation Modal */}
      {showClearAllModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-gray-200 text-center flex flex-col gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">မှတ်တမ်းအားလုံး ဖျက်မည်လား?</h4>
            <p className="text-xs text-gray-500">
              သိမ်းဆည်းထားသော ရက်ချုပ်မှတ်တမ်းများအားလုံး ပျက်ပြယ်သွားမည်ဖြစ်ပြီး ပြန်ယူ၍မရနိုင်ပါ။
            </p>
            <div className="flex gap-2 mt-2">
              <button
                onClick={() => setShowClearAllModal(false)}
                className="flex-1 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မဖျက်ပါ
              </button>
              <button
                onClick={handleConfirmClearAll}
                className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition"
              >
                အားလုံးဖျက်မည်
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
