import React, { useState } from 'react';
import {
  Users,
  LayoutGrid,
  Receipt,
  Plus,
  Calendar,
  ChevronDown,
  Settings,
  Sparkles,
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
    sessions,
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
    <div className="flex flex-col min-h-screen bg-[#F4F7F6]">
      {/* Top App Bar Header */}
      <header className="bg-[#235347] text-white shadow-md px-5 py-3.5 sticky top-0 z-30">
        <div className="max-w-md mx-auto flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-[#0C4E3E] border border-[#FEC72E] flex items-center justify-center shrink-0 overflow-hidden shadow-inner">
              <img
                src="/ic_lucky2d_logo.png"
                alt="Lucky2D"
                className="w-full h-full object-cover"
              />
            </div>
            <span className="font-extrabold text-xl tracking-wide text-white">Lucky2D</span>
          </div>

          <div className="text-xs bg-[#183B32] border border-emerald-600/40 px-2.5 py-1 rounded-full text-emerald-200 font-medium">
            2D မန်နေဂျာ
          </div>
        </div>
      </header>

      <main className="flex-1 max-w-md w-full mx-auto p-5 flex flex-col items-center">
        {/* Date Display Pill */}
        <div className="flex items-center justify-center gap-2 text-[#235347] font-bold text-base mb-6 pt-1">
          <Calendar className="w-5 h-5 text-[#235347]" />
          <span>ယနေ့ - {dotDate}</span>
        </div>

        {/* 2x2 Grid Menu Cards (ကော်မရှင်, ဂဏန်းများ, ဘောင်ချာ, တင်ကွက်) */}
        <div className="grid grid-cols-2 gap-4 w-full mb-6">
          {/* Card 1: ကော်မရှင် */}
          <button
            type="button"
            onClick={() => handleCardClick(() => navigateTo('AGENTS'))}
            className="aspect-square bg-[#235347] hover:bg-[#183B32] active:scale-98 text-white rounded-2xl shadow-lg p-4 flex flex-col items-center justify-center gap-3 transition-all cursor-pointer border border-emerald-800/40"
          >
            <div className="w-14 h-14 rounded-2xl bg-white/10 flex items-center justify-center">
              <Users className="w-8 h-8 text-white" />
            </div>
            <span className="font-bold text-base tracking-wide">ကော်မရှင်</span>
          </button>

          {/* Card 2: ဂဏန်းများ */}
          <button
            type="button"
            onClick={() => handleCardClick(() => navigateTo('NUMBERS'))}
            className="aspect-square bg-[#235347] hover:bg-[#183B32] active:scale-98 text-white rounded-2xl shadow-lg p-4 flex flex-col items-center justify-center gap-3 transition-all cursor-pointer border border-emerald-800/40"
          >
            <div className="w-14 h-14 rounded-2xl bg-white/10 flex items-center justify-center">
              <LayoutGrid className="w-8 h-8 text-white" />
            </div>
            <span className="font-bold text-base tracking-wide">ဂဏန်းများ</span>
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
            className="aspect-square bg-[#235347] hover:bg-[#183B32] active:scale-98 text-white rounded-2xl shadow-lg p-4 flex flex-col items-center justify-center gap-3 transition-all cursor-pointer border border-emerald-800/40"
          >
            <div className="w-14 h-14 rounded-2xl bg-white/10 flex items-center justify-center">
              <Receipt className="w-8 h-8 text-white" />
            </div>
            <span className="font-bold text-base tracking-wide">ဘောင်ချာ</span>
          </button>

          {/* Card 4: တင်ကွက် */}
          <button
            type="button"
            onClick={() => handleCardClick(() => navigateTo('BRAKE'))}
            className="aspect-square bg-[#235347] hover:bg-[#183B32] active:scale-98 text-white rounded-2xl shadow-lg p-4 flex flex-col items-center justify-center gap-3 transition-all cursor-pointer border border-emerald-800/40"
          >
            <div className="w-14 h-14 rounded-2xl bg-white/10 flex items-center justify-center">
              <Plus className="w-8 h-8 text-white" />
            </div>
            <span className="font-bold text-base tracking-wide">တင်ကွက်</span>
          </button>
        </div>

        {/* Repositioned Selection Controls directly BELOW 4-Grid Cards */}
        <div className="flex items-center gap-2 w-full mb-6">
          {/* Left: Date Selector */}
          <button
            type="button"
            onClick={() => setShowDatePicker(true)}
            className="flex-1 h-12 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl px-3 flex items-center justify-center gap-2 shadow-sm text-slate-800 font-bold text-sm cursor-pointer"
          >
            <Calendar className="w-4 h-4 text-[#235347]" />
            <span className="truncate">ရက်စွဲ : {dotDate}</span>
          </button>

          {/* Center: Round Selector */}
          <div className="relative flex-1">
            <button
              type="button"
              onClick={() => setShowRoundMenu(!showRoundMenu)}
              className="w-full h-12 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl px-3 flex items-center justify-between gap-1 shadow-sm text-slate-800 font-bold text-sm cursor-pointer"
            >
              <span className="truncate">
                {hasRound ? selectedSession : 'အကြိမ်မရှိ'}
              </span>
              <ChevronDown className="w-4 h-4 text-[#235347] shrink-0" />
            </button>

            {showRoundMenu && (
              <div className="absolute left-0 right-0 mt-1 bg-white rounded-xl shadow-xl py-1 border border-slate-200 z-40 max-h-48 overflow-y-auto">
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
                          ? 'font-bold text-[#235347] bg-emerald-50'
                          : 'text-slate-800'
                      }`}
                    >
                      <span>{round}</span>
                      {round === selectedSession && (
                        <span className="w-2 h-2 rounded-full bg-[#235347]"></span>
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
            className="w-12 h-12 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl flex items-center justify-center shadow-sm text-[#235347] shrink-0 cursor-pointer"
            title="ဆက်တင်"
          >
            <Settings className="w-6 h-6" />
          </button>
        </div>

        {/* Quick Shortcut Buttons for fast actions */}
        <div className="w-full grid grid-cols-2 gap-2 mb-6">
          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              navigateTo('SETTLEMENT');
            }}
            className="h-11 bg-white hover:bg-slate-50 border border-emerald-600/30 rounded-xl px-2.5 flex items-center justify-center gap-1.5 text-xs font-bold text-[#235347] shadow-sm whitespace-nowrap"
          >
            <Sparkles className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>ရက်ချုပ်အားလုံး</span>
          </button>

          <button
            type="button"
            onClick={() => {
              soundManager.playClick();
              navigateTo('LAYOFF_HISTORY');
            }}
            className="h-11 bg-white hover:bg-slate-50 border border-amber-600/30 rounded-xl px-2.5 flex items-center justify-center gap-1.5 text-xs font-bold text-amber-800 shadow-sm whitespace-nowrap"
          >
            <span>တင်ကွက်မှတ်တမ်း</span>
          </button>
        </div>

        <div className="mt-auto pt-6 text-center text-xs font-semibold text-slate-500">
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
