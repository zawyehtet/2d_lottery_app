import React, { useState, useMemo } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import {
  History,
  Copy,
  Check,
  Trash2,
  X,
  AlertTriangle,
} from 'lucide-react';
import { LayoffRecord } from '../types';
import { soundManager } from '../util/soundManager';

export const LayoffHistoryScreen: React.FC = () => {
  const {
    layoffs,
    deleteLayoffRecord,
  } = useLottery();

  const [selectedRecord, setSelectedRecord] = useState<LayoffRecord | null>(null);
  const [copiedId, setCopiedId] = useState<number | null>(null);
  const [recordToDelete, setRecordToDelete] = useState<LayoffRecord | null>(null);

  const sortedRecords = useMemo(() => {
    return [...layoffs].sort((a, b) => b.id - a.id);
  }, [layoffs]);

  const handleCopyLayoff = (record: LayoffRecord) => {
    soundManager.playClick();
    const lines = [
      `Lucky2D တင်ကွက်စာရင်း`,
      `အမှတ်: No.${record.recordNo}`,
      `ရက်စွဲ: ${formatDotDate(record.date)} (${record.session})`,
      `အချိန်: ${record.time}`,
      `-----------------------`,
    ];

    record.items.forEach((item) => {
      lines.push(`${item.n}=${item.a}`);
    });

    lines.push(`-----------------------`);
    lines.push(`စုစုပေါင်း: ${fmt(record.total)}`);

    navigator.clipboard.writeText(lines.join('\n'));
    setCopiedId(record.id);
    setTimeout(() => setCopiedId(null), 2500);
  };

  const handleConfirmDelete = () => {
    if (!recordToDelete) return;
    deleteLayoffRecord(recordToDelete.id);
    setRecordToDelete(null);
    if (selectedRecord?.id === recordToDelete.id) {
      setSelectedRecord(null);
    }
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="တင်ကွက် မှတ်တမ်းများ" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        {/* Header Summary */}
        <div className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-red-100 rounded-xl text-red-600">
              <History className="w-6 h-6" />
            </div>
            <div>
              <h2 className="font-bold text-gray-900 text-sm">တင်ကွက်မှတ်တမ်း စာရင်း</h2>
              <p className="text-xs text-gray-500">
                စုစုပေါင်း {sortedRecords.length} ကြိမ် တင်ထားသည်
              </p>
            </div>
          </div>
        </div>

        {/* Master Table */}
        <div className="bg-white rounded-2xl shadow-sm border border-emerald-900/10 overflow-hidden flex flex-col flex-1">
          <div className="grid grid-cols-12 bg-[#235347] text-white text-[11px] sm:text-xs font-semibold py-3 px-3 sm:px-4">
            <div className="col-span-2 text-center">အမှတ်</div>
            <div className="col-span-4">ရက်စွဲ / အချိန်</div>
            <div className="col-span-3 text-right pr-1">တင်ငွေ</div>
            <div className="col-span-3 text-center">လုပ်ဆောင်ချက်</div>
          </div>

          <div className="divide-y divide-gray-100 max-h-[65vh] overflow-y-auto">
            {sortedRecords.length === 0 ? (
              <div className="py-12 text-center text-gray-400 text-xs">
                တင်ကွက်မှတ်တမ်း မရှိသေးပါ
              </div>
            ) : (
              sortedRecords.map((item, index) => {
                const isCopied = copiedId === item.id;

                return (
                  <div
                    key={item.id}
                    className={`grid grid-cols-12 items-center py-2.5 px-3 sm:px-4 text-xs font-mono transition-colors ${
                      index % 2 === 1 ? 'bg-[#EAF0EE]/30' : 'bg-white'
                    }`}
                  >
                    <div className="col-span-2 text-center font-bold text-gray-700 font-sans tabular-nums">
                      No.{item.recordNo}
                    </div>
                    <div
                      className="col-span-4 cursor-pointer hover:text-[#235347] min-w-0 pr-1"
                      onClick={() => {
                        soundManager.playClick();
                        setSelectedRecord(item);
                      }}
                    >
                      <div className="font-sans font-semibold text-gray-800 truncate">
                        {formatDotDate(item.date)} ({item.session})
                      </div>
                      <div className="text-[11px] text-gray-400 truncate">
                        {item.time}
                      </div>
                    </div>
                    <div className="col-span-3 text-right font-bold tabular-nums text-red-600 pr-1">
                      {fmt(item.total)}
                    </div>
                    <div className="col-span-3 flex items-center justify-center gap-1 font-sans">
                      <button
                        type="button"
                        onClick={() => handleCopyLayoff(item)}
                        className={`px-2 py-1 rounded-lg text-[11px] font-semibold flex items-center gap-1 transition whitespace-nowrap ${
                          isCopied
                            ? 'bg-emerald-100 text-[#235347]'
                            : 'bg-gray-100 text-gray-700 hover:bg-emerald-50'
                        }`}
                      >
                        {isCopied ? <Check className="w-3 h-3 shrink-0" /> : <Copy className="w-3 h-3 shrink-0" />}
                        <span>{isCopied ? 'ကူးပြီး' : 'ကူးမည်'}</span>
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          soundManager.playClick();
                          setRecordToDelete(item);
                        }}
                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition shrink-0"
                        title="ဖျက်မည်"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </main>

      {/* Layoff Detail Modal */}
      {selectedRecord && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col max-h-[85vh]">
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <div>
                <h3 className="font-bold text-base">တင်ကွက် စာရင်းအသေးစိတ် (No.{selectedRecord.recordNo})</h3>
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

            <div className="p-4 flex-1 overflow-y-auto">
              <div className="border border-gray-200 rounded-xl overflow-hidden">
                <div className="bg-[#EAF0EE] grid grid-cols-2 px-3 py-2 text-[11px] font-semibold text-[#235347]">
                  <span>ဂဏန်း</span>
                  <span className="text-right">တင်ကွက်ငွေ</span>
                </div>
                <div className="divide-y divide-gray-100 max-h-60 overflow-y-auto font-mono text-xs tabular-nums">
                  {selectedRecord.items.map((it, idx) => (
                    <div key={idx} className="grid grid-cols-2 px-3 py-2 items-center">
                      <span className="font-bold text-gray-800">{it.n}</span>
                      <span className="text-right font-bold text-red-600">
                        {fmt(it.a)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="mt-3 p-3 bg-red-50 rounded-xl border border-red-200 flex items-center justify-between text-xs">
                <span className="font-semibold text-red-900">စုစုပေါင်း တင်ငွေ:</span>
                <span className="font-bold font-mono tabular-nums text-base text-red-700">
                  {fmt(selectedRecord.total)}
                </span>
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex items-center justify-between gap-2">
              <button
                type="button"
                onClick={() => handleCopyLayoff(selectedRecord)}
                className="px-3.5 py-2 bg-[#235347] hover:bg-[#1b4339] text-white text-xs font-bold rounded-xl transition flex items-center gap-1.5 shadow whitespace-nowrap"
              >
                <Copy className="w-3.5 h-3.5 shrink-0" />
                <span>စာသားကူးမည်</span>
              </button>

              <button
                type="button"
                onClick={() => setSelectedRecord(null)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl whitespace-nowrap"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {recordToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-emerald-900/15">
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-amber-300 shrink-0" />
                <h4 className="font-bold text-sm">တင်ကွက်မှတ်တမ်း ဖျက်မည်လား?</h4>
              </div>
              <button
                type="button"
                onClick={() => setRecordToDelete(null)}
                className="text-white/80 hover:text-white p-1 rounded-full hover:bg-white/10"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div className="p-5 flex flex-col gap-4">
              <p className="text-xs text-gray-600 leading-relaxed">
                ဤတင်ကွက်မှတ်တမ်း <strong className="font-mono text-gray-900">No.{recordToDelete.recordNo} ({fmt(recordToDelete.total)})</strong> အား ဖျက်ပါက လက်ခံကွက်များသို့ ပြန်လည်ပေါင်းထည့်သွားမည် ဖြစ်သည်။
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setRecordToDelete(null)}
                  className="flex-1 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl whitespace-nowrap transition"
                >
                  မဖျက်ပါ
                </button>
                <button
                  type="button"
                  onClick={handleConfirmDelete}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow whitespace-nowrap transition"
                >
                  ဖျက်မည်
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
