# Dranbleiben

Gewohnheiten für Android – ohne Konto, ohne Werbung, Teil der [App-Landschaft](https://fymisterion.github.io/app-landschaft/) neben Kopffrei, Teller, Eigengewicht und Microstakes Coach.

- **Heute:** geplante Gewohnheiten abhaken (Ja/Nein) oder Wert eintragen (messbar, z. B. kg, Min., Schritte), Wochenleiste mit Fortschritt je Tag, Serien.
- **Kalender:** Monatsübersicht für alle Gewohnheiten (Farbe = Anteil geschafft) oder eine einzelne, Tage nachtragen, Serie, beste Serie, Quote 30 Tage, Verlauf messbarer Werte.
- **Gewohnheiten:** Vorlagen, Emoji, Farbe, Wochentage, Tagesziel (mindestens/höchstens), Archiv, Reihenfolge.
- **Erinnerungen:** je Gewohnheit beliebig viele Uhrzeiten. Zur Zeit kommt eine Benachrichtigung mit allem, was noch offen ist; Antippen öffnet die **Abfrage**, die die offenen Gewohnheiten nacheinander abfragt. Ist nur eine Ja/Nein-Gewohnheit offen, geht „Ja“ / „Heute nicht“ direkt in der Benachrichtigung.
- **Claude:** gleicher Server und Connector wie Kopffrei und Teller ([kopffrei-cloud](https://github.com/Fymisterion/kopffrei-cloud), Werkzeuge `habit_*`). Verbinden über `dranbleiben://verbinden` von der Einrichtungsseite des Servers.

## Bauen

```bash
./gradlew assembleRelease     # signiert mit dem Familien-Schlüssel (keystore.properties, nicht im Git)
./gradlew testDebugUnitTest   # Regeln für Serien und Quote (gleich wie auf dem Server)
```

Bei jedem Push baut GitHub Actions eine Debug-APK (`.github/workflows/dranbleiben.yml`, Artefakt `dranbleiben-debug-apk`).

## Aufbau

| Datei | Inhalt |
|---|---|
| `data/Models.kt` | Datensätze (habit, log) und ihr JSON-Format – gleich auf dem Server (`kopffrei-cloud/src/habits.js`), Serien/Quote, Vorlagen |
| `data/Database.kt`, `data/Store.kt` | Room-Tabelle mit JSON-Datensätzen, Lesen/Schreiben, Abgleich-Vormerkung |
| `sync/SyncManager.kt` | Abgleich mit `/api/habits/sync` (2 s nach Änderungen, beim Öffnen, alle 15 Min.) |
| `reminders/*` | Ein Wecker je Erinnerungszeit, Benachrichtigung mit Abfrage bzw. direkter Antwort, Neuplanung nach Neustart |
| `ui/screens/*` | Heute, Kalender, Gewohnheiten (+ Bearbeiten), Abfrage, Mehr |

Ein Eintrag je Gewohnheit und Tag (uid `log-<gewohnheit>-<datum>`), daher keine Doppelten zwischen Handy und Claude. Konflikte: je Datensatz gewinnt die jüngere Änderung (`updatedAt`); Gelöschtes bleibt als `deleted` stehen.
