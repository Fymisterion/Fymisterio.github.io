import { AppText } from '@/components/AppText';
import { Card } from '@/components/Card';
import { Screen } from '@/components/Screen';
import { t, type TranslationKey } from '@/i18n';

const MODULES: TranslationKey[] = [
  'trainer.modules.rfi',
  'trainer.modules.vsOpen',
  'trainer.modules.vs3bet',
  'trainer.modules.vsLimp',
];

export default function TrainerScreen() {
  return (
    <Screen title={t('trainer.title')} intro={t('trainer.intro')}>
      {MODULES.map((key) => (
        <Card key={key}>
          <AppText variant="heading">{t(key)}</AppText>
          <AppText muted>{t('common.comingSoon', { phase: 2 })}</AppText>
        </Card>
      ))}
    </Screen>
  );
}
