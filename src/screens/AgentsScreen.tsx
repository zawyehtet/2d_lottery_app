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
import { Agent, AgentSummaryData } from '../types';
import { soundManager } from '../util/soundManager';
import { useLongPress } from '../hooks/useLongPress';

interface AgentCardProps {
  agent: Agent;
  summary: AgentSummaryData;
  onEdit: (agent: Agent) => void;
  onDelete: (agent: Agent) => void;
  onStartBet: (agent: Agent) => void;
  onLongPress: (agent: Agent) => void;
}

const AgentCard: React.FC<AgentCardProps> = ({
  agent,
  summary,
  onEdit,
  onDelete,
  onStartBet,
  onLongPress,
}) => {
  const longPressHandlers = useLongPress(() => {
    soundManager.playClick();
    onLongPress(agent);
  });

  return (
    <div
      {...longPressHandlers}
      className="bg-white rounded-2xl p-4 shadow-sm border border-emerald-900/10 flex flex-col justify-between hover:shadow transition"
    >
      {/* Card Header */}
      <div>
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-10 h-10 rounded-full bg-emerald-100/70 text-[#235347] font-bold flex items-center justify-center text-sm shadow-inner shrink-0">
              {agent.name.slice(0, 1).toUpperCase()}
            </div>
            <div className="min-w-0">
              <h3 className="font-bold text-gray-900 text-sm truncate">{agent.name}</h3>
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span className="flex items-center gap-0.5 text-emerald-700 font-semibold whitespace-nowrap">
                  <Percent className="w-3 h-3" />
                  {agent.rate}%
                </span>
                <span>•</span>
                <span className="text-gray-600 whitespace-nowrap">x{agent.mult} ဆ</span>
              </div>
              {agent.phoneNumber && agent.phoneNumber.trim() !== '' && (
                <div className="text-xs text-gray-600 font-mono mt-0.5 truncate">
                  📞 {agent.phoneNumber.trim()}
                </div>
              )}
            </div>
          </div>

          <div
            className="flex items-center gap-1 shrink-0"
            onTouchStart={(e) => e.stopPropagation()}
            onMouseDown={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              onClick={() => onEdit(agent)}
              className="p-1.5 text-gray-400 hover:text-[#235347] hover:bg-emerald-50 rounded-lg transition cursor-pointer"
              title="ပြင်မည်"
            >
              <Edit2 className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={() => {
                soundManager.playClick();
                onDelete(agent);
              }}
              className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition cursor-pointer"
              title="ဖျက်မည်"
            >
              <Trash2 className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 gap-2 mt-3 text-xs bg-gray-50 p-2.5 rounded-xl border border-gray-100">
          <div className="min-w-0">
            <span className="text-gray-500 text-[11px] block truncate">စုစုပေါင်းရောင်းကြေး</span>
            <span className="font-bold font-mono tabular-nums text-[#235347] text-sm">
              {fmt(summary.totalBet)}
            </span>
          </div>
          <div className="min-w-0">
            <span className="text-gray-500 text-[11px] block truncate">ကော်မရှင်ငွေ</span>
            <span className="font-bold font-mono tabular-nums text-amber-700 text-sm">
              {fmt(summary.commission)}
            </span>
          </div>
          <div className="min-w-0">
            <span className="text-gray-500 text-[11px] block truncate">ကျန်ငွေ / ပေးရန်</span>
            <span className="font-bold font-mono tabular-nums text-blue-700 text-sm">
              {fmt(summary.netAmount)}
            </span>
          </div>
          <div className="min-w-0">
            <span className="text-gray-500 text-[11px] flex items-center gap-1 truncate">
              <Receipt className="w-3 h-3 shrink-0" /> ဘောင်ချာ
            </span>
            <span className="font-bold font-mono tabular-nums text-gray-800 text-sm">
              {summary.voucherCount} စောင်
            </span>
          </div>

          {summary.payout > 0 && (
            <div className="col-span-2 pt-1.5 border-t border-gray-200 flex items-center justify-between text-red-600">
              <span className="font-semibold flex items-center gap-1">
                <Trophy className="w-3.5 h-3.5" /> ပေါက်ကြေး
              </span>
              <span className="font-bold font-mono tabular-nums text-sm">
                {fmt(summary.payout)}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Start Bet Action Button */}
      <div
        className="mt-3 pt-3 border-t border-gray-100"
        onTouchStart={(e) => e.stopPropagation()}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={() => onStartBet(agent)}
          className="w-full py-2.5 bg-[#235347] hover:bg-[#1b4339] text-white text-xs font-semibold rounded-xl flex items-center justify-center gap-1.5 transition shadow-sm whitespace-nowrap cursor-pointer"
        >
          <PlusCircle className="w-4 h-4 shrink-0" />
          <span>စာရင်းစတင်ထည့်မည်</span>
        </button>
      </div>
    </div>
  );
};

export const AgentsScreen: React.FC = () => {
  const {
    agents,
    addAgent,
    updateAgent,
    deleteAgent,
    startNumberEntry,
    getAgentSummaryData,
    currentWinningNumber,
  } = useLottery();

  const [searchQuery, setSearchQuery] = useState('');
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingAgent, setEditingAgent] = useState<Agent | null>(null);
  const [agentToDelete, setAgentToDelete] = useState<Agent | null>(null);
  const [agentActionTarget, setAgentActionTarget] = useState<Agent | null>(null);

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
      return agent.name.toLowerCase().includes(q);
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
    const cleanPhone = phoneInput.trim();

    if (editingAgent) {
      updateAgent({
        ...editingAgent,
        name: nameInput.trim(),
        rate,
        mult,
        phoneNumber: cleanPhone || undefined,
      });
    } else {
      addAgent(nameInput.trim(), rate, mult, cleanPhone || undefined);
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
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5">
          <div className="relative flex-1 min-w-0">
            <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="ကိုယ်စားလှယ် အမည်ရှာရန်..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 text-sm bg-white border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
            />
          </div>

          <button
            type="button"
            onClick={openAddModal}
            className="w-full sm:w-auto flex items-center justify-center gap-2 px-4 py-2.5 bg-[#235347] hover:bg-[#1b4339] text-white text-xs font-semibold rounded-xl shadow transition whitespace-nowrap shrink-0 cursor-pointer"
          >
            <UserPlus className="w-4 h-4 shrink-0" />
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
              type="button"
              onClick={openAddModal}
              className="text-xs font-semibold text-[#235347] hover:underline cursor-pointer"
            >
              + ကိုယ်စားလှယ်အသစ် ထည့်သွင်းပါ
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredAgents.map((agent) => {
              const summary = getAgentSummaryData(agent, currentWinningNumber);
              return (
                <AgentCard
                  key={agent.id}
                  agent={agent}
                  summary={summary}
                  onEdit={openEditModal}
                  onDelete={(ag) => setAgentToDelete(ag)}
                  onStartBet={handleStartBet}
                  onLongPress={(ag) => setAgentActionTarget(ag)}
                />
              );
            })}
          </div>
        )}
      </main>

      {/* Long-Press Action Dialog Box */}
      {agentActionTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-xs w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col">
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center justify-between gap-2">
              <div className="min-w-0">
                <h3 className="font-bold text-sm truncate">{agentActionTarget.name}</h3>
                {agentActionTarget.phoneNumber && agentActionTarget.phoneNumber.trim() !== '' && (
                  <p className="text-[11px] text-emerald-200 font-mono truncate">
                    📞 {agentActionTarget.phoneNumber.trim()}
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={() => setAgentActionTarget(null)}
                className="text-white/80 hover:text-white p-1 shrink-0"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 flex flex-col gap-2.5">
              <button
                type="button"
                onClick={() => {
                  const target = agentActionTarget;
                  setAgentActionTarget(null);
                  openEditModal(target);
                }}
                className="w-full py-2.5 px-4 bg-[#235347] hover:bg-[#183B32] text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition cursor-pointer"
              >
                <Edit2 className="w-4 h-4" />
                <span>ပြင်ဆင်မည် (Edit)</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  const target = agentActionTarget;
                  setAgentActionTarget(null);
                  soundManager.playClick();
                  setAgentToDelete(target);
                }}
                className="w-full py-2.5 px-4 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-bold flex items-center justify-center gap-2 shadow-sm transition cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
                <span>ဖျက်မည် (Delete)</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  soundManager.playClick();
                  setAgentActionTarget(null);
                }}
                className="w-full py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl text-xs font-semibold transition cursor-pointer"
              >
                မလုပ်ပါ
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Add / Edit Agent Modal */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 animate-in fade-in">
          <form
            onSubmit={handleSave}
            className="bg-white rounded-2xl max-w-sm w-full max-h-[90vh] shadow-2xl overflow-hidden border border-emerald-900/10 flex flex-col"
          >
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center justify-between shrink-0">
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

            <div className="p-4 flex flex-col gap-3 overflow-y-auto">
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
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
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
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
                  />
                  <span className="text-sm font-bold text-gray-600 shrink-0">%</span>
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
                    className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
                  />
                  <span className="text-sm font-bold text-gray-600 shrink-0">ဆ</span>
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
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-[#235347] select-text"
                />
              </div>
            </div>

            <div className="p-4 bg-gray-50 border-t border-gray-100 flex gap-2 justify-end shrink-0">
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-xl text-xs font-semibold whitespace-nowrap cursor-pointer"
              >
                မလုပ်တော့ပါ
              </button>
              <button
                type="submit"
                className="px-4 py-2 bg-[#235347] hover:bg-[#1b4339] text-white rounded-xl text-xs font-bold transition shadow whitespace-nowrap cursor-pointer"
              >
                သိမ်းဆည်းမည်
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Delete Confirmation Alert Box */}
      {agentToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 animate-in fade-in">
          <div className="bg-white rounded-2xl max-w-sm w-full shadow-2xl overflow-hidden border border-slate-200 flex flex-col">
            <div className="bg-[#235347] text-white px-4 py-3.5 flex items-center gap-2.5">
              <div className="w-7 h-7 rounded-full bg-red-500/25 text-red-200 flex items-center justify-center shrink-0">
                <Trash2 className="w-4 h-4" />
              </div>
              <h4 className="font-bold text-sm">ဖျက်ရန် သေချာပါသလား?</h4>
            </div>
            <div className="p-4 space-y-4">
              <p className="text-xs text-slate-600 leading-relaxed">
                ကိုယ်စားလှယ် <strong className="text-slate-900">&apos;{agentToDelete.name}&apos;</strong> အား ဖျက်မည် ဖြစ်သည်။
              </p>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setAgentToDelete(null)}
                  className="flex-1 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl whitespace-nowrap cursor-pointer"
                >
                  မဖျက်ပါ
                </button>
                <button
                  type="button"
                  onClick={handleDelete}
                  className="flex-1 py-2.5 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-xl shadow transition whitespace-nowrap cursor-pointer"
                >
                  ဖျက်မည်
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
