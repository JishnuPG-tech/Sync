import React from 'react';
import { Radar, Users, Bell, User } from 'lucide-react';

export type NavTab = 'radar' | 'requests' | 'matches' | 'profile';

interface BottomNavProps {
  activeTab: NavTab;
  onTabChange: (tab: NavTab) => void;
  pendingCount?: number;
  matchesCount?: number;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  activeTab,
  onTabChange,
  pendingCount = 0,
  matchesCount = 0,
}) => {
  const tabs = [
    { id: 'radar' as NavTab, label: 'Radar', icon: Radar },
    {
      id: 'requests' as NavTab,
      label: 'Requests',
      icon: Bell,
      badge: pendingCount,
    },
    {
      id: 'matches' as NavTab,
      label: 'Matches',
      icon: Users,
      badge: matchesCount,
    },
    { id: 'profile' as NavTab, label: 'Profile', icon: User },
  ];

  return (
    <nav className="fixed bottom-0 inset-x-0 z-30 bg-oled-card/85 backdrop-blur-xl border-t border-oled-border pb-safe">
      <div className="max-w-md mx-auto px-4 h-16 flex items-center justify-around">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;

          return (
            <button
              key={tab.id}
              onClick={() => {
                onTabChange(tab.id);
                if ('vibrate' in navigator) navigator.vibrate(8);
              }}
              className={`relative flex flex-col items-center justify-center flex-1 h-full transition duration-150 active:scale-90 ${
                isActive ? 'text-radar-blue font-bold' : 'text-ink-secondary hover:text-ink-primary'
              }`}
            >
              <div className="relative">
                <Icon className={`w-5 h-5 transition-transform ${isActive ? 'scale-110' : ''}`} />
                {tab.badge && tab.badge > 0 ? (
                  <span className="absolute -top-1.5 -right-2 min-w-[16px] h-4 px-1 rounded-full bg-radar-coral text-black font-mono font-bold text-[10px] flex items-center justify-center shadow-[0_0_8px_rgba(249,115,22,0.5)]">
                    {tab.badge}
                  </span>
                ) : null}
              </div>
              <span className="text-[11px] font-heading mt-1 transition-opacity">
                {tab.label}
              </span>
              {isActive && (
                <span className="absolute bottom-1 w-6 h-0.5 rounded-full bg-radar-blue shadow-[0_0_8px_#38BDF8]" />
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
};
