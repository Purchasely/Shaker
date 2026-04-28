// Shaker — shared SVG art & icons

// ─────────────────────────────────────────────────────────────
// Cocktail glass art — procedural SVG placeholders
// Each glass shape is drawn by hand and filled with the cocktail's tint
// ─────────────────────────────────────────────────────────────
function CocktailArt({ cocktail, style = {} }) {
  const { glass, tint1, tint2, hue, garnish } = cocktail;
  const bgA = `hsl(${hue}, 45%, 88%)`;
  const bgB = `hsl(${hue + 20}, 55%, 72%)`;
  const gradId = `grad-${cocktail.id}`;
  const liqId  = `liq-${cocktail.id}`;

  return (
    <svg viewBox="0 0 200 200" preserveAspectRatio="xMidYMid slice" style={{ display: 'block', width: '100%', height: '100%', ...style }}>
      <defs>
        <linearGradient id={gradId} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={bgA} />
          <stop offset="100%" stopColor={bgB} />
        </linearGradient>
        <linearGradient id={liqId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={tint1} />
          <stop offset="100%" stopColor={tint2} />
        </linearGradient>
        <radialGradient id={`shine-${cocktail.id}`} cx="0.3" cy="0.2" r="0.4">
          <stop offset="0%" stopColor="rgba(255,255,255,0.7)" />
          <stop offset="100%" stopColor="rgba(255,255,255,0)" />
        </radialGradient>
      </defs>

      {/* background */}
      <rect x="0" y="0" width="200" height="200" fill={`url(#${gradId})`} />
      {/* soft light bokeh */}
      <circle cx="40"  cy="40"  r="30" fill="rgba(255,255,255,0.35)" />
      <circle cx="165" cy="55"  r="22" fill="rgba(255,255,255,0.25)" />
      <circle cx="170" cy="160" r="35" fill="rgba(255,255,255,0.18)" />

      {/* countertop */}
      <path d="M0 160 Q100 150 200 160 L200 200 L0 200 Z" fill="rgba(0,0,0,0.08)" />

      {/* glassware */}
      {glass === 'martini' && <MartiniGlass liqId={liqId} id={cocktail.id} />}
      {glass === 'coupe'   && <CoupeGlass   liqId={liqId} id={cocktail.id} />}
      {glass === 'rocks'   && <RocksGlass   liqId={liqId} id={cocktail.id} />}
      {glass === 'highball'&& <HighballGlass liqId={liqId} id={cocktail.id} />}

      {/* garnish overlay */}
      {garnish === 'mint'      && <MintGarnish />}
      {garnish === 'cherry'    && <CherryGarnish />}
      {garnish === 'lime'      && <LimeGarnish />}
      {garnish === 'orange'    && <OrangeGarnish />}
      {garnish === 'pineapple' && <PineappleGarnish />}
      {garnish === 'beans'     && <BeansGarnish />}
    </svg>
  );
}

function MartiniGlass({ liqId, id }) {
  return (
    <g>
      {/* bowl liquid */}
      <path d="M65 75 L135 75 L102 115 Z" fill={`url(#${liqId})`} />
      {/* glass edges */}
      <path d="M60 72 L140 72 L102 118 Z M102 118 L102 155 M82 158 L122 158" stroke="rgba(255,255,255,0.85)" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
      {/* shine */}
      <path d="M72 76 L92 106" stroke="rgba(255,255,255,0.6)" strokeWidth="2" strokeLinecap="round" fill="none" />
    </g>
  );
}

function CoupeGlass({ liqId, id }) {
  return (
    <g>
      <path d="M65 78 Q100 110 135 78 A35 10 0 0 0 65 78 Z" fill={`url(#${liqId})`} />
      <path d="M60 78 Q100 120 140 78 M102 120 L102 155 M82 158 L122 158" stroke="rgba(255,255,255,0.85)" strokeWidth="2" fill="none" strokeLinecap="round"/>
      <path d="M72 82 Q76 98 90 108" stroke="rgba(255,255,255,0.55)" strokeWidth="2" fill="none" strokeLinecap="round"/>
    </g>
  );
}

function RocksGlass({ liqId, id }) {
  return (
    <g>
      <rect x="70" y="80" width="60" height="65" rx="3" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.8)" strokeWidth="2"/>
      <rect x="72" y="105" width="56" height="38" fill={`url(#${liqId})`} opacity="0.95"/>
      {/* ice cubes */}
      <rect x="80"  y="100" width="16" height="16" fill="rgba(255,255,255,0.6)" rx="1" transform="rotate(-5 88 108)"/>
      <rect x="102" y="108" width="16" height="16" fill="rgba(255,255,255,0.55)" rx="1" transform="rotate(8 110 116)"/>
      {/* shine */}
      <line x1="76" y1="85" x2="76" y2="138" stroke="rgba(255,255,255,0.55)" strokeWidth="2"/>
    </g>
  );
}

