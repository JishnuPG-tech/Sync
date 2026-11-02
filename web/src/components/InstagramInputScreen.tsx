import React, { useState } from 'react';
import { verifyInstagramAccount } from '../api';

interface InstagramInputScreenProps {
  initialHandle?: string;
  onContinue: (
    instagramHandle: string,
    extractedFullName?: string,
    extractedAvatarUrl?: string,
    extractedBio?: string
  ) => void;
  onSkip: () => void;
}

export const InstagramInputScreen: React.FC<InstagramInputScreenProps> = ({
  initialHandle = '',
  onContinue,
  onSkip
}) => {
  const [handle, setHandle] = useState(initialHandle.replace(/^@/, ''));
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const clean = handle.trim().replace(/^@/, '').toLowerCase();
    if (!clean) {
      setErrorMessage('Please enter your Instagram username (or tap Skip)');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      // Lookup for real avatar & bio with fallback
      const res = await Promise.race([
        verifyInstagramAccount(clean),
        new Promise<{ exists: boolean; username: string; full_name?: string; avatar_url?: string; description?: string }>((resolve) =>
          setTimeout(() => resolve({ exists: true, username: clean }), 3000)
        )
      ]);

      if (res && res.exists) {
        onContinue(clean, res.full_name || clean, res.avatar_url, res.description);
      } else {
        onContinue(clean, clean, undefined, undefined);
      }
    } catch {
      onContinue(clean, clean, undefined, undefined);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-40 flex flex-col justify-between bg-[#0B0C0B] px-6 py-5 pt-safe pb-safe select-none max-w-md mx-auto overflow-y-auto">
      {/* Top Header */}
      <div className="flex items-center justify-between w-full h-11">
        <div className="flex items-center gap-2">
          <img
            src="/sync_emblem.png"
            alt="Spott Logo"
            className="w-6 h-6 object-contain"
          />
          <span className="font-serif font-semibold text-[20px] text-[#F3F0E9]">
            Spott
          </span>
        </div>

        <button
          onClick={onSkip}
          className="px-2 py-1 rounded font-mono font-medium text-[11px] text-[#9B9B94] tracking-widest uppercase hover:text-[#F3F0E9] active:opacity-70 transition-colors"
        >
          SKIP
        </button>
      </div>

      {/* Center Section: Badge, Title, Input */}
      <div className="flex flex-col items-center text-center my-auto py-6">
        {/* Instagram Badge */}
        <div className="w-[72px] h-[72px] rounded-full border-[1.5px] border-[#E18462]/45 bg-[#E18462]/10 flex items-center justify-center mb-6 shadow-[0_0_24px_rgba(225,132,98,0.2)]">
          <span className="font-mono font-bold text-[32px] text-[#E18462]">
            @
          </span>
        </div>

        {/* Heading */}
        <h1 className="font-serif font-medium text-[28px] tracking-[-0.02em] text-[#F3F0E9]">
          Enter your Instagram
        </h1>

        {/* Subtitle */}
        <p className="font-sans font-normal text-[13.5px] leading-[19px] text-[#9B9B94] mt-2 max-w-[310px] mx-auto">
          Connect your Instagram handle so nearby peers on radar can view and follow your profile.
        </p>

        {/* Form Input */}
        <form onSubmit={handleSubmit} className="w-full mt-8">
          <div className="relative flex items-center w-full h-[54px] rounded-2xl bg-[#111311] border border-[#292C28] focus-within:border-[#E18462] px-4 transition-all">
            <span className="font-mono font-bold text-[16px] text-[#9B9B94] mr-2">
              @
            </span>
            <input
              type="text"
              value={handle}
              onChange={(e) => {
                const val = e.target.value.trim().replace(/^@/, '').toLowerCase().replace(/[^a-z0-9._]/g, '');
                setHandle(val);
                setErrorMessage(null);
              }}
              placeholder="username"
              disabled={isSubmitting}
              autoCapitalize="none"
              autoCorrect="off"
              className="flex-1 bg-transparent border-none outline-none font-sans font-semibold text-[16.5px] text-[#F3F0E9] placeholder-[#666963]"
            />
            {handle.length > 0 && !isSubmitting && (
              <button
                type="button"
                onClick={() => setHandle('')}
                className="p-1 text-[#666963] hover:text-[#9B9B94]"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            )}
          </div>

          {errorMessage && (
            <p className="mt-2.5 font-sans text-[12.5px] text-[#E06D6D]">
              {errorMessage}
            </p>
          )}

          {/* Primary Action: Continue Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full h-[52px] mt-6 rounded-2xl bg-[#E18462] hover:bg-[#F0A083] active:scale-[0.98] disabled:opacity-50 text-black font-sans font-bold text-[15.5px] transition-all flex items-center justify-center gap-2 shadow-[0_4px_16px_rgba(225,132,98,0.25)]"
          >
            {isSubmitting ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-black border-t-transparent rounded-full animate-spin" />
                <span>Extracting @{handle}...</span>
              </div>
            ) : (
              <>
                <span>Continue</span>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                </svg>
              </>
            )}
          </button>

          {/* Secondary Button: Open Instagram */}
          <a
            href={handle ? `https://instagram.com/${handle}` : 'https://instagram.com'}
            target="_blank"
            rel="noopener noreferrer"
            className="w-full h-11 mt-3.5 rounded-[14px] bg-transparent border border-[#292C28] hover:border-[#383D37] active:scale-[0.98] text-[#9B9B94] font-sans font-medium text-[13px] transition-all flex items-center justify-center gap-1.5"
          >
            <span>Open Instagram App</span>
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
            </svg>
          </a>
        </form>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-center gap-1.5 pb-2 text-center">
        <svg className="w-3.5 h-3.5 text-[#8BAD91]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
        <span className="font-mono text-[10px] text-[#666963]">
          Zero passwords · Offline BLE proximity · Direct profile link
        </span>
      </div>
    </div>
  );
};
