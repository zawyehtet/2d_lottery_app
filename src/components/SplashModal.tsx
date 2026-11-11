import React, { useEffect, useState } from 'react';

interface SplashModalProps {
  onFinish: () => void;
}

export const SplashModal: React.FC<SplashModalProps> = ({ onFinish }) => {
  const [opacity, setOpacity] = useState(1);

  useEffect(() => {
    const timer = setTimeout(() => {
      setOpacity(0);
      setTimeout(onFinish, 400);
    }, 1200);
    return () => clearTimeout(timer);
  }, [onFinish]);

  return (
    <div
      style={{ opacity, transition: 'opacity 0.4s ease-out' }}
      className="fixed inset-0 z-50 bg-[#235347] flex flex-col items-center justify-between p-8 text-white select-none"
    >
      <div />

      <div className="flex flex-col items-center">
        <div className="w-24 h-24 rounded-2xl bg-[#0C4E3E] border-2 border-[#FEC72E] p-1 shadow-2xl flex items-center justify-center overflow-hidden mb-4 animate-bounce">
          <img
            src="/ic_lucky2d_logo.png"
            alt="Lucky2D"
            className="w-full h-full object-cover rounded-xl"
          />
        </div>

        <h1 className="text-3xl font-extrabold tracking-wider text-white">Lucky2D</h1>
        <p className="text-sm font-medium text-emerald-200 mt-1">
          2D Lottery Accounting & Management
        </p>

        <div className="mt-8 flex items-center gap-1.5">
          <div className="w-2 h-2 rounded-full bg-[#FEC72E] animate-pulse"></div>
          <div className="w-2 h-2 rounded-full bg-[#FEC72E] animate-pulse delay-100"></div>
          <div className="w-2 h-2 rounded-full bg-[#FEC72E] animate-pulse delay-200"></div>
        </div>
      </div>

      <div className="text-xs text-emerald-200 font-semibold tracking-wide">
        Power by Lcky2d3d ShweKhinMyay
      </div>
    </div>
  );
};
