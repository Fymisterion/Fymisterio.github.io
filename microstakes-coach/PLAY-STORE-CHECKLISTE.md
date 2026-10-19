# Google Play – Checkliste bis zur Veröffentlichung

Stand: Oktober 2026. Vor dem Release die verlinkten Google-Seiten erneut prüfen – Regeln ändern sich.

## 1. Entwicklerkonto
- [ ] Konto in der **Google Play Console** anlegen (einmalig **25 $**), Kontotyp **Privatperson**.
- [ ] Identitätsprüfung (Ausweis) und Telefon-/E-Mail-Bestätigung abschließen.
- [ ] Hinweis: Die Adresse/Kontaktdaten können im Store öffentlich sichtbar sein → ggf. separate E-Mail-Adresse anlegen.

## 2. Pflicht für neue Privatkonten: geschlossener Test
- [ ] App im Track **„Geschlossener Test“** hochladen.
- [ ] Mindestens **12 Tester** (Google-Konten, z. B. Freunde/Familie) per E-Mail-Liste oder Google Group einladen.
- [ ] Die Tester müssen **14 Tage ununterbrochen** eingetragen (opt-in) bleiben – frühzeitig organisieren!
- [ ] Danach in der Console **„Zugriff auf Produktion beantragen“** (Fragen zum Test beantworten; Prüfung dauert ca. bis zu 7 Tage).
- Tipp: Feedback der Tester sammeln und 1–2 Updates im Testzeitraum veröffentlichen – das hilft beim Antrag.

## 3. Technische Anforderungen
- [ ] **targetSdkVersion 36** (Pflicht für neue Apps seit 31.08.2026).
- [ ] Format **Android App Bundle (.aab)** – per `eas build -p android --profile production`.
- [ ] **Play App Signing** aktiv lassen (Standard). EAS verwaltet den Upload-Key – **Keystore-Backup herunterladen und sicher aufbewahren** (`eas credentials`).
- [ ] `versionCode` bei jedem Upload erhöhen (EAS `autoIncrement`).
- [ ] Paketname endgültig festlegen (z. B. `de.gerry.microstakescoach`) – **später nicht mehr änderbar**.
- [ ] Keine unnötigen Berechtigungen (Ziel: keine; `INTERNET` entfernen, falls Expo sie standardmäßig setzt und sie nicht gebraucht wird).
- [ ] Test auf echtem Gerät im Flugmodus.

## 4. Richtlinien (wichtig für eine Poker-App)
- [ ] **Kein Echtgeld-Glücksspiel**, keine Links, Werbung oder Affiliate zu Pokerseiten/Casinos.
- [ ] Keine Markennamen/Logos (PokerStars, GGPoker, GTO Wizard …) in Name, Icon, Screenshots oder Beschreibung.
- [ ] Keine Gewinnversprechen („Werde profitabel“, „Verdiene Geld mit Poker“) – stattdessen „Lernen“, „Üben“, „Tracken“.
- [ ] Die App ist **kein simuliertes Glücksspiel** (keine Spielchips, kein Spiel gegen Bots) – nur Quiz und Notizbuch. So im Inhaltsfragebogen beantworten.
- [ ] Haftungsausschluss und Hinweis auf Hilfeangebote (check-dein-spiel.de) in der App.

## 5. Store-Eintrag (Hauptinformationen)
| Element | Vorgabe |
|---|---|
| App-Name | max. 30 Zeichen, ohne Markennamen |
| Kurzbeschreibung | max. 80 Zeichen |
| Vollständige Beschreibung | max. 4000 Zeichen |
| App-Symbol | 512 × 512 px, PNG (32-bit), max. 1 MB |
| Feature-Grafik | 1024 × 500 px, JPG oder PNG (ohne Transparenz) |
| Smartphone-Screenshots | mind. 2 (empfohlen 4–8), 16:9 oder 9:16, Seiten 320–3840 px |
| Kategorie | **Bildung** (Alternative: Karten) |
| Kontakt | E-Mail-Adresse (Pflicht) |

Vorschlag Kurzbeschreibung: „Preflop-Trainer, Poker-Mathe und Session-Tracker für Micro-Stakes-Cash-Games.“ (77 Zeichen)

## 6. App-Inhalte (Bereich „Richtlinien → App-Inhalte“)
- [ ] **Datenschutzerklärung**: öffentliche URL (z. B. GitHub Pages). Inhalt: keine Datenerhebung, alle Daten lokal, keine Weitergabe, Kontakt-E-Mail.
- [ ] **Datensicherheit**: „Diese App erhebt oder teilt keine Nutzerdaten.“ (Nur korrekt, wenn wirklich keine Analytics/Crash-SDKs eingebunden sind – keine einbauen!)
- [ ] **Werbung**: „Nein, enthält keine Werbung.“
- [ ] **App-Zugriff**: „Alle Funktionen ohne Einschränkung verfügbar“ (kein Login).
- [ ] **Zielgruppe**: nur **18+** auswählen.
- [ ] **Einstufung (IARC-Fragebogen)**: wahrheitsgemäß beantworten. Kategorie z. B. „Referenz, Nachrichten oder Bildung“ bzw. „Alle anderen Apps“. Bei Glücksspiel-Fragen: kein Echtgeld, kein simuliertes Glücksspiel; Thema Glücksspiel wird aber behandelt → ggf. angeben. Ergebnis (USK/PEGI) akzeptieren.
- [ ] **Nachrichten-App / Gesundheit / Finanzen / Behörden**: jeweils „Nein“.
- [ ] Glücksspiel-Erklärung: Falls die Console eine Erklärung zu „Echtgeld-Glücksspiel“ abfragt → „Nein“.

## 7. Release-Ablauf
1. Interner Test (bis 100 Tester, sofort verfügbar) – eigenes Handy.
2. Geschlossener Test – 12+ Tester, 14 Tage.
3. Produktion beantragen → nach Freigabe Release in Produktion (Länder: Deutschland, Österreich, Schweiz zuerst).
4. Updates: neue .aab mit höherem `versionCode` hochladen, kurze Versionshinweise auf Deutsch.

## Quellen
- Ziel-API-Level: https://support.google.com/googleplay/android-developer/answer/11926878
- Testanforderungen neue Privatkonten: https://support.google.com/googleplay/android-developer/answer/14151465
- Glücksspiel-Richtlinie: https://support.google.com/googleplay/android-developer/answer/9877032
- Datensicherheit: https://support.google.com/googleplay/android-developer/answer/10787469
- Store-Grafiken: https://support.google.com/googleplay/android-developer/answer/9866151
- Hilfe bei Glücksspielproblemen: https://www.check-dein-spiel.de
