# ReKo Android

ReKo ist eine local-first Android-App zur Erfassung und Organisation von Reisekosten, Bewirtungen und zugehörigen Belegen. Die App richtet sich vor allem an Selbstständige mit häufigen auswärtigen Einsätzen. Sie ist keine Buchhaltungssoftware und ersetzt keine Rechts- oder Steuerberatung.

## Stand

Aktuelle Testversion: **0.1.0**.

### In dieser V1 umgesetzt

- mehrere lokal getrennte Profile
- Reiseerfassung mit Start, Ende, Ziel, beruflichem Anlass und Notiz
- Bewirtungserfassung mit Datum, Betrieb, Betrag, Teilnehmern, Anlass und Trinkgeld
- profilbezogene Übersicht
- profilbezogener JSON-Backup-Export und validierter Import über Android Storage Access Framework
- Android Share Target für PDF, JPEG und PNG mit Größenlimit und lokaler Kopie
- Launcher-Shortcuts für „Reise eintragen“ und „Bewirtung erfassen“
- optionale READ_CALENDAR-Berechtigung, nur auf ausdrückliche Nutzeraktion
- Update-Link zur GitHub-Release-Seite
- Light/Dark über Material 3 DayNight
- keine Telemetrie, keine Werbung, kein zentraler ReKo-Server
- Android-Systembackup deaktiviert

### Noch nicht vollständig umgesetzt

Die Planungsgrundlage ist umfangreicher als diese erste Testversion. Geplant sind unter anderem Reisetage und Reiseabschnitte, Fahrzeuge und Reisenebenkosten, Mahlzeiten und Pauschalen, Dreimonatsregel-Hinweise, Dokumentzuordnung, Scanner-Bearbeitung, Kalenderauswahl je Profil, strukturierte Vollständigkeitsprüfung, Fremdwährungen, Regelversionierung, verschlüsselte Backups und eine optionale Lexware-Office-Integration.

## Datenschutz und Sicherheit

ReKo speichert fachliche Daten lokal auf dem Gerät. Es gibt standardmäßig keine Telemetrie und keine Übertragung an einen ReKo-Server. Das Manifest nutzt minimale Berechtigungen, keine breite Speicherberechtigung, kein WRITE_CALENDAR und kein Cleartext-Netzwerk. Backups werden nur nach Nutzeraktion an einen über SAF gewählten Ort geschrieben.

Das öffentliche Repository enthält keine API-Keys, Passwörter, Signing-Schlüssel oder Nutzerdaten. CI baut Debug und Release und führt Secret Scanning aus. Ein Dependency-Review-Workflow ist vorhanden; dafür muss zusätzlich GitHubs Dependency Graph in den Repository-Einstellungen aktiviert sein. Siehe [SECURITY.md](SECURITY.md).

## Build

Voraussetzungen: JDK 17, Android SDK 35 und Gradle 8.9.

```bash
gradle assembleDebug
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

## Testhinweise

Für den ersten Test besonders prüfen:

1. Profil anlegen und wechseln.
2. Reise und Bewirtung in unterschiedlichen Profilen erfassen.
3. Backup eines Profils exportieren und in ein anderes Profil importieren.
4. PDF oder Bild aus einer anderen Android-App an ReKo teilen.
5. App-Icon lange drücken und beide Shortcuts testen.
6. Light und Dark Mode testen.
7. Kalenderzugriff ablehnen und erneut aus Einstellungen anfordern.

## Sicherheit melden

Bitte Sicherheitslücken nicht als öffentliches Issue veröffentlichen. Der vorgesehene Meldeweg steht in [SECURITY.md](SECURITY.md).

## Lizenz

Noch keine Lizenz festgelegt. Ohne ausdrückliche Lizenz gelten die üblichen Urheberrechte des Repository-Inhabers.
