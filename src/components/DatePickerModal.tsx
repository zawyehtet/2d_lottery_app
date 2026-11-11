import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, X } from 'lucide-react';

interface DatePickerModalProps {
  currentDate: string; // YYYY-MM-DD
  onSelect: (date: string) => void;
  onClose: () => void;
}

export const DatePickerModal: React.FC<DatePickerModalProps> = ({
  currentDate,
  onSelect,
  onClose,
}) => {
  const parts = currentDate.split('-');
  const initialYear = parseInt(parts[0], 10) || new Date().getFullYear();
  const initialMonth = (parseInt(parts[1], 10) || 1) - 1; // 0-indexed

  const [viewYear, setViewYear] = useState(initialYear);
  const [viewMonth, setViewMonth] = useState(initialMonth);

  const monthNames = [
    'ဇန်နဝါရီ',
    'ဖေဖော်ဝါရီ',
    'မတ်',
    'ဧပြီ',
    'မေ',
    'ဇွန်',
    'ဇူလိုင်',
    'သြဂုတ်',
    'စက်တင်ဘာ',
    'အောက်တိုဘာ',
    'နိုဝင်ဘာ',
    'ဒီဇင်ဘာ',
  ];

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDayOfWeek = new Date(viewYear, viewMonth, 1).getDay();

  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewMonth(11);
      setViewYear((y) => y - 1);
    } else {
      setViewMonth((m) => m - 1);
    }
  };

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewMonth(0);
      setViewYear((y) => y + 1);
    } else {
      setViewMonth((m) => m + 1);
    }
  };

  const selectDay = (day: number) => {
    const m = String(viewMonth + 1).padStart(2, '0');
    const d = String(day).padStart(2, '0');
    onSelect(`${viewYear}-${m}-${d}`);
  };

  const today = new Date();
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
      <div className="bg-white rounded-2xl shadow-2xl max-w-sm w-full overflow-hidden border border-slate-200">
        <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="font-bold text-lg">ရက်စွဲ ရွေးချယ်ရန်</span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1 rounded-full hover:bg-white/10 active:bg-white/20"
          >
            <X className="w-5 h-5 text-white" />
          </button>
        </div>

        <div className="p-4">
          <div className="flex items-center justify-between mb-4">
            <button
              type="button"
              onClick={handlePrevMonth}
              className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100"
            >
              <ChevronLeft className="w-4 h-4 text-slate-700" />
            </button>
            <div className="text-center font-bold text-slate-800">
              {monthNames[viewMonth]} {viewYear}
            </div>
            <button
              type="button"
              onClick={handlePrevMonth}
              className="p-1.5 rounded-lg border border-slate-200 hover:bg-slate-100"
            >
              <ChevronRight className="w-4 h-4 text-slate-700" />
            </button>
          </div>

          <div className="grid grid-cols-7 gap-1 text-center text-xs font-bold text-slate-500 mb-2">
            <div>တနင်္ဂနွေ</div>
            <div>တနင်္လာ</div>
            <div>အင်္ဂါ</div>
            <div>ဗုဒ္ဓဟူး</div>
            <div>ကြာသပတေး</div>
            <div>သောကြာ</div>
            <div>စနေ</div>
          </div>

          <div className="grid grid-cols-7 gap-1 text-center text-sm">
            {Array.from({ length: firstDayOfWeek }).map((_, i) => (
              <div key={`empty-${i}`} />
            ))}

            {Array.from({ length: daysInMonth }).map((_, i) => {
              const day = i + 1;
              const dateStr = `${viewYear}-${String(viewMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
              const isSelected = dateStr === currentDate;
              const isToday = dateStr === todayStr;

              return (
                <button
                  key={day}
                  type="button"
                  onClick={() => selectDay(day)}
                  className={`h-9 w-9 mx-auto rounded-full flex items-center justify-center font-semibold text-xs transition-colors ${
                    isSelected
                      ? 'bg-[#235347] text-white shadow-md'
                      : isToday
                      ? 'border border-[#235347] text-[#235347] font-bold'
                      : 'text-slate-700 hover:bg-slate-100'
                  }`}
                >
                  {day}
                </button>
              );
            })}
          </div>

          <div className="mt-4 pt-3 border-t border-slate-200 flex justify-between">
            <button
              type="button"
              onClick={() => onSelect(todayStr)}
              className="px-3 py-1.5 text-xs font-bold text-[#235347] hover:bg-emerald-50 rounded-lg"
            >
              ယနေ့ရွေးမည်
            </button>
            <button
              type="button"
              onClick={onClose}
              className="px-3 py-1.5 text-xs font-bold text-slate-600 hover:bg-slate-100 rounded-lg"
            >
              မလုပ်တော့ပါ
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
