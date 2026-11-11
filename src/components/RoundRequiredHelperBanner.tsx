import React from 'react';
import { AlertCircle } from 'lucide-react';
import { useLottery } from '../context/LotteryContext';

export const RoundRequiredHelperBanner: React.FC<{ visible?: boolean }> = ({ visible = true }) => {
  const { hasRound, navigateTo } = useLottery();
  if (hasRound || !visible) return null;

  return (
    <div className="bg-amber-50 border border-amber-300 rounded-xl p-3 flex items-center justify-between text-amber-900 shadow-sm mx-3 my-2">
      <div className="flex items-center gap-2">
        <AlertCircle className="w-5 h-5 text-amber-600 shrink-0" />
        <span className="text-sm font-semibold">အကြိမ်ရေ အရင်ထည့်ပါ / ရွေးချယ်ပါ</span>
      </div>
      <button
        type="button"
        onClick={() => navigateTo('SETTINGS')}
        className="px-2.5 py-1 bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold rounded-lg shrink-0 transition-colors"
      >
        ဆက်တင်သို့
      </button>
    </div>
  );
};
