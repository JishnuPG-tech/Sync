import React, { useEffect, useState } from 'react';

interface SplashScreenProps {
  onSplashFinished: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({ onSplashFinished }) => {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const startTime = Date.now();
    const duration = 2000;

    const interval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const current = Math.min(1, elapsed / duration);
      setProgress(current);

      if (current >= 1) {
        clearInterval(interval);
        setTimeout(onSplashFinished, 180);
      }
    }, 16);

    return () => clearInterval(interval);
  }, [onSplashFinished]);

  return (
    <div
      onClick={onSplashFinished}
      className="fixed inset-0 z-50 flex flex-col items-center justify-center bg-[#0B0C0B] cursor-pointer select-none px-6"
    >
      <div className="flex flex-col items-center justify-center -translate-y-4">
        {/* Synchronous Helix S-Monogram Logo */}
        <div className="relative mb-6 flex items-center justify-center">
          <img
            src="/sync_emblem.png"
            alt="Spott Logo"
            className="w-24 h-24 object-contain animate-pulse-subtle filter drop-shadow-[0_4px_24px_rgba(225,132,98,0.25)]"
          />
        </div>

        {/* Brand Title: Spott. in Playfair Display */}
        <div className="flex items-baseline justify-center tracking-[-0.04em]">
          <span className="font-serif font-medium text-[42px] leading-tight text-[#F3F0E9]">
            Spott
          </span>
          <span className="font-serif font-medium text-[42px] leading-tight text-[#E18462]">
            .
          </span>
        </div>

        {/* Subtitle Tagline in DM Sans */}
        <p className="mt-1.5 font-sans font-normal text-[14px] text-[#9B9B94] text-center">
          Meet nearby. Connect by choice.
        </p>

        {/* Subtle loader bar */}
        <div className="mt-8 w-[140px] h-[3px] rounded-full bg-[#292C28] overflow-hidden">
          <div
            className="h-full bg-[#E18462] rounded-full transition-all duration-75 ease-out"
            style={{ width: `${progress * 100}%` }}
          />
        </div>
      </div>
    </div>
  );
};
