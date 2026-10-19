# Microstakes Coach – Projektbriefing für Claude Code

> Arbeitstitel „Microstakes Coach“ – Name vor Store-Release final festlegen (keine Markennamen wie PokerStars, GTO Wizard o. Ä. verwenden).

## 1. Ziel
Eine **kostenlose, offline nutzbare Android-App** (später iOS aus derselben Codebasis) für Spieler, die **No-Limit Hold'em Cash Games an den Micro Stakes (NL2–NL25), 6-max** lernen und ihr Spiel tracken wollen. Lernen und Spielen in einer App:

1. **Trainer** – Preflop-Entscheidungen und Poker-Mathe üben, mit sofortigem Feedback.
2. **Session-Tracker** – Ergebnisse, Bankroll und Winrate festhalten, Bankroll-Regeln anwenden.

Zielgruppe: Freizeitspieler, deutschsprachig, Einsteiger bis Fortgeschrittene, spielen online (z. B. Zoom) mit kleinem Budget (50–500 $). Der Auftraggeber ist selbst so ein Spieler und testet jede Version.

## 2. Harte Rahmenbedingungen
- **Kein Echtgeld, keine Wetten, keine virtuelle Währung**, keine Links/Werbung/Affiliate zu Glücksspielanbietern. Die App ist ein Lern- und Notizwerkzeug.
- **Keine Werbung, keine In-App-Käufe, kein Konto, kein Server.** Alle Daten bleiben lokal auf dem Gerät.
- **Keine Netzwerkzugriffe** nötig; App muss vollständig offline funktionieren (Fonts lokal einbinden).
- **Zielgruppe 18+.** Nicht für Kinder/Families-Programm.
- **Google Play:** Neue Apps müssen ab 31.08.2026 **targetSdkVersion 36 (Android 16)** haben. Ausgabe als **Android App Bundle (.aab)**.
- Sprache der UI: **Deutsch** (Texte zentral in i18n-Dateien, damit Englisch später leicht ergänzt werden kann).
- **Verantwortungsvolles Spielen** ist Teil der App (siehe 5.5).

## 3. Tech-Stack (Vorgabe, Abweichungen nur nach Rückfrage)
- **Expo (React Native) + TypeScript**, aktuelle stabile Expo-SDK-Version, die targetSdk 36 erfüllt (prüfen, ggf. über `expo-build-properties` setzen).
- **expo-router** für Navigation (Tabs: Trainer, Ranges, Mathe, Sessions, Mehr).
- **expo-sqlite** für Sessions, Antworten/Statistik; kleine Einstellungen über AsyncStorage oder SQLite.
- Diagramme mit **react-native-svg** (eigene schlanke Komponenten, keine schweren Chart-Libs nötig).
- **Jest** für Unit-Tests der Logik.
- **EAS Build** für Release-Builds (.aab), damit lokal kein Android Studio nötig ist. Für die Entwicklung: Expo Go / Development Build auf dem Android-Handy.
- Schriften: IBM Plex Sans / IBM Plex Mono / Bricolage Grotesque (SIL OFL, lokal eingebunden über `expo-font`), Lizenzdateien mitliefern.

## 4. Vorhandene Inhalte (wiederverwenden!)
- `content/poker-content.json` – **alle Ranges und Texte** des bestehenden Trainers:
  - `rfi` – Open-Raise-Ranges UTG, HJ, CO, BTN, SB (+ Erklärtexte). Notation: `22+`, `A2s+`, `AJo+`, `K2s-K4s`, `+UTG` (= alle Hände der UTG-Range).
  - `vsOpen` – 15 Spots „Gegen Open“ (v = 3-Bet Value, b = 3-Bet Bluff, c = Call, Rest Fold).
  - `vs3bet` – 9 Spots „Gegen 3-Bet“ (v = 4-Bet Value, b = 4-Bet Bluff, c = Call; nur Hände der eigenen RFI-Range).
  - `vsLimp` – 8 Spots „Gegen Limper“ (v = Iso-Raise, c = Overlimp/Komplettieren/Check, Rest Fold; `BB_X` hat kein Fold).
  - `math` – Pot-Odds-Tabelle und Bluff-Tabelle.
