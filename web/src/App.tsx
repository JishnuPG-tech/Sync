import React, { useState, useEffect, useRef } from 'react';
import { UserProfile, NearbyUserCard } from './types';
import { SplashScreen } from './components/SplashScreen';
import { WelcomeScreen } from './components/WelcomeScreen';
import { InstagramInputScreen } from './components/InstagramInputScreen';
import { ProfileSetupScreen } from './components/ProfileSetupScreen';
import { SyncTopBar } from './components/SyncTopBar';
import { AuraBottomBar, ActiveTab } from './components/AuraBottomBar';
import { RadarView } from './components/RadarView';
import { DiscoveredView } from './components/DiscoveredView';
import { ProfileView } from './components/ProfileView';
import { PeerDrawer } from './components/PeerDrawer';
import { MutualMatchModal } from './components/MutualMatchModal';
import {
  pingLocation,
  getNearbyUsers,
  sendConnectionRequest,
  getMutualConnections,
  getPendingRequests
} from './api';

const STORAGE_KEY = 'spott_user_session_v2';
const ONBOARDING_COMPLETED_KEY = 'spott_onboarding_completed_v2';

export const App: React.FC = () => {
  // Screen Stage state: 'splash' | 'welcome' | 'instagram' | 'profile_setup' | 'main'
  const [stage, setStage] = useState<'splash' | 'welcome' | 'instagram' | 'profile_setup' | 'main'>('splash');
  const [activeTab, setActiveTab] = useState<ActiveTab>('radar');

  // User Profile
  const [profile, setProfile] = useState<UserProfile | null>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null;
    }
  });

  // Staged profile data during onboarding
  const [stagedInstagram, setStagedInstagram] = useState<string>('');
  const [stagedFullName, setStagedFullName] = useState<string>('');
  const [stagedAvatar, setStagedAvatar] = useState<string>('');
  const [stagedBio, setStagedBio] = useState<string>('');

  // Peers & Interaction states
  const [nearbyPeers, setNearbyPeers] = useState<NearbyUserCard[]>([]);
  const [selectedPeer, setSelectedPeer] = useState<NearbyUserCard | null>(null);
  const [requestedPeerIds, setRequestedPeerIds] = useState<Set<string>>(new Set());
  const [connectedPeerIds, setConnectedPeerIds] = useState<Set<string>>(new Set());
  const [celebrationPeer, setCelebrationPeer] = useState<NearbyUserCard | null>(null);

  // Geolocation coordinates (real-time GPS / Network)
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null);
  const prevCelebratedRef = useRef<Set<string>>(new Set());

  // Real-time Geolocation tracking
  useEffect(() => {
    if (typeof navigator !== 'undefined' && 'geolocation' in navigator) {
      const watchId = navigator.geolocation.watchPosition(
        (pos) => {
          setCoords({
            lat: pos.coords.latitude,
            lng: pos.coords.longitude
          });
        },
        () => {
          // Fallback to IP location if GPS denied or timed out
          fetch('/api/v1/radar/ip-location')
            .then((r) => r.json())
            .then((data) => {
              if (data.latitude && data.longitude) {
                setCoords((prev) => prev || { lat: data.latitude, lng: data.longitude });
              }
            })
            .catch(() => {});
        },
        {
          enableHighAccuracy: true,
          timeout: 15000,
          maximumAge: 10000
        }
      );

      return () => {
        navigator.geolocation.clearWatch(watchId);
      };
    } else {
      fetch('/api/v1/radar/ip-location')
        .then((r) => r.json())
        .then((data) => {
          if (data.latitude && data.longitude) {
            setCoords({ lat: data.latitude, lng: data.longitude });
          }
        })
        .catch(() => {});
    }
  }, []);

  // Poll backend for real peers & connection updates
  useEffect(() => {
    if (stage !== 'main' || !profile) return;

    let isMounted = true;
    const fetchPeers = async () => {
      try {
        if (coords) {
          await pingLocation(
            profile.id,
            coords.lat,
            coords.lng,
            profile.display_name,
            profile.avatar_url
          );
        }

        const livePeers = await getNearbyUsers(profile.id, coords?.lat, coords?.lng, 50000);
        if (isMounted) {
          setNearbyPeers(livePeers);

          // Update connection status from live peers
          livePeers.forEach((p) => {
            if (p.connection_status === 'ACCEPTED') {
              setConnectedPeerIds((prev) => new Set(prev).add(p.user_id));
            } else if (p.connection_status === 'REQUEST_SENT' || p.is_connection_pending) {
              setRequestedPeerIds((prev) => new Set(prev).add(p.user_id));
            }
          });
        }

        // Fetch mutual connections
        const mutuals = await getMutualConnections(profile.id);
        if (isMounted && Array.isArray(mutuals)) {
          mutuals.forEach((m) => {
            setConnectedPeerIds((prev) => new Set(prev).add(m.user_id));
            // Trigger celebration if newly unlocked
            if (!prevCelebratedRef.current.has(m.user_id)) {
              prevCelebratedRef.current.add(m.user_id);
              const matchedPeer = livePeers.find((p) => p.user_id === m.user_id) || {
                user_id: m.user_id,
                display_name: m.display_name,
                bio: m.bio || '',
                location_name: 'Nearby',
                avatar_url: m.avatar_url,
                distance_meters: 1.0,
                confidence: 'HIGH',
                interests: [],
                instagram_username: m.instagram_username
              };
              setCelebrationPeer(matchedPeer);
            }
          });
        }
      } catch (err) {
        console.warn('Radar sync note:', err);
      }
    };

    fetchPeers();
    const interval = setInterval(fetchPeers, 3500);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, [stage, profile, coords]);

  // Handle splash completion
  const handleSplashFinished = () => {
    const hasCompletedOnboarding = localStorage.getItem(ONBOARDING_COMPLETED_KEY);
    if (profile && hasCompletedOnboarding) {
      setStage('main');
    } else {
      setStage('welcome');
    }
  };

  // Onboarding -> Instagram Connect
  const handleGetStarted = () => {
    setStage('instagram');
  };

  // Instagram Connect -> Profile Setup
  const handleInstagramContinue = (
    igHandle: string,
    fullName?: string,
    avatarUrl?: string,
    bio?: string
  ) => {
    setStagedInstagram(igHandle);
    setStagedFullName(fullName || igHandle);
    setStagedAvatar(avatarUrl || '/portraits/portrait_01.jpg');
    setStagedBio(bio || '');
    setStage('profile_setup');
  };

  // Profile Created -> Enter Main App
  const handleProfileCreated = (newProfile: UserProfile) => {
    setProfile(newProfile);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(newProfile));
    localStorage.setItem(ONBOARDING_COMPLETED_KEY, 'true');
    setStage('main');
  };

  // Handle Connect Request
  const handleConnect = async (peer: NearbyUserCard) => {
    setRequestedPeerIds((prev) => new Set(prev).add(peer.user_id));
    if (profile) {
      try {
        await sendConnectionRequest(profile.id, peer.user_id);
      } catch (e) {
        console.warn('Connect request error:', e);
      }
    }
  };

  // Reset Session
  const handleResetSession = () => {
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(ONBOARDING_COMPLETED_KEY);
    setProfile(null);
    setNearbyPeers([]);
    setRequestedPeerIds(new Set());
    setConnectedPeerIds(new Set());
    setStage('welcome');
  };

  // Re-run Onboarding Tour
  const handleRerunOnboarding = () => {
    setStage('welcome');
  };

  const handleManualScan = async () => {
    if (!profile) return;
    try {
      if (coords) {
        await pingLocation(profile.id, coords.lat, coords.lng, profile.display_name, profile.avatar_url);
      }
      const live = await getNearbyUsers(profile.id, coords?.lat, coords?.lng, 50000);
      setNearbyPeers(live);
    } catch {}
  };

  return (
    <div className="min-h-screen bg-[#0B0C0B] text-[#F3F0E9] font-sans antialiased overflow-x-hidden">
      {/* 1. Splash Screen */}
      {stage === 'splash' && (
        <SplashScreen onSplashFinished={handleSplashFinished} />
      )}

      {/* 2. Welcome / Onboarding Carousel */}
      {stage === 'welcome' && (
        <WelcomeScreen onGetStarted={handleGetStarted} />
      )}

      {/* 3. Instagram Input Screen */}
      {stage === 'instagram' && (
        <InstagramInputScreen
          initialHandle={profile?.instagram_username || ''}
          onContinue={handleInstagramContinue}
          onSkip={() => setStage('profile_setup')}
        />
      )}

      {/* 4. Profile Setup Screen */}
      {stage === 'profile_setup' && (
        <ProfileSetupScreen
          initialProfile={{
            id: profile?.id,
            display_name: stagedFullName || profile?.display_name || '',
            sync_username: stagedInstagram || profile?.sync_username || '',
            instagram_username: stagedInstagram || profile?.instagram_username || '',
            avatar_url: stagedAvatar || profile?.avatar_url || '/portraits/portrait_01.jpg',
            bio: stagedBio || profile?.bio || '',
            location: profile?.location || 'Nearby'
          }}
          onProfileCreated={handleProfileCreated}
        />
      )}

      {/* 5. Main App Experience with 3 Tabs */}
      {stage === 'main' && profile && (
        <div className="flex flex-col min-h-screen">
          <SyncTopBar onRefresh={handleManualScan} />

          <main className="flex-1 max-w-md mx-auto w-full">
            {activeTab === 'radar' && (
              <RadarView
                nearbyPeers={nearbyPeers}
                onConnect={handleConnect}
                requestedPeerIds={requestedPeerIds}
                connectedPeerIds={connectedPeerIds}
                myAvatar={profile.avatar_url || '/portraits/portrait_01.jpg'}
                myName={profile.display_name}
                onSelectPeer={(p) => setSelectedPeer(p)}
              />
            )}

            {activeTab === 'discovered' && (
              <DiscoveredView
                peers={nearbyPeers}
                onConnect={handleConnect}
                requestedPeerIds={requestedPeerIds}
                connectedPeerIds={connectedPeerIds}
                onSelectPeer={(p) => setSelectedPeer(p)}
              />
            )}

            {activeTab === 'profile' && (
              <ProfileView
                profile={profile}
                encountersCount={nearbyPeers.length}
                connectionsCount={connectedPeerIds.size}
                onEditProfile={() => setStage('profile_setup')}
                onRerunOnboarding={handleRerunOnboarding}
                onResetSession={handleResetSession}
              />
            )}
          </main>

          <AuraBottomBar
            currentTab={activeTab}
            onTabSelected={(tab) => setActiveTab(tab)}
            pendingCount={requestedPeerIds.size}
          />
        </div>
      )}

      {/* Peer Drawer Modal */}
      {selectedPeer && (
        <PeerDrawer
          peer={selectedPeer}
          isConnected={connectedPeerIds.has(selectedPeer.user_id) || selectedPeer.connection_status === 'ACCEPTED'}
          isPending={Boolean(requestedPeerIds.has(selectedPeer.user_id) || selectedPeer.is_connection_pending)}
          onConnect={handleConnect}
          onClose={() => setSelectedPeer(null)}
        />
      )}

      {/* Mutual Match Celebration Modal */}
      {celebrationPeer && profile && (
        <MutualMatchModal
          peer={celebrationPeer}
          myAvatar={profile.avatar_url || '/portraits/portrait_01.jpg'}
          onClose={() => setCelebrationPeer(null)}
        />
      )}
    </div>
  );
};

export default App;
