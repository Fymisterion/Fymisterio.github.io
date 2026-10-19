import { AppText } from '@/components/AppText';
import { Card } from '@/components/Card';
import { Screen } from '@/components/Screen';
import { t } from '@/i18n';

export default function SessionsScreen() {
  return (
    <Screen title={t('sessions.title')} intro={t('sessions.intro')}>
      <Card>
        <AppText muted>{t('common.comingSoon', { phase: 4 })}</AppText>
      </Card>
    </Screen>
  );
}
