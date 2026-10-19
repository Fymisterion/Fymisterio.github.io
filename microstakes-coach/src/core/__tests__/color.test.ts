import { contrastRatio, relativeLuminance } from '../color';
import { palettes } from '../../theme/tokens';

describe('contrastRatio', () => {
  it('liefert 21:1 für Schwarz auf Weiß', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 5);
    expect(contrastRatio('#fff', '#000')).toBeCloseTo(21, 5);
  });

  it('liefert 1:1 für gleiche Farben', () => {
    expect(contrastRatio('#1F6B4A', '#1F6B4A')).toBe(1);
  });

  it('wirft bei ungültigen Farben', () => {
    expect(() => relativeLuminance('#12')).toThrow();
  });
});

describe('Farb-Tokens erfüllen WCAG AA (≥ 4,5:1 für Text)', () => {
  const pairs = [
    ['text', 'background'],
    ['text', 'surface'],
    ['text', 'surfaceAlt'],
    ['textMuted', 'background'],
    ['textMuted', 'surface'],
    ['onAccent', 'accent'],
    ['onGold', 'gold'],
    ['onRaise', 'raise'],
    ['onCall', 'call'],
    ['onFold', 'fold'],
    ['onFelt', 'felt'],
  ] as const;

  for (const scheme of ['light', 'dark'] as const) {
    for (const [fg, bg] of pairs) {
      it(`${scheme}: ${fg} auf ${bg}`, () => {
        const p = palettes[scheme];
        expect(contrastRatio(p[fg], p[bg])).toBeGreaterThanOrEqual(4.5);
      });
    }
  }

  it.each(['light', 'dark'] as const)('%s: Akzentfarbe als Text auf Hintergrund (≥ 4,5:1)', (scheme) => {
    const p = palettes[scheme];
    expect(contrastRatio(p.accent, p.background)).toBeGreaterThanOrEqual(4.5);
  });
});