- `reference/trainer-v1.3.html` – **funktionierende Referenzimplementierung** (Web) mit Quiz-Logik, „Grenzfälle“-Auswahl, Feedback-Texten, Mathe-Kursen (Tutorial-Lektionen), Übungsgeneratoren, Positionsgrafik. Logik und Texte von hier übernehmen und sauber in TypeScript-Module überführen.
- `reference/cash-tracker-web.html` – Referenz für den Session-Tracker (Felder, Kennzahlen, Bankroll-Stufen).
- **Wichtig:** Die Ranges sind vereinfachte, GTO-nahe Richtwerte (keine Solver-Ausgabe). In der App so kennzeichnen. Das Datenformat so bauen, dass Ranges später durch Solver-Daten mit **gemischten Frequenzen** (z. B. 40 % Raise / 60 % Call) ersetzt werden können.

## 5. Funktionen MVP (v1.0 Store-Release)

### 5.1 Trainer – Preflop (4 Module)
| Modul | Spot | Antworten |
|---|---|---|
| Open-Raise | alle folden zu dir (UTG–SB) | Fold / Raise |
| Gegen Open | jemand eröffnet vor dir (15 Spots) | Fold / Call / 3-Bet |
| Gegen 3-Bet | du hast eröffnet, 3-Bet kommt (9 Spots) | Fold / Call / 4-Bet |
| Gegen Limper | ein Limper vor dir (8 Spots) | Fold / Limp / Iso-Raise (BB: Check statt Fold) |
- Tisch-Grafik 6-max mit Positionen im Uhrzeigersinn, Dealer-Button, Einsätzen (Open 2,5 BB, SB 3 BB, 3-Bet IP 7,5 / OOP ~10 BB, Limp 1 BB).
- Auswahl: Position/Spot oder „Alle gemischt“; Modus „Grenzfälle“ (Hände an Range-Grenzen, Standard) oder „Alle Hände“ (gewichtet nach Kombinationen).
- Nach jeder Antwort: richtig/falsch, Erklärung (Texte aus Referenz), 13×13-Matrix mit markierter Hand.
- **Fehlertraining (neu):** Hände, die falsch beantwortet wurden, kommen gezielt häufiger (einfache Spaced Repetition, z. B. Leitner-Boxen).

### 5.2 Ranges (Nachschlagen)
- 13×13-Matrix je Modul/Spot, Farben: Raise/Value rot, Bluff gold, Call/Limp grün, Fold grau; Anteil in %.
- Positionsgrafik + Tabelle (Reihenfolge preflop/postflop).
- Hinweis zur Herkunft der Ranges.

### 5.3 Mathe
- **Kurs 1 Pot Odds** (7 Lektionen) und **Kurs 2 Fold Equity** (5 Lektionen) mit Kontrollfrage pro Lektion – Texte aus Referenz übernehmen.
- Übungen: Outs zählen, Outs → Equity (Regel 2/4), Bet-Tabelle lernen, Bet-Größe erkennen, Pot Odds, Call oder Fold, Bluff-Tabelle, Bluffen oder aufgeben, Semi-Bluff.
- **Regel aus Nutzertests:** In Rechenaufgaben **nur Dollar-Beträge** zeigen (Pot $1,39, Bet $1,01) – nie „½ Pot“ vorgeben; Brüche erst in der Auflösung. Lerntabelle vereinfacht: ¼→17, ⅓→20, ½→25, **⅔–¾→30**, Pot→33, 2×→40 %.
- Spickzettel mit Lerntabellen inkl. „Werte verdecken“-Selbsttest.

### 5.4 Session-Tracker
- Session: Datum, Limit (NL2/NL5/NL10/NL25), Spielart (Zoom / normaler Tisch), Hände, Gewinn/Verlust in $, Dauer, Tilt/Stimmung 1–5, Notiz (unklare Hände).
- Kennzahlen: Bankroll, Gewinn, **bb/100**, $/Stunde, Hände gesamt; Bankroll-Kurve (Linie über Sessions).
- Start-Bankroll und Währung ($/€) in den Einstellungen.
- **Bankroll-Regeln** (anzeigen + Hinweis-Badge): bis $150 NL2 · ab $150 NL5 · ab $300 NL10 · ab $750 NL25 (≈30 Buy-ins); Abstieg unter 20 Buy-ins; Aufstieg erst nach 20.000 Händen mit positiver Winrate; Stop-Loss 3 Buy-ins pro Session.
- Hinweis zur Aussagekraft: Winrate erst ab ~50.000 Händen belastbar.

