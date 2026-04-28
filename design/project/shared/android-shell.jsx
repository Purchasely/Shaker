// Shaker — Android device chrome + tab bar (theme-aware)

function AndroidFrame({ children, width = 412, height = 892, theme }) {
  const statusColor = theme.statusDark ? theme.indigo : '#fff';
  return (
    <div style={{
      width, height, borderRadius: 44, overflow: 'hidden', position: 'relative',
      background: theme.bg,
      boxShadow: '0 30px 60px rgba(20,20,40,0.22), 0 0 0 9px #1a1a1a inset, 0 0 0 11px #2e2e2e',
      fontFamily: 'Roboto, "Google Sans Text", system-ui, sans-serif',
      WebkitFontSmoothing: 'antialiased',
      boxSizing: 'content-box',
    }}>
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0, height: 38, zIndex: 40,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '6px 20px 0', color: statusColor, pointerEvents: 'none',
      }}>
        <div style={{ fontSize: 14, fontWeight: 500, letterSpacing: 0.1 }}>9:41</div>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke={statusColor} strokeWidth="2">
            <path d="M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z"/>
          </svg>
        </div>
        <div style={{
          position: 'absolute', left: '50%', top: 10, transform: 'translateX(-50%)',
          width: 22, height: 22, borderRadius: 11, background: '#000',
        }} />
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill={statusColor}>
            <path d="M2 22h20L12 2 2 22z"/>
          </svg>
          <svg width="14" height="14" viewBox="0 0 24 24" fill={statusColor}>
            <path d="M12 6c3.5 0 6.7 1.3 9 3.4l-2 2A10 10 0 0 0 12 9a10 10 0 0 0-7 2.4l-2-2A12.7 12.7 0 0 1 12 6zM12 12c2 0 3.8.7 5 2l-2 2a4 4 0 0 0-6 0l-2-2a7 7 0 0 1 5-2zm0 5a2 2 0 1 1 0 4 2 2 0 0 1 0-4z"/>
          </svg>
          <svg width="16" height="14" viewBox="0 0 24 14" fill={statusColor}>
            <rect x="0" y="1" width="20" height="12" rx="2"/>
            <rect x="20" y="5" width="2" height="4" rx="0.5"/>
          </svg>
        </div>
      </div>
      <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column' }}>
        {children}
      </div>
      <div style={{
        position: 'absolute', bottom: 8, left: '50%', transform: 'translateX(-50%)',
        width: 120, height: 4, borderRadius: 2,
        background: theme.dark ? 'rgba(255,255,255,0.5)' : 'rgba(28,30,44,0.4)', zIndex: 60, pointerEvents: 'none',
      }} />
    </div>
  );
}

function AndroidTabBar({ active = 'home', theme, onTab }) {
  const tabs = [
    { id: 'home',      label: 'Home',      icon: 'home' },
    { id: 'favorites', label: 'Favorites', icon: 'heart' },
    { id: 'settings',  label: 'Settings',  icon: 'settings' },
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      paddingBottom: 28, paddingTop: 10,
      background: theme.dark ? theme.bgCard : '#ECE9F4',
      borderTop: `1px solid ${theme.hair}`,
      display: 'flex', justifyContent: 'space-around',
      zIndex: 30,
    }}>
      {tabs.map(t => {
        const isActive = t.id === active;
        const activeC = theme.dark ? '#FFF' : theme.indigo;
        const idleC   = theme.textSec;
        return (
          <div key={t.id}
            onClick={() => onTab && onTab(t.id)}
            style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, minWidth: 64, cursor: onTab ? 'pointer' : 'default' }}
          >
            <div style={{
              width: 64, height: 32, borderRadius: 16,
              background: isActive ? (theme.dark ? 'rgba(255,255,255,0.12)' : theme.indigoSoft) : 'transparent',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <Icon name={t.icon === 'heart' && isActive ? 'heartFill' : t.icon}
                    size={24} color={isActive ? activeC : idleC} strokeWidth={2}/>
            </div>
            <div style={{ fontSize: 12, fontWeight: 500, color: isActive ? activeC : idleC }}>
              {t.label}
            </div>
          </div>
        );
      })}
    </div>
  );
}

Object.assign(window, { AndroidFrame, AndroidTabBar });
