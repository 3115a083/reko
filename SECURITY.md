# Security Policy

## Scope

This repository contains the source code for the ReKo web and Android applications. Both applications are designed to process sensitive travel, hospitality and receipt data locally wherever possible.

## Reporting a vulnerability

Do not publish suspected vulnerabilities, secrets, tokens, personal data or exploit details in public issues.

Report security problems privately to the repository owner through a private GitHub contact channel. If no private contact method is configured yet, the repository owner should enable GitHub Private Vulnerability Reporting before accepting external security reports.

Include:

- affected component and version or commit
- reproducible steps
- expected and actual behavior
- security impact
- suggested remediation, if known

## Security requirements for contributions

- Never commit API keys, passwords, tokens, signing keys, certificates, user databases, backups or real receipts.
- Use environment variables or platform-native secure storage for secrets.
- Keep dependencies minimal and remove unused packages.
- Review dependency updates and security alerts promptly.
- Validate and sanitize all untrusted input.
- Protect against XSS and unsafe HTML rendering in the web application.
- Use least-privilege permissions in the Android application.
- Do not enable debug behavior, verbose sensitive logging or test credentials in production builds.
- Avoid unnecessary network requests. Sensitive application data must not be transmitted unless a feature explicitly requires it and the user has opted in.
- Treat imported calendar data, files, backups and receipt data as untrusted input.
- Perform a security review before releases.

## Sensitive data

The project must not use production user data for tests, examples, screenshots or fixtures. Test data must be synthetic.
