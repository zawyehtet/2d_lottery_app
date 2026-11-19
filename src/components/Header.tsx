import React, { useState } from 'react';
import { ArrowLeft, Calendar, ChevronDown, Settings } from 'lucide-react';
import { useLottery, formatDotDate } from '../context/LotteryContext';
import { DatePickerModal } from './DatePickerModal';
import { Session } from '../types';

interface HeaderProps {
  title: string;
  onBack?: () => void;
  showBack?: boolean;
  showSessionPill?: boolean;
  actions?: React.ReactNode;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  onBack,
  showBack,
  showSessionPill = true,
  actions,
}) => {
  const {
    selectedDate,
    setDate,
    selectedSession,
    setSession,
    sessions,
    availableRounds,
    hasRound,
    goBack,
    navigateTo,
  } = useLottery();

  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showSessionMenu, setShowSessionMenu] = useState(false);

  return (
    <>
      <header className="bg-[#235347] text-white shadow-md select-none sticky top-0 z-30">
        <div className="flex items-center justify-between px-3 py-2.5">
          <div className="flex items-center gap-2 flex-1 min-w-0">
            {(onBack || showBack) ? (
              <button
                type="button"
                onClick={onBack || goBack}
                className="p-1.5 rounded-full hover:bg-white/10 active:bg-white/20 transition-colors"
                title="နောက်သို့"
              >
                <ArrowLeft className="w-5 h-5 text-white" />
              </button>
            ) : null}

            <div className="flex items-center gap-2 min-w-0">
              <div className="w-7 h-7 rounded-full bg-[#0C4E3E] border border-[#FEC72E] flex items-center justify-center shrink-0 overflow-hidden">
                <img
                  src="/ic_lucky2d_logo.png"
                  alt="Lucky2D"
                  className="w-full h-full object-cover"
                />
              </div>
              <h1 className="text-lg font-bold truncate tracking-wide">{title}</h1>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {actions}

            {showSessionPill && (
              <div className="flex items-center gap-1.5 bg-[#183B32] border border-white/20 rounded-lg px-2 py-1 text-[11px] sm:text-xs">
                {/* Date Button */}
                <button
                  type="button"
                  onClick={() => setShowDatePicker(true)}
                  className="flex items-center gap-1 text-white/90 hover:text-white font-medium whitespace-nowrap"
                  title="ရက်စွဲ ရွေးရန်"
                >
                  <Calendar className="w-3.5 h-3.5 text-[#FEC72E] shrink-0" />
                  <span>{formatDotDate(selectedDate)}</span>
                </button>

                <span className="text-white/40">|</span>

                {/* Round Dropdown */}
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => setShowSessionMenu(!showSessionMenu)}
                    className="flex items-center gap-0.5 text-white/90 hover:text-white font-bold whitespace-nowrap"
                  >
                    <span>{hasRound ? selectedSession : 'အကြိမ်မရှိ'}</span>
                    <ChevronDown className="w-3.5 h-3.5 shrink-0" />
                  </button>

                  {showSessionMenu && (
                    <div className="absolute right-0 mt-1 w-36 bg-white rounded-lg shadow-xl py-1 text-black text-sm z-50 border border-slate-200">
                      {availableRounds.length === 0 ? (
                        <div className="px-3 py-2 text-xs text-slate-500">
                          အကြိမ် မရှိသေးပါ
                        </div>
                      ) : (
                        availableRounds.map((round) => (
                          <button
                            key={round}
                            type="button"
                            onClick={() => {
                              setSession(round);
                              setShowSessionMenu(false);
                            }}
                            className={`w-full text-left px-3 py-1.5 hover:bg-slate-100 flex items-center justify-between ${
                              round === selectedSession
                                ? 'font-bold text-[#235347] bg-emerald-50'
                                : 'text-slate-800'
                            }`}
                          >
                            <span>{round}</span>
                            {round === selectedSession && (
                              <span className="w-1.5 h-1.5 rounded-full bg-[#235347]"></span>
                            )}
                          </button>
                        ))
                      )}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {showDatePicker && (
        <DatePickerModal
          currentDate={selectedDate}
          onSelect={(d) => {
            setDate(d);
            setShowDatePicker(false);
          }}
          onClose={() => setShowDatePicker(false)}
        />
      )}
    </>
  );
};
