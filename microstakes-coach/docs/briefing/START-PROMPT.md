# Start-Prompt für Claude Code

So gehst du vor:
1. Lege einen neuen, leeren Ordner an, z. B. `microstakes-coach`.
2. Kopiere den kompletten Inhalt dieses Briefing-Pakets hinein (CLAUDE.md, content/, reference/, PLAY-STORE-CHECKLISTE.md).
3. Öffne den Ordner in Claude Code und füge den Text unten als erste Nachricht ein.

---

```
Lies zuerst CLAUDE.md vollständig, dann content/poker-content.json und die beiden Dateien in reference/.

Wir bauen daraus die App „Microstakes Coach“ (Arbeitstitel): Expo + React Native + TypeScript, offline, kostenlos, für Google Play (Android) – genau nach CLAUDE.md.

Arbeite in diesen Phasen und stoppe nach jeder Phase für meinen Test auf dem Handy:

Phase 0 – Setup
- Expo-Projekt mit TypeScript, expo-router (Tabs: Trainer, Ranges, Mathe, Sessions, Mehr), ESLint, Jest.
- Lokale Fonts, Farb-Tokens für Hell/Dunkel, Grundlayout.
- targetSdkVersion 36 sicherstellen, Paketname de.<mein-name>.microstakescoach (frag mich nach dem Namen).
- Kurze Anleitung, wie ich die App mit Expo Go / Development Build auf meinem Android-Handy starte (ich arbeite unter Windows).

Phase 1 – Kern-Logik (src/core, ohne UI)
- Range-Parser für die Notation in poker-content.json + 169-Hände-Matrix + Kombinatorik.
- Spot-Engine für die 4 Preflop-Module (Aktion je Hand, Grenzfälle, gewichtete Auswahl).
- Mathe: Outs-Szenarien, Equity (Regel 2/4 und exakt), Pot Odds, Bluff-Break-even, Semi-Bluff-EV – wie in reference/trainer-v1.3.html.
- Bankroll-Regeln und Kennzahlen (bb/100, $/h).
- Spaced Repetition für Fehler.
- Vollständige Jest-Tests. Prüfe u. a., dass sich in keinem Spot die Aktionen überschneiden und Modul 3 nur Hände der eigenen Open-Range nutzt.

Phase 2 – Trainer & Ranges (UI)
- Preflop-Quiz mit Tisch-Grafik, Feedback, Matrix; Ranges-Ansicht; Statistik.

Phase 3 – Mathe (UI)
- Beide Kurse mit Lektionen und Kontrollfragen, alle Übungen, Spickzettel.

Phase 4 – Session-Tracker & Einstellungen
- Sessions in SQLite, Kennzahlen, Bankroll-Kurve, Bankroll-Stufen, Stop-Loss-Hinweis, Export/Import JSON, Verantwortungsvoll-spielen-Seite, Über/Haftungsausschluss.

Phase 5 – Store-Vorbereitung
- App-Icon (adaptiv) und Splash, eas.json für Release-AAB, Versionierung.
- Datenschutzerklärung als einfache HTML-Seite (zum Hosten z. B. über GitHub Pages) – die App sammelt keine Daten.
- Texte für den Play Store (Kurz-/Langbeschreibung DE), Vorschläge für 4–6 Screenshots.
- Antworten-Spickzettel für „Datensicherheit“ und den Inhaltsfragebogen (IARC) gemäß PLAY-STORE-CHECKLISTE.md.

Fang mit Phase 0 an. Stell mir vorher die Fragen, die du für Phase 0 wirklich brauchst (max. 3).
```
