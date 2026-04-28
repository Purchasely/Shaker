// Shaker — Detail, Favorites, Paywall, Settings (theme-aware)

function Detail({ platform, theme, cocktail, locked = true, isFav = false, onBack, onFav, onUnlock, onStartMixing }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', fontFamily: FONT_STACK[platform], overflow: 'hidden', background: theme.bg }}>
      <div style={{ position: 'relative', height: 300, flexShrink: 0 }}>
        <CocktailArt cocktail={cocktail}/>
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 100, background: 'linear-gradient(to bottom, rgba(0,0,0,0.35), transparent)', zIndex: 1 }}/>
        <div style={{ position: 'absolute', top: 62, left: 16, right: 16, display: 'flex', justifyContent: 'space-between', zIndex: 2 }}>
          <div onClick={onBack} style={{ width: 40, height: 40, borderRadius: 20, background: 'rgba(255,255,255,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(10px)', cursor: 'pointer' }}>
            <Icon name="back" size={20} color="#1C1E2C" strokeWidth={2.5}/>
          </div>
          <div onClick={onFav} style={{ width: 40, height: 40, borderRadius: 20, background: 'rgba(255,255,255,0.85)', display: 'flex', alignItems: 'center', justifyContent: 'center', backdropFilter: 'blur(10px)', cursor: 'pointer' }}>
            <Icon name={isFav ? 'heartFill' : 'heart'} size={20} color={isFav ? theme.danger : '#1C1E2C'} strokeWidth={2}/>
          </div>
        </div>
        {locked && (
          <div style={{ position: 'absolute', bottom: 40, left: 16, zIndex: 2, padding: '6px 12px', borderRadius: 100, background: 'rgba(28,30,44,0.82)', color: '#FFD572', fontSize: 11, fontWeight: 800, letterSpacing: 0.4, display: 'inline-flex', alignItems: 'center', gap: 6, textTransform: 'uppercase' }}>
            <Icon name="lock" size={11} color="#FFD572" strokeWidth={2.5}/>PRO recipe
          </div>
        )}
      </div>

      <div style={{ marginTop: -28, flex: 1, background: theme.bg, borderTopLeftRadius: 28, borderTopRightRadius: 28, padding: '20px 24px 100px', overflow: 'auto', position: 'relative' }}>
        <div style={{ width: 40, height: 4, borderRadius: 2, background: theme.hairStrong, margin: '0 auto 16px' }}/>
        <h1 style={{ fontSize: 28, fontWeight: 700, color: theme.text, margin: 0, letterSpacing: -0.6 }}>{cocktail.name}</h1>
        <p style={{ fontSize: 15, color: theme.textSec, marginTop: 8, lineHeight: 1.5 }}>{cocktail.blurb}</p>
        <div style={{ display: 'flex', gap: 8, marginTop: 16, flexWrap: 'wrap' }}>
          <Chip platform={platform} theme={theme}>{cocktail.cat}</Chip>
          <Chip platform={platform} theme={theme}>{cocktail.spirit}</Chip>
          <Chip platform={platform} theme={theme}>{cocktail.diff}</Chip>
        </div>

        <div style={{ marginTop: 24 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 12 }}>
            <div style={{ fontSize: 18, fontWeight: 700, color: theme.text, letterSpacing: -0.3 }}>Ingredients</div>
            <div style={{ fontSize: 13, color: theme.textSec }}>{cocktail.ingredients.length} items</div>
          </div>
          <div style={{ background: theme.bgCard, borderRadius: 16, border: `1px solid ${theme.hair}`, overflow: 'hidden' }}>
            {cocktail.ingredients.map((ing, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', borderBottom: i < cocktail.ingredients.length - 1 ? `1px solid ${theme.hair}` : 'none' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <div style={{ width: 26, height: 26, borderRadius: 6, background: theme.indigoSoft, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, color: theme.indigoText }}>{i+1}</div>
                  <div style={{ fontSize: 15, color: theme.text }}>{ing}</div>
                </div>
                <div style={{ fontSize: 13, color: theme.textTer }}>—</div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 24 }}>
          <div style={{ fontSize: 18, fontWeight: 700, color: theme.text, letterSpacing: -0.3, marginBottom: 12 }}>Instructions</div>
          {locked ? (
            <div style={{
              borderRadius: 20, padding: 24, textAlign: 'center',
              background: theme.dark
                ? 'linear-gradient(140deg, #1D2040 0%, #2A1D3F 100%)'
                : 'linear-gradient(140deg, #2D345F 0%, #3F2A55 100%)',
              color: '#fff', position: 'relative', overflow: 'hidden',
            }}>
              <div style={{ position: 'absolute', top: -20, right: -20, width: 120, height: 120, borderRadius: 60, background: 'radial-gradient(circle, rgba(245,185,58,0.25), transparent 70%)' }}/>
              <div style={{ width: 56, height: 56, borderRadius: 28, background: 'rgba(245,185,58,0.18)', margin: '0 auto 14px', display: 'flex', alignItems: 'center', justifyContent: 'center', border: '1.5px solid rgba(245,185,58,0.4)' }}>
                <Icon name="lock" size={24} color="#F5B93A" strokeWidth={2}/>
              </div>
              <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3 }}>Pro recipe</div>
              <div style={{ fontSize: 14, color: 'rgba(255,255,255,0.75)', marginTop: 6, lineHeight: 1.5, maxWidth: 260, margin: '6px auto 0' }}>
                Unlock Shaker Pro to see the {cocktail.steps.length}-step guided recipe, plus 35+ more cocktails.
              </div>
              <div style={{ display: 'flex', gap: 16, justifyContent: 'center', marginTop: 14, fontSize: 12, color: 'rgba(255,255,255,0.6)' }}>
                <span>✦ 35+ recipes</span><span>✦ Save unlimited</span>
              </div>
              <div onClick={onUnlock} style={{
                marginTop: 18, display: 'inline-flex', alignItems: 'center', gap: 8,
                height: 48, padding: '0 28px', borderRadius: 100,
                background: '#F5B93A', color: '#2A1D05',
                fontSize: 15, fontWeight: 700, cursor: 'pointer',
                boxShadow: '0 8px 24px rgba(245,185,58,0.35)',
              }}>
                Unlock Shaker Pro — €9.99/mo
              </div>
            </div>
          ) : (
            <div style={{ background: theme.bgCard, borderRadius: 16, border: `1px solid ${theme.hair}`, padding: '4px 0' }}>
              {cocktail.steps.map((s, i) => (
                <div key={i} style={{ display: 'flex', gap: 12, padding: '12px 16px', borderBottom: i < cocktail.steps.length - 1 ? `1px solid ${theme.hair}` : 'none' }}>
                  <div style={{ minWidth: 24, height: 24, borderRadius: 12, background: theme.indigo, color: theme.dark ? '#0F1020' : '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 700, flexShrink: 0 }}>{i+1}</div>
                  <div style={{ fontSize: 15, color: theme.text, lineHeight: 1.5 }}>{s}</div>
                </div>
              ))}
            </div>
          )}
        </div>

        {!locked && (
          <div style={{ marginTop: 24, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <PillButton variant="indigo" platform={platform} theme={theme} full onClick={onStartMixing}
              leadingIcon={<Icon name="chef" size={18} color={theme.dark ? '#0F1020' : '#fff'} strokeWidth={2}/>}>
              Start mixing
            </PillButton>
            <PillButton variant="outline" platform={platform} theme={theme} full onClick={onFav}
              leadingIcon={<Icon name={isFav ? 'heartFill' : 'heart'} size={16} color={isFav ? theme.danger : theme.indigoText} strokeWidth={2}/>}>
              {isFav ? 'Saved' : 'Save'}
            </PillButton>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Guided mixing mode ────────────────────────────────────
function MixingMode({ platform, theme, cocktail, step, onStep, onDone, onExit }) {
  const total = cocktail.steps.length;
  const currentStep = cocktail.steps[step] || '';
  const pct = ((step + 1) / total) * 100;
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', fontFamily: FONT_STACK[platform], background: theme.dark ? '#0A0B16' : '#15182B', color: '#fff', paddingTop: 54 }}>
      <div style={{ padding: '14px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div onClick={onExit} style={{ width: 36, height: 36, borderRadius: 18, background: 'rgba(255,255,255,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <Icon name="close" size={18} color="#fff" strokeWidth={2}/>
        </div>
        <div style={{ fontSize: 13, fontWeight: 600, color: 'rgba(255,255,255,0.7)', letterSpacing: 0.3 }}>
          Step {step + 1} of {total}
        </div>
        <div style={{ width: 36 }}/>
      </div>
      <div style={{ padding: '0 20px 8px' }}>
        <div style={{ height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.1)', overflow: 'hidden' }}>
          <div style={{ height: '100%', width: pct + '%', background: theme.gold, transition: 'width 0.3s' }}/>
        </div>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 32px', textAlign: 'center', gap: 24 }}>
        <div style={{ width: 140, height: 140, borderRadius: 70, overflow: 'hidden', boxShadow: '0 0 0 3px rgba(255,255,255,0.1)' }}>
          <CocktailArt cocktail={cocktail}/>
        </div>
        <div style={{ fontSize: 14, color: 'rgba(255,255,255,0.6)', letterSpacing: 0.4, textTransform: 'uppercase', fontWeight: 700 }}>{cocktail.name}</div>
        <div style={{ fontSize: 26, fontWeight: 700, letterSpacing: -0.5, lineHeight: 1.3, maxWidth: 340 }}>
          {currentStep}
        </div>
      </div>

      <div style={{ padding: '0 20px 48px', display: 'flex', gap: 10 }}>
        <PillButton variant="outline" platform={platform} theme={{ ...theme, indigoText: '#fff' }} full onClick={() => step > 0 && onStep(step - 1)} style={{ opacity: step === 0 ? 0.4 : 1 }}>Back</PillButton>
        {step < total - 1
          ? <PillButton variant="primary" platform={platform} theme={theme} full onClick={() => onStep(step + 1)}>Next step</PillButton>
          : <PillButton variant="primary" platform={platform} theme={{ ...theme, accent: theme.green }} full onClick={onDone}>Done — Cheers!</PillButton>}
      </div>
    </div>
  );
}

function Favorites({ platform, theme, favs = [], onCocktail, onUnlock, onBrowse }) {
  if (favs.length === 0) {
    return (
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', paddingTop: 54, fontFamily: FONT_STACK[platform], background: theme.bg }}>
        <div style={{ padding: '16px 20px 8px', fontSize: 28, fontWeight: 700, color: theme.text, letterSpacing: -0.5 }}>Favorites</div>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: 32, paddingBottom: 120 }}>
          <div style={{ width: 110, height: 110, borderRadius: 55, background: theme.indigoSoft, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 24 }}>
            <Icon name="heart" size={52} color={theme.indigoText} strokeWidth={1.5}/>
          </div>
          <div style={{ fontSize: 22, fontWeight: 700, color: theme.text, letterSpacing: -0.3 }}>No favorites yet</div>
          <div style={{ fontSize: 15, color: theme.textSec, textAlign: 'center', marginTop: 8, lineHeight: 1.5, maxWidth: 280 }}>
            Tap the heart on a cocktail to save it here. Unlock Shaker Pro to save as many as you like.
          </div>
          <div style={{ marginTop: 28, display: 'flex', flexDirection: 'column', gap: 10, width: '100%', maxWidth: 300 }}>
            <PillButton variant="indigo" platform={platform} theme={theme} full onClick={onUnlock} leadingIcon={<Icon name="lock" size={16} color={theme.dark ? '#0F1020' : '#fff'} strokeWidth={2.5}/>}>
              Unlock Favorites
            </PillButton>
            <PillButton variant="outline" platform={platform} theme={theme} full onClick={onBrowse}>Browse cocktails</PillButton>
          </div>
        </div>
      </div>
    );
  }
  const list = COCKTAILS.filter(c => favs.includes(c.id));
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', paddingTop: 54, fontFamily: FONT_STACK[platform], background: theme.bg, overflow: 'hidden' }}>
      <div style={{ padding: '16px 20px 8px', fontSize: 28, fontWeight: 700, color: theme.text, letterSpacing: -0.5 }}>Favorites</div>
      <div style={{ fontSize: 14, color: theme.textSec, padding: '0 20px 16px' }}>{list.length} cocktail{list.length > 1 ? 's' : ''} saved</div>
      <div style={{ flex: 1, overflow: 'auto', padding: '0 20px 120px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {list.map(c => <CocktailCard key={c.id} cocktail={c} platform={platform} theme={theme} onClick={() => onCocktail && onCocktail(c)} isFav/>)}
      </div>
    </div>
  );
}

function Paywall({ platform, theme, plan = 'annual', onSelectPlan, onPurchase, onClose, onRestore }) {
  const plans = [
    { id: 'annual',   label: 'Shaker Pro — Annual', price: '€99.99', cadence: '/year', sub: '€8.33 billed monthly', badge: 'BEST VALUE · SAVE 17%' },
    { id: 'semi',     label: '6 months',            price: '€59.99', cadence: '/semester', sub: '€9.99 billed monthly' },
    { id: 'monthly',  label: '1 month',             price: '€9.99',  cadence: '/month', sub: 'Billed monthly' },
  ];
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', fontFamily: FONT_STACK[platform], overflow: 'hidden', background: theme.bg }}>
      <div style={{ height: 240, position: 'relative', flexShrink: 0, background: 'radial-gradient(circle at 30% 30%, #6A4A24 0%, #2A1509 70%, #120804 100%)' }}>
        {Array.from({length: 14}).map((_, i) => (
          <div key={i} style={{ position: 'absolute', left: `${(i*37)%100}%`, top: `${(i*23)%90}%`, width: 40 + (i%3)*15, height: 40 + (i%3)*15, borderRadius: '50%', background: `radial-gradient(circle, rgba(255,${180+(i%4)*10},${70+(i%3)*20},0.35) 0%, transparent 70%)`, filter: 'blur(4px)' }}/>
        ))}
        <svg viewBox="0 0 300 240" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}>
          <g transform="translate(100 30)">
            <path d="M50 0 Q52 20 60 40 L62 45" stroke="#F5B93A" strokeWidth="3" fill="none"/>
            <path d="M10 55 Q50 105 90 55 Z" fill="#E8A540" opacity="0.95"/>
            <path d="M5 55 Q50 115 95 55 M50 115 L50 170 M25 175 L75 175" stroke="rgba(255,255,255,0.9)" strokeWidth="2" fill="none" strokeLinecap="round"/>
          </g>
          <g transform="translate(25 115)" opacity="0.75">
            <rect x="0" y="0" width="50" height="80" rx="3" fill="rgba(255,255,255,0.18)" stroke="rgba(255,255,255,0.5)" strokeWidth="1.5"/>
            <rect x="3" y="30" width="44" height="48" fill="#B87538" opacity="0.8"/>
          </g>
          <g transform="translate(215 135)" opacity="0.7">
            <rect x="0" y="0" width="45" height="75" rx="3" fill="rgba(255,255,255,0.18)" stroke="rgba(255,255,255,0.5)" strokeWidth="1.5"/>
            <rect x="3" y="25" width="39" height="47" fill="#A3652B" opacity="0.7"/>
          </g>
        </svg>
        <div onClick={onClose} style={{ position: 'absolute', top: 62, right: 16, width: 36, height: 36, borderRadius: 18, background: 'rgba(255,255,255,0.2)', backdropFilter: 'blur(8px)', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <Icon name="close" size={18} color="#fff" strokeWidth={2.5}/>
        </div>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '24px 24px 40px', background: theme.bgCard, marginTop: -24, borderTopLeftRadius: 28, borderTopRightRadius: 28, position: 'relative', zIndex: 2 }}>
        <h1 style={{ fontSize: 26, fontWeight: 700, color: theme.text, margin: 0, letterSpacing: -0.5, textAlign: 'center' }}>Unlock the full bar</h1>
        <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[['sparkle', 'Access 35+ exclusive cocktails'], ['heartFill', 'Save unlimited favorites'], ['chef', 'Get expert mixology tips']].map(([icn, txt], i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <div style={{ width: 30, height: 30, borderRadius: 15, background: theme.indigoSoft, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <Icon name={icn} size={16} color={theme.indigoText} strokeWidth={2}/>
              </div>
              <div style={{ fontSize: 14, color: theme.text }}>{txt}</div>
            </div>
          ))}
        </div>
        <div style={{ marginTop: 16, padding: '8px 12px', borderRadius: 12, background: theme.goldSoft, border: `1px solid ${theme.gold}`, fontSize: 12, color: theme.dark ? theme.gold : '#8A6A20', textAlign: 'center' }}>
          ★ Loved by thousands of home bartenders
        </div>

        <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 10 }}>
          {plans.map(p => {
            const selected = p.id === plan;
            const isGold = p.id === 'annual';
            return (
              <div key={p.id} onClick={() => onSelectPlan && onSelectPlan(p.id)} style={{
                position: 'relative', padding: '14px 16px', borderRadius: 16,
                background: selected && isGold ? theme.goldSoft : selected ? theme.indigoSoft : theme.bgSubtle,
                border: `2px solid ${selected ? (isGold ? theme.gold : theme.indigoText) : theme.hair}`,
                display: 'flex', alignItems: 'center', gap: 12, cursor: 'pointer',
              }}>
                {p.badge && <div style={{ position: 'absolute', top: -10, right: 14, padding: '3px 10px', borderRadius: 100, background: theme.gold, color: '#3A2A05', fontSize: 10, fontWeight: 800, letterSpacing: 0.5 }}>{p.badge}</div>}
                <div style={{ width: 22, height: 22, borderRadius: 11, border: `2px solid ${selected ? (isGold ? theme.gold : theme.indigoText) : theme.textTer}`, display: 'flex', alignItems: 'center', justifyContent: 'center', background: theme.bgCard }}>
                  {selected && <div style={{ width: 10, height: 10, borderRadius: 5, background: isGold ? theme.gold : theme.indigoText }}/>}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: 14, fontWeight: 700, color: theme.text }}>{p.label}</div>
                  <div style={{ fontSize: 12, color: theme.textSec }}>{p.sub}</div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 16, fontWeight: 700, color: theme.text }}>{p.price}</div>
                  <div style={{ fontSize: 11, color: theme.textSec }}>{p.cadence}</div>
                </div>
              </div>
            );
          })}
        </div>

        <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 6 }}>
          <PillButton variant="primary" platform={platform} theme={theme} full onClick={onPurchase}>Unlock Shaker Pro</PillButton>
          <div onClick={onClose} style={{ textAlign: 'center', fontSize: 13, color: theme.textSec, padding: 10, cursor: 'pointer' }}>Maybe later</div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 16, marginTop: 4, fontSize: 11, color: theme.textTer }}>
          <span onClick={onRestore} style={{ cursor: 'pointer' }}>Restore purchases</span>
          <span>·</span><span>Terms</span><span>·</span><span>Privacy</span>
        </div>
      </div>
    </div>
  );
}

