import React, { useState } from 'react';
import { useLottery, fmt, formatDotDate } from '../context/LotteryContext';
import { Header } from '../components/Header';
import {
  Volume2,
  VolumeX,
  ShieldCheck,
  Ban,
  Clock,
  Trophy,
  Sliders,
  RotateCcw,
  Key,
  X,
  Plus,
  Trash2,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';
import { getDeviceId, generateExpectedKey } from '../security/licenseManager';
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
    'blocked' | 'sessions' | 'winning' | 'brake' | 'reset' | 'license' | null
  >(null);

  // Blocked number input
  const [newBlockedNumber, setNewBlockedNumber] = useState('');

  // Session inputs
  const [newSessionName, setNewSessionName] = useState('');
  const [newSessionRate, setNewSessionRate] = useState('15');
  const [newSessionMult, setNewSessionMult] = useState('80');

  // Winning number input
  const [winNumInput, setWinNumInput] = useState('');

  // Brake input
  const [brakeInput, setBrakeInput] = useState(settings.brakeLimit.toString());

  // Reset confirmation input
  const [resetConfirmText, setResetConfirmText] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const [adminGenKey, setAdminGenKey] = useState<string | null>(null);

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

  const handleShowAdminKey = async () => {
    const devId = getDeviceId();
    const k = await generateExpectedKey(devId);
    setAdminGenKey(k);
  };

  const dotDate = formatDotDate(selectedDate);
  const blockedList = Array.from(blocked);

  return (
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ဆက်တင်များ" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        {successMsg && (
          <div className="bg-emerald-100 border border-emerald-400 text-emerald-800 px-4 py-2.5 rounded-xl flex items-center gap-2 text-sm animate-in fade-in">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 flex-shrink-0" />
            <span>{successMsg}</span>
          </div>
        )}

        <div className="bg-white rounded-2xl shadow-sm border border-emerald-900/10 overflow-hidden divide-y divide-gray-100">
          {/* Sound Toggle */}
          <div className="p-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-emerald-50 rounded-xl text-[#235347]">
                {settings.soundEnabled ? (
                  <Volume2 className="w-5 h-5" />
                ) : (
                  <VolumeX className="w-5 h-5 text-gray-400" />
                )}
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">အသံဖွင့်/ပိတ်</h4>
                <p className="text-xs text-gray-500">
                  ကီးနှိပ်သံ၊ အောင်မြင်သံနှင့် ဖျက်သံများကို ဖွင့်/ပိတ်မည်
                </p>
              </div>
            </div>

            <button
              onClick={handleToggleSound}
              className={`w-12 h-6 rounded-full transition-colors relative p-0.5 ${
                settings.soundEnabled ? 'bg-[#235347]' : 'bg-gray-300'
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white shadow transition-transform ${
                  settings.soundEnabled ? 'translate-x-6' : 'translate-x-0'
                }`}
              />
            </button>
          </div>

          {/* Winning Number Entry for Round */}
          <div
            onClick={() => {
              soundManager.playClick();
              setWinNumInput(currentWinningNumber || '');
              setActiveModal('winning');
            }}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition"
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-amber-50 rounded-xl text-amber-600">
                <Trophy className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">ပေါက်ဂဏန်း သတ်မှတ်ခြင်း</h4>
                <p className="text-xs text-gray-500">
                  {dotDate} ({selectedSession || 'အပိုင်းမရွေးရသေး'}) : လက်ရှိပေါက်ဂဏန်း [
                  <span className="font-bold text-red-600 font-mono mx-1">
                    {currentWinningNumber || 'မရှိသေး'}
                  </span>
                  ]
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-[#235347]">ထည့်မည် →</span>
          </div>

          {/* Default Brake Setting */}
          <div
            onClick={() => {
              soundManager.playClick();
              setBrakeInput(settings.brakeLimit.toString());
              setActiveModal('brake');
            }}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition"
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-red-50 rounded-xl text-red-600">
                <Sliders className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">မူလ ဘရိတ်သတ်မှတ်ငွေ</h4>
                <p className="text-xs text-gray-500">
                  လက်ရှိသတ်မှတ်ချက်: {fmt(settings.brakeLimit)} Ks
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-[#235347]">ပြင်မည် →</span>
          </div>

          {/* Blocked Numbers */}
          <div
            onClick={() => {
              soundManager.playClick();
              setActiveModal('blocked');
            }}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition"
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-red-50 rounded-xl text-red-600">
                <Ban className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">ပိတ်ဂဏန်းများ သတ်မှတ်ခြင်း</h4>
                <p className="text-xs text-gray-500">
                  ပိတ်ထားသော ဂဏန်းပေါင်း {blockedList.length} လုံး
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-[#235347]">ကြည့်မည် →</span>
          </div>

          {/* Sessions / Rounds Management */}
          <div
            onClick={() => {
              soundManager.playClick();
              setActiveModal('sessions');
            }}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition"
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-emerald-50 rounded-xl text-[#235347]">
                <Clock className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">အပိုင်းများ (Sessions/Rounds)</h4>
                <p className="text-xs text-gray-500">
                  အချိန်ပိုင်း {sessions.length} ခု သတ်မှတ်ထားသည်
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-[#235347]">စီမံမည် →</span>
          </div>

          {/* License & Device Info */}
          <div
            onClick={() => {
              soundManager.playClick();
              setActiveModal('license');
            }}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition"
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-emerald-50 rounded-xl text-[#235347]">
                <Key className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-semibold text-gray-900 text-sm">စက်ပစ္စည်း လိုင်စင်နှင့် သော့</h4>
                <p className="text-xs text-gray-500">
                  လိုင်စင်အခြေအနေ: <span className="text-emerald-700 font-bold">ဖွင့်လှစ်ထားပြီး (Active)</span>
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-[#235347]">အသေးစိတ် →</span>
          </div>

          {/* Master Reset */}
          <div
            onClick={() => {
              soundManager.playClick();
              setResetConfirmText('');
              setActiveModal('reset');
            }}
            className="p-4 flex items-center justify-between cursor-pointer hover:bg-red-50/40 transition"
          >
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-red-50 rounded-xl text-red-600">
                <RotateCcw className="w-5 h-5" />
              </div>
              <div>
                <h4 className="font-semibold text-red-700 text-sm">ဒေတာများအားလုံး ရှင်းလင်းမည်</h4>
                <p className="text-xs text-gray-500">
                  ဘောင်ချာများ၊ ကိုယ်စားလှယ်များနှင့် မှတ်တမ်းများအားလုံး ဖျက်မည်
                </p>
              </div>
            </div>
            <span className="text-xs font-semibold text-red-600">ရှင်းလင်းမည် →</span>
          </div>
        </div>
      </main>

      {/* Winning Number Modal */}
      {activeModal === 'winning' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSetWinning}
            className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden flex flex-col"
          >
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
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
                ရက်စွဲ: <strong className="text-gray-900">{dotDate}</strong> ({selectedSession})
              </p>
              <input
                type="text"
                maxLength={2}
                autoFocus
                placeholder="00"
                value={winNumInput}
                onChange={(e) => setWinNumInput(e.target.value.replace(/\D/g, ''))}
                className="w-full py-3 text-center text-3xl font-bold font-mono border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#235347]"
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
                className="px-4 py-2 bg-[#235347] text-white rounded-xl text-xs font-bold shadow"
              >
                အတည်ပြုမည်
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
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">မူလ ဘရိတ်သတ်မှတ်ငွေ ပြင်ဆင်ခြင်း</h3>
              <button
                type="button"
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-4 flex flex-col gap-3">
              <label className="text-xs text-gray-600">ဂဏန်းတစ်လုံးချင်းစီအတွက် မူလဘရိတ် (Ks)</label>
              <input
                type="number"
                step="5000"
                min="0"
                value={brakeInput}
                onChange={(e) => setBrakeInput(e.target.value)}
                className="w-full py-2 px-3 text-center text-lg font-bold font-mono border border-gray-300 rounded-xl"
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
                className="px-4 py-2 bg-[#235347] text-white rounded-xl text-xs font-bold shadow"
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
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">ပိတ်ဂဏန်းများ စီမံခြင်း</h3>
              <button
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
                  className="flex-1 px-3 py-2 text-sm font-mono border border-gray-300 rounded-xl focus:ring-2 focus:ring-[#235347]"
                />
                <button
                  type="submit"
                  className="px-4 py-2 bg-[#235347] text-white text-xs font-semibold rounded-xl flex items-center gap-1 shadow"
                >
                  <Plus className="w-4 h-4" /> ပိတ်မည်
                </button>
              </form>

              <div className="border border-gray-200 rounded-xl p-3 min-h-[140px] max-h-60 overflow-y-auto flex flex-wrap gap-2">
                {blockedList.length === 0 ? (
                  <p className="text-gray-400 text-xs text-center w-full my-auto">
                    ပိတ်ထားသော ဂဏန်းမရှိပါ
                  </p>
                ) : (
                  blockedList.map((num) => (
                    <div
                      key={num}
                      className="flex items-center gap-1.5 bg-red-50 text-red-700 border border-red-200 px-2.5 py-1 rounded-lg text-xs font-mono font-bold"
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
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">အပိုင်းများ (Sessions) စီမံခြင်း</h3>
              <button
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3 flex-1 overflow-y-auto">
              <form onSubmit={handleAddSession} className="flex flex-col gap-2 bg-gray-50 p-3 rounded-xl border border-gray-200">
                <input
                  type="text"
                  required
                  placeholder="အပိုင်းအမည် (ဥပမာ - 04:30 PM)"
                  value={newSessionName}
                  onChange={(e) => setNewSessionName(e.target.value)}
                  className="px-3 py-1.5 text-xs border border-gray-300 rounded-lg"
                />
                <div className="flex gap-2">
                  <input
                    type="number"
                    step="0.5"
                    placeholder="ကော်မရှင် %"
                    value={newSessionRate}
                    onChange={(e) => setNewSessionRate(e.target.value)}
                    className="flex-1 px-3 py-1.5 text-xs border border-gray-300 rounded-lg"
                  />
                  <input
                    type="number"
                    placeholder="အမြှောက်နှုန်း ဆ"
                    value={newSessionMult}
                    onChange={(e) => setNewSessionMult(e.target.value)}
                    className="flex-1 px-3 py-1.5 text-xs border border-gray-300 rounded-lg"
                  />
                </div>
                <button
                  type="submit"
                  className="py-1.5 bg-[#235347] text-white text-xs font-semibold rounded-lg shadow"
                >
                  + အပိုင်းအသစ် ထည့်သွင်းမည်
                </button>
              </form>

              <div className="divide-y divide-gray-100 border border-gray-200 rounded-xl overflow-hidden">
                {sessions.map((s) => (
                  <div key={s.id} className="flex items-center justify-between p-3 text-xs">
                    <div>
                      <span className="font-bold text-gray-800">{s.name}</span>
                      <span className="text-gray-400 font-mono ml-2">
                        ({s.rate}% • x{s.mult})
                      </span>
                    </div>
                    {sessions.length > 1 && (
                      <button
                        onClick={() => deleteSessionRecord(s.id, s.name)}
                        className="text-gray-400 hover:text-red-600 p-1"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
                onClick={() => setActiveModal(null)}
                className="px-4 py-2 bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl"
              >
                ပိတ်မည်
              </button>
            </div>
          </div>
        </div>
      )}

      {/* License Modal */}
      {activeModal === 'license' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden flex flex-col">
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-emerald-300" />
                <h3 className="font-bold text-sm">လိုင်စင် အချက်အလက်</h3>
              </div>
              <button
                onClick={() => setActiveModal(null)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3 text-xs">
              <div className="bg-gray-50 p-3 rounded-xl border border-gray-200">
                <span className="text-gray-500 block text-[11px]">စက်ပစ္စည်း ID</span>
                <span className="font-bold font-mono text-gray-800 text-sm select-all">
                  {getDeviceId()}
                </span>
              </div>

              <div className="bg-emerald-50 text-[#235347] p-2.5 rounded-xl border border-emerald-200 flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                <span>လိုင်စင် အောင်မြင်စွာ စစ်ဆေးပြီးဖြစ်ပါသည်။</span>
              </div>

              {/* Admin key generator toggle */}
              <div className="pt-2 border-t border-gray-100">
                <button
                  type="button"
                  onClick={handleShowAdminKey}
                  className="text-[11px] text-[#235347] hover:underline"
                >
                  Admin Key ကြည့်ရန် (Offline Generator)
                </button>
                {adminGenKey && (
                  <div className="mt-1 font-mono font-bold text-gray-800 bg-gray-100 p-2 rounded-lg text-center">
                    {adminGenKey}
                  </div>
                )}
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex justify-end">
              <button
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
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">ဒေတာများအားလုံး ဖျက်ဆီးမည်လား?</h4>
            <p className="text-xs text-gray-500">
              ဤလုပ်ဆောင်ချက်သည် ဘောင်ချာများ၊ ကိုယ်စားလှယ်များ၊ တင်ကွက်များနှင့် ရက်ချုပ်မှတ်တမ်းများ အားလုံးကို အပြီးတိုင် ဖျက်ဆီးပါမည်။
            </p>
            <div className="text-left">
              <label className="text-[11px] font-semibold text-gray-600 block mb-1">
                အတည်ပြုရန် <strong className="text-red-600 font-mono">reset</strong> ဟု ရိုက်ထည့်ပါ:
              </label>
              <input
                type="text"
                value={resetConfirmText}
                onChange={(e) => setResetConfirmText(e.target.value)}
                placeholder="reset"
                className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:ring-2 focus:ring-red-500"
              />
            </div>
            <div className="flex gap-2 mt-2">
              <button
                onClick={() => setActiveModal(null)}
                className="flex-1 py-2 bg-gray-200 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မလုပ်ပါ
              </button>
              <button
                disabled={resetConfirmText.toLowerCase() !== 'reset'}
                onClick={handleResetData}
                className="flex-1 py-2 bg-red-600 hover:bg-red-700 disabled:opacity-40 text-white text-xs font-bold rounded-xl shadow"
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
