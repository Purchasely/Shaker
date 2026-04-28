// Shaker — iOS device chrome + tab bar (theme-aware)

function IOSFrame({ children, width = 390, height = 844, theme }) {
  const statusDark = theme.statusDark;
  return (
    <div style={{
      width, height, borderRadius: 52, overflow: 'hidden', position: 'relative',
      background: theme.bg,
      boxShadow: '0 30px 60px rgba(20,20,40,0.22), 0 0 0 10px #111 inset, 0 0 0 11px #333',
      fontFamily: '-apple-system, BlinkMacSystemFont, "SF Pro Text", system-ui, sans-serif',
      WebkitFontSmoothing: 'antialiased',
      boxSizing: 'content-box',
      border: '3px solid #1a1a1a',
    }}>
      <div style={{
        position: 'absolute', top: 11, left: '50%', transform: 'translateX(-50%)',
        width: 120, height: 34, borderRadius: 20, background: '#000', zIndex: 50,
      }} />
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0, height: 54, zIndex: 40,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '18px 32px 0',
        color: statusDark ? '#0A0A1A' : '#fff',
        fontSize: 16, fontWeight: 600, letterSpacing: -0.2, pointerEvents: 'none',
      }}>
        <div style={{ width: 70, paddingLeft: 8 }}>9:41</div>
        <div style={{ flex: 1 }} />
        <div style={{ display: 'flex', gap: 6, alignItems: 'center', paddingRight: 6 }}>
          <svg width="18" height="12" viewBox="0 0 18 12" fill={statusDark ? '#0A0A1A' : '#fff'}>
            <rect x="0"  y="8" width="3" height="4" rx="0.5"/>
            <rect x="5"  y="6" width="3" height="6" rx="0.5"/>
            <rect x="10" y="3" width="3" height="9" rx="0.5"/>
            <rect x="15" y="0" width="3" height="12" rx="0.5"/>
          </svg>
          <svg width="16" height="12" viewBox="0 0 16 12" fill={statusDark ? '#0A0A1A' : '#fff'}>
            <path d="M8 3a9 9 0 0 1 6 2.3l-1.2 1.2A7 7 0 0 0 8 4.5 7 7 0 0 0 3.2 6.5L2 5.3A9 9 0 0 1 8 3z"/>
            <path d="M8 6.5a5 5 0 0 1 3.3 1.3l-1.2 1.2A3 3 0 0 0 8 8 3 3 0 0 0 5.9 9L4.7 7.8A5 5 0 0 1 8 6.5z"/>
            <circle cx="8" cy="10.3" r="1.2"/>
          </svg>
          <svg width="26" height="12" viewBox="0 0 26 12" fill="none">
            <rect x="0.5" y="0.5" width="22" height="11" rx="3" stroke={statusDark ? '#0A0A1A' : '#fff'} strokeOpacity="0.4"/>
            <rect x="2" y="2" width="18" height="8" rx="1.5" fill={statusDark ? '#0A0A1A' : '#fff'}/>
            <rect x="23" y="4" width="2" height="4" rx="1" fill={statusDark ? '#0A0A1A' : '#fff'} fillOpacity="0.5"/>
          </svg>
        </div>
      </div>
      <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column' }}>
        {children}
      </div>
      <div style={{
        position: 'absolute', bottom: 8, left: '50%', transform: 'translateX(-50%)',
        width: 134, height: 5, borderRadius: 3,
        background: statusDark ? 'rgba(0,0,0,0.35)' : 'rgba(255,255,255,0.6)', zIndex: 60, pointerEvents: 'none',
      }} />
    </div>
  );
}

function IOSTabBar({ active = 'home', theme, onTab }) {
  const tabs = [
    { id: 'home',      label: 'Home',      icon: 'home' },
    { id: 'favorites', label: 'Favorites', icon: 'heart' },
    { id: 'settings',  label: 'Settings',  icon: 'settings' },
  ];
  const activeC = theme.dark ? '#fff' : theme.indigo;
  const idle = theme.textSec;
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      paddingBottom: 28, paddingTop: 10,
      background: theme.dark ? 'rgba(27,29,48,0.85)' : 'rgba(243,241,249,0.85)',
      backdropFilter: 'blur(20px) saturate(180%)',
      WebkitBackdropFilter: 'blur(20px) saturate(180%)',
      borderTop: `0.5px solid ${theme.hair}`,
      display: 'flex', justifyContent: 'space-around',
      zIndex: 30,
    }}>
      {tabs.map(t => {
        const isActive = t.id === active;
        return (
          <div key={t.id}
            onClick={() => onTab && onTab(t.id)}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, minWidth: 54, cursor: onTab ? 'pointer' : 'default' }}
          >
            <div style={{
              width: 60, height: 32, borderRadius: 16,
              background: isActive ? (theme.dark ? 'rgba(255,255,255,0.14)' : theme.indigoSoft) : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name={t.icon === 'heart' && isActive ? 'heartFill' : t.icon}
                    size={22} color={isActive ? activeC : idle} strokeWidth={1.8}/>
            </div>
            <div style={{ fontSize: 10, fontWeight: 500, color: isActive ? activeC : idle, letterSpacing: 0.1 }}>
              {t.label}
            </div>
          </div>
        );
      })}
    </div>
  );
}

Object.assign(window, { IOSFrame, IOSTabBar });
