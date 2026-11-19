import React, { useState } from 'react';
import { useLottery, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import {
  Volume2,
  VolumeX,
  Ban,
  Clock,
  Trophy,
  Lock,
  Trash2,
  Gauge,
  ChevronRight,
  X,
  Plus,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';
import { soundManager } from '../util/soundManager';

export const SettingsScreen: React.FC = () => {
  const {
    settings,
    updateSettings,
    sessions,
    addSession,
    deleteSessionRecord,
    blocked,
    toggleBlockedNumber,
    currentWinningNumber,
    setWinningNumber,
    selectedDate,
    selectedSession,
    resetAllData,
  } = useLottery();

  // Modals state
  const [activeModal, setActiveModal] = useState<
    'blocked' | 'sessions' | 'winning' | 'password' | 'brake' | 'reset' | null
  >(null);

  // Blocked number input
  const [newBlockedNumber, setNewBlockedNumber] = useState('');

  // Session inputs
  const [newSessionName, setNewSessionName] = useState('');
  const [newSessionRate, setNewSessionRate] = useState('15');
  const [newSessionMult, setNewSessionMult] = useState('80');

  // Winning number input
  const [winNumInput, setWinNumInput] = useState('');

  // Change Password inputs
  const [newPasswordInput, setNewPasswordInput] = useState('');
  const [confirmPasswordInput, setConfirmPasswordInput] = useState('');
  const [passwordError, setPasswordError] = useState('');

  // Brake input
  const [brakeInput, setBrakeInput] = useState(settings.brakeLimit.toString());

  // Reset confirmation input
  const [resetConfirmText, setResetConfirmText] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const showSuccess = (msg: string) => {
    setSuccessMsg(msg);
    setTimeout(() => setSuccessMsg(''), 3000);
  };

  const handleToggleSound = () => {
    soundManager.playClick();
    updateSettings({ soundEnabled: !settings.soundEnabled });
  };

  const handleAddBlocked = (e: React.FormEvent) => {
    e.preventDefault();
    const val = newBlockedNumber.trim().padStart(2, '0');
    if (val.length === 2 && !isNaN(parseInt(val, 10))) {
      toggleBlockedNumber(val);
      soundManager.playSave();
      setNewBlockedNumber('');
    }
  };

  const handleAddSession = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newSessionName.trim()) return;
    const rate = parseFloat(newSessionRate) || 15;
    const mult = parseInt(newSessionMult, 10) || 80;
    addSession(newSessionName.trim(), rate, mult);
    setNewSessionName('');
  };

  const handleSetWinning = (e: React.FormEvent) => {
    e.preventDefault();
    const val = winNumInput.trim().padStart(2, '0');
    if (val.length === 2 && !isNaN(parseInt(val, 10))) {
      setWinningNumber(val);
      showSuccess(`ပေါက်ဂဏန်း ${val} သတ်မှတ်ပြီးပါပြီ`);
      setActiveModal(null);
    }
  };

  const handleSavePassword = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newPasswordInput.trim();
    if (!trimmed) {
      setPasswordError('စကားဝှက် အသစ် ရိုက်ထည့်ပါ');
      return;
    }
    if (trimmed !== confirmPasswordInput.trim()) {
      setPasswordError('စကားဝှက် နှစ်ခု မတူညီပါ');
      return;
    }
    updateSettings({ password: trimmed });
    soundManager.playSave();
    setPasswordError('');
    setNewPasswordInput('');
    setConfirmPasswordInput('');
    setActiveModal(null);
    showSuccess('စကားဝှက် အသစ် ပြောင်းလဲပြီးပါပြီ');
  };

  const handleSaveBrake = (e: React.FormEvent) => {
    e.preventDefault();
    const val = parseInt(brakeInput, 10);
    if (!isNaN(val) && val >= 0) {
      updateSettings({ brakeLimit: val });
      soundManager.playSave();
      showSuccess('ဘရိတ်သတ်မှတ်ချက် ပြင်ဆင်ပြီးပါပြီ');
      setActiveModal(null);
    }
  };

  const handleResetData = () => {
    if (resetConfirmText.toLowerCase() === 'reset') {
      resetAllData();
      setActiveModal(null);
      setResetConfirmText('');
      showSuccess('ဒေတာများအားလုံး မူလအတိုင်း ရှင်းလင်းပြီးပါပြီ');
    }
  };

  const dotDate = formatDotDate(selectedDate);
  const blockedList = Array.from(blocked);

  return (
    <div className="flex flex-col min-h-screen bg-[#F5F7F6] select-none">
      <Header title="ဆက်တင်" showBack showSessionPill={false} />

      <main className="flex-1 max-w-2xl w-full mx-auto px-4 pt-4 pb-8 flex flex-col gap-3">
        {successMsg && (
          <div className="bg-emerald-100 border border-emerald-400 text-[#065240] px-4 py-2.5 rounded-xl flex items-center gap-2 text-sm animate-in fade-in">
            <CheckCircle2 className="w-5 h-5 text-[#065240] flex-shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        <h2 className="text-[18px] font-medium text-[#065240] px-0.5 mb-0.5">
          ပင်မ သတ်မှတ်ချက်များ
        </h2>

        {/* 1. မရဂဏန်းများ */}
        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            setActiveModal('blocked');
          }}
          className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3 text-left active:scale-[0.99] transition"
        >
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#FDECEC] text-[#D93025] flex items-center justify-center shrink-0">
              <Ban className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">မရဂဏန်းများ</div>
              <div className="text-[14px] text-slate-500 truncate">
                {blockedList.length === 0
                  ? 'ပိတ်ထားသော ဂဏန်း မရှိသေးပါ'
                  : blockedList.join(', ')}
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-slate-500 shrink-0" />
        </button>

        {/* 2. အကြိမ်ရေ */}
        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            setActiveModal('sessions');
          }}
          className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3 text-left active:scale-[0.99] transition"
        >
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#E9F0ED] text-[#065240] flex items-center justify-center shrink-0">
              <Clock className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">အကြိမ်ရေ</div>
              <div className="text-[14px] text-slate-500 truncate">
                {selectedSession || 'အကြိမ်မရှိ'}
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-slate-500 shrink-0" />
        </button>

        {/* 3. ထွက်ဂဏန်းများ */}
        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            setWinNumInput(currentWinningNumber || '');
            setActiveModal('winning');
          }}
          className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3 text-left active:scale-[0.99] transition"
        >
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#FEF3E6] text-[#E67E00] flex items-center justify-center shrink-0">
              <Trophy className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">ထွက်ဂဏန်းများ</div>
              <div className="text-[14px] text-slate-500 truncate">
                {currentWinningNumber
                  ? `ပေါက်သီး: ${currentWinningNumber}`
                  : 'ပေါက်သီး ထည့်သွင်းရန်နှင့် မှတ်တမ်းကြည့်ရန်'}
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-slate-500 shrink-0" />
        </button>

        {/* 4. Change Password */}
        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            setPasswordError('');
            setNewPasswordInput('');
            setConfirmPasswordInput('');
            setActiveModal('password');
          }}
          className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3 text-left active:scale-[0.99] transition"
        >
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#FEF3E6] text-[#E67E00] flex items-center justify-center shrink-0">
              <Lock className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">Change Password</div>
              <div className="text-[14px] text-slate-500 truncate">
                လုံခြုံရေး စကားဝှက် အသစ် သတ်မှတ်ရန်
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-slate-500 shrink-0" />
        </button>

        {/* 5. Reset */}
        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            setResetConfirmText('');
            setActiveModal('reset');
          }}
          className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3 text-left active:scale-[0.99] transition"
        >
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#FDECEC] text-[#D93025] flex items-center justify-center shrink-0">
              <Trash2 className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">Reset</div>
              <div className="text-[14px] text-slate-500 truncate">
                ဘောင်ချာနှင့် မှတ်တမ်းအားလုံး အသစ်ပြန်စရန်
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-slate-500 shrink-0" />
        </button>

        {/* 6. ဘရိတ် သတ်မှတ်ချက် */}
        <button
          type="button"
          onClick={() => {
            soundManager.playClick();
            setBrakeInput(settings.brakeLimit.toString());
            setActiveModal('brake');
          }}
          className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3 text-left active:scale-[0.99] transition"
        >
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#EAF4EB] text-[#0A8819] flex items-center justify-center shrink-0">
              <Gauge className="w-5 h-5" />
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">ဘရိတ် သတ်မှတ်ချက်</div>
              <div className="text-[14px] text-slate-500 truncate">
                ဘရိတ်: {settings.brakeLimit}
              </div>
            </div>
          </div>
          <ChevronRight className="w-5 h-5 text-slate-500 shrink-0" />
        </button>

        {/* 7. အသံ (Sound) */}
        <div className="w-full bg-white rounded-2xl px-4 py-4 shadow-sm border border-[#E3EAE7] flex items-center justify-between gap-3">
          <div className="flex items-center gap-3.5 min-w-0">
            <div className="w-11 h-11 rounded-xl bg-[#E9F0ED] text-[#065240] flex items-center justify-center shrink-0">
              {settings.soundEnabled ? (
                <Volume2 className="w-5 h-5" />
              ) : (
                <VolumeX className="w-5 h-5 text-slate-400" />
              )}
            </div>
            <div className="min-w-0">
              <div className="text-[17px] font-medium text-slate-900">အသံ (Sound)</div>
              <div className="text-[14px] text-slate-500 truncate">
                {settings.soundEnabled ? 'အသံဖွင့်ထားသည်' : 'အသံပိတ်ထားသည်'}
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={handleToggleSound}
            aria-label="Toggle Sound"
            className={`w-14 h-8 rounded-full transition-colors relative p-1 shrink-0 ${
              settings.soundEnabled ? 'bg-[#065240]' : 'bg-slate-300'
            }`}
          >
            <div
              className={`w-6 h-6 rounded-full bg-white shadow transition-transform ${
                settings.soundEnabled ? 'translate-x-6' : 'translate-x-0'
              }`}
            />
          </button>
        </div>
      </main>

      {/* Winning Number Modal */}
      {activeModal === 'winning' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSetWinning}
            className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden flex flex-col"
          >
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">ပေါက်ဂဏန်း ထည့်သွင်းမည်</h3>
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-4 flex flex-col gap-3">
              <p className="text-xs text-gray-600">
                ရက်စွဲ: <strong className="text-gray-900">{dotDate}</strong> ({selectedSession || 'အကြိမ်မရှိ'})
              </p>
              <input
                type="text"
                maxLength={2}
                autoFocus
                placeholder="00"
                value={winNumInput}
                onChange={(e) => setWinNumInput(e.target.value.replace(/\D/g, ''))}
                className="w-full py-3 text-center text-3xl font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
              />
            </div>
            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-xl text-xs font-semibold"
              >
                မလုပ်ပါ
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-[#065240] text-white rounded-xl text-xs font-bold shadow"
              >
                အတည်ပြုမည်
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Change Password Modal */}
      {activeModal === 'password' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSavePassword}
            className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden flex flex-col"
          >
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">Change Password</h3>
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-4 flex flex-col gap-3">
              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">
                  စကားဝှက် အသစ်
                </label>
                <input
                  type="password"
                  value={newPasswordInput}
                  onChange={(e) => setNewPasswordInput(e.target.value)}
                  placeholder="စကားဝှက် အသစ် ရိုက်ထည့်ပါ"
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">
                  စကားဝှက် ထပ်မံအတည်ပြုပါ
                </label>
                <input
                  type="password"
                  value={confirmPasswordInput}
                  onChange={(e) => setConfirmPasswordInput(e.target.value)}
                  placeholder="စကားဝှက် ပြန်ရိုက်ထည့်ပါ"
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
                />
              </div>
              {passwordError && (
                <p className="text-xs text-[#F42222] font-medium">{passwordError}</p>
              )}
            </div>
            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-xl text-xs font-semibold"
              >
                မလုပ်ပါ
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-[#065240] text-white rounded-xl text-xs font-bold shadow"
              >
                သိမ်းဆည်းမည်
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Brake Modal */}
      {activeModal === 'brake' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSaveBrake}
            className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden flex flex-col"
          >
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">ဘရိတ် သတ်မှတ်ချက်</h3>
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-4 flex flex-col gap-3">
              <label className="text-xs text-gray-600">
                ဂဏန်းတစ်လုံးချင်းစီအတွက် မူလဘရိတ်
              </label>
              <input
                type="number"
                step="5000"
                min="0"
                value={brakeInput}
                onChange={(e) => setBrakeInput(e.target.value)}
                className="w-full py-2.5 px-3 text-center text-lg font-bold font-mono border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
              />
            </div>
            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 bg-gray-200 text-gray-700 rounded-xl text-xs font-semibold"
              >
                မလုပ်ပါ
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-[#065240] text-white rounded-xl text-xs font-bold shadow"
              >
                သိမ်းဆည်းမည်
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Blocked Numbers Modal */}
      {activeModal === 'blocked' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">မရဂဏန်းများ စီမံခြင်း</h3>
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-4 flex flex-col gap-3 flex-1 overflow-y-auto">
              <form onSubmit={handleAddBlocked} className="flex gap-2">
                <input
                  type="text"
                  maxLength={2}
                  placeholder="ဂဏန်း (ဥပမာ- 99)"
                  value={newBlockedNumber}
                  onChange={(e) => setNewBlockedNumber(e.target.value.replace(/\D/g, ''))}
                  className="flex-1 px-3 py-2 text-sm font-mono border border-gray-300 rounded-xl focus:outline-none focus:border-[#065240]"
                />
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#065240] text-white text-xs font-semibold rounded-xl flex items-center gap-1 shadow"
                >
                  <Plus className="w-4 h-4" /> ပိတ်မည်
                </button>
              </form>

              <div className="border border-gray-200 rounded-xl p-3 min-h-[140px] max-h-60 overflow-y-auto flex flex-wrap gap-2">
                {blockedList.length === 0 ? (
                  <p className="text-gray-400 text-xs text-center w-full my-auto">
                    ပိတ်ထားသော ဂဏန်း မရှိသေးပါ
                  </p>
                ) : (
                  blockedList.map((num) => (
                    <div
                      key={num}
                      className="flex items-center gap-1.5 bg-red-50 text-[#D93025] border border-red-200 px-2.5 py-1 rounded-lg text-xs font-mono font-bold"
                    >
                      <span>{num}</span>
                      <button
                        type="button"
                        onClick={() => toggleBlockedNumber(num)}
                        className="hover:text-red-900"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Sessions Management Modal */}
      {activeModal === 'sessions' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">အကြိမ်ရေ စီမံခြင်း</h3>
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3 flex-1 overflow-y-auto">
              <form
                onSubmit={handleAddSession}
                className="flex flex-col gap-2 bg-gray-50 p-3 rounded-xl border border-gray-200"
              >
                <input
                  type="text"
                  required
                  placeholder="အကြိမ်အမည် (ဥပမာ - a သို့မဟုတ် 12:01 PM)"
                  value={newSessionName}
                  onChange={(e) => setNewSessionName(e.target.value)}
                  className="px-3 py-2 text-xs border border-gray-300 rounded-lg focus:outline-none focus:border-[#065240]"
                />
                <div className="flex gap-2">
                  <input
                    type="number"
                    step="0.5"
                    placeholder="ကော်မရှင် %"
                    value={newSessionRate}
                    onChange={(e) => setNewSessionRate(e.target.value)}
                    className="flex-1 px-3 py-2 text-xs border border-gray-300 rounded-lg focus:outline-none focus:border-[#065240]"
                  />
                  <input
                    type="number"
                    placeholder="အဆ"
                    value={newSessionMult}
                    onChange={(e) => setNewSessionMult(e.target.value)}
                    className="flex-1 px-3 py-2 text-xs border border-gray-300 rounded-lg focus:outline-none focus:border-[#065240]"
                  />
                </div>
                <button
                  type="submit"
                  className="py-2 bg-[#065240] text-white text-xs font-semibold rounded-lg shadow"
                >
                  + အကြိမ်အသစ် ထည့်သွင်းမည်
                </button>
              </form>

              <div className="divide-y divide-gray-100 border border-gray-200 rounded-xl overflow-hidden">
                {sessions.length === 0 ? (
                  <div className="p-4 text-center text-xs text-gray-400">
                    အကြိမ်ရေ မရှိသေးပါ
                  </div>
                ) : (
                  sessions.map((s) => (
                    <div key={s.id} className="flex items-center justify-between p-3 text-xs">
                      <div>
                        <span className="font-bold text-gray-800">{s.name}</span>
                        <span className="text-gray-400 font-mono ml-2">
                          ({s.rate}% • x{s.mult})
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => deleteSessionRecord(s.id, s.name)}
                        className="text-gray-400 hover:text-[#F42222] p-1"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Master Reset Modal */}
      {activeModal === 'reset' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-gray-200 text-center flex flex-col gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-[#F42222] flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">
              ဒေတာများအားလုံး ဖျက်ဆီးမည်လား?
            </h4>
            <p className="text-xs text-gray-500">
              ဤလုပ်ဆောင်ချက်သည် ဘောင်ချာများ၊ ကော်မရှင်များ၊ တင်ကွက်များနှင့်
              ရက်ချုပ်မှတ်တမ်းများ အားလုံးကို အပြီးတိုင် ဖျက်ဆီးပါမည်။
            </p>
            <div className="text-left">
              <label className="text-[11px] font-semibold text-gray-600 block mb-1">
                အတည်ပြုရန် <strong className="text-[#F42222] font-mono">reset</strong> ဟု
                ရိုက်ထည့်ပါ:
              </label>
              <input
                type="text"
                value={resetConfirmText}
                onChange={(e) => setResetConfirmText(e.target.value)}
                placeholder="reset"
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:border-[#F42222]"
              />
            </div>
            <div className="flex gap-2 mt-2">
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="flex-1 py-2.5 bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မလုပ်ပါ
              </button>
              <button
                type="button"
                disabled={resetConfirmText.toLowerCase() !== 'reset'}
                onClick={handleResetData}
                className="flex-1 py-2.5 bg-[#F42222] hover:bg-red-700 disabled:opacity-40 text-white text-xs font-bold rounded-xl shadow"
              >
                ရှင်းလင်းမည်
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
