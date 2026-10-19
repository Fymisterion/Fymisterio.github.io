import type { ColorValue } from 'react-native';
import Svg, { Circle, Path, Rect } from 'react-native-svg';

export type TabIconName = 'trainer' | 'ranges' | 'math' | 'sessions' | 'more';

/** Schlanke eigene Tab-Icons (keine Icon-Font nötig, funktioniert offline). */
export function TabIcon({ name, color, size = 24 }: { name: TabIconName; color: ColorValue; size?: number }) {
  const stroke = { stroke: color, strokeWidth: 2, fill: 'none', strokeLinecap: 'round', strokeLinejoin: 'round' } as const;
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" accessibilityElementsHidden importantForAccessibility="no">
      {name === 'trainer' && (
        // Pik-Symbol
        <Path
          {...stroke}
          d="M12 3c3 3.5 7 6 7 9.5a3.5 3.5 0 0 1-6 2.4c.2 2 .9 3.6 2 5.1H9c1.1-1.5 1.8-3.1 2-5.1a3.5 3.5 0 0 1-6-2.4C5 9 9 6.5 12 3Z"
        />
      )}
      {name === 'ranges' && (
        <>
          <Rect {...stroke} x={3.5} y={3.5} width={17} height={17} rx={2} />
          <Path {...stroke} d="M3.5 9.2h17M3.5 14.8h17M9.2 3.5v17M14.8 3.5v17" />
        </>
      )}
      {name === 'math' && (
        <>
          <Path {...stroke} d="M18 6 6 18" />
          <Circle {...stroke} cx={7.5} cy={7.5} r={2.5} />
          <Circle {...stroke} cx={16.5} cy={16.5} r={2.5} />
        </>
      )}
      {name === 'sessions' && <Path {...stroke} d="M3.5 19.5h17M4.5 15l4.5-5 4 3 6.5-7.5" />}
      {name === 'more' && (
        <>
          <Circle cx={5.5} cy={12} r={1.8} fill={color} />
          <Circle cx={12} cy={12} r={1.8} fill={color} />
          <Circle cx={18.5} cy={12} r={1.8} fill={color} />
        </>
      )}
    </Svg>
  );
}
