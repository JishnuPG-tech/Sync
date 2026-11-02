import React from 'react';
import { NearbyUserCard } from '../types';

interface PeerDrawerProps {
  peer: NearbyUserCard;
  isConnected: boolean;
  isPending: boolean;
  onConnect: (peer: NearbyUserCard) => void;
  onClose: () => void;
}

export const PeerDrawer: React.FC<PeerDrawerProps> = ({
  peer,
  isConnected,
  isPending,
  onConnect,
  onClose
}) => {
  const simulatedHandle = peer.display_name.toLowerCase().replace(/\s+/g, '_');

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/70 backdrop-blur-sm select-none">
      <div className="w-full max-w-md rounded-t-3xl bg-[#111311] border-t border-[#292C28] p-6 pb-safe animate-slide-up">
        {/* Drag handle */}
        <div className="w-10 h-1 bg-[#292C28] rounded-full mx-auto mb-4" />

        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            <img
              src={peer.avatar_url || '/portraits/portrait_02.jpg'}
              alt={peer.display_name}
              className="w-14 h-14 rounded-full object-cover border-2 border-[#E18462] shadow-md"
            />
            <div>
              <h3 className="font-serif font-semibold text-[20px] text-[#F3F0E9]">
                {peer.display_name}
              </h3>
              <p className="font-mono text-[12px] text-[#8BAD91] flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-[#8BAD91] animate-pulse" />
                {(() => { const d = Number(peer.distance_meters ?? peer.approx_distance_meters ?? 5); return d < 1000 ? d.toFixed(1) + 'm' : (d / 1000).toFixed(1) + 'km'; })()} away · In Physical Space
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-[#666963] hover:text-[#9B9B94] hover:bg-[#171A17]"
          >
            ✕
          </button>
        </div>

        <div className="p-3.5 rounded-xl bg-[#171A17] border border-[#292C28] mb-4">
          <p className="font-sans text-[13.5px] leading-[19px] text-[#F3F0E9]">
            {peer.bio || 'Exploring nearby spaces with Spott.'}
          </p>
          <div className="flex items-center gap-2 mt-2 pt-2 border-t border-[#292C28]/60 font-mono text-[11px] text-[#9B9B94]">
            <span>Physical BLE Signal: High</span>
            <span>·</span>
            <span>Location: {peer.location_name || 'Nearby'}</span>
          </div>
        </div>

        {/* Instagram Status */}
        {isConnected ? (
          <div className="space-y-3">
            <div className="p-3 rounded-xl bg-[#8BAD91]/10 border border-[#8BAD91]/30 flex items-center justify-between">
              <span className="font-mono font-bold text-[13.5px] text-[#F3F0E9]">
                @{simulatedHandle}
              </span>
              <span className="font-mono text-[10px] text-[#8BAD91] font-bold">
                ✓ Unlocked
              </span>
            </div>
            <a
              href={`https://instagram.com/${simulatedHandle}`}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full h-12 rounded-xl bg-[#E18462] hover:bg-[#F0A083] text-white font-sans font-bold text-[14px] flex items-center justify-center gap-2 shadow"
            >
              <span>Open Instagram Profile</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          </div>
        ) : (
          <div className="space-y-3">
            <div className="p-3 rounded-xl bg-[#111311] border border-[#292C28] flex items-center justify-between text-[#9B9B94]">
              <span className="font-mono text-[12px] flex items-center gap-1.5">
                <svg className="w-3.5 h-3.5 text-[#E18462]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                Instagram handle locked
              </span>
              <span className="font-mono text-[10px] text-[#666963]">
                Mutual consent required
              </span>
            </div>

            {isPending ? (
              <div className="w-full h-12 rounded-xl bg-[#E0B66E]/20 border border-[#E0B66E]/40 font-sans font-semibold text-[14px] text-[#E0B66E] flex items-center justify-center">
                Connection Request Sent · Waiting
              </div>
            ) : (
              <button
                onClick={() => {
                  onConnect(peer);
                }}
                className="w-full h-12 rounded-xl bg-[#E18462] hover:bg-[#F0A083] active:scale-[0.98] text-white font-sans font-bold text-[14px] flex items-center justify-center gap-2 shadow"
              >
                <span>Request Connection</span>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
