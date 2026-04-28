// Shaker — theme helper
// Returns tokens for light or dark mode

function makeTheme(dark) {
  if (dark) {
    return {
      dark: true,
      bg:          '#0F1020',
      bgElev:      '#1B1D30',
      bgCard:      '#1B1D30',
      bgSubtle:    '#13142A',
      indigo:      '#8A96C9',       // lightened so it's readable on dark
      indigoText:  '#B3BDE4',
      indigoSoft:  'rgba(138,150,201,0.18)',
      accent:      '#4F92F0',
      accentSoft:  'rgba(79,146,240,0.15)',
      orange:      '#F08B5F',
      gold:        '#F5B93A',
      goldSoft:    'rgba(245,185,58,0.15)',
      green:       '#3ED58B',
      danger:      '#F56A6A',
      text:        '#F4F3FB',
      textSec:     '#9EA2B6',
      textTer:     '#6E7392',
      hair:        'rgba(255,255,255,0.08)',
      hairStrong:  'rgba(255,255,255,0.14)',
      statusDark:  false,       // light status icons on dark bg
      inputBg:     'rgba(255,255,255,0.06)',
    };
  }
  return {
    dark: false,
    bg:          '#F3F1F9',
    bgElev:      '#FFFFFF',
    bgCard:      '#FFFFFF',
    bgSubtle:    '#ECE9F4',
    indigo:      '#3C4876',
    indigoText:  '#3C4876',
    indigoSoft:  '#E4E6F3',
    accent:      '#2B79E4',
    accentSoft:  'rgba(43,121,228,0.1)',
    orange:      '#E8723F',
    gold:        '#F5B93A',
    goldSoft:    '#FFF4D9',
    green:       '#23C071',
    danger:      '#D33A3A',
    text:        '#1C1E2C',
    textSec:     '#6A6F88',
    textTer:     '#9EA2B6',
    hair:        'rgba(60,72,118,0.12)',
    hairStrong:  'rgba(60,72,118,0.2)',
    statusDark:  true,
    inputBg:     '#ECE9F4',
  };
}

Object.assign(window, { makeTheme });
