import React from 'react';

interface SyncTopBarProps {
  onRefresh?: () => void;
}

export const SyncTopBar: React.FC<SyncTopBarProps> = ({ onRefresh }) => {
  return (
    <header className="w-full bg-[#0B0C0B] border-b border-[#292C28]/40 px-5 py-3 pt-safe sticky top-0 z-30 select-none">
      <div className="flex items-center justify-between max-w-md mx-auto">
        {/* Brand Lockup */}
        <div className="flex items-center gap-2">
          <img
            src="/sync_emblem.png"
            alt="Spott Logo"
            className="w-6 h-6 object-contain"
          />
          <span className="font-serif font-semibold text-[20px] tracking-[-0.02em] text-[#F3F0E9]">
            Spott
          </span>
        </div>

        {/* Right Status Indicator */}
        <div className="flex items-center gap-2">
          {onRefresh && (
            <button
              onClick={onRefresh}
              className="p-1.5 rounded-full hover:bg-[#171A17] text-[#9B9B94] transition-colors"
              title="Refresh Radar"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </button>
          )}
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#111311] border border-[#292C28]">
            <span className="w-2 h-2 rounded-full bg-[#8BAD91] animate-pulse" />
            <span className="font-mono font-medium text-[10px] text-[#9B9B94] uppercase tracking-wider">
              BLE RADAR
            </span>
          </div>
        </div>
      </div>
    </header>
  );
};
