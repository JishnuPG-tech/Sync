import React, { useState, useEffect, useRef } from 'react';
import { NearbyUserCard } from '../types';

const formatDistance = (m?: number): string => {
  const d = Number(m ?? 0);
  if (isNaN(d) || d <= 0) return 'Nearby';
  if (d < 1000) return `${d.toFixed(1)}m`;
  return `${(d / 1000).toFixed(1)}km`;
};

interface RadarViewProps {
  nearbyPeers: NearbyUserCard[];
  onConnect: (peer: NearbyUserCard) => void;
  requestedPeerIds: Set<string>;
  connectedPeerIds: Set<string>;
  myAvatar: string;
  myName: string;
  onSelectPeer: (peer: NearbyUserCard) => void;
}

export const RadarView: React.FC<RadarViewProps> = ({
  nearbyPeers,
  onConnect,
  requestedPeerIds,
  connectedPeerIds,
  myAvatar,
  myName,
  onSelectPeer
}) => {
  const [powerMode, setPowerMode] = useState<'ECO' | 'BALANCED' | 'HIGH'>('BALANCED');
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  // Radar Canvas Animation
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let animId: number;
    let angle = 0;
    let pulse = 0;

    const render = () => {
      const width = canvas.width;
      const height = canvas.height;
      const cx = width / 2;
      const cy = height / 2;
      const maxRadius = width * 0.45;

      ctx.clearRect(0, 0, width, height);

      // Concentric Rings (Warm Terracotta tones)
      const rings = [0.25, 0.5, 0.75, 1.0];
      rings.forEach((ratio, idx) => {
        const r = maxRadius * ratio;
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.strokeStyle = idx === rings.length - 1 ? 'rgba(41, 44, 40, 0.8)' : 'rgba(41, 44, 40, 0.4)';
        ctx.lineWidth = 1;
        ctx.stroke();
      });

      // Crosshairs
      ctx.beginPath();
      ctx.moveTo(cx - maxRadius, cy);
      ctx.lineTo(cx + maxRadius, cy);
      ctx.moveTo(cx, cy - maxRadius);
      ctx.lineTo(cx, cy + maxRadius);
      ctx.strokeStyle = 'rgba(41, 44, 40, 0.3)';
      ctx.lineWidth = 1;
      ctx.setLineDash([2, 4]);
      ctx.stroke();
      ctx.setLineDash([]);

      // Rotating Terracotta Radar Sweep
      angle += 0.02;
      ctx.save();
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, maxRadius, angle, angle + 0.5);
      ctx.closePath();
      const sweepGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, maxRadius);
      sweepGrad.addColorStop(0, 'rgba(225, 132, 98, 0.25)');
      sweepGrad.addColorStop(1, 'rgba(225, 132, 98, 0.0)');
      ctx.fillStyle = sweepGrad;
      ctx.fill();
      ctx.restore();

      // Pulsing Center Core
      pulse = (pulse + 0.04) % (Math.PI * 2);
      const pulseR = 8 + Math.sin(pulse) * 3;
      ctx.beginPath();
      ctx.arc(cx, cy, pulseR + 4, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(225, 132, 98, 0.2)';
      ctx.fill();

      // Center Dot
      ctx.beginPath();
      ctx.arc(cx, cy, 5, 0, Math.PI * 2);
      ctx.fillStyle = '#E18462';
      ctx.fill();

      animId = requestAnimationFrame(render);
    };

    render();
    return () => cancelAnimationFrame(animId);
  }, []);

  return (
    <div className="flex flex-col w-full max-w-md mx-auto px-4 pb-24 select-none">
      {/* Power Mode Pills */}
      <div className="flex items-center justify-between mt-3 mb-2 px-1">
        <span className="font-sans font-medium text-[13px] text-[#9B9B94]">
          Proximity Mode
        </span>
        <div className="flex items-center gap-1 p-0.5 rounded-full bg-[#111311] border border-[#292C28]">
          {(['ECO', 'BALANCED', 'HIGH'] as const).map((mode) => (
            <button
              key={mode}
              onClick={() => setPowerMode(mode)}
              className={`px-2.5 py-1 rounded-full font-mono text-[10px] font-semibold transition-all ${
                powerMode === mode
                  ? 'bg-[#E18462] text-white shadow-sm'
                  : 'text-[#666963] hover:text-[#9B9B94]'
              }`}
            >
              {mode}
            </button>
          ))}
        </div>
      </div>

      {/* Radar Canvas with Orbiting Peer Pins */}
      <div className="relative w-full aspect-square max-w-[340px] mx-auto my-2 flex items-center justify-center">
        <canvas
          ref={canvasRef}
          width={340}
          height={340}
          className="absolute inset-0 w-full h-full pointer-events-none"
        />

        {/* Center My Avatar */}
        <div className="relative z-10 w-11 h-11 rounded-full border-2 border-[#E18462] overflow-hidden shadow-[0_0_16px_rgba(225,132,98,0.4)]">
          <img src={myAvatar} alt={myName} className="w-full h-full object-cover" />
        </div>

        {/* Nearby Peer Pins on Canvas */}
        {nearbyPeers.map((peer, idx) => {
          // Calculate polar position based on distance & index
          const angle = (idx * (2 * Math.PI / Math.max(1, nearbyPeers.length))) + 0.6;
          const rawDist = Number(peer.distance_meters ?? peer.approx_distance_meters ?? 5);
          const safeDist = isNaN(rawDist) ? 5 : Math.min(rawDist, 25);
          const radiusNorm = Math.min(130, 42 + (safeDist * 3.5));
          const distLabel = formatDistance(rawDist);
          const x = Math.cos(angle) * radiusNorm;
          const y = Math.sin(angle) * radiusNorm;
          const isConnected = connectedPeerIds.has(peer.user_id);
          const isPending = requestedPeerIds.has(peer.user_id);

          return (
            <button
              key={peer.user_id}
              onClick={() => onSelectPeer(peer)}
              style={{
                transform: `translate(${x}px, ${y}px)`
              }}
              className="absolute z-20 group transition-transform active:scale-90"
              title={`${peer.display_name} (${distLabel})`}
            >
              <div className="relative">
                <img
                  src={peer.avatar_url || `/portraits/portrait_0${(idx % 8) + 2}.jpg`}
                  alt={peer.display_name}
                  className={`w-10 h-10 rounded-full object-cover border-2 shadow-lg transition-all ${
                    isConnected
                      ? 'border-[#8BAD91]'
                      : isPending
                      ? 'border-[#E0B66E]'
                      : 'border-[#E18462]'
                  }`}
                />
                <span className="absolute -bottom-1 -right-1 px-1 rounded bg-[#111311] border border-[#292C28] font-mono text-[9px] text-[#9B9B94]">
                  {distLabel}
                </span>
              </div>
            </button>
          );
        })}
      </div>

      {/* People Nearby Section */}
      <div className="mt-4">
        <div className="flex items-center justify-between mb-3 px-1">
          <h2 className="font-serif font-semibold text-[18px] text-[#F3F0E9]">
            People in radar range
          </h2>
          <span className="font-mono text-[11px] text-[#8BAD91] bg-[#8BAD91]/10 px-2 py-0.5 rounded-full border border-[#8BAD91]/20">
            {nearbyPeers.length} active
          </span>
        </div>

        {nearbyPeers.length === 0 ? (
          <div className="p-8 text-center rounded-2xl bg-[#111311] border border-[#292C28]">
            <div className="w-10 h-10 mx-auto rounded-full bg-[#E18462]/10 border border-[#E18462]/30 flex items-center justify-center mb-3">
              <svg className="w-5 h-5 text-[#E18462] animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
              </svg>
            </div>
            <p className="font-sans font-medium text-[14px] text-[#F3F0E9]">
              Scanning physical space...
            </p>
            <p className="font-sans text-[12.5px] text-[#9B9B94] mt-1">
              Peers with Bluetooth active will automatically illuminate here.
            </p>
          </div>
        ) : (
          <div className="space-y-2.5">
            {nearbyPeers.map((peer, idx) => {
              const isConnected = connectedPeerIds.has(peer.user_id);
              const isPending = requestedPeerIds.has(peer.user_id);
              const distLabel = formatDistance(peer.distance_meters ?? peer.approx_distance_meters);

              return (
                <div
                  key={peer.user_id}
                  className="flex items-center justify-between p-3.5 rounded-2xl bg-[#111311] border border-[#292C28] hover:border-[#383D37] transition-all"
                >
                  <div
                    onClick={() => onSelectPeer(peer)}
                    className="flex items-center gap-3 cursor-pointer flex-1 min-w-0"
                  >
                    <div className="relative">
                      <img
                        src={peer.avatar_url || `/portraits/portrait_0${(idx % 8) + 2}.jpg`}
                        alt={peer.display_name}
                        className="w-12 h-12 rounded-full object-cover border-2 border-[#E18462]"
                      />
                      <span className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-[#8BAD91] border-2 border-[#111311]" />
                    </div>

                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="font-sans font-semibold text-[14.5px] text-[#F3F0E9] truncate">
                          {peer.display_name}
                        </span>
                        <span className="font-mono text-[10px] text-[#8BAD91] bg-[#8BAD91]/15 px-1.5 py-0.2 rounded">
                          {distLabel}
                        </span>
                      </div>
                      <p className="font-sans text-[12px] text-[#9B9B94] truncate mt-0.5">
                        {peer.bio || 'Exploring physical space with Spott'}
                      </p>
                    </div>
                  </div>

                  <div className="ml-2">
                    {isConnected ? (
                      <span className="px-3 py-1.5 rounded-xl bg-[#8BAD91]/20 border border-[#8BAD91]/40 font-mono font-bold text-[11px] text-[#8BAD91] flex items-center gap-1">
                        <span>✓</span> Connected
                      </span>
                    ) : isPending ? (
                      <span className="px-3 py-1.5 rounded-xl bg-[#E0B66E]/20 border border-[#E0B66E]/40 font-mono font-medium text-[11px] text-[#E0B66E]">
                        Pending...
                      </span>
                    ) : (
                      <button
                        onClick={() => onConnect(peer)}
                        className="px-3.5 py-1.5 rounded-xl bg-[#E18462] hover:bg-[#F0A083] active:scale-95 text-white font-sans font-semibold text-[12px] transition-all shadow-sm"
                      >
                        Connect
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
