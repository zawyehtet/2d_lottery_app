import React from 'react';
import { AlertTriangle, Ban, CheckCircle2 } from 'lucide-react';
import { soundManager } from '../util/soundManager';

export interface AlertBoxSection {
  label: string;
  reason: string;
  items: string[];
  tone?: 'danger' | 'warning';
}

export interface AlertBoxState {
  title: string;
  message?: string;
  acceptedNote?: string;
  sections?: AlertBoxSection[];
}

interface AlertBoxModalProps {
  alert: AlertBoxState | null;
  onClose: () => void;
}

export const AlertBoxModal: React.FC<AlertBoxModalProps> = ({ alert, onClose }) => {
  if (!alert) return null;

  const hasBlockedSection = alert.sections?.some((s) => s.tone !== 'warning');

  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center bg-black/60 p-4 animate-in fade-in">
      <div className="bg-white rounded-2xl max-w-sm w-full max-h-[85vh] shadow-2xl overflow-hidden border border-slate-200 flex flex-col">
        {/* Header */}
        <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center gap-2.5 shrink-0">
          <div className="w-8 h-8 rounded-full bg-white/15 flex items-center justify-center shrink-0">
            {hasBlockedSection ? (
              <Ban className="w-4 h-4 text-[#FEC72E]" />
            ) : (
              <AlertTriangle className="w-4 h-4 text-[#FEC72E]" />
            )}
          </div>
          <h3 className="font-bold text-sm leading-snug">{alert.title}</h3>
        </div>

        {/* Body */}
        <div className="p-4 space-y-3 overflow-y-auto">
          {alert.message && (
            <p className="text-xs text-slate-700 leading-relaxed">{alert.message}</p>
          )}

          {alert.sections &&
            alert.sections.map((sec, idx) => {
              const isDanger = sec.tone !== 'warning';
              return (
                <div
                  key={`${sec.label}-${idx}`}
                  className={`rounded-xl p-3 border space-y-2 ${
                    isDanger
                      ? 'bg-red-50/70 border-red-200'
                      : 'bg-amber-50/70 border-amber-200'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span
                      className={`text-xs font-bold ${
                        isDanger ? 'text-red-800' : 'text-amber-900'
                      }`}
                    >
                      {sec.label}
                    </span>
                    <span
                      className={`text-[11px] font-bold px-2 py-0.5 rounded-md ${
                        isDanger
                          ? 'bg-red-100 text-red-700'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {sec.items.length} ခု
                    </span>
                  </div>

                  <p
                    className={`text-[11px] leading-relaxed ${
                      isDanger ? 'text-red-700' : 'text-amber-800'
                    }`}
                  >
                    {sec.reason}
                  </p>

                  {sec.items.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 pt-0.5 max-h-36 overflow-y-auto">
                      {sec.items.map((item, i) => (
                        <span
                          key={`${item}-${i}`}
                          className={`px-2 py-1 rounded-lg text-xs font-mono font-bold border ${
                            isDanger
                              ? 'bg-white text-red-700 border-red-200'
                              : 'bg-white text-amber-900 border-amber-200'
                          }`}
                        >
                          {item}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}

          {alert.acceptedNote && (
            <div className="bg-emerald-50 border border-emerald-200 text-[#235347] rounded-xl px-3 py-2 text-xs font-semibold flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0 text-[#2E7D32]" />
              <span>{alert.acceptedNote}</span>
            </div>
          )}
        </div>

        {/* Single Dismiss Button */}
        <div className="p-3.5 bg-slate-50 border-t border-slate-100 shrink-0">
          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              onClose();
            }}
            className="w-full py-2.5 bg-[#235347] hover:bg-[#183B32] text-white rounded-xl font-bold text-xs shadow-sm transition cursor-pointer"
          >
            ပိတ်မည်
          </button>
        </div>
      </div>
    </div>
  );
};
