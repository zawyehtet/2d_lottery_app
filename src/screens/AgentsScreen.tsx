import React, { useState, useMemo } from 'react';
import { useLottery } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import {
  Users,
  Edit2,
  Trash2,
  Plus,
  Search,
  X,
  Trophy,
} from 'lucide-react';
import { Agent } from '../types';
import { soundManager } from '../util/soundManager';

export const AgentsScreen: React.FC = () => {
  const {
    agents,
    addAgent,
    updateAgent,
    deleteAgent,
    startNumberEntry,
    openAgentVouchers,
    getAgentSummaryData,
    currentWinningNumber,
  } = useLottery();

  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingAgent, setEditingAgent] = useState<Agent | null>(null);
  const [agentToDelete, setAgentToDelete] = useState<Agent | null>(null);

  // Form state
  const [nameInput, setNameInput] = useState('');
  const [rateInput, setRateInput] = useState('15');
  const [multInput, setMultInput] = useState('80');
  const [phoneInput, setPhoneInput] = useState('');

  // Filtered agents
  const filteredAgents = useMemo(() => {
    return agents.filter((agent) => {
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase().trim();
      return (
        agent.name.toLowerCase().includes(q) ||
        (agent.phoneNumber || '').toLowerCase().includes(q)
      );
    });
  }, [agents, searchQuery]);

  const openAddModal = () => {
    soundManager.playClick();
    setNameInput('');
    setRateInput('15');
    setMultInput('80');
    setPhoneInput('');
    setEditingAgent(null);
    setShowAddModal(true);
  };

  const openEditModal = (agent: Agent) => {
    soundManager.playClick();
    setNameInput(agent.name);
    setRateInput(agent.rate.toString());
    setMultInput(agent.mult.toString());
    setPhoneInput(agent.phoneNumber || '');
    setEditingAgent(agent);
    setShowAddModal(true);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nameInput.trim()) return;

    const rate = parseFloat(rateInput) || 0;
    const mult = parseInt(multInput, 10) || 80;
    const phoneNumber = phoneInput.trim();

    if (editingAgent) {
      updateAgent({
        ...editingAgent,
        name: nameInput.trim(),
        rate,
        mult,
        phoneNumber,
      });
    } else {
      addAgent(nameInput.trim(), rate, mult, phoneNumber);
    }

    setShowAddModal(false);
  };

  const handleDelete = () => {
    if (!agentToDelete) return;
    deleteAgent(agentToDelete.id);
    setAgentToDelete(null);
  };

  const handleStartBet = (agent: Agent) => {
    soundManager.playClick();
    startNumberEntry(agent);
  };

  return (
    <div className="flex flex-col min-h-screen bg-[#F5F7F6] pb-24">
      <Header title="ကော်မရှင်" showBack />

      <main className="flex-1 max-w-2xl w-full mx-auto px-3.5 pt-3.5 flex flex-col gap-3.5">
        <RoundRequiredHelperBanner />

        {/* Search Input */}
        <div className="relative w-full">
          <Search className="w-5 h-5 absolute left-4 top-1/2 -translate-y-1/2 text-slate-800" />
          <input
            type="text"
            placeholder="ကော်မရှင် ရှာရန်"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-12 pr-4 py-3.5 text-base bg-white border border-slate-300 rounded-2xl text-slate-900 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-[#065240]"
          />
        </div>

        {/* Agents List Cards */}
        {filteredAgents.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center border border-slate-200 shadow-sm flex flex-col items-center justify-center gap-3">
            <div className="p-3 bg-emerald-50 text-[#065240] rounded-full">
              <Users className="w-8 h-8" />
            </div>
            <p className="text-slate-500 text-sm">ကော်မရှင် မရှိသေးပါ</p>
            <button
              type="button"
              onClick={openAddModal}
              className="text-xs font-semibold text-[#065240] hover:underline"
            >
              + ကော်မရှင်အသစ် ထည့်သွင်းပါ
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-3.5">
            {filteredAgents.map((agent, index) => {
              const summary = getAgentSummaryData(agent, currentWinningNumber);

              return (
                <div
                  key={agent.id}
                  className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden"
                >
                  {/* Dark-Green Card Header */}
                  <div className="bg-[#065240] text-white px-4 py-3 flex items-center justify-between gap-2">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="w-10 h-10 rounded-full bg-white text-[#065240] font-medium flex items-center justify-center text-base shrink-0">
                        {index + 1}
                      </div>
                      <div className="min-w-0">
                        <h3 className="font-medium text-white text-lg leading-tight truncate">
                          {agent.name}
                        </h3>
                        <div className="text-xs text-white/90 mt-0.5">
                          ကော်: {agent.rate}% | အဆ: {agent.mult}
                        </div>
                        {(agent.phoneNumber || '').trim() !== '' && (
                          <div className="text-xs text-emerald-100 mt-0.5 font-mono truncate">
                            📞 {(agent.phoneNumber || '').trim()}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={() => openEditModal(agent)}
                        className="p-1.5 text-white/80 hover:text-white hover:bg-white/15 rounded-lg transition"
                        title="ပြင်မည်"
                      >
                        <Edit2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          soundManager.playClick();
                          setAgentToDelete(agent);
                        }}
                        className="p-1.5 text-white/80 hover:text-red-200 hover:bg-white/15 rounded-lg transition"
                        title="ဖျက်မည်"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <button
                        type="button"
                        onClick={() => handleStartBet(agent)}
                        className="ml-1 px-3.5 py-2 bg-white hover:bg-emerald-50 text-[#065240] text-sm font-bold rounded-xl shadow-sm flex items-center gap-1 transition cursor-pointer"
                      >
                        <Plus className="w-4 h-4" strokeWidth={2.5} />
                        <span>ထိုးရန်</span>
                      </button>
                    </div>
                  </div>

                  {/* White Card Body */}
                  <div className="px-4 py-3.5">
                    <div className="grid grid-cols-3 gap-2 pb-3 border-b border-slate-100">
                      <div>
                        <span className="text-slate-600 text-xs block">စုစုပေါင်းထိုးကြေး</span>
                        <span className="font-medium text-slate-900 text-lg mt-1 block">
                          {summary.totalBet}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-600 text-xs block">
                          ကော်မရှင် ({agent.rate}%)
                        </span>
                        <span className="font-medium text-slate-900 text-lg mt-1 block">
                          {summary.commission}
                        </span>
                      </div>
                      <div>
                        <span className="text-slate-600 text-xs block">ကော်နုတ်ပြီး</span>
                        <span className="font-medium text-[#065240] text-lg mt-1 block">
                          {summary.netAmount}
                        </span>
                      </div>
                    </div>

                    {summary.payout > 0 && (
                      <div className="py-2 border-b border-slate-100 flex items-center justify-between text-red-600 text-sm">
                        <span className="font-semibold flex items-center gap-1">
                          <Trophy className="w-4 h-4" /> ပေါက်ကြေး
                        </span>
                        <span className="font-bold">{summary.payout}</span>
                      </div>
                    )}

                    <div className="pt-3">
                      <button
                        type="button"
                        onClick={() => {
                          soundManager.playClick();
                          openAgentVouchers(agent.id, agent.name);
                        }}
                        className="px-4 py-2 rounded-xl bg-[#EDF3F0] hover:bg-[#E1ECE7] border border-[#CBD8D2] text-[#065240] font-medium text-sm transition cursor-pointer"
                      >
                        ဘောင်ချာ = {summary.voucherCount}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Floating Circular Dark-Green + Button */}
      <button
        type="button"
        onClick={openAddModal}
        className="fixed bottom-6 right-5 z-30 w-14 h-14 rounded-full bg-[#065240] hover:bg-[#054435] active:scale-95 text-white shadow-[0_6px_16px_rgba(6,82,64,0.35)] flex items-center justify-center transition cursor-pointer"
        title="ကော်မရှင်အသစ်"
      >
        <Plus className="w-7 h-7" strokeWidth={2.5} />
      </button>

      {/* Add / Edit Agent Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <form
            onSubmit={handleSave}
            className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col"
          >
            <div className="bg-[#065240] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">
                {editingAgent ? 'ကော်မရှင် အချက်အလက်ပြင်ဆင်မည်' : 'ကော်မရှင် အသစ်ထည့်သွင်းမည်'}
              </h3>
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="text-white/80 hover:text-white p-1"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-3">
              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">
                  ကော်မရှင် အမည် <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="ဥပမာ - ကိုကျော်"
                  value={nameInput}
                  onChange={(e) => setNameInput(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#065240]"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">
                  ကော်မရှင် ရာခိုင်နှုန်း (%)
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    max="100"
                    value={rateInput}
                    onChange={(e) => setRateInput(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#065240]"
                  />
                  <span className="text-sm font-bold text-gray-600">%</span>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">
                  ပေါက်ကြေး အမြှောက်နှုန်း (ဆ)
                </label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min="1"
                    max="200"
                    value={multInput}
                    onChange={(e) => setMultInput(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#065240]"
                  />
                  <span className="text-sm font-bold text-gray-600">ဆ</span>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">
                  ဖုန်းနံပါတ် (Optional)
                </label>
                <input
                  type="tel"
                  placeholder="09xxxxxxxxx"
                  value={phoneInput}
                  onChange={(e) => setPhoneInput(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#065240]"
                />
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-xl text-xs font-semibold"
              >
                မလုပ်တော့ပါ
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-[#065240] hover:bg-[#054435] text-white rounded-xl text-xs font-bold transition shadow"
              >
                သိမ်းဆည်းမည်
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {agentToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-gray-200 text-center flex flex-col gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">ဖျက်ရန် သေချာပါသလား?</h4>
            <p className="text-xs text-gray-500">
              ကော်မရှင် &apos;{agentToDelete.name}&apos; အား ဖျက်မည် ဖြစ်သည်။
            </p>
            <div className="flex gap-2 mt-2">
              <button
                type="button"
                onClick={() => setAgentToDelete(null)}
                className="flex-1 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မဖျက်ပါ
              </button>
              <button
                type="button"
                onClick={handleDelete}
                className="flex-1 py-2 bg-[#F42222] hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition"
              >
                ဖျက်မည်
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
