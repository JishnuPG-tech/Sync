import React, { useState } from 'react';

interface WelcomeScreenProps {
  onGetStarted: () => void;
}

export const WelcomeScreen: React.FC<WelcomeScreenProps> = ({ onGetStarted }) => {
  const [currentSlide, setCurrentSlide] = useState(0);

  const slides = [
    {
      title: (
        <>
          <span className="block text-[#F3F0E9]">Meet nearby.</span>
          <span className="block text-[#F3F0E9]">Connect by <span className="italic text-[#E18462]">choice.</span></span>
        </>
      ),
      description: 'Discover opted-in people around you through Bluetooth radar. Zero cloud tracking, zero location broadcasting.',
      renderIllustration: () => (
        <div className="relative w-[220px] h-[220px] flex items-center justify-center my-4">
          {/* Concentric radar circles matching WelcomeScreen.kt */}
          <div className="absolute w-[210px] h-[210px] rounded-full border border-[#292C28]" />
          <div className="absolute w-[136px] h-[136px] rounded-full border border-[#292C28]" />
          <div className="absolute w-[66px] h-[66px] rounded-full border border-[#E18462]/40 bg-[#E18462]/10" />
          
          {/* Center Target Bullseye */}
          <div className="relative w-6 h-6 rounded-full bg-[#E18462] flex items-center justify-center shadow-[0_0_12px_rgba(225,132,98,0.5)]">
            <div className="w-1.5 h-1.5 rounded-full bg-white" />
          </div>

          {/* 4 Orbit Avatars matching native APK exactly */}
          <div className="absolute -top-1 left-3">
            <img
              src="/portraits/portrait_02.jpg"
              alt="Peer"
              className="w-[42px] h-[42px] rounded-full object-cover border-2 border-[#E18462] shadow-md"
            />
          </div>
          <div className="absolute -top-0 right-3">
            <img
              src="/portraits/portrait_03.jpg"
              alt="Peer"
              className="w-[42px] h-[42px] rounded-full object-cover border-2 border-[#E18462] shadow-md"
            />
          </div>
          <div className="absolute bottom-2 left-4">
            <img
              src="/portraits/portrait_04.jpg"
              alt="Peer"
              className="w-[42px] h-[42px] rounded-full object-cover border-2 border-[#E18462] shadow-md"
            />
          </div>
          <div className="absolute bottom-3 right-4">
            <img
              src="/portraits/portrait_01.jpg"
              alt="Peer"
              className="w-[42px] h-[42px] rounded-full object-cover border-2 border-[#E18462] shadow-md"
            />
          </div>
        </div>
      )
    },
    {
      title: (
        <>
          <span className="block text-[#F3F0E9]">Instagram stays</span>
          <span className="block italic text-[#E18462]">strictly private.</span>
        </>
      ),
      description: 'Your Instagram username remains locked and hidden until both people mutually accept a connection request.',
      renderIllustration: () => (
        <div className="w-[220px] h-[220px] flex items-center justify-center my-4">
          <div className="w-[120px] h-[120px] rounded-full bg-[#E18462]/15 border-2 border-[#E18462] flex items-center justify-center shadow-[0_0_24px_rgba(225,132,98,0.2)]">
            <svg className="w-14 h-14 text-[#E18462]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
          </div>
        </div>
      )
    },
    {
      title: (
        <>
          <span className="block text-[#F3F0E9]">Encounters stay</span>
          <span className="block italic text-[#E18462]">saved in feed.</span>
        </>
      ),
      description: "When people walk out of radar range, they won't vanish. You can review profiles and connect whenever you choose.",
      renderIllustration: () => (
        <div className="w-[220px] h-[220px] flex items-center justify-center my-4">
          <div className="w-[120px] h-[120px] rounded-full bg-[#8BAD91]/15 border-2 border-[#8BAD91] flex items-center justify-center shadow-[0_0_24px_rgba(139,173,145,0.2)]">
            <svg className="w-14 h-14 text-[#8BAD91]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.6" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
        </div>
      )
    }
  ];

  const current = slides[currentSlide];

  return (
    <div className="fixed inset-0 z-40 flex flex-col justify-between bg-[#0B0C0B] px-6 py-6 pt-safe pb-safe select-none max-w-md mx-auto">
      {/* Top Header: Brand Lockup + SKIP */}
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
          onClick={onGetStarted}
          className="px-2 py-1 rounded font-mono font-medium text-[11px] text-[#9B9B94] tracking-widest uppercase hover:text-[#F3F0E9] active:opacity-70 transition-colors"
        >
          SKIP
        </button>
      </div>

      {/* Slide Content */}
      <div className="flex flex-col items-center justify-center flex-1 my-auto text-center">
        {current.renderIllustration()}

        <h2 className="font-serif font-medium text-[28px] leading-[34px] tracking-[-0.035em] mt-3">
          {current.title}
        </h2>

        <p className="font-sans font-normal text-[13px] leading-[19px] text-[#9B9B94] mt-2.5 max-w-[300px] mx-auto">
          {current.description}
        </p>
      </div>

      {/* Carousel Indicators + Action Buttons */}
      <div className="flex flex-col items-center w-full mt-4">
        {/* Pill Indicators */}
        <div className="flex items-center gap-1.5 mb-5">
          {[0, 1, 2].map((idx) => {
            const isActive = idx === currentSlide;
            return (
              <button
                key={idx}
                onClick={() => setCurrentSlide(idx)}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  isActive ? 'w-5 bg-[#E18462]' : 'w-1.5 bg-[#292C28]'
                }`}
                aria-label={`Slide ${idx + 1}`}
              />
            );
          })}
        </div>

        {/* Primary Action Button */}
        <button
          onClick={() => {
            if (currentSlide < 2) {
              setCurrentSlide(currentSlide + 1);
            } else {
              onGetStarted();
            }
          }}
          className="w-full h-12 rounded-[14px] bg-[#E18462] hover:bg-[#F0A083] active:scale-[0.98] text-[#FFFFFF] font-sans font-semibold text-[14.5px] transition-all flex items-center justify-center shadow-[0_4px_16px_rgba(225,132,98,0.25)]"
        >
          {currentSlide < 2 ? 'Next' : 'Get Started'}
        </button>

        {/* Secondary Action Button */}
        <button
          onClick={onGetStarted}
          className="w-full h-11 mt-2 rounded-[14px] bg-transparent border border-[#292C28] hover:border-[#383D37] active:scale-[0.98] text-[#9B9B94] font-sans font-medium text-[13.5px] transition-all flex items-center justify-center"
        >
          I already have an account
        </button>
      </div>
    </div>
  );
};