function Toggle({ on, theme, onClick }) {
  return (
    <div onClick={onClick} style={{ width: 51, height: 31, borderRadius: 16, background: on ? theme.indigo : (theme.dark ? '#3D3F54' : '#E0DEE9'), position: 'relative', transition: 'all 0.2s', flexShrink: 0, cursor: 'pointer' }}>
      <div style={{ position: 'absolute', top: 2, left: on ? 22 : 2, width: 27, height: 27, borderRadius: 14, background: '#fff', boxShadow: '0 2px 4px rgba(0,0,0,0.15)', transition: 'left 0.2s' }}/>
    </div>
  );
}

function ToggleRow({ title, subtitle, on, onToggle, theme, last = false }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', padding: '12px 16px', borderBottom: last ? 'none' : `1px solid ${theme.hair}`, gap: 12 }}>
      <div style={{ flex: 1 }}>
        <div style={{ fontSize: 15, fontWeight: 500, color: theme.text }}>{title}</div>
        {subtitle && <div style={{ fontSize: 13, color: theme.textSec, marginTop: 2 }}>{subtitle}</div>}
      </div>
      <Toggle on={on} theme={theme} onClick={onToggle}/>
    </div>
  );
}

function SegmentedControl({ options, active, onSelect, theme }) {
  return (
    <div style={{ display: 'flex', padding: 4, gap: 4, background: theme.inputBg, borderRadius: 12, margin: '0 20px' }}>
      {options.map(o => (
        <div key={o} onClick={() => onSelect && onSelect(o)} style={{
          flex: 1, height: 36, borderRadius: 10,
          background: o === active ? (theme.dark ? 'rgba(138,150,201,0.25)' : theme.indigoSoft) : 'transparent',
          color: o === active ? theme.indigoText : theme.textSec,
          border: o === active ? `1px solid ${theme.indigoText}40` : 'none',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          fontSize: 13, fontWeight: o === active ? 600 : 500, cursor: 'pointer',
        }}>
          {o === active && <Icon name="check" size={14} color={theme.indigoText} strokeWidth={3}/>}
          {o}
        </div>
      ))}
    </div>
  );
}

