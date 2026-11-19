import React, { useState, useEffect } from 'react';
import { CheckCircle, Copy, Key, Lock, Smartphone, Clipboard } from 'lucide-react';
import {
  getDeviceId,
  normalizeKeyInput,
  formatKeyWithDashes,
  verifyAndSaveLicense,
} from '../security/licenseManager';
import { soundManager } from '../util/soundManager';

interface ActivationModalProps {
  onActivated: () => void;
}

export const ActivationModal: React.FC<ActivationModalProps> = ({ onActivated }) => {
  const [deviceId, setDeviceId] = useState('');
  const [inputKey, setInputKey] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    setDeviceId(getDeviceId());
  }, []);

  const handleCopyDeviceId = () => {
    navigator.clipboard.writeText(deviceId);
    soundManager.playClick();
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handlePasteKey = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setInputKey(normalizeKeyInput(text));
        setErrorMessage(null);
        soundManager.playClick();
      }
    } catch {
      // Fallback
    }
  };

  const handleActivate = async () => {
    const normalized = normalizeKeyInput(inputKey);
    if (normalized.length < 12) {
      setErrorMessage('သော့ကုဒ် မှားယွင်းနေပါသည်၊ မှန်ကန်သော သော့ကုဒ် ထည့်သွင်းပါ');
      soundManager.playDelete();
      return;
    }

    setIsVerifying(true);
    setErrorMessage(null);

    try {
      const success = await verifyAndSaveLicense(normalized);
      setIsVerifying(false);

      if (success) {
        soundManager.playComplete();
        setShowSuccess(true);
        setTimeout(() => {
          setShowSuccess(false);
          onActivated();
        }, 1200);
      } else {
        soundManager.playDelete();
        setErrorMessage('သော့ကုဒ် မှားယွင်းနေပါသည်၊ မှန်ကန်သော သော့ကုဒ် ထည့်သွင်းပါ');
      }
    } catch {
      setIsVerifying(false);
      setErrorMessage('အတည်ပြုရာတွင် ချို့ယွင်းချက်ဖြစ်ပေါ်ပါသည်');
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#F5F7F6] flex flex-col items-center justify-center p-4 overflow-y-auto">
      <div className="max-w-md w-full bg-white rounded-2xl shadow-xl border border-slate-200 overflow-hidden my-auto">
        {/* Header */}
        <div className="bg-[#065240] text-white p-6 text-center">
          <div className="w-16 h-16 rounded-2xl bg-[#043E30] border-2 border-[#FEC72E] mx-auto p-1 shadow-lg flex items-center justify-center overflow-hidden mb-3">
            <img
              src="/ic_lucky2d_logo.png"
              alt="Lucky2D"
              className="w-full h-full object-cover rounded-xl"
            />
          </div>
          <h1 className="text-2xl font-black tracking-wide">Lucky2D</h1>
          <h2 className="text-base font-bold text-white mt-1">ဆော့ဝဲလ် အသက်သွင်းခြင်း</h2>
          <p className="text-xs text-emerald-200 mt-0.5">တစ်ကြိမ်သာ အသက်သွင်းရန် လိုအပ်ပါသည်</p>
        </div>

        <div className="p-6 space-y-5">
          {/* Device ID */}
          <div className="space-y-1.5">
            <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
              <Smartphone className="w-4 h-4 text-[#065240]" />
              <span>သင့်စက်အမှတ် (Device ID)</span>
            </div>

            <div className="flex items-center justify-between bg-slate-100 border border-slate-300 rounded-xl px-3 py-2.5">
              <span className="font-mono font-bold text-base text-[#065240] tracking-wider truncate">
                {deviceId}
              </span>
              <button
                type="button"
                onClick={handleCopyDeviceId}
                className="flex items-center gap-1 text-xs font-bold text-white bg-[#065240] hover:bg-[#043E30] px-3 py-1.5 rounded-lg shrink-0 transition-colors shadow-sm ml-2"
              >
                <Copy className="w-3.5 h-3.5" />
                <span>{copied ? 'ကူးပြီးပြီ' : 'ကူးမည်'}</span>
              </button>
            </div>
            <p className="text-xs text-slate-500">
              သင့်စက်အမှတ်ကို Admin ထံပေးပို့ပြီး အသက်သွင်းကုဒ် ရယူပါ
            </p>
          </div>

          <hr className="border-slate-200" />

          {/* Key Input */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
                <Key className="w-4 h-4 text-[#065240]" />
                <span>အသက်သွင်းကုဒ် ထည့်သွင်းရန်</span>
              </div>
              <button
                type="button"
                onClick={handlePasteKey}
                className="flex items-center gap-1 text-xs font-bold text-[#065240] hover:text-[#043E30]"
              >
                <Clipboard className="w-3.5 h-3.5" />
                <span>PASTE</span>
              </button>
            </div>

            <input
              type="text"
              value={formatKeyWithDashes(inputKey)}
              onChange={(e) => {
                setInputKey(normalizeKeyInput(e.target.value));
                setErrorMessage(null);
              }}
              placeholder="XXXX-XXXX-XXXX"
              maxLength={14}
              className={`w-full text-center font-mono text-lg font-black tracking-widest px-4 py-3 border-2 rounded-xl focus:outline-none transition-colors ${
                errorMessage
                  ? 'border-red-500 text-red-600 bg-red-50'
                  : 'border-[#065240] text-slate-900 bg-slate-50 focus:bg-white'
              }`}
            />

            {errorMessage && (
              <p className="text-xs font-semibold text-red-600 text-center">
                {errorMessage}
              </p>
            )}
          </div>

          {/* Submit Button */}
          <button
            type="button"
            disabled={isVerifying || inputKey.length === 0}
            onClick={handleActivate}
            className="w-full bg-[#065240] hover:bg-[#043E30] disabled:bg-slate-300 text-white font-bold py-3.5 rounded-xl shadow-md transition-all flex items-center justify-center gap-2 cursor-pointer disabled:cursor-not-allowed"
          >
            {isVerifying ? (
              <span>စစ်ဆေးနေပါသည်...</span>
            ) : (
              <>
                <Lock className="w-4 h-4" />
                <span>စစ်ဆေးပြီး စတင်အသုံးပြုမည်</span>
              </>
            )}
          </button>
        </div>

        <div className="bg-slate-50 border-t border-slate-200 py-3 text-center text-xs font-medium text-slate-500">
          Power by Lcky2d3d ShweKhinMyay
        </div>
      </div>

      {showSuccess && (
        <div className="fixed inset-0 z-60 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl p-6 text-center max-w-sm w-full shadow-2xl">
            <CheckCircle className="w-16 h-16 text-emerald-600 mx-auto mb-3" />
            <h3 className="text-xl font-bold text-slate-900 mb-1">
              အသက်သွင်းမှု အောင်မြင်ပါသည်
            </h3>
            <p className="text-sm text-slate-600">
              သင့်စက်အတွက် လိုင်စင်အတည်ပြုပြီးပါပြီ။ မင်္ဂလာရှိသော နေ့ရက်ဖြစ်ပါစေ။
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
