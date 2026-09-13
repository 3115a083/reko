# ReKo Android

ReKo ist eine **native local-first Android-App** zur Erfassung und Organisation von Reisekosten, Bewirtungen und zugehörigen Belegen. Die App richtet sich vor allem an Selbstständige mit häufigen auswärtigen Einsätzen. Sie ist keine Buchhaltungssoftware und ersetzt keine Rechts- oder Steuerberatung.

Die fachlich sinnvollen Funktionen der ReKo-Web-App werden auch für Android umgesetzt, aber ausdrücklich **nicht** als WebView oder Browserkopie. Android verwendet native Komponenten, Android-Berechtigungen, Storage Access Framework, Content Provider und lokale App-Datenhaltung.

## Stand

Aktuelle Testversion: **0.2.0**.

### In dieser Testversion umgesetzt

- Material-3-Oberfläche mit Cards, Outlined Fields, Material-3-Typografie und systemgerechter Edge-to-edge-Behandlung für targetSdk 35
- Übersicht als Startseite und genau vier Hauptbereiche: Übersicht, Reise, Bewirtung und Einstellungen
- mehrere vollständig getrennte lokale Profile
- Profile anlegen, wechseln, umbenennen und mit deutlicher Bestätigung löschen
- Reiseerfassung mit Start, Ende, Ziel, beruflichem Anlass und Notiz
- Bewirtungserfassung mit Datum, Betrieb, Betrag, Teilnehmern, Anlass und Trinkgeld
- lokale Dokument-Inbox für PDF, JPEG und PNG
- Belegimport über Android-Dateiauswahl, Kamera und Android Share Target
- SHA-256-Duplikaterkennung für exakte Belegduplikate
- sichere Formatprüfung anhand des tatsächlichen Dateiinhalts, Größenlimit und ausschließlich lokale Speicherung
- Zuordnung eines Belegs zu Reise, Bewirtung oder Dokument-Inbox
- unverändertes Original plus separat gespeicherte bearbeitete Bildkopie
- native Bildbearbeitung mit 90°-Drehung und manuell einstellbarem Zuschnitt
- gezieltes Teilen eines ausgewählten Belegs an andere Android-Apps, als erste datensparsame Lexware-/Export-Stufe
- vollständiges profilbezogenes ZIP-Backup über Storage Access Framework mit strukturierten Daten und lokalen Belegdateien
- validierter ZIP-Restore ohne stilles Überschreiben; exakte Dokumentduplikate werden übersprungen
- lokaler ICS-Import; Termine werden ausschließlich als Reisevorschläge behandelt und erst nach Bestätigung übernommen
- optionale Android-Kalenderintegration mit READ_CALENDAR nur nach Nutzeraktion
- gezielte Kalender-Allowlist je Profil statt ungefragter fachlicher Auswertung aller Kalender
- Reisevorschläge nur aus freigegebenen Kalendern, mit Nutzerbestätigung vor dem Speichern
- versionierte Rechenwerte mit sichtbarem Standardwert, persönlichem Override, Quelle, Gültigkeit und Rücksetzen
- amtliche 2026-Basiswerte für dienstliche Kraftwagen-Kilometer und Inlands-Verpflegungspauschalen als sichtbare Standardwerte; Nutzerwerte werden nie still überschrieben
- Info-Dialoge zu Fachbegriffen und Nachweis-/Originalformat-Hinweisen
- Launcher-Shortcuts für „Reise eintragen“ und „Bewirtung erfassen“
- Adaptive Launcher Icon auf Basis des freigegebenen ReKo-App-Icons
- GitHub-Link, Updateprüfung und `Vibecoded with ❤️` im Footer der Einstellungen
- Light/Dark über Material 3 DayNight
- keine Telemetrie, keine Werbung, kein zentraler ReKo-Server
- Android-Systembackup deaktiviert

### Noch offen gegenüber der vollständigen Planungsgrundlage

Noch nicht vollständig umgesetzt sind insbesondere automatische Kantenerkennung und Perspektivkorrektur beim Scanner, echte Mehrseitenscan-Erstellung, strukturierte ZUGFeRD-/XRechnung-Auswertung, mögliche Duplikate anhand unscharfer Metadaten, verschlüsselte Backups, vollständige Reisetage/Reiseabschnitte/Fahrzeuge/Reisenebenkosten/Mahlzeiten/Fremdwährungen, Vollständigkeits- und Konfliktassistenten sowie eine direkte optionale Lexware-API-Anbindung mit sicherer Token-Speicherung.

