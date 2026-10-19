import Constants from 'expo-constants';
import { Pressable, StyleSheet, View } from 'react-native';

import { AppText } from '@/components/AppText';
import { Card } from '@/components/Card';
import { Screen } from '@/components/Screen';
import { t } from '@/i18n';
import { useAppTheme, type ThemePreference } from '@/theme/ThemeProvider';
import { minTouch, radii, spacing } from '@/theme/tokens';

const THEME_OPTIONS: { value: ThemePreference; label: string }[] = [
  { value: 'system', label: t('more.themeSystem') },
  { value: 'light', label: t('more.themeLight') },
  { value: 'dark', label: t('more.themeDark') },
];

export default function MoreScreen() {
  const { colors, preference, setPreference } = useAppTheme();

  const swatches = [
    { label: 'Raise', bg: colors.raise, fg: colors.onRaise },
    { label: 'Bluff', bg: colors.gold, fg: colors.onGold },
    { label: 'Call', bg: colors.call, fg: colors.onCall },
    { label: 'Fold', bg: colors.fold, fg: colors.onFold },
  ];

  return (
    <Screen title={t('more.title')}>
      <Card>
        <AppText variant="heading">{t('more.appearance')}</AppText>
        <View style={[styles.segment, { borderColor: colors.border }]} accessibilityRole="radiogroup">
          {THEME_OPTIONS.map((opt) => {
            const active = opt.value === preference;
            return (
              <Pressable
                key={opt.value}
                onPress={() => setPreference(opt.value)}
                accessibilityRole="radio"
                accessibilityState={{ checked: active }}
                style={[styles.segmentItem, active && { backgroundColor: colors.accent }]}
              >
                <AppText variant="bodyStrong" color={active ? colors.onAccent : colors.text}>
                  {opt.label}
                </AppText>
              </Pressable>
            );
          })}
        </View>
        <View style={styles.swatches}>
          {swatches.map((s) => (
            <View key={s.label} style={[styles.swatch, { backgroundColor: s.bg }]}>
              <AppText variant="monoStrong" color={s.fg}>
                {s.label}
              </AppText>
            </View>
          ))}
        </View>
      </Card>

      <Card>
        <AppText variant="heading">{t('more.typeSample')}</AppText>
        <AppText variant="title">Bricolage Grotesque</AppText>
        <AppText>IBM Plex Sans – Pot Odds, Equity und Fold Equity.</AppText>
        <AppText variant="mono">{'IBM Plex Mono · A♠K♦ · $1,39 · 25\u00A0%'}</AppText>
      </Card>

      <Card>
        <AppText variant="heading">{t('more.responsible')}</AppText>
        <AppText muted>{t('common.comingSoon', { phase: 4 })}</AppText>
      </Card>

      <Card>
        <AppText variant="heading">{t('more.about')}</AppText>
        <AppText muted>
          {t('more.version')} {Constants.expoConfig?.version ?? '–'}
        </AppText>
      </Card>
    </Screen>
  );
}

const styles = StyleSheet.create({
  segment: {
    flexDirection: 'row',
    borderWidth: 1,
    borderRadius: radii.md,
    overflow: 'hidden',
  },
  segmentItem: {
    flex: 1,
    minHeight: minTouch,
    alignItems: 'center',
    justifyContent: 'center',
  },
  swatches: { flexDirection: 'row', gap: spacing.sm, marginTop: spacing.sm },
  swatch: {
    flex: 1,
    minHeight: minTouch,
    borderRadius: radii.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
