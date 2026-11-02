import React from 'react';
import { UserProfile } from '../types';

interface ProfileViewProps {
  profile: UserProfile;
  encountersCount: number;
  connectionsCount: number;
  onEditProfile: () => void;
  onRerunOnboarding: () => void;
  onResetSession: () => void;
}

export const ProfileView: React.FC<ProfileViewProps> = ({
  profile,
  encountersCount,
  connectionsCount,
  onEditProfile,
  onRerunOnboarding,
  onResetSession
}) => {
  return (
    <div className="flex flex-col w-full max-w-md mx-auto px-4 pb-24 select-none">
      {/* Profile Hero Card */}
      <div className="p-5 mt-3 rounded-2xl bg-[#111311] border border-[#292C28]">
        <div className="flex items-center gap-4">
          <img
            src={profile.avatar_url || '/portraits/portrait_01.jpg'}
            alt={profile.display_name}
            className="w-16 h-16 rounded-full object-cover border-2 border-[#E18462] shadow-md"
          />
          <div className="flex-1 min-w-0">
            <h2 className="font-serif font-semibold text-[20px] text-[#F3F0E9] truncate">
              {profile.display_name}
            </h2>
            <p className="font-mono text-[12px] text-[#9B9B94]">
              @{profile.sync_username || profile.instagram_username}
            </p>
            <div className="flex items-center gap-1.5 mt-1">
              <span className="w-1.5 h-1.5 rounded-full bg-[#8BAD91]" />
              <span className="font-mono text-[11px] text-[#8BAD91]">
                @{profile.instagram_username} · Verified
              </span>
            </div>
          </div>
        </div>

        {profile.bio && (
          <p className="font-sans text-[13px] leading-[18px] text-[#F3F0E9] mt-4 pt-3 border-t border-[#292C28]/60">
            {profile.bio}
          </p>
        )}

        <div className="flex items-center justify-between mt-4 pt-3 border-t border-[#292C28]/60 font-mono text-[11px] text-[#9B9B94]">
          <span>Location: {profile.location || 'Local physical space'}</span>
          <span className="text-[#8BAD91] flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-[#8BAD91]" /> Radar Online
          </span>
        </div>
      </div>

      {/* Stats Counter */}
      <div className="grid grid-cols-2 gap-3 mt-3">
        <div className="p-4 rounded-2xl bg-[#111311] border border-[#292C28] text-center">
          <span className="font-mono font-bold text-[24px] text-[#F3F0E9]">
            {encountersCount}
          </span>
          <p className="font-sans text-[12px] text-[#9B9B94] mt-0.5">
            Physical Encounters
          </p>
        </div>
        <div className="p-4 rounded-2xl bg-[#111311] border border-[#292C28] text-center">
          <span className="font-mono font-bold text-[24px] text-[#E18462]">
            {connectionsCount}
          </span>
          <p className="font-sans text-[12px] text-[#9B9B94] mt-0.5">
            Mutual Connections
          </p>
        </div>
      </div>

      {/* Action Settings */}
      <div className="space-y-2 mt-4">
        <button
          onClick={onEditProfile}
          className="w-full p-4 rounded-2xl bg-[#111311] border border-[#292C28] hover:border-[#383D37] active:scale-[0.99] text-left flex items-center justify-between transition-all"
        >
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-[#E18462]/10 border border-[#E18462]/30 flex items-center justify-center">
              <svg className="w-4 h-4 text-[#E18462]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </div>
            <div>
              <span className="font-sans font-medium text-[14px] text-[#F3F0E9]">
                Edit Profile
              </span>
              <p className="font-sans text-[11.5px] text-[#9B9B94]">
                Change photo, display name, handle, or bio
              </p>
            </div>
          </div>
          <svg className="w-4 h-4 text-[#666963]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <button
          onClick={onRerunOnboarding}
          className="w-full p-4 rounded-2xl bg-[#111311] border border-[#292C28] hover:border-[#383D37] active:scale-[0.99] text-left flex items-center justify-between transition-all"
        >
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-[#8BAD91]/10 border border-[#8BAD91]/30 flex items-center justify-center">
              <svg className="w-4 h-4 text-[#8BAD91]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <span className="font-sans font-medium text-[14px] text-[#F3F0E9]">
                View Introduction Tour
              </span>
              <p className="font-sans text-[11.5px] text-[#9B9B94]">
                Replay splash animation and onboarding slides
              </p>
            </div>
          </div>
          <svg className="w-4 h-4 text-[#666963]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <button
          onClick={onResetSession}
          className="w-full p-4 rounded-2xl bg-[#111311] border border-[#292C28] hover:border-[#E06D6D]/40 active:scale-[0.99] text-left flex items-center justify-between transition-all"
        >
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-[#E06D6D]/10 border border-[#E06D6D]/30 flex items-center justify-center">
              <svg className="w-4 h-4 text-[#E06D6D]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </div>
            <div>
              <span className="font-sans font-medium text-[14px] text-[#E06D6D]">
                Reset Session & Identity
              </span>
              <p className="font-sans text-[11.5px] text-[#9B9B94]">
                Clears local profile and restarts onboarding
              </p>
            </div>
          </div>
        </button>
      </div>
    </div>
  );
};
