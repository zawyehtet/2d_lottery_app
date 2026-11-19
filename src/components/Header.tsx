import React, { useState } from 'react';
import { ArrowLeft, Calendar, ChevronDown } from 'lucide-react';
import { useLottery } from '../context/LotteryContext';
import { DatePickerModal } from './DatePickerModal';

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
    availableRounds,
    hasRound,
    goBack,
  } = useLottery();

  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showSessionMenu, setShowSessionMenu] = useState(false);

  return (
    <>
      <header className="bg-[#065240] text-white shadow-sm select-none sticky top-0 z-30">
        <div className="flex items-center justify-between gap-2 px-3 py-3">
          <div className="flex items-center gap-2 flex-1 min-w-0">
            {(onBack || showBack) ? (
              <button
                type="button"
                onClick={onBack || goBack}
                className="p-1 rounded-full hover:bg-white/10 active:bg-white/20 transition-colors shrink-0"
                title="နောက်သို့"
              >
                <ArrowLeft className="w-6 h-6 text-white" />
              </button>
            ) : null}

            <div className="flex items-center gap-2.5 min-w-0">
              <div className="w-8 h-8 rounded-full bg-[#043E30] border border-[#FEC72E] flex items-center justify-center shrink-0 overflow-hidden">
                <img
                  src="/ic_lucky2d_logo.png"
                  alt="Lucky2D"
                  className="w-full h-full object-cover"
                />
              </div>
              <h1 className="text-[19px] font-medium truncate leading-snug">{title}</h1>
            </div>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {actions}

            {showSessionPill && (
              <>
                {/* Date Pill */}
                <button
                  type="button"
                  onClick={() => setShowDatePicker(true)}
                  className="flex items-center gap-1.5 bg-white/15 hover:bg-white/20 border border-white/50 rounded-full px-3 py-1.5 text-xs font-medium text-white transition"
                  title="ရက်စွဲ ရွေးရန်"
                >
                  <Calendar className="w-3.5 h-3.5 text-white shrink-0" />
                  <span>{selectedDate}</span>
                </button>

                {/* Session Pill Dropdown */}
                <div className="relative">
                  <button
                    type="button"
                    onClick={() => setShowSessionMenu(!showSessionMenu)}
                    className="flex items-center gap-1 bg-white/15 hover:bg-white/20 border border-white/50 rounded-full px-3 py-1.5 text-xs font-medium text-white transition"
                  >
                    <span className="max-w-[70px] truncate">
                      {hasRound ? selectedSession : 'အကြိမ်မရှိ'}
                    </span>
                    <ChevronDown className="w-3.5 h-3.5 shrink-0" />
                  </button>

                  {showSessionMenu && (
                    <div className="absolute right-0 mt-1.5 w-36 bg-white rounded-xl shadow-xl py-1 text-black text-sm z-50 border border-slate-200">
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
                            className={`w-full text-left px-3 py-2 hover:bg-slate-100 flex items-center justify-between ${
                              round === selectedSession
                                ? 'font-bold text-[#065240] bg-emerald-50'
                                : 'text-slate-800'
                            }`}
                          >
                            <span>{round}</span>
                            {round === selectedSession && (
                              <span className="w-1.5 h-1.5 rounded-full bg-[#065240]"></span>
                            )}
                          </button>
                        ))
                      )}
                    </div>
                  )}
                </div>
              </>
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