function HighballGlass({ liqId, id }) {
  return (
    <g>
      <rect x="75" y="55" width="50" height="100" rx="3" fill="rgba(255,255,255,0.15)" stroke="rgba(255,255,255,0.8)" strokeWidth="2"/>
      <rect x="77" y="85" width="46" height="68" fill={`url(#${liqId})`} opacity="0.92"/>
      {/* bubbles */}
      <circle cx="92"  cy="110" r="2.5" fill="rgba(255,255,255,0.8)"/>
      <circle cx="102" cy="125" r="1.8" fill="rgba(255,255,255,0.7)"/>
      <circle cx="112" cy="100" r="2"   fill="rgba(255,255,255,0.7)"/>
      <circle cx="97"  cy="140" r="1.5" fill="rgba(255,255,255,0.6)"/>
      {/* straw */}
      <line x1="112" y1="52" x2="105" y2="150" stroke="#E85A68" strokeWidth="3" strokeLinecap="round"/>
      <line x1="115" y1="52" x2="108" y2="150" stroke="#F4F4F4" strokeWidth="3" strokeLinecap="round" opacity="0.6"/>
      {/* shine */}
      <line x1="80" y1="60" x2="80" y2="148" stroke="rgba(255,255,255,0.55)" strokeWidth="2"/>
    </g>
  );
}

// ─── Garnishes
function MintGarnish() {
  return (
    <g>
      <ellipse cx="95" cy="58" rx="8" ry="12" fill="#4FA265" transform="rotate(-20 95 58)"/>
      <ellipse cx="108" cy="52" rx="9" ry="13" fill="#5BB673" transform="rotate(15 108 52)"/>
      <ellipse cx="100" cy="48" rx="7" ry="10" fill="#6ECB86" transform="rotate(-5 100 48)"/>
    </g>
  );
}
function CherryGarnish() {
  return (
    <g>
      <path d="M108 50 Q112 42 118 46" stroke="#6A4A2A" strokeWidth="1.5" fill="none"/>
      <circle cx="106" cy="54" r="5" fill="#C42E3A"/>
      <circle cx="104" cy="53" r="1.5" fill="rgba(255,255,255,0.6)"/>
    </g>
  );
}
function LimeGarnish() {
  return (
    <g transform="translate(120 52) rotate(20)">
      <circle r="8" fill="#B6D84A" stroke="#8AA836" strokeWidth="1"/>
      <circle r="5" fill="#D9EC8E"/>
      <line x1="-5" y1="0" x2="5" y2="0" stroke="#8AA836" strokeWidth="0.5"/>
      <line x1="0" y1="-5" x2="0" y2="5" stroke="#8AA836" strokeWidth="0.5"/>
    </g>
  );
}
function OrangeGarnish() {
  return (
    <g>
      <path d="M100 55 Q110 48 115 58 Q112 63 105 62 Q99 60 100 55 Z" fill="#F4A64A"/>
      <path d="M100 55 Q110 48 115 58" stroke="#D98230" strokeWidth="1" fill="none"/>
    </g>
  );
}
function PineappleGarnish() {
  return (
    <g>
      <path d="M115 40 L118 55 L122 38 L126 55 L129 42 L127 60 L115 60 Z" fill="#3E8B3E"/>
      <path d="M115 58 Q122 50 129 58 L128 65 Q122 68 116 65 Z" fill="#F5C94C"/>
    </g>
  );
}
function BeansGarnish() {
  return (
    <g>
      <ellipse cx="95"  cy="72" rx="3" ry="5" fill="#3B2416" transform="rotate(-15 95 72)"/>
      <ellipse cx="102" cy="68" rx="3" ry="5" fill="#2F1D10" transform="rotate(5 102 68)"/>
      <ellipse cx="109" cy="72" rx="3" ry="5" fill="#3B2416" transform="rotate(20 109 72)"/>
    </g>
  );
}

