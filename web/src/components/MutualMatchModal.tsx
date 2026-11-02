import React from 'react';
import { NearbyUserCard } from '../types';

interface MutualMatchModalProps {
  peer: NearbyUserCard;
  myAvatar: string;
  onClose: () => void;
}

export const MutualMatchModal: React.FC<MutualMatchModalProps> = ({
  peer,
  myAvatar,
  onClose
}) => {
  const instagramHandle = (peer.display_name.toLowerCase().replace(/\s+/g, '_') || 'spott_user');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in select-none">
      <div className="w-full max-w-sm rounded-3xl bg-[#111311] border border-[#292C28] p-6 text-center shadow-2xl animate-scale-up">
        {/* Dual Avatars with Interlocking Logo */}
        <div className="relative flex items-center justify-center gap-4 my-2">
          <div className="relative w-16 h-16 rounded-full border-2 border-[#E18462] overflow-hidden shadow-md">
            <img src={myAvatar} alt="You" className="w-full h-full object-cover" />
          </div>

          <div className="z-10 -mx-3 w-10 h-10 rounded-full bg-[#171A17] border-2 border-[#E18462] flex items-center justify-center shadow-lg p-1.5">
            <img src="/sync_emblem.png" alt="Mutual" className="w-full h-full object-contain" />
          </div>

          <div className="relative w-16 h-16 rounded-full border-2 border-[#8BAD91] overflow-hidden shadow-md">
            <img
              src={peer.avatar_url || '/portraits/portrait_02.jpg'}
              alt={peer.display_name}
              className="w-full h-full object-cover"
            />
          </div>
        </div>

        {/* Headline */}
        <h3 className="font-serif font-medium text-[24px] text-[#F3F0E9] mt-4">
          Connection Unlocked!
        </h3>
        <p className="font-sans text-[13px] leading-[18px] text-[#9B9B94] mt-1.5">
          Both of you agreed to connect. Instagram handle is now unlocked:
        </p>

        {/* Unlocked Instagram Card */}
        <div className="my-5 p-3.5 rounded-2xl bg-[#8BAD91]/10 border border-[#8BAD91]/30 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#8BAD91]" />
            <span className="font-mono font-bold text-[14px] text-[#F3F0E9]">
              @{instagramHandle}
            </span>
          </div>
          <span className="px-2 py-0.5 rounded-full bg-[#8BAD91]/20 font-mono text-[10px] font-bold text-[#8BAD91]">
            ✓ Unlocked
          </span>
        </div>

        {/* Action Buttons */}
        <div className="space-y-2">
          <a
            href={`https://instagram.com/${instagramHandle}`}
            target="_blank"
            rel="noopener noreferrer"
            className="w-full h-12 rounded-xl bg-[#E18462] hover:bg-[#F0A083] active:scale-[0.98] text-white font-sans font-bold text-[14.5px] transition-all flex items-center justify-center gap-2 shadow-md"
          >
            <span>Open in Instagram</span>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          </a>

          <button
            onClick={onClose}
            className="w-full h-11 rounded-xl bg-transparent border border-[#292C28] hover:border-[#383D37] text-[#9B9B94] font-sans font-medium text-[13.5px] transition-all"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
};
