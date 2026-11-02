import React, { useEffect, useRef } from 'react';
import { NearbyUserCard } from '../types';

interface RadarCanvasProps {
  peers: NearbyUserCard[];
  userAvatar?: string;
  userInitial: string;
  onSelectPeer: (peer: NearbyUserCard) => void;
  selectedPeerId?: string;
  isScanning?: boolean;
}

export const RadarCanvas: React.FC<RadarCanvasProps> = ({
  peers,
  userAvatar,
  userInitial,
  onSelectPeer,
  selectedPeerId,
  isScanning = true,
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const angleRef = useRef(0);
  const animFrameRef = useRef<number | null>(null);

  // Cached images for avatars
  const avatarImagesRef = useRef<Map<string, HTMLImageElement>>(new Map());

  // Deterministic angle mapping for peers based on their user_id
  const getPeerAngle = (userId: string): number => {
    let hash = 0;
    for (let i = 0; i < userId.length; i++) {
      hash = (hash << 5) - hash + userId.charCodeAt(i);
      hash |= 0;
    }
    return ((Math.abs(hash) % 360) * Math.PI) / 180;
  };

  useEffect(() => {
    // Preload peer avatar images
    peers.forEach((peer) => {
      if (peer.avatar_url && !avatarImagesRef.current.has(peer.user_id)) {
        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.src = peer.avatar_url;
        avatarImagesRef.current.set(peer.user_id, img);
      }
    });
  }, [peers]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    let dpr = window.devicePixelRatio || 1;

    const render = () => {
      const rect = canvas.getBoundingClientRect();
      const size = Math.min(rect.width, rect.height);
      if (canvas.width !== size * dpr || canvas.height !== size * dpr) {
        canvas.width = size * dpr;
        canvas.height = size * dpr;
      }

      ctx.save();
      ctx.scale(dpr, dpr);

      const cx = size / 2;
      const cy = size / 2;
      const maxRadius = size * 0.45;

      // Clear Canvas
      ctx.clearRect(0, 0, size, size);

      // 1. Draw Concentric Range Rings (10m, 25m, 50m, 100m)
      const ringDistances = [
        { d: '10m', r: 0.25 },
        { d: '25m', r: 0.5 },
        { d: '50m', r: 0.75 },
        { d: '100m', r: 1.0 },
      ];

      ringDistances.forEach((ring, idx) => {
        const r = maxRadius * ring.r;
        ctx.beginPath();
        ctx.arc(cx, cy, r, 0, Math.PI * 2);
        ctx.strokeStyle = idx === ringDistances.length - 1 ? 'rgba(56, 189, 248, 0.25)' : 'rgba(31, 41, 61, 0.6)';
        ctx.lineWidth = 1;
        ctx.setLineDash(idx % 2 === 1 ? [4, 4] : []);
        ctx.stroke();
        ctx.setLineDash([]);

        // Distance label
        ctx.fillStyle = 'rgba(100, 116, 139, 0.6)';
        ctx.font = '10px "DM Mono", monospace';
        ctx.fillText(ring.d, cx + 4, cy - r + 12);
      });

      // Crosshairs
      ctx.beginPath();
      ctx.moveTo(cx, cy - maxRadius);
      ctx.lineTo(cx, cy + maxRadius);
      ctx.moveTo(cx - maxRadius, cy);
      ctx.lineTo(cx + maxRadius, cy);
      ctx.strokeStyle = 'rgba(31, 41, 61, 0.4)';
      ctx.lineWidth = 1;
      ctx.stroke();

      // 2. Animated Radar Sweep Beam
      if (isScanning) {
        angleRef.current = (angleRef.current + 0.02) % (Math.PI * 2);
        const sweepAngle = angleRef.current;

        // Sector Sweep Gradient
        const gradient = ctx.createRadialGradient(cx, cy, 0, cx, cy, maxRadius);
        gradient.addColorStop(0, 'rgba(56, 189, 248, 0.2)');
        gradient.addColorStop(1, 'rgba(56, 189, 248, 0.0)');

        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, maxRadius, sweepAngle - 0.5, sweepAngle);
        ctx.closePath();
        ctx.fillStyle = gradient;
        ctx.fill();

        // Beam Leading Edge
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(cx + Math.cos(sweepAngle) * maxRadius, cy + Math.sin(sweepAngle) * maxRadius);
        ctx.strokeStyle = 'rgba(56, 189, 248, 0.8)';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }

      // 3. Draw Peer Blips
      peers.forEach((peer) => {
        const peerAngle = getPeerAngle(peer.user_id);
        // Map distance (0 to 100m) to radius (0.15 to 0.95 of maxRadius)
        const normalizedDist = Math.min(Math.max(peer.distance_meters, 3), 100);
        const distRatio = 0.15 + (normalizedDist / 100) * 0.8;
        const px = cx + Math.cos(peerAngle) * maxRadius * distRatio;
        const py = cy + Math.sin(peerAngle) * maxRadius * distRatio;

        const isSelected = peer.user_id === selectedPeerId;
        const isVerified = peer.confidence === 'HIGH';
        const blipRadius = isSelected ? 22 : 18;

        // Outer Glow
        ctx.beginPath();
        ctx.arc(px, py, blipRadius + 4, 0, Math.PI * 2);
        ctx.fillStyle = isSelected
          ? 'rgba(56, 189, 248, 0.35)'
          : isVerified
          ? 'rgba(16, 185, 129, 0.25)'
          : 'rgba(249, 115, 22, 0.2)';
        ctx.fill();

        // Avatar Circle Background
        ctx.beginPath();
        ctx.arc(px, py, blipRadius, 0, Math.PI * 2);
        ctx.fillStyle = '#0C111C';
        ctx.fill();
        ctx.strokeStyle = isSelected
          ? '#38BDF8'
          : isVerified
          ? '#10B981'
          : '#F97316';
        ctx.lineWidth = isSelected ? 2.5 : 1.5;
        ctx.stroke();

        // Draw Cached Avatar or Fallback Initial
        const img = avatarImagesRef.current.get(peer.user_id);
        if (img && img.complete && img.naturalWidth > 0) {
          ctx.save();
          ctx.beginPath();
          ctx.arc(px, py, blipRadius - 1.5, 0, Math.PI * 2);
          ctx.clip();
          ctx.drawImage(img, px - blipRadius, py - blipRadius, blipRadius * 2, blipRadius * 2);
          ctx.restore();
        } else {
          ctx.fillStyle = '#94A3B8';
          ctx.font = 'bold 12px "Space Grotesk", sans-serif';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText((peer.display_name || '?').charAt(0).toUpperCase(), px, py);
        }

        // Distance Tag below blip
        ctx.fillStyle = '#E2E8F0';
        ctx.font = '10px "DM Mono", monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(`${Math.round(peer.distance_meters)}m`, px, py + blipRadius + 4);
      });

      // 4. Draw Center User Core ("You")
      ctx.beginPath();
      ctx.arc(cx, cy, 22, 0, Math.PI * 2);
      ctx.fillStyle = '#0F172A';
      ctx.fill();
      ctx.strokeStyle = '#10B981';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Pulsing Center Ring
      ctx.beginPath();
      ctx.arc(cx, cy, 28, 0, Math.PI * 2);
      ctx.strokeStyle = 'rgba(16, 185, 129, 0.4)';
      ctx.lineWidth = 1;
      ctx.stroke();

      ctx.fillStyle = '#10B981';
      ctx.font = 'bold 13px "Space Grotesk", sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(userInitial.toUpperCase() || 'ME', cx, cy);

      ctx.restore();
      animFrameRef.current = requestAnimationFrame(render);
    };

    render();

    return () => {
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [peers, selectedPeerId, isScanning, userInitial]);

  // Click / Touch Detection on Peer Blips
  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const size = Math.min(rect.width, rect.height);
    const scale = size / rect.width;
    const x = (e.clientX - rect.left) * scale;
    const y = (e.clientY - rect.top) * scale;
    const cx = size / 2;
    const cy = size / 2;
    const maxRadius = size * 0.45;

    for (const peer of peers) {
      const peerAngle = getPeerAngle(peer.user_id);
      const normalizedDist = Math.min(Math.max(peer.distance_meters, 3), 100);
      const distRatio = 0.15 + (normalizedDist / 100) * 0.8;
      const px = cx + Math.cos(peerAngle) * maxRadius * distRatio;
      const py = cy + Math.sin(peerAngle) * maxRadius * distRatio;

      const dist = Math.hypot(x - px, y - py);
      if (dist <= 26) {
        onSelectPeer(peer);
        if ('vibrate' in navigator) navigator.vibrate(10);
        return;
      }
    }
  };

  return (
    <div className="relative w-full aspect-square max-w-[420px] mx-auto flex items-center justify-center p-2">
      <canvas
        ref={canvasRef}
        onClick={handleCanvasClick}
        className="w-full h-full cursor-pointer touch-none drop-shadow-[0_0_35px_rgba(56,189,248,0.12)]"
      />
    </div>
  );
};
