/** Deutsche UI-Texte. Neue Sprachen: gleiche Struktur in eigener Datei (Typ `Strings`). */
export const de = {
  app: {
    name: 'Microstakes Coach',
  },
  tabs: {
    trainer: 'Trainer',
    ranges: 'Ranges',
    math: 'Mathe',
    sessions: 'Sessions',
    more: 'Mehr',
  },
  trainer: {
    title: 'Preflop-Trainer',
    intro: 'Übe Preflop-Entscheidungen für 6-max Cash Games mit sofortigem Feedback.',
    modules: {
      rfi: 'Open-Raise',
      vsOpen: 'Gegen Open',
      vs3bet: 'Gegen 3-Bet',
      vsLimp: 'Gegen Limper',
    },
  },
  ranges: {
    title: 'Ranges',
    intro: '13×13-Matrizen für alle Spots zum Nachschlagen.',
    disclaimer: 'Vereinfachte, GTO-nahe Richtwerte – keine Solver-Ausgabe.',
  },
  math: {
    title: 'Poker-Mathe',
    intro: 'Pot Odds, Equity und Fold Equity – Kurse, Übungen und Spickzettel.',
    courses: {
      potOdds: 'Kurs 1 · Pot Odds',
      foldEquity: 'Kurs 2 · Fold Equity',
    },
  },
  sessions: {
    title: 'Sessions',
    intro: 'Ergebnisse, Bankroll und Winrate festhalten – alles bleibt auf deinem Gerät.',
  },
  more: {
    title: 'Mehr',
    appearance: 'Darstellung',
    themeSystem: 'System',
    themeLight: 'Hell',
    themeDark: 'Dunkel',
    typeSample: 'Schriftprobe',
    responsible: 'Verantwortungsvoll spielen',
    about: 'Über die App',
    version: 'Version',
  },
  common: {
    comingSoon: 'Kommt in Phase {phase}.',
  },
} as const;

type Widen<T> = { -readonly [K in keyof T]: T[K] extends string ? string : Widen<T[K]> };
export type Strings = Widen<typeof de>;
