import { de, type Strings } from './de';

type Join<K, P> = K extends string ? (P extends string ? `${K}.${P}` : never) : never;
type Leaves<T> = T extends string ? never : { [K in keyof T]: T[K] extends string ? K : Join<K, Leaves<T[K]>> }[keyof T];

export type TranslationKey = Leaves<Strings>;

const strings: Strings = de;

/** Text zu einem Schlüssel wie `tabs.trainer`; `{name}`-Platzhalter werden aus `params` ersetzt. */
export function t(key: TranslationKey, params?: Record<string, string | number>): string {
  let node: unknown = strings;
  for (const part of key.split('.')) {
    node = (node as Record<string, unknown>)[part];
  }
  if (typeof node !== 'string') return key;
  if (!params) return node;
  return node.replace(/\{(\w+)\}/g, (match, name: string) => (name in params ? String(params[name]) : match));
}
