import { Tabs } from 'expo-router/js-tabs';

import { TabIcon, type TabIconName } from '@/components/TabIcon';
import { t, type TranslationKey } from '@/i18n';
import { useAppTheme } from '@/theme/ThemeProvider';
import { fonts } from '@/theme/tokens';

const TABS: { name: string; icon: TabIconName; label: TranslationKey }[] = [
  { name: 'index', icon: 'trainer', label: 'tabs.trainer' },
  { name: 'ranges', icon: 'ranges', label: 'tabs.ranges' },
  { name: 'mathe', icon: 'math', label: 'tabs.math' },
  { name: 'sessions', icon: 'sessions', label: 'tabs.sessions' },
  { name: 'mehr', icon: 'more', label: 'tabs.more' },
];

export default function TabLayout() {
  const { colors } = useAppTheme();
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.accent,
        tabBarInactiveTintColor: colors.textMuted,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
        tabBarLabelStyle: { fontFamily: fonts.bodyMedium, fontSize: 12 },
      }}
    >
      {TABS.map((tab) => (
        <Tabs.Screen
          key={tab.name}
          name={tab.name}
          options={{
            title: t(tab.label),
            tabBarIcon: ({ color }) => <TabIcon name={tab.icon} color={color} />,
          }}
        />
      ))}
    </Tabs>
  );
}
