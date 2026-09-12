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
- Eingehende Share-Dateien werden als nicht vertrauenswürdig behandelt und größenbegrenzt lokal kopiert.
- Backup-Dateien werden vor Import auf Format und Version geprüft.
- Release-Builds sind nicht debuggable und werden minifiziert.

## Schwachstellen melden

Bitte keine Sicherheitslücken, Tokens, Schlüssel oder personenbezogenen Beispieldaten in öffentlichen Issues posten. Nutze GitHubs privaten Security-Advisory-Meldeweg für dieses Repository, sofern verfügbar.

Eine gute Meldung enthält betroffene Version, reproduzierbare Schritte, erwartetes und tatsächliches Verhalten sowie eine Einschätzung möglicher Auswirkungen. Keine echten Nutzerdaten mitschicken.

## Repository-Härtung

CI baut Debug und Release und führt Gitleaks Secret Scanning aus. Ein Dependency-Review-Workflow ist vorhanden. Damit er wirksam wird, muss GitHubs Dependency Graph in den Repository-Einstellungen aktiviert werden. Branch Protection bzw. ein Ruleset für `main` sowie GitHubs serverseitige Secret-Scanning-Funktionen sollten zusätzlich aktiviert werden, sofern für das Repository verfügbar.

Diese Repository-Admin-Einstellungen lassen sich nicht über die aktuell verwendete GitHub-App-Verbindung ändern und müssen deshalb in GitHub selbst gesetzt werden.
