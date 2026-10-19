import type { ReactNode } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/theme/ThemeProvider';
import { spacing } from '@/theme/tokens';

import { AppText } from './AppText';

interface Props {
  title: string;
  intro?: string;
  children?: ReactNode;
}

/** Grundlayout eines Tabs: Titel, optionaler Einleitungstext, scrollbarer Inhalt. */
export function Screen({ title, intro, children }: Props) {
  const { colors } = useAppTheme();
  return (
    <SafeAreaView edges={['top']} style={[styles.safe, { backgroundColor: colors.background }]}>
      <ScrollView contentContainerStyle={styles.content}>
        <AppText variant="title">{title}</AppText>
        {intro ? (
          <AppText muted style={styles.intro}>
            {intro}
          </AppText>
        ) : null}
        <View style={styles.body}>{children}</View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1 },
  content: { padding: spacing.lg, paddingBottom: spacing.xxl },
  intro: { marginTop: spacing.sm },
  body: { marginTop: spacing.xl, gap: spacing.md },
});
