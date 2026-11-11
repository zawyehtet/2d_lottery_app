import React, { useState, useMemo } from 'react';
import { useLottery, fmt } from '../context/LotteryContext';
import { Header } from '../components/Header';
import { RoundRequiredHelperBanner } from '../components/RoundRequiredHelperBanner';
import {
  Users,
  UserPlus,
  Edit2,
  Trash2,
  PlusCircle,
  Search,
  Percent,
  X,
  Receipt,
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
    getAgentSummaryData,
    currentWinningNumber,
    selectedDate,
    selectedSession,
  } = useLottery();

  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingAgent, setEditingAgent] = useState<Agent | null>(null);
  const [agentToDelete, setAgentToDelete] = useState<Agent | null>(null);

  // Form state
  const [nameInput, setNameInput] = useState('');
  const [rateInput, setRateInput] = useState('15');
  const [multInput, setMultInput] = useState('80');

  // Filtered agents
  const filteredAgents = useMemo(() => {
    return agents.filter((agent) => {
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase().trim();
      return agent.name.toLowerCase().includes(q);
    });
  }, [agents, searchQuery]);

  const openAddModal = () => {
    soundManager.playClick();
    setNameInput('');
    setRateInput('15');
    setMultInput('80');
    setEditingAgent(null);
    setShowAddModal(true);
  };

  const openEditModal = (agent: Agent) => {
    soundManager.playClick();
    setNameInput(agent.name);
    setRateInput(agent.rate.toString());
    setMultInput(agent.mult.toString());
    setEditingAgent(agent);
    setShowAddModal(true);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (!nameInput.trim()) return;

    const rate = parseFloat(rateInput) || 0;
    const mult = parseInt(multInput, 10) || 80;

    if (editingAgent) {
      updateAgent({
        ...editingAgent,
        name: nameInput.trim(),
        rate,
        mult,
      });
    } else {
      addAgent(nameInput.trim(), rate, mult);
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
    <div className="flex flex-col min-h-screen bg-[#F4F7F5]">
      <Header title="ကိုယ်စားလှယ်များ" showBack />

      <main className="flex-1 max-w-4xl w-full mx-auto p-4 flex flex-col gap-4">
        <RoundRequiredHelperBanner />

        {/* Top Controls: Search + Add Button */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="relative flex-1 w-full">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="ကိုယ်စားလှယ် အမည်ရှာရန်..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 text-sm bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347]"
            />
          </div>

          <button
            onClick={openAddModal}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2.5 bg-[#235347] hover:bg-[#1b4339] text-white text-xs font-semibold rounded-xl shadow transition"
          >
            <UserPlus className="w-4 h-4" />
            <span>ကိုယ်စားလှယ်အသစ်</span>
          </button>
        </div>

        {/* Agents List Cards */}
        {filteredAgents.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center border border-gray-200/80 shadow-sm flex flex-col items-center justify-center gap-3">
            <div className="p-3 bg-emerald-50 text-[#235347] rounded-full">
              <Users className="w-8 h-8" />
            </div>
            <p className="text-gray-500 text-sm">ကိုယ်စားလှယ် မရှိသေးပါ</p>
            <button
              onClick={openAddModal}
              className="text-xs font-semibold text-[#235347] hover:underline"
            >
              + ကိုယ်စားလှယ်အသစ် ထည့်သွင်းပါ
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredAgents.map((agent) => {
              const summary = getAgentSummaryData(agent, currentWinningNumber);

              return (
                <div
                  key={agent.id}
                  className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col justify-between hover:shadow transition"
                >
                  {/* Card Header */}
                  <div>
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="w-10 h-10 rounded-full bg-emerald-100/70 text-[#235347] font-bold flex items-center justify-center text-sm shadow-inner">
                          {agent.name.slice(0, 1).toUpperCase()}
                        </div>
                        <div>
                          <h3 className="font-bold text-gray-900 text-sm">{agent.name}</h3>
                          <div className="flex items-center gap-2 text-xs text-gray-500">
                            <span className="flex items-center gap-0.5 text-emerald-700 font-semibold">
                              <Percent className="w-3 h-3" />
                              {agent.rate}%
                            </span>
                            <span>•</span>
                            <span className="text-gray-600">x{agent.mult} ဆ</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => openEditModal(agent)}
                          className="p-1.5 text-gray-400 hover:text-[#235347] hover:bg-emerald-50 rounded-lg transition"
                          title="ပြင်မည်"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => {
                            soundManager.playClick();
                            setAgentToDelete(agent);
                          }}
                          className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                          title="ဖျက်မည်"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {/* Stats Grid */}
                    <div className="grid grid-cols-2 gap-2 mt-3 text-xs bg-gray-50 p-2.5 rounded-xl border border-gray-100">
                      <div>
                        <span className="text-gray-500 text-[11px] block">စုစုပေါင်းရောင်းကြေး</span>
                        <span className="font-bold font-mono text-[#235347] text-sm">
                          {fmt(summary.totalBet)} Ks
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 text-[11px] block">ကော်မရှင်ငွေ</span>
                        <span className="font-bold font-mono text-amber-700 text-sm">
                          {fmt(summary.commission)} Ks
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 text-[11px] block">ကျန်ငွေ / ပေးရန်</span>
                        <span className="font-bold font-mono text-blue-700 text-sm">
                          {fmt(summary.netAmount)} Ks
                        </span>
                      </div>
                      <div>
                        <span className="text-gray-500 text-[11px] block flex items-center gap-1">
                          <Receipt className="w-3 h-3" /> ဘောင်ချာ
                        </span>
                        <span className="font-bold font-mono text-gray-800 text-sm">
                          {summary.voucherCount} စောင်
                        </span>
                      </div>

                      {summary.payout > 0 && (
                        <div className="col-span-2 pt-1 border-t border-gray-200 flex items-center justify-between text-red-600">
                          <span className="font-semibold flex items-center gap-1">
                            <Trophy className="w-3.5 h-3.5" /> ပေါက်ကြေး
                          </span>
                          <span className="font-bold font-mono text-sm">
                            {fmt(summary.payout)} Ks
                          </span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Start Bet Action Button */}
                  <div className="mt-3 pt-3 border-t border-gray-100">
                    <button
                      onClick={() => handleStartBet(agent)}
                      className="w-full py-2 bg-[#235347] hover:bg-[#1b4339] text-white text-xs font-semibold rounded-xl flex items-center justify-center gap-1.5 transition shadow-sm"
                    >
                      <PlusCircle className="w-4 h-4" />
                      <span>စာရင်းစတင်ထည့်မည်</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </main>

      {/* Add / Edit Agent Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <form
            onSubmit={handleSave}
            className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col"
          >
            <div className="bg-[#235347] text-white p-4 flex items-center justify-between">
              <h3 className="font-bold text-sm">
                {editingAgent ? 'ကိုယ်စားလှယ် အချက်အလက်ပြင်ဆင်မည်' : 'ကိုယ်စားလှယ် အသစ်ထည့်သွင်းမည်'}
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
                  ကိုယ်စားလှယ် အမည် <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="ဥပမာ - ကိုကျော်"
                  value={nameInput}
                  onChange={(e) => setNameInput(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347]"
                />
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">ကော်မရှင် ရာခိုင်နှုန်း (%)</label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    step="0.5"
                    min="0"
                    max="100"
                    value={rateInput}
                    onChange={(e) => setRateInput(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347]"
                  />
                  <span className="text-sm font-bold text-gray-600">%</span>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold text-gray-700 block mb-1">ပေါက်ကြေး အမြှောက်နှုန်း (ဆ)</label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min="1"
                    max="200"
                    value={multInput}
                    onChange={(e) => setMultInput(e.target.value)}
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347]"
                  />
                  <span className="text-sm font-bold text-gray-600">ဆ</span>
                </div>
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
                className="px-4 py-2 bg-[#235347] hover:bg-[#1b4339] text-white rounded-xl text-xs font-bold transition shadow"
              >
                သိမ်းဆည်းမည်
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {agentToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full p-5 shadow-2xl border border-gray-200 text-center flex flex-col gap-3">
            <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center mx-auto">
              <Trash2 className="w-6 h-6" />
            </div>
            <h4 className="font-bold text-gray-900 text-base">ဖျက်ရန် သေချာပါသလား?</h4>
            <p className="text-xs text-gray-500">
              ကိုယ်စားလှယ် &apos;{agentToDelete.name}&apos; အား ဖျက်မည် ဖြစ်သည်။
            </p>
            <div className="flex gap-2 mt-2">
              <button
                onClick={() => setAgentToDelete(null)}
                className="flex-1 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 text-xs font-semibold rounded-xl"
              >
                မဖျက်ပါ
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition"
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
