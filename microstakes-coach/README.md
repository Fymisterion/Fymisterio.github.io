# Microstakes Coach

Kostenlose Offline-App (Expo / React Native / TypeScript) zum Lernen von NL-Hold'em-Cash-Games an den Micro Stakes: Preflop-Trainer, Poker-Mathe und Session-Tracker. Projektbriefing: [CLAUDE.md](CLAUDE.md).

- Expo SDK 58 · React Native 0.88 · expo-router (Tabs)
- Android-Paket: `de.gerry.microstakescoach`, `targetSdkVersion` 36 (über `expo-build-properties` fest gesetzt)
- Keine Werbung, kein Konto, kein Server – alle Daten bleiben auf dem Gerät

## Auf dem Android-Handy starten (Windows)

Einmalig:

1. **Node.js LTS** installieren (mind. 20.19, empfohlen 22): <https://nodejs.org> → „LTS“ → Installer durchklicken.
2. **Git für Windows** installieren: <https://git-scm.com/download/win>.
3. Auf dem Handy **Expo Go** aus dem Play Store installieren (Version für SDK 58).
4. Repository klonen und Abhängigkeiten installieren – in PowerShell:

   ```powershell
   git clone https://github.com/fymisterion/fymisterio.github.io.git
   cd fymisterio.github.io
   git checkout claude/new-session-33or78
   cd microstakes-coach
   npm install
   ```

Bei jedem Test:

```powershell
cd fymisterio.github.io\microstakes-coach
git pull
npx expo start
```

Im Terminal erscheint ein QR-Code. In **Expo Go** auf „Scan QR code“ tippen und scannen – die App lädt.

- Handy und PC müssen im **selben WLAN** sein. Fragt die Windows-Firewall beim ersten Start nach Node.js, „Private Netzwerke“ erlauben.
- Klappt die Verbindung nicht (z. B. Gäste-WLAN), stattdessen `npx expo start --tunnel` nutzen (installiert beim ersten Mal ein Zusatzpaket).
- Änderungen am Code werden sofort neu geladen. Im Terminal `r` drücken lädt die App komplett neu.
- **Flugmodus-Test:** Expo Go braucht für das Laden aus dem Dev-Server das WLAN. Echtes Offline-Verhalten testen wir mit dem Release-Build (Phase 5). Die App selbst macht keine Netzwerkzugriffe.

## Befehle

| Befehl | Zweck |
|---|---|
| `npm start` | Dev-Server (`expo start`) |
| `npm test` | Jest-Tests |
| `npm run lint` | ESLint |
| `npm run typecheck` | TypeScript-Prüfung |
| `npm run check` | alle drei nacheinander |

Neue Pakete immer mit `npx expo install <paket>` hinzufügen (wählt SDK-kompatible Versionen).

## Struktur

```
src/
  app/          Routen (expo-router): _layout.tsx, (tabs)/index|ranges|mathe|sessions|mehr
  components/   UI-Bausteine (AppText, Screen, Card, TabIcon)
  core/         reine Logik ohne React, mit Jest-Tests (__tests__)
  content/      Inhalte als Daten (poker-content.json)
  i18n/         UI-Texte (de.ts) und t()-Funktion
  theme/        Farb-Tokens Hell/Dunkel, Typo, Abstände, ThemeProvider
assets/fonts/   Bricolage Grotesque, IBM Plex Sans/Mono (lokal) + OFL-Lizenzen
reference/      Web-Referenzimplementierungen (Trainer v1.3, Cash-Tracker)
```