// ─────────────────────────────────────────────────────────────
// Icons — stroke-based line icons, sized via size prop
// ─────────────────────────────────────────────────────────────
const Icon = ({ name, size = 24, color = 'currentColor', strokeWidth = 2, ...p }) => {
  const sw = strokeWidth;
  const paths = {
    home:     <><path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/></>,
    heart:    <path d="M12 21s-7-4.5-9.5-9A5 5 0 0 1 12 6a5 5 0 0 1 9.5 6c-2.5 4.5-9.5 9-9.5 9z"/>,
    heartFill:<path d="M12 21s-7-4.5-9.5-9A5 5 0 0 1 12 6a5 5 0 0 1 9.5 6c-2.5 4.5-9.5 9-9.5 9z" fill={color} stroke="none"/>,
    settings: <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></>,
    search:   <><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></>,
    filter:   <><path d="M4 5h16M7 12h10M10 19h4"/></>,
    sliders:  <><path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M18 18h2"/><circle cx="16" cy="6" r="2"/><circle cx="10" cy="12" r="2"/><circle cx="14" cy="18" r="2"/></>,
    back:     <><path d="M15 18l-6-6 6-6"/></>,
    close:    <><path d="M18 6L6 18M6 6l12 12"/></>,
    lock:     <><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/></>,
    chevR:    <path d="M9 6l6 6-6 6"/>,
    copy:     <><rect x="9" y="9" width="11" height="11" rx="2"/><path d="M5 15V5a2 2 0 0 1 2-2h10"/></>,
    user:     <><circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></>,
    check:    <path d="M5 12l5 5 10-10"/>,
    shield:   <><path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/></>,
    sparkle:  <><path d="M12 3v4M12 17v4M3 12h4M17 12h4M5.6 5.6l2.8 2.8M15.6 15.6l2.8 2.8M5.6 18.4l2.8-2.8M15.6 8.4l2.8-2.8"/></>,
    sun:      <><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></>,
    moon:     <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>,
    logout:   <><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4M10 17l5-5-5-5M15 12H3"/></>,
    plus:     <><path d="M12 5v14M5 12h14"/></>,
    chef:     <><path d="M12 21h-6a2 2 0 0 1-2-2v-3h16v3a2 2 0 0 1-2 2h-6z"/><path d="M4 14V9a5 5 0 0 1 5-5 3 3 0 0 1 6 0 5 5 0 0 1 5 5v5"/></>,
    trash:    <><path d="M4 7h16M9 7V4h6v3M7 7l1 13h8l1-13"/></>,
  };
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" {...p}>
      {paths[name]}
    </svg>
  );
}

// ─────────────────────────────────────────────────────────────
// Shaker logo mark
// ─────────────────────────────────────────────────────────────
function ShakerLogo({ size = 40, color = '#3C4876' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" fill="none">
      <path d="M13 6h14l-1.5 4h-11L13 6z" fill={color}/>
      <path d="M14 10h12l-1.5 18a4 4 0 0 1-4 3.6h-1a4 4 0 0 1-4-3.6L14 10z" fill={color}/>
      <circle cx="20" cy="18" r="2.5" fill="white"/>
      <circle cx="22" cy="23" r="1.5" fill="white" opacity="0.7"/>
      <circle cx="18" cy="25" r="1.2" fill="white" opacity="0.5"/>
    </svg>
  );
}

Object.assign(window, { CocktailArt, Icon, ShakerLogo });