### 5.5 Verantwortungsvoll spielen
- Stop-Loss-Warnung beim Eintragen einer Session, optionales Session-Zeitlimit als Erinnerung.
- Hilfeseite mit seriöser Anlaufstelle in Deutschland: **check-dein-spiel.de** (BIÖG), Hotline 0800 1 37 27 00 (kostenlos, anonym) – Nummer vor Release erneut prüfen.

### 5.6 Statistik & Einstellungen
- Trefferquote je Modul/Position/Übung, häufigste Fehler, Tagesanzahl, Serie.
- Einstellungen: Währung, Start-Bankroll, Dark/Light/System, Daten **exportieren/importieren (JSON)** als Backup, alles löschen (mit Bestätigung).
- Über/Impressum: Version, Lizenzhinweise (Fonts), Datenschutz-Link, Haftungsausschluss („keine Anlageberatung, kein Gewinnversprechen, Ranges vereinfacht“).

## 6. Nicht im MVP
Postflop-Trainer, Hand-History-Import, Cloud-Sync/Konto, Englisch, iOS-Release, Solver-Daten. Architektur aber so wählen, dass Postflop-Module und Solver-Ranges (gemischte Frequenzen) später ergänzt werden können.

## 7. Architektur-Leitlinien
- Reine Logik (Range-Parser, Kombinatorik, Pot-Odds/EV-Rechner, Aufgaben-Generatoren, Spaced Repetition, Bankroll-Regeln) in `src/core/` – **ohne React**, vollständig mit Jest getestet.
- Inhalte (Ranges, Lektionen, Texte) als Daten (`src/content/`), nicht im UI-Code.
- Range-Datenmodell: `Record<HandCode, {raise?: number; bluff?: number; call?: number}>` mit Frequenzen 0–1 (heute nur 0 oder 1).
- Deterministische Zufallsfunktion injizierbar (Tests).
- Barrierearm: ausreichende Kontraste in Hell und Dunkel, Touch-Ziele ≥ 48 dp, Screenreader-Labels für Karten/Matrix.

## 8. Design
- Ruhige „Filztisch“-Ästhetik: dunkles Grün als Akzent (#1F6B4A, dunkel #4CB585), Chip-Gold (#B8862B), neutraler Hintergrund (#F3F5F2 / dunkel #0F1512). Rot nur für Raise/Fehler, Grün für Call/richtig.
- Typo: Bricolage Grotesque (Überschriften), IBM Plex Sans (Text), IBM Plex Mono (Zahlen, Karten, Matrix).
- Light/Dark nach Systemeinstellung. Hochformat, Smartphone first.

## 9. Qualität / Definition of Done pro Feature
- TypeScript strict, ESLint ohne Fehler, Jest-Tests grün.
- Läuft offline (Flugmodus-Test) auf einem echten Android-Gerät.
- Keine Berechtigungen im Manifest außer den unbedingt nötigen (Ziel: keine).
- Daten überleben App-Neustart und App-Update.

## 10. Arbeitsweise
- In kleinen Schritten arbeiten, nach jeder Phase kurz zusammenfassen, was fertig ist und wie man es testet.
- Vor größeren Architektur- oder Paketentscheidungen kurz nachfragen.
- Commits mit aussagekräftigen Nachrichten.

## 11. Projektstand (wird je Phase gepflegt)
- **Phase 0 erledigt:** Expo SDK 58, expo-router-Tabs unter `src/app/`, Tokens in `src/theme/tokens.ts`, Texte in `src/i18n/de.ts`, Fonts lokal in `assets/fonts/`.
- Pfade: Inhalte liegen in `src/content/poker-content.json` (statt `content/`).
- Paketname `de.gerry.microstakescoach`; Test über Expo Go; Projekt liegt im Unterordner `microstakes-coach/` des GitHub-Pages-Repos (Datenschutzerklärung später unter `/microstakes-coach/privacy.html`).
- Netzwerk dieser Cloud-Umgebung: `docs.expo.dev` und `api.expo.dev` gesperrt → `npx expo install` mit `EXPO_OFFLINE=1` ausführen (nutzt `expo/bundledNativeModules.json`).