function SettingsCard({ children, theme }) {
  return <div style={{ background: theme.bgCard, borderRadius: 16, border: `1px solid ${theme.hair}`, margin: '0 20px 16px', overflow: 'hidden' }}>{children}</div>;
}

function Settings({ platform, theme, loggedIn = false, userId = '', onUserIdChange, onLogin, onLogout, onRestore, onShowOnboarding, onShowPaywall,
  sdkMode = 'Paywall Observer', onSdkMode, appearance = 'System', onAppearance, displayMode = 'Full', onDisplayMode,
  privacy = {}, onTogglePrivacy }) {
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', paddingTop: 54, fontFamily: FONT_STACK[platform], overflow: 'hidden', background: theme.bg }}>
      <div style={{ padding: '14px 20px 16px', fontSize: 28, fontWeight: 700, color: theme.text, letterSpacing: -0.5 }}>Settings</div>
      <div style={{ flex: 1, overflow: 'auto', paddingBottom: 120 }}>
        <SectionHeader platform={platform} theme={theme}>Account</SectionHeader>
        <SettingsCard theme={theme}>
          {loggedIn ? (
            <div style={{ padding: 16, display: 'flex', alignItems: 'center', gap: 12, borderBottom: `1px solid ${theme.hair}` }}>
              <div style={{ width: 44, height: 44, borderRadius: 22, background: theme.indigo, color: theme.dark ? '#0F1020' : '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 16, fontWeight: 700 }}>{userId.slice(0,2).toUpperCase() || 'KB'}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 12, color: theme.textSec }}>Logged in as</div>
                <div style={{ fontSize: 16, fontWeight: 600, color: theme.text }}>{userId || 'kevinb'}</div>
              </div>
              <div onClick={onLogout} style={{ color: theme.danger, fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
                <Icon name="logout" size={16} color={theme.danger} strokeWidth={2}/>Logout
              </div>
            </div>
          ) : (
            <div style={{ padding: 12, display: 'flex', alignItems: 'center', gap: 10, borderBottom: `1px solid ${theme.hair}` }}>
              <input value={userId} onChange={e => onUserIdChange && onUserIdChange(e.target.value)} placeholder="User ID" style={{
                flex: 1, height: 44, border: `1px solid ${theme.hair}`, borderRadius: 12, padding: '0 14px',
                fontSize: 15, color: theme.text, background: theme.bgSubtle, outline: 'none',
                fontFamily: 'inherit',
              }}/>
              <div onClick={() => userId && onLogin && onLogin(userId)} style={{ height: 44, padding: '0 18px', borderRadius: 12, background: userId ? theme.accent : (theme.dark ? '#3D3F54' : '#D7D4E2'), color: userId ? '#fff' : theme.textSec, fontSize: 14, fontWeight: 600, display: 'flex', alignItems: 'center', cursor: userId ? 'pointer' : 'default' }}>Login</div>
            </div>
          )}
          <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: `1px solid ${theme.hair}` }}>
            <div style={{ fontSize: 15, color: theme.text }}>Premium status</div>
            <div style={{ padding: '3px 10px', borderRadius: 100, background: theme.indigoSoft, color: theme.indigoText, fontSize: 12, fontWeight: 700, letterSpacing: 0.2 }}>FREE</div>
          </div>
          <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: 13, color: theme.textSec }}>Anonymous ID</div>
              <div style={{ fontSize: 11, color: theme.text, fontFamily: 'ui-monospace, Menlo, monospace', marginTop: 2 }}>5b014805-e281-413a-9143-8dfdc669952b</div>
            </div>
            <div style={{ width: 32, height: 32, borderRadius: 8, background: theme.inputBg, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
              <Icon name="copy" size={16} color={theme.textSec} strokeWidth={2}/>
            </div>
          </div>
        </SettingsCard>

        <SectionHeader platform={platform} theme={theme}>Purchases</SectionHeader>
        <div style={{ padding: '0 20px', display: 'flex', flexDirection: 'column', gap: 10, marginBottom: 16 }}>
          <PillButton variant="outline" platform={platform} theme={theme} full onClick={onRestore}>Restore purchases</PillButton>
          <PillButton variant="outline" platform={platform} theme={theme} full onClick={onShowOnboarding}>Show onboarding</PillButton>
          <PillButton variant="outline" platform={platform} theme={theme} full onClick={onShowPaywall}>Redeem promo code</PillButton>
        </div>

        <SectionHeader platform={platform} theme={theme}>Purchasely SDK</SectionHeader>
        <div style={{ margin: '0 20px 6px' }}>
          <SegmentedControl options={['Paywall Observer', 'Full']} active={sdkMode} onSelect={onSdkMode} theme={theme}/>
        </div>
        <div style={{ padding: '4px 20px 20px', fontSize: 12, color: theme.textSec }}>
          Default mode is Paywall Observer — Shaker observes purchases but uses its own paywall UI.
        </div>

        <SectionHeader platform={platform} theme={theme}>Data privacy</SectionHeader>
        <SettingsCard theme={theme}>
          {[
            ['analytics', 'Analytics', 'Anonymous audience measurement'],
            ['identified','Identified analytics', 'User-identified analytics'],
            ['personalization','Personalization', 'Personalized content & offers'],
            ['campaigns','Campaigns', 'Promotional campaigns'],
            ['thirdparty','Third-party integrations', 'External analytics & integrations'],
          ].map(([k,title,sub], i, arr) => (
            <ToggleRow key={k} theme={theme} title={title} subtitle={sub} on={privacy[k] !== false} onToggle={() => onTogglePrivacy && onTogglePrivacy(k)} last={i === arr.length - 1}/>
          ))}
        </SettingsCard>
        <div style={{ padding: '0 20px 16px', fontSize: 12, color: theme.textSec }}>
          Technical processing required for app operation cannot be disabled.
        </div>

        <SectionHeader platform={platform} theme={theme}>Appearance</SectionHeader>
        <div style={{ marginBottom: 20 }}>
          <SegmentedControl options={['Light','Dark','System']} active={appearance} onSelect={onAppearance} theme={theme}/>
        </div>

        <SectionHeader platform={platform} theme={theme}>Screen display mode</SectionHeader>
        <div style={{ fontSize: 12, color: theme.textSec, padding: '0 20px 10px' }}>How paywalls are presented on screen</div>
        <div style={{ marginBottom: 20 }}>
          <SegmentedControl options={['Full','Modal','Drawer','Popin']} active={displayMode} onSelect={onDisplayMode} theme={theme}/>
        </div>

        <SectionHeader platform={platform} theme={theme}>About</SectionHeader>
        <SettingsCard theme={theme}>
          <div style={{ padding: '12px 16px', display: 'flex', justifyContent: 'space-between', borderBottom: `1px solid ${theme.hair}` }}>
            <div style={{ fontSize: 15, color: theme.text }}>Version</div>
            <div style={{ fontSize: 15, color: theme.textSec }}>1.0.0</div>
          </div>
          <div style={{ padding: '12px 16px', display: 'flex', justifyContent: 'space-between' }}>
            <div style={{ fontSize: 15, color: theme.text }}>Purchasely SDK</div>
            <div style={{ fontSize: 15, color: theme.textSec }}>5.7.3</div>
          </div>
        </SettingsCard>
        <div style={{ padding: '0 20px 20px', fontSize: 12, color: theme.textTer, textAlign: 'center' }}>Powered by Purchasely</div>
      </div>
    </div>
  );
}

Object.assign(window, { Detail, MixingMode, Favorites, Paywall, Settings, Toggle, ToggleRow, SegmentedControl, SettingsCard });
