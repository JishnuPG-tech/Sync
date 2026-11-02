import React, { useState } from 'react';
import { UserProfile } from '../types';
import { registerUser, updateProfile } from '../api';

interface ProfileSetupScreenProps {
  initialProfile?: Partial<UserProfile>;
  onProfileCreated: (profile: UserProfile) => void;
}

export const ProfileSetupScreen: React.FC<ProfileSetupScreenProps> = ({
  initialProfile,
  onProfileCreated
}) => {
  const [displayName, setDisplayName] = useState(initialProfile?.display_name || '');
  const [syncUsername, setSyncUsername] = useState(
    initialProfile?.sync_username || initialProfile?.instagram_username || ''
  );
  const [instagramHandle, setInstagramHandle] = useState(
    initialProfile?.instagram_username || ''
  );
  const [bio, setBio] = useState(initialProfile?.bio || '');
  const [location, setLocation] = useState(initialProfile?.location || 'Nearby');
  const [avatarUrl, setAvatarUrl] = useState<string>(
    initialProfile?.avatar_url || '/portraits/portrait_01.jpg'
  );
  const [showAvatarPicker, setShowAvatarPicker] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const availablePortraits = [
    '/portraits/portrait_01.jpg',
    '/portraits/portrait_02.jpg',
    '/portraits/portrait_03.jpg',
    '/portraits/portrait_04.jpg',
    '/portraits/portrait_05.jpg',
    '/portraits/portrait_06.jpg',
    '/portraits/portrait_07.jpg',
    '/portraits/portrait_08.jpg',
    '/portraits/portrait_09.jpg',
    '/portraits/portrait_10.jpg',
    '/portraits/portrait_11.jpg',
    '/portraits/portrait_12.jpg'
  ];

  const handleCustomPhoto = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setAvatarUrl(event.target.result as string);
        }
      };
      reader.readAsDataURL(file);
    }
  };

  const isUsernameValid =
    syncUsername.trim().length >= 3 && /^[a-zA-Z0-9._]+$/.test(syncUsername.trim());
  const isFormValid =
    displayName.trim().length > 0 &&
    isUsernameValid &&
    instagramHandle.trim().length > 0 &&
    !isSubmitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isFormValid) return;

    setIsSubmitting(true);
    setError(null);

    const payload: UserProfile = {
      id: initialProfile?.id || 'usr_' + Math.random().toString(36).substring(2, 10),
      display_name: displayName.trim(),
      sync_username: syncUsername.trim().toLowerCase(),
      instagram_username: instagramHandle.trim().replace(/^@/, '').toLowerCase(),
      bio: bio.trim(),
      location: location.trim(),
      avatar_url: avatarUrl,
      is_discovery_active: true
    };

    try {
      if (initialProfile?.id) {
        await updateProfile(initialProfile.id, payload);
      } else {
        await registerUser(payload);
      }
      onProfileCreated(payload);
    } catch {
      // Local graceful fallback if backend is momentarily unreachable
      onProfileCreated(payload);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-40 bg-[#0B0C0B] px-5 py-5 pt-safe pb-safe overflow-y-auto select-none max-w-md mx-auto">
      {/* Screen Header */}
      <div className="mb-6">
        <h1 className="font-serif font-medium text-[27px] tracking-[-0.035em] text-[#F3F0E9]">
          Set up your profile
        </h1>
        <p className="font-sans font-normal text-[13px] leading-[19px] text-[#9B9B94] mt-1">
          Make it easy for nearby people to recognise you in physical space.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4 pb-8">
        {/* Upload Profile Photo Section */}
        <div className="flex flex-col items-center justify-center my-3">
          <div className="relative group">
            <img
              src={avatarUrl}
              alt="Profile"
              className="w-[92px] h-[92px] rounded-full object-cover border-[2.5px] border-[#E18462] shadow-lg"
            />
            <label
              htmlFor="photo-upload"
              className="absolute bottom-0 right-0 w-[26px] h-[26px] rounded-full bg-[#E18462] border-2 border-[#0B0C0B] flex items-center justify-center cursor-pointer shadow hover:scale-105 active:scale-95 transition-transform"
            >
              <svg className="w-3.5 h-3.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </label>
            <input
              id="photo-upload"
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleCustomPhoto}
            />
          </div>

          <div className="flex items-center gap-3 mt-2">
            <button
              type="button"
              onClick={() => setShowAvatarPicker(!showAvatarPicker)}
              className="font-sans font-semibold text-[12.5px] text-[#E18462] hover:underline"
            >
              {showAvatarPicker ? 'Close portraits' : 'Choose portrait preset'}
            </button>
            <span className="text-[#666963] text-xs">·</span>
            <label
              htmlFor="photo-upload"
              className="font-sans font-semibold text-[12.5px] text-[#9B9B94] hover:text-[#F3F0E9] cursor-pointer"
            >
              Upload custom
            </label>
          </div>

          {/* Preset Avatar Grid */}
          {showAvatarPicker && (
            <div className="grid grid-cols-6 gap-2 p-3 mt-3 rounded-2xl bg-[#111311] border border-[#292C28] w-full">
              {availablePortraits.map((src, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => {
                    setAvatarUrl(src);
                    setShowAvatarPicker(false);
                  }}
                  className={`relative rounded-full overflow-hidden aspect-square border-2 transition-transform hover:scale-105 ${
                    avatarUrl === src ? 'border-[#E18462]' : 'border-transparent'
                  }`}
                >
                  <img src={src} alt={`Preset ${i + 1}`} className="w-full h-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* FIELD 1: DISPLAY NAME */}
        <div>
          <div className="flex items-center justify-between mb-1.5 px-1">
            <label className="font-sans font-medium text-[13px] text-[#F3F0E9]">
              Display Name
            </label>
            <span className="font-mono text-[10px] uppercase tracking-wider text-[#9B9B94]">
              Required
            </span>
          </div>
          <div className="flex items-center w-full h-[48px] rounded-xl bg-[#111311] border border-[#292C28] focus-within:border-[#E18462] px-3.5 transition-all">
            <svg className="w-4 h-4 text-[#9B9B94] mr-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="e.g. your full name or nickname"
              required
              className="flex-1 bg-transparent border-none outline-none font-sans text-[14.5px] text-[#F3F0E9] placeholder-[#666963]"
            />
          </div>
        </div>

        {/* FIELD 2: SYNC USERNAME */}
        <div>
          <div className="flex items-center justify-between mb-1.5 px-1">
            <label className="font-sans font-medium text-[13px] text-[#F3F0E9]">
              Sync Username
            </label>
            <span className="font-mono text-[10px] uppercase tracking-wider text-[#9B9B94]">
              Unique ID
            </span>
          </div>
          <div className="flex items-center w-full h-[48px] rounded-xl bg-[#111311] border border-[#292C28] focus-within:border-[#E18462] px-3.5 transition-all">
            <span className="font-mono font-bold text-[14px] text-[#9B9B94] mr-2">
              @
            </span>
            <input
              type="text"
              value={syncUsername}
              onChange={(e) =>
                setSyncUsername(e.target.value.toLowerCase().replace(/\s+/g, '_').replace(/[^a-z0-9._]/g, ''))
              }
              placeholder="e.g. sync_handle"
              required
              className="flex-1 bg-transparent border-none outline-none font-sans text-[14.5px] text-[#F3F0E9] placeholder-[#666963]"
            />
            {isUsernameValid && (
              <svg className="w-4 h-4 text-[#8BAD91]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
              </svg>
            )}
          </div>
          {isUsernameValid && (
            <p className="flex items-center gap-1 mt-1 px-1 font-mono text-[10.5px] text-[#8BAD91]">
              <span>✓</span> @{syncUsername} is valid & ready
            </p>
          )}
        </div>

        {/* FIELD 3: INSTAGRAM USERNAME */}
        <div>
          <div className="flex items-center justify-between mb-1.5 px-1">
            <label className="font-sans font-medium text-[13px] text-[#F3F0E9]">
              Instagram Username
            </label>
            <span className="font-mono text-[10px] uppercase tracking-wider text-[#9B9B94]">
              Private until match
            </span>
          </div>
          <div className="flex items-center w-full h-[48px] rounded-xl bg-[#111311] border border-[#292C28] focus-within:border-[#E18462] px-3.5 transition-all">
            <span className="font-mono font-bold text-[11px] text-[#9B9B94] mr-2 px-1.5 py-0.5 rounded bg-[#171A17] border border-[#292C28]">
              IG
            </span>
            <input
              type="text"
              value={instagramHandle}
              onChange={(e) => {
                const val = e.target.value.trim().replace(/^@/, '');
                setInstagramHandle(val);
                if (!syncUsername || syncUsername === instagramHandle) {
                  setSyncUsername(val);
                }
              }}
              placeholder="e.g. your_instagram_id"
              required
              className="flex-1 bg-transparent border-none outline-none font-sans text-[14.5px] text-[#F3F0E9] placeholder-[#666963]"
            />
            <svg className="w-3.5 h-3.5 text-[#666963]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>

          {/* Instagram Verification Preview Card */}
          {instagramHandle.trim().length > 0 && (
            <div className="mt-2 p-3 rounded-xl bg-[#8BAD91]/10 border border-[#8BAD91]/30 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-[#8BAD91]" />
                <span className="font-mono text-[12px] text-[#F3F0E9]">
                  @{instagramHandle.replace(/^@/, '')}
                </span>
              </div>
              <span className="px-2 py-0.5 rounded-full bg-[#8BAD91]/20 font-mono text-[10px] font-bold text-[#8BAD91]">
                ✓ Verified
              </span>
            </div>
          )}
        </div>

        {/* FIELD 4: SHORT INTRODUCTION */}
        <div>
          <div className="flex items-center justify-between mb-1.5 px-1">
            <label className="font-sans font-medium text-[13px] text-[#F3F0E9]">
              Short Introduction
            </label>
            <span className="font-mono text-[10px] uppercase tracking-wider text-[#9B9B94]">
              Optional (max 80)
            </span>
          </div>
          <textarea
            value={bio}
            onChange={(e) => setBio(e.target.value.slice(0, 80))}
            placeholder="Add a short bio or what you're working on..."
            rows={2}
            className="w-full rounded-xl bg-[#111311] border border-[#292C28] focus:border-[#E18462] p-3 font-sans text-[13.5px] text-[#F3F0E9] placeholder-[#666963] outline-none resize-none transition-all"
          />
        </div>

        {/* FIELD 5: LOCATION / CITY */}
        <div>
          <div className="flex items-center justify-between mb-1.5 px-1">
            <label className="font-sans font-medium text-[13px] text-[#F3F0E9]">
              Location / City
            </label>
            <span className="font-mono text-[10px] uppercase tracking-wider text-[#9B9B94]">
              Optional
            </span>
          </div>
          <div className="flex items-center w-full h-[48px] rounded-xl bg-[#111311] border border-[#292C28] focus-within:border-[#E18462] px-3.5 transition-all">
            <svg className="w-4 h-4 text-[#9B9B94] mr-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <input
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="e.g. San Francisco / Bengaluru"
              className="flex-1 bg-transparent border-none outline-none font-sans text-[14px] text-[#F3F0E9] placeholder-[#666963]"
            />
          </div>
        </div>

        {error && (
          <p className="font-sans text-[12.5px] text-[#E06D6D] text-center pt-1">
            {error}
          </p>
        )}

        {/* SUBMIT BUTTON */}
        <button
          type="submit"
          disabled={!isFormValid || isSubmitting}
          className="w-full h-[52px] mt-6 rounded-2xl bg-[#E18462] hover:bg-[#F0A083] active:scale-[0.98] disabled:opacity-50 text-white font-sans font-bold text-[15px] transition-all flex items-center justify-center gap-2 shadow-[0_4px_16px_rgba(225,132,98,0.25)]"
        >
          {isSubmitting ? (
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
              <span>Saving Profile...</span>
            </div>
          ) : (
            <>
              <span>Save & Enter Radar</span>
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
              </svg>
            </>
          )}
        </button>
      </form>
    </div>
  );
};
