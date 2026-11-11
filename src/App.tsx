import React, { useState } from 'react';
import { LotteryProvider, useLottery } from './context/LotteryContext';
import { SplashModal } from './components/SplashModal';
import { ActivationModal } from './components/ActivationModal';
import { isAppActivated } from './security/licenseManager';

import { HomeScreen } from './screens/HomeScreen';
import { AgentsScreen } from './screens/AgentsScreen';
import { NumbersScreen } from './screens/NumbersScreen';
import { VouchersScreen } from './screens/VouchersScreen';
import { BrakeScreen } from './screens/BrakeScreen';
import { SettlementScreen } from './screens/SettlementScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { LayoffHistoryScreen } from './screens/LayoffHistoryScreen';
import { AgentSettlementScreen } from './screens/AgentSettlementScreen';
import { NumberEntryScreen } from './screens/NumberEntryScreen';

const MainNavigator: React.FC = () => {
  const { currentScreen, toastMessage } = useLottery();
  const [showSplash, setShowSplash] = useState(true);
  const [isActivated, setIsActivated] = useState(() => isAppActivated());

  const renderScreen = () => {
    switch (currentScreen) {
      case 'HOME':
        return <HomeScreen />;
      case 'AGENTS':
        return <AgentsScreen />;
      case 'NUMBERS':
        return <NumbersScreen />;
      case 'VOUCHERS':
        return <VouchersScreen />;
      case 'BRAKE':
        return <BrakeScreen />;
      case 'SETTLEMENT':
        return <SettlementScreen />;
      case 'SETTINGS':
        return <SettingsScreen />;
      case 'LAYOFF_HISTORY':
        return <LayoffHistoryScreen />;
      case 'AGENT_SETTLEMENT':
        return <AgentSettlementScreen />;
      case 'NUMBER_ENTRY':
        return <NumberEntryScreen />;
      default:
        return <HomeScreen />;
    }
  };

  return (
    <div className="min-h-screen bg-[#F4F7F5] text-gray-900 font-sans selection:bg-[#235347] selection:text-white antialiased">
      {/* Launch Splash Screen */}
      {showSplash && <SplashModal onFinish={() => setShowSplash(false)} />}

      {/* Offline Device License Screen */}
      {!isActivated && !showSplash && (
        <ActivationModal onActivated={() => setIsActivated(true)} />
      )}

      {/* Screen View */}
      {renderScreen()}

      {/* Global Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-5 left-1/2 -translate-x-1/2 z-50 bg-gray-900/90 text-white px-4 py-2.5 rounded-full text-xs font-medium shadow-xl backdrop-blur animate-in fade-in slide-in-from-bottom-2">
          {toastMessage}
        </div>
      )}
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <LotteryProvider>
      <MainNavigator />
    </LotteryProvider>
  );
};

export default App;
