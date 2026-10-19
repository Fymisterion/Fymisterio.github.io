import { AppText } from '@/components/AppText';
import { Card } from '@/components/Card';
import { Screen } from '@/components/Screen';
import { t } from '@/i18n';

export default function MathScreen() {
  return (
    <Screen title={t('math.title')} intro={t('math.intro')}>
      {(['math.courses.potOdds', 'math.courses.foldEquity'] as const).map((key) => (
        <Card key={key}>
          <AppText variant="heading">{t(key)}</AppText>
          <AppText muted>{t('common.comingSoon', { phase: 3 })}</AppText>
        </Card>
      ))}
    </Screen>
  );
}
