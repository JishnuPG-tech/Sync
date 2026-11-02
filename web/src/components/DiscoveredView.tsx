import React, { useState } from 'react';
import { NearbyUserCard } from '../types';

const formatDistance = (m?: number): string => {
  const d = Number(m ?? 0);
  if (isNaN(d) || d <= 0) return 'Nearby';
  if (d < 1000) return `${d.toFixed(1)}m`;
  return `${(d / 1000).toFixed(1)}km`;
};

interface DiscoveredViewProps {
  peers: NearbyUserCard[];
  onConnect: (peer: NearbyUserCard) => void;
  requestedPeerIds: Set<string>;
  connectedPeerIds: Set<string>;
  onSelectPeer: (peer: NearbyUserCard) => void;
}

export const DiscoveredView: React.FC<DiscoveredViewProps> = ({
  peers,
  onConnect,
  requestedPeerIds,
  connectedPeerIds,
  onSelectPeer
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState<'ALL' | 'IN_RADAR' | 'OUT_OF_RANGE' | 'CONNECTED'>('ALL');

  const filteredPeers = peers.filter((peer) => {
    const matchesSearch =
      peer.display_name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      peer.bio.toLowerCase().includes(searchTerm.toLowerCase());
    if (!matchesSearch) return false;

    const d = Number(peer.distance_meters ?? peer.approx_distance_meters ?? 5);
    if (filter === 'IN_RADAR') return d <= 15;
    if (filter === 'OUT_OF_RANGE') return d > 15;
    if (filter === 'CONNECTED') return connectedPeerIds.has(peer.user_id);
    return true;
  });

  return (
    <div className="flex flex-col w-full max-w-md mx-auto px-4 pb-24 select-none">
      {/* Search Input */}
      <div className="relative flex items-center w-full h-[46px] rounded-xl bg-[#111311] border border-[#292C28] focus-within:border-[#E18462] px-3.5 mt-3 transition-all">
        <svg className="w-4 h-4 text-[#9B9B94] mr-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="text"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          placeholder="Search encounters by name or bio..."
          className="flex-1 bg-transparent border-none outline-none font-sans text-[13.5px] text-[#F3F0E9] placeholder-[#666963]"
        />
        {searchTerm && (
          <button onClick={() => setSearchTerm('')} className="p-1 text-[#666963] hover:text-[#9B9B94]">
            ✕
          </button>
        )}
      </div>

      {/* Filter Chips */}
      <div className="flex items-center gap-1.5 overflow-x-auto py-3 scrollbar-none">
        {(['ALL', 'IN_RADAR', 'OUT_OF_RANGE', 'CONNECTED'] as const).map((tab) => {
          const isActive = filter === tab;
          const labels: Record<string, string> = {
            ALL: 'All Encounters',
            IN_RADAR: 'In Radar (<10m)',
            OUT_OF_RANGE: 'Out of Range',
            CONNECTED: 'Connected'
          };
          return (
            <button
              key={tab}
              onClick={() => setFilter(tab)}
              className={`px-3 py-1.5 rounded-full font-mono text-[11px] whitespace-nowrap transition-all border ${
                isActive
                  ? 'bg-[#E18462] border-[#E18462] text-white font-semibold'
                  : 'bg-[#111311] border-[#292C28] text-[#9B9B94] hover:border-[#383D37]'
              }`}
            >
              {labels[tab]}
            </button>
          );
        })}
      </div>

      {/* Persistent Encounters List */}
      <div className="space-y-3 mt-1">
        {filteredPeers.length === 0 ? (
          <div className="p-8 text-center rounded-2xl bg-[#111311] border border-[#292C28]">
            <p className="font-sans font-medium text-[14px] text-[#F3F0E9]">
              No encounters match your filter
            </p>
            <p className="font-sans text-[12.5px] text-[#9B9B94] mt-1">
              As you move around in physical space, real peer encounters remain saved here.
            </p>
          </div>
        ) : (
          filteredPeers.map((peer, idx) => {
            const isConnected = connectedPeerIds.has(peer.user_id);
            const isPending = requestedPeerIds.has(peer.user_id);

            return (
              <div
                key={peer.user_id}
                className="p-3.5 rounded-2xl bg-[#111311] border border-[#292C28] hover:border-[#383D37] transition-all"
              >
                <div className="flex items-start justify-between">
                  <div
                    onClick={() => onSelectPeer(peer)}
                    className="flex items-start gap-3 cursor-pointer flex-1 min-w-0"
                  >
                    <img
                      src={peer.avatar_url || `/portraits/portrait_0${(idx % 8) + 2}.jpg`}
                      alt={peer.display_name}
                      className="w-12 h-12 rounded-full object-cover border-2 border-[#E18462] mt-0.5"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-sans font-semibold text-[15px] text-[#F3F0E9] truncate">
                          {peer.display_name}
                        </span>
                        {isConnected ? (
                          <span className="font-mono text-[10px] text-[#8BAD91] bg-[#8BAD91]/15 px-1.5 py-0.5 rounded">
                            Unlocked
                          </span>
                        ) : (
                          <span className="font-mono text-[10px] text-[#9B9B94] bg-[#171A17] border border-[#292C28] px-1.5 py-0.5 rounded flex items-center gap-1">
                            <svg className="w-2.5 h-2.5 text-[#E18462]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                            </svg>
                            Locked
                          </span>
                        )}
                      </div>
                      <p className="font-sans text-[12.5px] text-[#9B9B94] mt-0.5 line-clamp-1">
                        {peer.bio || 'Spott community member in physical space'}
                      </p>
                      <div className="flex items-center gap-2 mt-1.5 font-mono text-[10.5px] text-[#666963]">
                        <span>Last seen: {formatDistance(peer.distance_meters ?? peer.approx_distance_meters)}</span>
                        <span>·</span>
                        <span>Encountered 1x</span>
                      </div>
                    </div>
                  </div>

                  <div className="ml-2">
                    {isConnected ? (
                      <button
                        onClick={() => onSelectPeer(peer)}
                        className="px-3 py-1.5 rounded-xl bg-[#8BAD91]/20 border border-[#8BAD91]/40 font-mono font-bold text-[11px] text-[#8BAD91]"
                      >
                        View IG ↗
                      </button>
                    ) : isPending ? (
                      <span className="px-3 py-1.5 rounded-xl bg-[#E0B66E]/20 border border-[#E0B66E]/40 font-mono text-[11px] text-[#E0B66E]">
                        Pending
                      </span>
                    ) : (
                      <button
                        onClick={() => onConnect(peer)}
                        className="px-3 py-1.5 rounded-xl bg-[#E18462] hover:bg-[#F0A083] active:scale-95 text-white font-sans font-semibold text-[12px] transition-all"
                      >
                        Connect
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
