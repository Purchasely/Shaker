// Shaker — Onboarding screens (theme-aware)

function OnboardingWelcome({ platform, theme, onContinue, onSkip }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', fontFamily: FONT_STACK[platform], paddingTop: 54, position: 'relative', background: theme.bg }}>
      <div onClick={onSkip} style={{ position: 'absolute', top: 66, right: 20, zIndex: 10, width: 36, height: 36, borderRadius: 18, background: theme.dark ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.9)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', backdropFilter: 'blur(8px)', cursor: 'pointer' }}>
        <Icon name="close" size={18} color={theme.dark ? '#fff' : theme.text} />
      </div>

      <div style={{
        height: 360, margin: '20px 20px 0', borderRadius: 28, overflow: 'hidden', position: 'relative',
        background: 'linear-gradient(135deg, #FFE4B5 0%, #FFB47A 50%, #FF8A5C 100%)',
      }}>
        <svg viewBox="0 0 300 300" style={{ width: '100%', height: '100%' }} preserveAspectRatio="xMidYMid slice">
          <defs>
            <radialGradient id="ob-sun" cx="0.2" cy="0.2" r="0.8">
              <stop offset="0%" stopColor="#FFE59E"/>
              <stop offset="100%" stopColor="#FF8A5C"/>
            </radialGradient>
          </defs>
          <rect width="300" height="300" fill="url(#ob-sun)"/>
          {Array.from({length: 8}).map((_, i) => (
            <rect key={i} x="0" y={i*36} width="300" height="2" fill="rgba(255,255,255,0.2)" transform={`rotate(${i*5} 150 150)`}/>
          ))}
          <g transform="translate(60 90)">
            <path d="M0 20 L50 20 L55 15 L45 0 L5 0 L-5 15 Z" fill="#2A3046"/>
            <path d="M-5 20 L55 20 L60 180 Q55 200 25 200 Q-5 200 -10 180 Z" fill="#3A4258" stroke="#1D2236" strokeWidth="2"/>
            <ellipse cx="25" cy="20" rx="30" ry="6" fill="#4A5374"/>
            <path d="M3 40 L3 160" stroke="rgba(255,255,255,0.4)" strokeWidth="3"/>
          </g>
          <g transform="translate(150 105)">
            <rect x="0" y="0" width="70" height="140" rx="4" fill="rgba(255,255,255,0.45)" stroke="#fff" strokeWidth="2"/>
            <rect x="3" y="40" width="64" height="97" fill="#FFA03A" opacity="0.95"/>
            <rect x="10" y="50" width="20" height="20" fill="rgba(255,255,255,0.7)" rx="2" transform="rotate(-5 20 60)"/>
            <rect x="38" y="70" width="18" height="18" fill="rgba(255,255,255,0.6)" rx="2" transform="rotate(8 47 79)"/>
            <circle cx="20" cy="100" r="3" fill="rgba(255,255,255,0.8)"/>
            <circle cx="45" cy="115" r="2" fill="rgba(255,255,255,0.8)"/>
            <circle cx="55" cy="-6" r="18" fill="#F4A64A" stroke="#D57E20" strokeWidth="2"/>
            <circle cx="55" cy="-6" r="12" fill="#F6BE6F"/>
            <line x1="48" y1="-30" x2="38" y2="135" stroke="#C62A2A" strokeWidth="5" strokeLinecap="round"/>
          </g>
          <g transform="translate(230 170)">
            <path d="M0 0 L40 0 L20 35 Z" fill="rgba(255,255,255,0.6)"/>
            <circle cx="20" cy="8" r="4" fill="#EC4A5D"/>
          </g>
          <g transform="translate(30 230)">
            <ellipse cx="0" cy="0" rx="14" ry="6" fill="#6BB374" transform="rotate(-20)"/>
            <ellipse cx="14" cy="-4" rx="12" ry="5" fill="#4FA265" transform="rotate(10)"/>
          </g>
        </svg>
      </div>

      <div style={{ padding: '36px 24px 0', flex: 1 }}>
        <h1 style={{ fontSize: 30, fontWeight: 700, color: theme.text, margin: 0, letterSpacing: -0.7, textAlign: 'center' }}>
          Welcome to Shaker
        </h1>
        <p style={{ fontSize: 16, color: theme.textSec, marginTop: 12, lineHeight: 1.5, textAlign: 'center' }}>
          The easiest way to discover cocktails you can actually make — tonight.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 28 }}>
          <div style={{ width: 18, height: 6, borderRadius: 3, background: theme.accent }}/>
          <div style={{ width: 6, height: 6, borderRadius: 3, background: theme.indigoSoft }}/>
          <div style={{ width: 6, height: 6, borderRadius: 3, background: theme.indigoSoft }}/>
        </div>
      </div>

      <div style={{ padding: '0 24px 60px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <PillButton variant="primary" platform={platform} theme={theme} full onClick={onContinue}>Continue</PillButton>
        <PillButton variant="ghost"   platform={platform} theme={theme} full onClick={onSkip}>Skip onboarding</PillButton>
      </div>
    </div>
  );
}

