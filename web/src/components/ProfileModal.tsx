import React, { useState } from 'react';
import { UserProfile } from '../types';
import { verifyInstagram, registerUser } from '../api';
import { CheckCircle2, Instagram, Sparkles, AlertCircle, Camera } from 'lucide-react';

interface ProfileModalProps {
  currentProfile?: UserProfile | null;
  onSave: (profile: UserProfile) => void;
  isOpen: boolean;
  onClose?: () => void;
  isInitialSetup?: boolean;
}

export const ProfileModal: React.FC<ProfileModalProps> = ({
  currentProfile,
  onSave,
  isOpen,
  onClose,
  isInitialSetup = false,
}) => {
  const [instagramHandle, setInstagramHandle] = useState(currentProfile?.instagram_username || '');
  const [displayName, setDisplayName] = useState(currentProfile?.display_name || '');
  const [syncUsername, setSyncUsername] = useState(currentProfile?.sync_username || '');
  const [bio, setBio] = useState(currentProfile?.bio || '');
  const [location, setLocation] = useState(currentProfile?.location || 'Kochi, Kerala');
  const [avatarUrl, setAvatarUrl] = useState(currentProfile?.avatar_url || '');

  const [isVerifying, setIsVerifying] = useState(false);
  const [isVerified, setIsVerified] = useState(Boolean(currentProfile?.instagram_username));
  const [verifyError, setVerifyError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleVerify = async () => {
    const clean = instagramHandle.trim().replace(/^@/, '');
    if (!clean) return;
    setIsVerifying(true);
    setVerifyError(null);
    try {
      const data = await verifyInstagram(clean);
      if (data.exists) {
        setIsVerified(true);
        if (data.avatar_url) setAvatarUrl(data.avatar_url);
        if (data.full_name && !displayName) setDisplayName(data.full_name);
        if (data.description && !bio) setBio(data.description.slice(0, 60));
        if (!syncUsername) setSyncUsername(clean);
        if ('vibrate' in navigator) navigator.vibrate(20);
      } else {
        setVerifyError(data.error || 'Account not found');
        setIsVerified(false);
      }
    } catch (err: any) {
      setVerifyError(err.message || 'Verification failed');
      setIsVerified(false);
    } finally {
      setIsVerifying(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!instagramHandle.trim() || !displayName.trim()) {
      setSubmitError('Display Name and Instagram username are required.');
      return;
    }
    setIsSubmitting(true);
    setSubmitError(null);
    try {
      const saved = await registerUser({
        display_name: displayName.trim(),
        sync_username: (syncUsername || instagramHandle).trim().toLowerCase(),
        instagram_username: instagramHandle.trim().replace(/^@/, '').toLowerCase(),
        bio: bio.trim(),
        location: location.trim(),
        avatar_url: avatarUrl || undefined,
      });
      onSave(saved);
      if (onClose) onClose();
    } catch (err: any) {
      setSubmitError(err.message || 'Failed to save profile. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-md flex items-center justify-center p-4 overflow-y-auto animate-in fade-in duration-200">
      <div className="w-full max-w-lg bg-oled-card border border-oled-border rounded-3xl p-6 sm:p-8 shadow-2xl relative my-auto">
        {/* Header */}
        <div className="text-center">
          <h2 className="font-heading font-bold text-2xl text-ink-primary">
            {isInitialSetup ? 'Create Your Spott Profile' : 'Edit Profile'}
          </h2>
          <p className="text-xs font-mono text-ink-secondary mt-1">
            Connect physically with people nearby and reveal socials.
          </p>
        </div>

        {/* Avatar Display */}
        <div className="mt-6 flex flex-col items-center">
          <div className="relative group">
            <div className="w-24 h-24 rounded-full overflow-hidden border-2 border-radar-blue/60 bg-oled-surface shadow-[0_0_25px_rgba(56,189,248,0.2)] flex items-center justify-center">
              {avatarUrl ? (
                <img src={avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
              ) : (
                <div className="flex flex-col items-center text-ink-muted">
                  <Camera className="w-6 h-6 mb-1" />
                  <span className="text-[10px] font-mono">No Photo</span>
                </div>
              )}
            </div>
            {isVerified && (
              <div className="absolute -bottom-1 -right-1 bg-radar-green text-black rounded-full p-1 border-2 border-oled shadow-md">
                <CheckCircle2 className="w-4 h-4" />
              </div>
            )}
          </div>
          <span className="text-[11px] font-mono text-ink-muted mt-2">
            {avatarUrl ? 'Photo synced from Instagram' : 'Will sync photo from Instagram'}
          </span>
        </div>

        <form onSubmit={handleSubmit} className="mt-6 space-y-4">
          {/* Instagram Handle Input */}
          <div>
            <label className="block text-xs font-mono text-ink-secondary mb-1">
              Instagram Username *
            </label>
            <div className="flex gap-2">
              <div className="relative flex-1">
                <span className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-ink-muted">
                  <Instagram className="w-4 h-4 text-radar-blue" />
                </span>
                <input
                  type="text"
                  value={instagramHandle}
                  onChange={(e) => {
                    setInstagramHandle(e.target.value);
                    setIsVerified(false);
                  }}
                  placeholder="e.g. jishnu_p.g"
                  required
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-oled-surface border border-oled-border text-sm text-ink-primary font-mono placeholder:text-ink-muted focus:outline-none focus:border-radar-blue transition"
                />
              </div>
              <button
                type="button"
                onClick={handleVerify}
                disabled={isVerifying || !instagramHandle.trim()}
                className="px-4 py-2.5 rounded-xl bg-oled-surface hover:bg-oled-hover border border-oled-border text-xs font-mono font-bold text-radar-blue flex items-center gap-1.5 transition disabled:opacity-50"
              >
                {isVerifying ? (
                  <span className="inline-block w-3.5 h-3.5 border-2 border-radar-blue border-t-transparent rounded-full animate-spin" />
                ) : isVerified ? (
                  <>
                    <CheckCircle2 className="w-3.5 h-3.5 text-radar-green" />
                    Verified
                  </>
                ) : (
                  <>
                    <Sparkles className="w-3.5 h-3.5" />
                    Verify
                  </>
                )}
              </button>
            </div>
            {verifyError && (
              <p className="text-xs text-red-400 font-mono mt-1 flex items-center gap-1">
                <AlertCircle className="w-3 h-3" />
                {verifyError}
              </p>
            )}
          </div>

          {/* Display Name */}
          <div>
            <label className="block text-xs font-mono text-ink-secondary mb-1">
              Display Name *
            </label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="Your name or alias"
              required
              className="w-full px-3 py-2.5 rounded-xl bg-oled-surface border border-oled-border text-sm text-ink-primary font-sans placeholder:text-ink-muted focus:outline-none focus:border-radar-blue transition"
            />
          </div>

          {/* Short Bio */}
          <div>
            <label className="block text-xs font-mono text-ink-secondary mb-1">
              Bio / What are you up to?
            </label>
            <input
              type="text"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="e.g. Android dev & coffee explorer"
              maxLength={70}
              className="w-full px-3 py-2.5 rounded-xl bg-oled-surface border border-oled-border text-sm text-ink-primary font-sans placeholder:text-ink-muted focus:outline-none focus:border-radar-blue transition"
            />
            <p className="text-[10px] text-ink-muted font-mono mt-1 text-right">
              {bio.length}/70
            </p>
          </div>

          {/* Location */}
          <div>
            <label className="block text-xs font-mono text-ink-secondary mb-1">
              Location City
            </label>
            <input
              type="text"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="e.g. Kochi, Kerala"
              className="w-full px-3 py-2.5 rounded-xl bg-oled-surface border border-oled-border text-sm text-ink-primary font-sans placeholder:text-ink-muted focus:outline-none focus:border-radar-blue transition"
            />
          </div>

          {submitError && (
            <div className="p-3 rounded-xl bg-red-950/40 border border-red-500/30 text-xs text-red-400 font-mono flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{submitError}</span>
            </div>
          )}

          {/* Submit Button */}
          <div className="pt-2 flex gap-3">
            {onClose && !isInitialSetup && (
              <button
                type="button"
                onClick={onClose}
                className="w-1/3 py-3 rounded-xl bg-oled-surface hover:bg-oled-hover text-ink-secondary font-heading font-bold text-sm transition"
              >
                Cancel
              </button>
            )}
            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-1 py-3 px-6 rounded-xl bg-gradient-to-r from-radar-green to-emerald-400 text-black font-heading font-bold text-sm hover:opacity-95 shadow-[0_0_25px_rgba(16,185,129,0.3)] transition active:scale-[0.98] disabled:opacity-50"
            >
              {isSubmitting ? 'Saving...' : isInitialSetup ? 'Start Exploring Radar' : 'Save Changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
