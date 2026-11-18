import React from 'react';
import { MessageSquare, Send, MessageCircle, X } from 'lucide-react';
import { soundManager } from '../util/soundManager';

interface QuickShareModalProps {
  agentName: string;
  phoneNumber: string;
  shareText: string;
  onShowToast: (msg: string) => void;
  onClose: () => void;
}

async function copyTextToClipboard(text: string): Promise<void> {
  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard && navigator.clipboard.writeText) {
      await navigator.clipboard.writeText(text);
      return;
    }
  } catch {
    // Fallback below
  }

  try {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    document.execCommand('copy');
    document.body.removeChild(textarea);
  } catch {
    // Ignore clipboard error
  }
}

export const QuickShareModal: React.FC<QuickShareModalProps> = ({
  agentName,
  phoneNumber,
  shareText,
  onShowToast,
  onClose,
}) => {
  const cleanPhone = phoneNumber.trim();

  const handleSmsShare = () => {
    soundManager.playClick();
    window.location.href =
      'sms:' + encodeURIComponent(cleanPhone) + '?body=' + encodeURIComponent(shareText);
  };

  const handleViberShare = async () => {
    soundManager.playClick();
    await copyTextToClipboard(shareText);
    onShowToast('စာသားကူးယူပြီး Viber ဖွင့်နေပါသည်');
    window.location.href = 'viber://forward?text=' + encodeURIComponent(shareText);
  };

  const handleMessengerShare = async () => {
    soundManager.playClick();
    await copyTextToClipboard(shareText);
    onShowToast('စာသားကူးယူပြီး Messenger ဖွင့်နေပါသည်');

    const messengerUrl =
      'fb-messenger://share/?link=&app_id=&text=' + encodeURIComponent(shareText);
    window.location.href = messengerUrl;

    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      setTimeout(() => {
        if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
          navigator.share({ text: shareText }).catch(() => {
            // Ignore cancel or unsupported share
          });
        }
      }, 450);
    }
  };

  const handleDismiss = () => {
    soundManager.playClick();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 animate-in fade-in">
      <div className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-emerald-900/15 flex flex-col">
        {/* Emerald Green Header */}
        <div className="bg-[#0A4D3C] text-white px-5 py-4 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="font-bold text-base leading-tight">စလစ် မက်ဆေ့ ပို့မည်</h3>
            <p className="text-xs text-emerald-100/90 mt-1 truncate">
              {agentName} • {cleanPhone}
            </p>
          </div>
          <button
            type="button"
            onClick={handleDismiss}
            className="p-1 rounded-full text-white/80 hover:text-white hover:bg-white/10 transition shrink-0"
            aria-label="ပိတ်မည်"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="p-5 space-y-4">
          {/* Preview Card */}
          <div className="bg-[#F4F7F6] rounded-xl border border-slate-200 px-4 py-4 text-center">
            <div className="text-[11px] font-semibold text-slate-500 mb-1">ပို့မည့် စလစ်စာသား</div>
            <div className="font-mono font-black text-2xl tracking-wide text-[#0A4D3C] select-all">
              {shareText}
            </div>
          </div>

          {/* 3 Channel Action Buttons */}
          <div className="grid grid-cols-3 gap-2.5">
            <button
              type="button"
              onClick={handleSmsShare}
              className="flex flex-col items-center justify-center gap-1.5 py-3 px-2 rounded-xl bg-[#0A4D3C] hover:bg-[#083D2F] active:scale-95 text-white font-bold text-xs shadow-sm transition cursor-pointer"
            >
              <MessageSquare className="w-5 h-5" />
              <span>SMS</span>
            </button>

            <button
              type="button"
              onClick={handleViberShare}
              className="flex flex-col items-center justify-center gap-1.5 py-3 px-2 rounded-xl bg-[#7360F2] hover:bg-[#5F4CE0] active:scale-95 text-white font-bold text-xs shadow-sm transition cursor-pointer"
            >
              <Send className="w-5 h-5" />
              <span>Viber</span>
            </button>

            <button
              type="button"
              onClick={handleMessengerShare}
              className="flex flex-col items-center justify-center gap-1.5 py-3 px-2 rounded-xl bg-[#0084FF] hover:bg-[#0070DB] active:scale-95 text-white font-bold text-xs shadow-sm transition cursor-pointer"
            >
              <MessageCircle className="w-5 h-5" />
              <span>Messenger</span>
            </button>
          </div>

          {/* Bottom Dismiss Button */}
          <button
            type="button"
            onClick={handleDismiss}
            className="w-full py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 active:bg-slate-300 text-slate-700 font-bold text-xs transition cursor-pointer"
          >
            မလုပ်ပါ
          </button>
        </div>
      </div>
    </div>
  );
};
