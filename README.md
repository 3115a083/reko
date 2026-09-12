# ReKo Android

ReKo ist eine local-first Android-App zur Erfassung und Organisation von Reisekosten, Bewirtungen und zugehörigen Belegen. Die App richtet sich vor allem an Selbstständige mit häufigen auswärtigen Einsätzen. Sie ist keine Buchhaltungssoftware und ersetzt keine Rechts- oder Steuerberatung.

## Stand

Aktuelle Testversion: **0.1.1**.

### In dieser Testversion umgesetzt

- Material-3-Oberfläche mit MaterialToolbar, Cards, Outlined Fields und Material-3-Typografie
- systemgerechte Edge-to-edge-Behandlung für Android mit targetSdk 35, sichtbare Inhalte werden um Status- und Navigationsleisten eingerückt
- Übersicht als Startseite
- passende eigene Navigations-Icons für Übersicht, Reise, Bewirtung und Einstellungen
- GitHub-Link und Updateprüfung im Footer der Einstellungen
- Adaptive Launcher Icon auf Basis des freigegebenen ReKo-App-Icons
- mehrere lokal getrennte Profile
- Reiseerfassung mit Start, Ende, Ziel, beruflichem Anlass und Notiz
- Bewirtungserfassung mit Datum, Betrieb, Betrag, Teilnehmern, Anlass und Trinkgeld
- profilbezogene Übersicht
- profilbezogener JSON-Backup-Export und validierter Import über Android Storage Access Framework
- Android Share Target für PDF, JPEG und PNG mit Größenlimit und lokaler Kopie
- Launcher-Shortcuts für „Reise eintragen“ und „Bewirtung erfassen“
- optionale READ_CALENDAR-Berechtigung, nur auf ausdrückliche Nutzeraktion
- Light/Dark über Material 3 DayNight
- keine Telemetrie, keine Werbung, kein zentraler ReKo-Server
- Android-Systembackup deaktiviert

### Noch nicht vollständig umgesetzt

Die Planungsgrundlage ist umfangreicher als diese Testversion. Geplant sind unter anderem Reisetage und Reiseabschnitte, Fahrzeuge und Reisenebenkosten, Mahlzeiten und Pauschalen, Dreimonatsregel-Hinweise, vollständige Dokumentzuordnung und Scanner-Bearbeitung, Kalenderauswahl je Profil, strukturierte Vollständigkeitsprüfung, Fremdwährungen, Regelversionierung, verschlüsselte Backups und eine optionale Lexware-Office-Integration.

## Bedienung und Design

Die App verwendet vier Hauptbereiche in der Bottom Navigation: Übersicht, Reise, Bewirtung und Einstellungen. Übersicht ist beim normalen Start ausgewählt. Auf Geräten mit aktueller Android-Edge-to-edge-Darstellung werden Systemleisten-Inset explizit berücksichtigt, damit Bedienelemente nicht hinter Statusleiste, Benachrichtigungsbereich oder Systemnavigation liegen.

GitHub-Link, Updateprüfung und `Vibecoded with ❤️` stehen gemeinsam am Ende der Einstellungen im Footer.

## Datenschutz und Sicherheit

ReKo speichert fachliche Daten lokal auf dem Gerät. Es gibt standardmäßig keine Telemetrie und keine Übertragung an einen ReKo-Server. Das Manifest nutzt minimale Berechtigungen, keine breite Speicherberechtigung, kein WRITE_CALENDAR und kein Cleartext-Netzwerk. Backups werden nur nach Nutzeraktion an einen über SAF gewählten Ort geschrieben.

Das öffentliche Repository enthält keine API-Keys, Passwörter, Signing-Schlüssel oder Nutzerdaten. CI baut Debug und Release und führt Secret Scanning aus. Ein Dependency-Review-Workflow ist vorhanden; dafür muss zusätzlich GitHubs Dependency Graph in den Repository-Einstellungen aktiviert sein. Siehe [SECURITY.md](SECURITY.md).

## Build

Voraussetzungen: JDK 17, Android SDK 35 und Gradle 8.9.

```bash
gradle assembleDebug
gradle assembleRelease
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

## Testhinweise

Für diesen Stand besonders prüfen:

1. App starten und prüfen, dass Übersicht die Startseite ist.
2. Auf Geräten mit Gestensteuerung und klassischer Navigation prüfen, dass nichts hinter den Systemleisten liegt.
3. Alle vier Bottom-Navigation-Icons und deren Zuordnung prüfen.
4. Einstellungen bis zum Ende scrollen und GitHub-Link, Updateprüfung und Footer prüfen.
5. Launcher-Icon mit runder und Squircle-Maske prüfen.
6. Profil anlegen und wechseln.
7. Reise und Bewirtung in unterschiedlichen Profilen erfassen.
8. Backup eines Profils exportieren und importieren.
9. PDF oder Bild aus einer anderen Android-App an ReKo teilen.
10. Light und Dark Mode testen.

## Sicherheit melden

Bitte Sicherheitslücken nicht als öffentliches Issue veröffentlichen. Der vorgesehene Meldeweg steht in [SECURITY.md](SECURITY.md).

## Lizenz

Noch keine Lizenz festgelegt. Ohne ausdrückliche Lizenz gelten die üblichen Urheberrechte des Repository-Inhabers.