## Native Android-Funktionen

Android-spezifisch unterstützt ReKo:

- Android Share Target für PDF/JPEG/PNG
- Launcher-Shortcuts
- Storage Access Framework für Import, Export und Backups ohne breite Speicherberechtigung
- Android FileProvider für sichere Kamera- und gezielte Share-URIs
- Android Calendar Provider mit Nutzer-Allowlist je Profil
- externe Kamera-App über systemgerechten Capture-Intent, ohne dauerhafte Kamera-Hintergrundnutzung

## Datenschutz und Sicherheit

ReKo verarbeitet fachliche Daten lokal auf dem Gerät. Es gibt standardmäßig keine Telemetrie und keine Übertragung an einen ReKo-Server. Das Manifest nutzt minimale Berechtigungen, keine breite Speicherberechtigung, kein WRITE_CALENDAR und kein Cleartext-Netzwerk.

Eingehende Dateien und Backups werden als nicht vertrauenswürdig behandelt. ReKo begrenzt Größen, plausibilisiert Dateiformate, prüft Content-URIs, verhindert unsichere ZIP-Pfade und führt importierte Inhalte nicht aus. Exakte Dokumentduplikate werden lokal per SHA-256 erkannt. Bearbeitungen erzeugen zusätzliche Dateien; das Original bleibt bestehen.

Das öffentliche Repository enthält keine API-Keys, Passwörter, Signing-Schlüssel oder Nutzerdaten. CI baut Debug und Release, führt Secret Scanning und CodeQL aus. Ein Dependency-Review-Workflow ist vorhanden; dafür muss GitHubs Dependency Graph in den Repository-Einstellungen aktiviert sein. Siehe [SECURITY.md](SECURITY.md).

## Rechenwerte

Standardwerte und persönliche Werte werden getrennt gespeichert. Die App zeigt Quelle, Gültigkeitsstand und Override sichtbar an und überschreibt persönliche Werte nicht still. Die 2026-Basiswerte stammen aus dem amtlichen Lohnsteuer-Handbuch des Bundesministeriums der Finanzen. Vor steuerlicher Nutzung bleibt eine Prüfung des Einzelfalls erforderlich.

## Build

Voraussetzungen: JDK 17, Android SDK 35 und Gradle 8.9.

```bash
gradle assembleDebug
gradle assembleRelease
```

Die Debug-APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

## Testhinweise

Für Version 0.2.0 besonders prüfen:

1. Profile anlegen, wechseln, umbenennen und löschen. Daten dürfen nicht zwischen Profilen erscheinen.
2. Reise und Bewirtung speichern und Belege gezielt zuordnen.
3. PDF/JPEG/PNG aus Dateimanager oder anderer App importieren und ein exaktes Duplikat erneut importieren.
4. Beleg fotografieren, Bild drehen und manuell zuschneiden. Original und bearbeitete Kopie müssen getrennt bestehen bleiben.
5. Beleg gezielt an eine andere Android-App teilen.
6. ZIP-Profilbackup exportieren und in ein Profil importieren.
7. Manipuliertes oder zu großes Backup ablehnen lassen.
8. ICS-Datei importieren und einen Termin erst nach Bestätigung als Reise übernehmen.
9. READ_CALENDAR aktivieren, einzelne Kalender auswählen und nur daraus Vorschläge anzeigen lassen.
10. Persönlichen Rechenwert setzen und wieder auf den Standard zurücksetzen.
11. Auf 360 dp Breite, Light/Dark sowie Gesten- und klassischer Systemnavigation prüfen.
12. Launcher-Shortcuts und Share Target testen.

## Sicherheit melden

Bitte Sicherheitslücken nicht als öffentliches Issue veröffentlichen. Der vorgesehene Meldeweg steht in [SECURITY.md](SECURITY.md).

## Lizenz

Noch keine Lizenz festgelegt. Ohne ausdrückliche Lizenz gelten die üblichen Urheberrechte des Repository-Inhabers.
