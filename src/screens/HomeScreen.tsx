import React, { useState } from 'react';
import {
  Users,
  LayoutGrid,
  Receipt,
  Plus,
  Calendar,
  Settings,
} from 'lucide-react';
import { useLottery, formatDotDate } from '../context/LotteryContext';
import { DatePickerModal } from '../components/DatePickerModal';
import { soundManager } from '../util/soundManager';

export const HomeScreen: React.FC = () => {
  const {
    selectedDate,
    setDate,
    selectedSession,
    setSession,
    availableRounds,
    hasRound,
    navigateTo,
    clearVoucherFilter,
  } = useLottery();

  const [showDatePicker, setShowDatePicker] = useState(false);
  const [showRoundMenu, setShowRoundMenu] = useState(false);

  const dotDate = formatDotDate(selectedDate);

  const handleCardClick = (action: () => void) => {
    soundManager.playClick();
    action();
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F5F7F6]">
      {/* Top App Bar Header */}
      <header className="bg-[#065240] text-white shadow-sm px-4 py-3.5 sticky top-0 z-30">
        <div className="max-w-md mx-auto flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-[#043E30] border border-[#FEC72E] flex items-center justify-center shrink-0 overflow-hidden shadow-inner">
            <img
              src="/ic_lucky2d_logo.png"
              alt="Lucky2D"
              className="w-full h-full object-cover"
            />
          </div>
          <span className="font-medium text-2xl tracking-normal text-white">Lucky2D</span>
        </div>
      </header>

      <main className="flex-1 max-w-md w-full mx-auto px-6 pt-8 pb-6 flex flex-col items-center">
        {/* Date Display */}
        <div className="flex items-center justify-center gap-2.5 text-[#065240] font-medium text-xl mb-8">
          <Calendar className="w-5 h-5 text-[#065240]" />
          <span>ယနေ့ - {dotDate}</span>
        </div>

        {/* 2x2 Grid Menu Cards (ကော်မရှင်, ဂဏန်းများ, ဘောင်ချာ, တင်ကွက်) */}
        <div className="grid grid-cols-2 gap-5 w-full mb-7">
          {/* Card 1: ကော်မရှင် */}
          <button
            type="button"
            onClick={() => handleCardClick(() => navigateTo('AGENTS'))}
            className="aspect-square bg-[#065240] hover:bg-[#054435] active:scale-[0.98] text-white rounded-2xl shadow-[0_6px_14px_rgba(6,82,64,0.22)] p-4 flex flex-col items-center justify-center gap-4 transition-all cursor-pointer"
          >
            <Users className="w-12 h-12 text-white" strokeWidth={2} />
            <span className="font-medium text-lg tracking-wide">ကော်မရှင်</span>
          </button>

          {/* Card 2: ဂဏန်းများ */}
          <button
            type="button"
            onClick={() => handleCardClick(() => navigateTo('NUMBERS'))}
            className="aspect-square bg-[#065240] hover:bg-[#054435] active:scale-[0.98] text-white rounded-2xl shadow-[0_6px_14px_rgba(6,82,64,0.22)] p-4 flex flex-col items-center justify-center gap-4 transition-all cursor-pointer"
          >
            <LayoutGrid className="w-12 h-12 text-white" strokeWidth={2} />
            <span className="font-medium text-lg tracking-wide">ဂဏန်းများ</span>
          </button>

          {/* Card 3: ဘောင်ချာ */}
          <button
            type="button"
            onClick={() =>
              handleCardClick(() => {
                clearVoucherFilter();
                navigateTo('VOUCHERS');
              })
            }
            className="aspect-square bg-[#065240] hover:bg-[#054435] active:scale-[0.98] text-white rounded-2xl shadow-[0_6px_14px_rgba(6,82,64,0.22)] p-4 flex flex-col items-center justify-center gap-4 transition-all cursor-pointer"
          >
            <Receipt className="w-12 h-12 text-white" strokeWidth={2} />
            <span className="font-medium text-lg tracking-wide">ဘောင်ချာ</span>
          </button>

          {/* Card 4: တင်ကွက် */}
          <button
            type="button"
            onClick={() => handleCardClick(() => navigateTo('BRAKE'))}
            className="aspect-square bg-[#065240] hover:bg-[#054435] active:scale-[0.98] text-white rounded-2xl shadow-[0_6px_14px_rgba(6,82,64,0.22)] p-4 flex flex-col items-center justify-center gap-4 transition-all cursor-pointer"
          >
            <Plus className="w-12 h-12 text-white" strokeWidth={2.5} />
            <span className="font-medium text-lg tracking-wide">တင်ကွက်</span>
          </button>
        </div>

        {/* Selection Controls Row directly BELOW 4-Grid Cards */}
        <div className="flex items-center gap-3 w-full mb-7">
          {/* Left: Date Selector */}
          <button
            type="button"
            onClick={() => setShowDatePicker(true)}
            className="flex-[1.35] h-14 bg-white hover:bg-slate-50 border border-slate-200 rounded-2xl px-3.5 flex items-center gap-2 shadow-[0_2px_8px_rgba(0,0,0,0.08)] text-slate-900 font-normal text-[15px] cursor-pointer min-w-0"
          >
            <Calendar className="w-4 h-4 text-[#065240] shrink-0" />
            <span className="truncate">ရက်စွဲ : {dotDate}</span>
          </button>

          {/* Center: Round Selector */}
          <div className="relative flex-1 min-w-0">
            <button
              type="button"
              onClick={() => setShowRoundMenu(!showRoundMenu)}
              className="w-full h-14 bg-white hover:bg-slate-50 border border-slate-200 rounded-2xl px-4 flex items-center justify-between gap-1 shadow-[0_2px_8px_rgba(0,0,0,0.08)] text-slate-900 font-normal text-[15px] cursor-pointer"
            >
              <span className="truncate">
                {hasRound ? selectedSession : 'အကြိမ်မရှိ'}
              </span>
              <span className="text-[#065240] text-xs shrink-0">▼</span>
            </button>

            {showRoundMenu && (
              <div className="absolute left-0 right-0 mt-1.5 bg-white rounded-xl shadow-xl py-1 border border-slate-200 z-40 max-h-48 overflow-y-auto">
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
                        setShowRoundMenu(false);
                      }}
                      className={`w-full text-left px-4 py-2 hover:bg-slate-100 flex items-center justify-between text-sm ${
                        round === selectedSession
                          ? 'font-bold text-[#065240] bg-emerald-50'
                          : 'text-slate-800'
                      }`}
                    >
                      <span>{round}</span>
                      {round === selectedSession && (
                        <span className="w-2 h-2 rounded-full bg-[#065240]"></span>
                      )}
                    </button>
                  ))
                )}
              </div>
            )}
          </div>

          {/* Right: Settings Button */}
          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              navigateTo('SETTINGS');
            }}
            className="w-14 h-14 bg-white hover:bg-slate-50 border border-slate-200 rounded-2xl flex items-center justify-center shadow-[0_2px_8px_rgba(0,0,0,0.08)] text-[#065240] shrink-0 cursor-pointer"
            title="ဆက်တင်"
          >
            <Settings className="w-6 h-6" />
          </button>
        </div>

        {/* Footer Brand */}
        <div className="text-center text-[15px] font-normal text-slate-500">
          Power by Lcky2d3d ShweKhinMyay
        </div>
      </main>

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
    </div>
  );
};
