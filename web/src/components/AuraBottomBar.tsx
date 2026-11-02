import React from 'react';

export type ActiveTab = 'radar' | 'discovered' | 'profile';

interface AuraBottomBarProps {
  currentTab: ActiveTab;
  onTabSelected: (tab: ActiveTab) => void;
  pendingCount?: number;
}

export const AuraBottomBar: React.FC<AuraBottomBarProps> = ({
  currentTab,
  onTabSelected,
  pendingCount = 0
}) => {
  const tabs = [
    {
      id: 'radar' as ActiveTab,
      label: 'Radar',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <circle cx="12" cy="12" r="9" strokeWidth="1.8" />
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M12 3a9 9 0 019 9" />
          <circle cx="12" cy="12" r="3" strokeWidth="1.8" />
        </svg>
      )
    },
    {
      id: 'discovered' as ActiveTab,
      label: 'Discovered',
      badge: pendingCount,
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
        </svg>
      )
    },
    {
      id: 'profile' as ActiveTab,
      label: 'Profile',
      icon: (
        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
        </svg>
      )
    }
  ];

  return (
    <nav className="w-full bg-[#111311] border-t border-[#292C28] pb-safe fixed bottom-0 left-0 right-0 z-30 select-none">
      <div className="flex items-center justify-around max-w-md mx-auto h-[60px] px-2">
        {tabs.map((tab) => {
          const isSelected = currentTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => onTabSelected(tab.id)}
              className={`flex-1 flex flex-col items-center justify-center py-1 transition-all relative ${
                isSelected ? 'text-[#E18462]' : 'text-[#9B9B94] hover:text-[#F3F0E9]'
              }`}
            >
              <div className={`relative px-3 py-1 rounded-full transition-all ${isSelected ? 'bg-[#E18462]/15' : ''}`}>
                {tab.icon}
                {tab.badge && tab.badge > 0 ? (
                  <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-[#E18462] text-white font-mono font-bold text-[9px] flex items-center justify-center">
                    {tab.badge}
                  </span>
                ) : null}
              </div>
              <span className={`font-sans text-[11.5px] mt-0.5 ${isSelected ? 'font-semibold' : 'font-normal'}`}>
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