function OnboardingMix({ platform, theme, onContinue, onSkip }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', fontFamily: FONT_STACK[platform], paddingTop: 54, position: 'relative', background: theme.bg }}>
      <div onClick={onSkip} style={{ position: 'absolute', top: 66, right: 20, zIndex: 10, width: 36, height: 36, borderRadius: 18, background: theme.dark ? 'rgba(255,255,255,0.15)' : 'rgba(255,255,255,0.9)', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.08)', cursor: 'pointer' }}>
        <Icon name="close" size={18} color={theme.dark ? '#fff' : theme.text} />
      </div>

      <div style={{
        height: 360, margin: '20px 20px 0', borderRadius: 28, overflow: 'hidden', position: 'relative',
        background: 'linear-gradient(140deg, #E7643D 0%, #D44A3A 50%, #A3342E 100%)',
      }}>
        <svg viewBox="0 0 300 300" preserveAspectRatio="xMidYMid slice" style={{ width: '100%', height: '100%' }}>
          {Array.from({length: 8}).map((_, r) =>
            Array.from({length: 8}).map((_, c) => (
              <rect key={`${r}-${c}`} x={c*40} y={r*40} width="38" height="38" rx="2"
                    fill={`hsl(${15+r*3}, 70%, ${55-c*2}%)`}/>
            ))
          )}
          <g transform="translate(30 140)">
            <rect width="50" height="100" rx="3" fill="rgba(255,255,255,0.25)" stroke="#fff" strokeWidth="2"/>
            <rect x="3" y="30" width="44" height="67" fill="#FFC24B"/>
            <rect x="8" y="40" width="14" height="14" fill="rgba(255,255,255,0.7)" rx="2"/>
          </g>
          <g transform="translate(130 100)">
            <path d="M0 25 L80 25 L40 75 Z" fill="#F58A9E"/>
            <path d="M-5 22 L85 22 L40 80 Z M40 80 L40 135 M20 138 L60 138" stroke="#fff" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
            <circle cx="35" cy="35" r="5" fill="#C42E3A"/>
            <path d="M35 30 Q42 20 50 25" stroke="#6A4A2A" strokeWidth="1.5" fill="none"/>
            <circle cx="10" cy="10" r="3" fill="#fff" opacity="0.7"/>
            <circle cx="72" cy="8"  r="4" fill="#fff" opacity="0.7"/>
            <circle cx="40" cy="0"  r="3" fill="#fff" opacity="0.7"/>
          </g>
          <g transform="translate(220 160)">
            <path d="M0 0 Q30 40 60 0 Z" fill="#6ED9B4"/>
            <path d="M-3 -3 Q30 45 63 -3 M30 40 L30 80 M15 82 L45 82" stroke="#fff" strokeWidth="2" fill="none" strokeLinecap="round"/>
            <rect x="15" y="5" width="10" height="10" fill="rgba(255,255,255,0.7)" rx="1"/>
            <rect x="32" y="10" width="8" height="8" fill="rgba(255,255,255,0.6)" rx="1"/>
          </g>
          <ellipse cx="105" cy="230" rx="25" ry="18" fill="#F6D94A"/>
          <ellipse cx="105" cy="225" rx="22" ry="14" fill="#FCE87C"/>
          <path d="M245 265 A30 30 0 0 1 295 265 L270 300 Z" fill="#E54C5C"/>
          <path d="M245 265 A30 30 0 0 1 295 265" fill="none" stroke="#5FAE3A" strokeWidth="6"/>
        </svg>
      </div>

      <div style={{ padding: '36px 24px 0', flex: 1 }}>
        <h1 style={{ fontSize: 30, fontWeight: 700, color: theme.text, margin: 0, letterSpacing: -0.7, textAlign: 'center' }}>
          Mix with what you have
        </h1>
        <p style={{ fontSize: 16, color: theme.textSec, marginTop: 12, lineHeight: 1.5, textAlign: 'center' }}>
          Tell us your ingredients and Shaker finds the perfect cocktails. No fancy shopping list needed.
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginTop: 28 }}>
          <div style={{ width: 6, height: 6, borderRadius: 3, background: theme.indigoSoft }}/>
          <div style={{ width: 18, height: 6, borderRadius: 3, background: theme.orange }}/>
          <div style={{ width: 6, height: 6, borderRadius: 3, background: theme.indigoSoft }}/>
        </div>
      </div>

      <div style={{ padding: '0 24px 60px', display: 'flex', flexDirection: 'column', gap: 10 }}>
        <PillButton variant="orange" platform={platform} theme={theme} full onClick={onContinue}>Show me more</PillButton>
        <PillButton variant="ghost"  platform={platform} theme={theme} full onClick={onSkip}>Skip</PillButton>
      </div>
    </div>
  );
}

Object.assign(window, { OnboardingWelcome, OnboardingMix });
