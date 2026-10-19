import { AppText } from '@/components/AppText';
import { Card } from '@/components/Card';
import { Screen } from '@/components/Screen';
import { t } from '@/i18n';

export default function RangesScreen() {
  return (
    <Screen title={t('ranges.title')} intro={t('ranges.intro')}>
      <Card>
        <AppText muted>{t('ranges.disclaimer')}</AppText>
        <AppText muted>{t('common.comingSoon', { phase: 2 })}</AppText>
      </Card>
    </Screen>
  );
}
