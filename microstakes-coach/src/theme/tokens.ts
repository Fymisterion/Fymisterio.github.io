/**
 * Design-Tokens – ruhige „Filztisch“-Ästhetik (siehe CLAUDE.md §8).
 * Rot nur für Raise/Fehler, Grün für Call/richtig, Gold für Bluff/Chips.
 */

export type ColorScheme = 'light' | 'dark';

export interface Palette {
  background: string;
  surface: string;
  surfaceAlt: string;
  border: string;
  text: string;
  textMuted: string;
  accent: string;
  onAccent: string;
  gold: string;
  onGold: string;
  /** Raise / Value / Fehler */
  raise: string;
  onRaise: string;
  /** Call / Limp / richtig */
  call: string;
  onCall: string;
  /** Fold */
  fold: string;
  onFold: string;
  felt: string;
  onFelt: string;
}

export const palettes: Record<ColorScheme, Palette> = {
  light: {
    background: '#F3F5F2',
    surface: '#FFFFFF',
    surfaceAlt: '#E6ECE7',
    border: '#CBD5CE',
    text: '#14201A',
    textMuted: '#4E5D55',
    accent: '#1F6B4A',
    onAccent: '#FFFFFF',
    gold: '#B8862B',
    onGold: '#1A1305',
    raise: '#B3261E',
    onRaise: '#FFFFFF',
    call: '#1F6B4A',
    onCall: '#FFFFFF',
    fold: '#D5DBD7',
    onFold: '#33403A',
    felt: '#1F6B4A',
    onFelt: '#FFFFFF',
  },
  dark: {
    background: '#0F1512',
    surface: '#17201B',
    surfaceAlt: '#202B25',
    border: '#33413A',
    text: '#E7EEE9',
    textMuted: '#A2B0A8',
    accent: '#4CB585',
    onAccent: '#0B1A12',
    gold: '#D6A548',
    onGold: '#1A1305',
    raise: '#E5675C',
    onRaise: '#1A0503',
    call: '#4CB585',
    onCall: '#0B1A12',
    fold: '#3A4640',
    onFold: '#DCE4DF',
    felt: '#17533A',
    onFelt: '#F1F6F3',
  },
};

/** Familiennamen müssen zu den Schlüsseln in useFonts (src/app/_layout.tsx) passen. */
export const fonts = {
  heading: 'BricolageGrotesque_700Bold',
  headingSemi: 'BricolageGrotesque_600SemiBold',
  body: 'IBMPlexSans_400Regular',
  bodyMedium: 'IBMPlexSans_500Medium',
  bodySemi: 'IBMPlexSans_600SemiBold',
  mono: 'IBMPlexMono_400Regular',
  monoSemi: 'IBMPlexMono_600SemiBold',
} as const;

export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 } as const;

export const radii = { sm: 6, md: 10, lg: 16, pill: 999 } as const;

/** Mindestgröße für Touch-Ziele (CLAUDE.md §7). */
export const minTouch = 48;

export const typography = {
  title: { fontFamily: fonts.heading, fontSize: 28, lineHeight: 34 },
  heading: { fontFamily: fonts.headingSemi, fontSize: 20, lineHeight: 26 },
  body: { fontFamily: fonts.body, fontSize: 16, lineHeight: 24 },
  bodyStrong: { fontFamily: fonts.bodySemi, fontSize: 16, lineHeight: 24 },
  small: { fontFamily: fonts.body, fontSize: 14, lineHeight: 20 },
  label: { fontFamily: fonts.bodyMedium, fontSize: 12, lineHeight: 16 },
  mono: { fontFamily: fonts.mono, fontSize: 16, lineHeight: 22 },
  monoStrong: { fontFamily: fonts.monoSemi, fontSize: 16, lineHeight: 22 },
} as const;

export type TypographyVariant = keyof typeof typography;
