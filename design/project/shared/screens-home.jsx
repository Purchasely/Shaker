// Shaker — Home (theme-aware)

function SearchBar({ platform, theme, onClick }) {
  return (
    <div onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 10,
      height: 44, padding: '0 16px',
      borderRadius: platform === 'ios' ? 12 : 100,
      background: theme.inputBg,
      margin: '0 20px', cursor: onClick ? 'pointer' : 'default',
    }}>
      <Icon name="search" size={18} color={theme.textSec} strokeWidth={2}/>
      <div style={{ flex: 1, fontSize: 15, color: theme.textSec }}>Search cocktails…</div>
      <Icon name="sliders" size={18} color={theme.textSec} strokeWidth={2}/>
    </div>
  );
}

function CocktailCard({ cocktail, platform, theme, onClick, isFav = false }) {
  return (
    <div onClick={onClick} style={{
      borderRadius: platform === 'ios' ? 16 : 20,
      overflow: 'hidden', background: theme.bgCard,
      border: `1px solid ${theme.hair}`,
      position: 'relative', cursor: onClick ? 'pointer' : 'default',
    }}>
      {cocktail.premium && (
        <div style={{
          position: 'absolute', top: 8, left: 8, zIndex: 2,
          padding: '4px 8px', borderRadius: 100,
          background: 'rgba(28,30,44,0.72)', color: '#FFD572',
          fontSize: 10, fontWeight: 700, letterSpacing: 0.3,
          display: 'flex', alignItems: 'center', gap: 4, textTransform: 'uppercase',
        }}>
          <Icon name="lock" size={10} color="#FFD572" strokeWidth={2.5}/>PRO
        </div>
      )}
      <div style={{
        position: 'absolute', top: 8, right: 8, zIndex: 2,
        width: 32, height: 32, borderRadius: 16,
        background: theme.dark ? 'rgba(0,0,0,0.5)' : 'rgba(255,255,255,0.9)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        backdropFilter: 'blur(6px)',
      }}>
        <Icon name={isFav ? 'heartFill' : 'heart'} size={16} color={isFav ? theme.danger : (theme.dark ? '#fff' : theme.text)} strokeWidth={2}/>
      </div>
      <div style={{ aspectRatio: '1 / 1' }}>
        <CocktailArt cocktail={cocktail}/>
      </div>
      <div style={{ padding: '10px 12px 12px' }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: theme.text, letterSpacing: -0.2 }}>{cocktail.name}</div>
        <div style={{ fontSize: 12, color: theme.textSec, marginTop: 2 }}>{cocktail.cat} · {cocktail.diff}</div>
      </div>
    </div>
  );
}

function Home({ platform, theme, variantB = false, activeCat = 'All', onCocktail, onCat, onSearch, onDismissBanner, onGetOffer, favs = [] }) {
  const cats = ['All', 'Classic', 'Non-alcoholic', 'Tropical', 'Easy'];
  const list = activeCat === 'All' ? COCKTAILS
             : activeCat === 'Easy' ? COCKTAILS.filter(c => c.diff === 'Easy')
             : COCKTAILS.filter(c => c.cat === activeCat);
  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', paddingTop: 54, fontFamily: FONT_STACK[platform], overflow: 'hidden', background: theme.bg }}>
      <div style={{ padding: '14px 20px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <ShakerLogo size={28} color={theme.indigoText}/>
          <div style={{ fontSize: 22, fontWeight: 700, color: theme.indigoText, letterSpacing: -0.5 }}>Shaker</div>
        </div>
        <div style={{ padding: '4px 10px', borderRadius: 100, background: theme.indigoSoft, fontSize: 11, fontWeight: 700, color: theme.indigoText, letterSpacing: 0.2 }}>
          FREE
        </div>
      </div>
      <div style={{ paddingBottom: 12 }}>
        <SearchBar platform={platform} theme={theme} onClick={onSearch}/>
      </div>

      <div style={{ flex: 1, overflow: 'auto', paddingBottom: 100 }}>
        {variantB && (
          <div style={{ margin: '4px 20px 16px', borderRadius: 20, background: theme.dark ? '#0A0B18' : '#15182B', padding: 16, position: 'relative' }}>
            <div onClick={onDismissBanner} style={{ position: 'absolute', top: 12, right: 12, cursor: 'pointer' }}>
              <Icon name="close" size={18} color="rgba(255,255,255,0.7)"/>
            </div>
            <div style={{ fontSize: 20, fontWeight: 700, color: '#fff', marginBottom: 4, letterSpacing: -0.3 }}>
              Unlock the full bar
            </div>
            <div style={{ fontSize: 13, color: 'rgba(255,255,255,0.7)', marginBottom: 12 }}>
              Variant B · A/B test — 14 exclusive recipes inside
            </div>
            <div onClick={onGetOffer} style={{
              display: 'inline-flex', height: 38, padding: '0 18px',
              borderRadius: 100, background: theme.green, color: '#fff',
              alignItems: 'center', fontSize: 14, fontWeight: 600, cursor: 'pointer',
            }}>Get offer</div>
          </div>
        )}

        <div style={{ display: 'flex', gap: 8, padding: '4px 20px 14px', overflow: 'auto', msOverflowStyle: 'none', scrollbarWidth: 'none' }}>
          {cats.map(c => (
            <div key={c} onClick={() => onCat && onCat(c)} style={{
              padding: '7px 14px', borderRadius: 100,
              background: c === activeCat ? theme.indigo : theme.bgCard,
              color: c === activeCat ? (theme.dark ? '#0F1020' : '#fff') : theme.text,
              border: c === activeCat ? 'none' : `1px solid ${theme.hair}`,
              fontSize: 13, fontWeight: 500, whiteSpace: 'nowrap', cursor: 'pointer', flexShrink: 0,
            }}>{c}</div>
          ))}
        </div>

        {!variantB && activeCat === 'All' && (
          <div onClick={() => onCocktail && onCocktail(COCKTAILS[3])} style={{ margin: '0 20px 16px', borderRadius: 20, overflow: 'hidden', position: 'relative', aspectRatio: '16 / 9', cursor: 'pointer' }}>
            <CocktailArt cocktail={COCKTAILS[3]} />
            <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(to top, rgba(15,16,32,0.75), rgba(15,16,32,0) 60%)' }}/>
            <div style={{ position: 'absolute', top: 12, left: 12, padding: '4px 10px', borderRadius: 100, background: 'rgba(255,255,255,0.9)', fontSize: 11, fontWeight: 700, letterSpacing: 0.3, color: SHAKER.indigo }}>
              ✦ TONIGHT'S PICK
            </div>
            <div style={{ position: 'absolute', bottom: 14, left: 16, right: 16, color: '#fff' }}>
              <div style={{ fontSize: 22, fontWeight: 700, letterSpacing: -0.3 }}>Manhattan</div>
              <div style={{ fontSize: 13, opacity: 0.9, marginTop: 2 }}>A sophisticated whiskey classic · 4 ingredients</div>
            </div>
          </div>
        )}

        <div style={{ padding: '0 20px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          {list.map(c => (
            <CocktailCard key={c.id} cocktail={c} platform={platform} theme={theme}
              onClick={() => onCocktail && onCocktail(c)}
              isFav={favs.includes(c.id)}/>
          ))}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { Home, CocktailCard, SearchBar });
