# Security Policy

## Unterstützte Version

Aktuell wird ausschließlich die neueste Version auf `main` unterstützt.

## Sicherheitsprinzipien

- Local-first, keine zentrale Speicherung persönlicher ReKo-Daten.
- Keine Telemetrie oder Werbe-SDKs.
- Minimale Android-Berechtigungen.
- Kein `WRITE_CALENDAR` und keine breite Speicherberechtigung.
- Kein Cleartext-Netzwerkverkehr.
- Android-Systembackup für App-Daten deaktiviert.
- Build- und Signing-Secrets gehören nicht ins Repository.
- Eingehende Content-URIs werden als nicht vertrauenswürdig behandelt. Scheme, Authority und verdächtige Systempfade werden vor dem Lesen geprüft.
- Dokumentimporte sind auf bewusst freigegebene Formate und 25 MB begrenzt. PDF/JPEG/PNG werden zusätzlich anhand der tatsächlichen Datei-Signatur plausibilisiert.
- Importierte Dateien werden ausschließlich als Daten gespeichert und nicht ausgeführt oder als aktiver HTML-Inhalt gerendert.
- Exakte Dokumentduplikate werden lokal per SHA-256 erkannt.
- Kamera- und ausgehende Dokumentfreigaben verwenden einen nicht exportierten Android `FileProvider` mit eng begrenzten App-Pfaden.
- Originaldokumente bleiben bei Bildbearbeitungen unverändert; bearbeitete Varianten werden separat gespeichert.
- Profilbackups werden als versionierte ZIP-Dateien verarbeitet. Restore begrenzt Gesamt- und Einzeldateigrößen, prüft das Manifest und blockiert absolute bzw. Traversal-Pfade wie `../`.
- Wiederherstellungen überschreiben bestehende Daten nicht still; exakte Belegduplikate werden übersprungen.
- Kalenderdaten und ICS-Dateien gelten ebenfalls als nicht vertrauenswürdige Eingaben. Kalendertermine erzeugen erst nach Nutzerbestätigung einen fachlichen Reiseeintrag.
- `READ_CALENDAR` wird nur nach Nutzeraktion angefordert; eine profilbezogene Allowlist begrenzt die fachliche Auswertung zusätzlich.
- Release-Builds sind nicht debuggable und werden minifiziert.

## Schwachstellen melden

Bitte keine Sicherheitslücken, Tokens, Schlüssel oder personenbezogenen Beispieldaten in öffentlichen Issues posten. Nutze GitHubs privaten Security-Advisory-Meldeweg für dieses Repository, sofern verfügbar.

Eine gute Meldung enthält betroffene Version, reproduzierbare Schritte, erwartetes und tatsächliches Verhalten sowie eine Einschätzung möglicher Auswirkungen. Keine echten Nutzerdaten mitschicken.

## Repository-Härtung

CI baut Debug und Release und führt Gitleaks Secret Scanning sowie CodeQL aus. Ein Dependency-Review-Workflow ist vorhanden. Damit er wirksam wird, muss GitHubs Dependency Graph in den Repository-Einstellungen aktiviert werden. Branch Protection bzw. ein Ruleset für `main` sowie GitHubs serverseitige Secret-Scanning-Funktionen sollten zusätzlich aktiviert werden, sofern für das Repository verfügbar.

Diese Repository-Admin-Einstellungen lassen sich nicht über die aktuell verwendete GitHub-App-Verbindung ändern und müssen deshalb in GitHub selbst gesetzt werden.
