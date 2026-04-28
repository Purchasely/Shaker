// Shaker — primitives (theme-aware)

const FONT_STACK = {
  ios: '-apple-system, "SF Pro Text", BlinkMacSystemFont, system-ui, sans-serif',
  android: '"Google Sans Text", Roboto, "Google Sans", system-ui, sans-serif',
};

function Chip({ children, platform, theme }) {
  return (
    <div style={{
      padding: platform === 'ios' ? '7px 14px' : '8px 16px',
      borderRadius: platform === 'ios' ? 8 : 100,
      border: `1px solid ${theme.hair}`,
      background: theme.bgCard,
      fontSize: 13, color: theme.text,
      whiteSpace: 'nowrap',
    }}>{children}</div>
  );
}

function PillButton({ children, variant = 'primary', platform, theme, full = false, leadingIcon = null, onClick, style = {} }) {
  const base = {
    height: platform === 'ios' ? 50 : 52,
    borderRadius: platform === 'ios' ? 14 : 100,
    display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
    fontWeight: 600, fontSize: 16, width: full ? '100%' : undefined,
    cursor: 'pointer', border: 'none',
    padding: '0 24px', letterSpacing: platform === 'android' ? 0.1 : -0.2,
    transition: 'transform 0.1s, opacity 0.15s',
  };
  if (variant === 'primary') Object.assign(base, { background: theme.accent, color: '#fff' });
  if (variant === 'indigo')  Object.assign(base, { background: theme.indigo, color: theme.dark ? '#0F1020' : '#fff' });
  if (variant === 'outline') Object.assign(base, { background: 'transparent', color: theme.indigoText, border: `1.5px solid ${theme.indigoText}` });
  if (variant === 'ghost')   Object.assign(base, { background: theme.indigoSoft, color: theme.indigoText });
  if (variant === 'orange')  Object.assign(base, { background: theme.orange, color: '#fff' });
  if (variant === 'danger')  Object.assign(base, { background: 'transparent', color: theme.danger });
  return (
    <button
      onClick={onClick}
      onMouseDown={e => e.currentTarget.style.transform = 'scale(0.97)'}
      onMouseUp={e => e.currentTarget.style.transform = ''}
      onMouseLeave={e => e.currentTarget.style.transform = ''}
      style={{ ...base, ...style }}
    >
      {leadingIcon}
      {children}
    </button>
  );
}

function SectionHeader({ children, platform, theme }) {
  return (
    <div style={{
      fontSize: platform === 'ios' ? 17 : 16, fontWeight: 600,
      color: theme.indigoText,
      padding: '0 20px', marginBottom: 12, marginTop: 4,
      letterSpacing: platform === 'android' ? 0.1 : -0.3,
    }}>{children}</div>
  );
}

Object.assign(window, { Chip, PillButton, SectionHeader, FONT_STACK });
