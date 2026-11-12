# Security Policy

## Supported versions

Security fixes are applied to the latest commit on `main`.

## Reporting a vulnerability

Please do not open a public issue for security problems.

Use GitHub's private vulnerability reporting for this repository (Security tab, "Report a vulnerability"). Include:

- a description of the issue and its impact,
- steps or a proof of concept to reproduce it,
- the affected version or commit.

You can expect an acknowledgement within a few days. Once a fix is available the report can be disclosed together with credit to the reporter, if wanted.

## Deployment checklist

- Set `SPRING_PROFILES_ACTIVE=prod`, a unique `JWT_SECRET` (32+ characters) and database credentials.
- Run with `SEED_DEMO_DATA=false`, so the well-known demo accounts do not exist.
- Terminate TLS in front of the application and do not publish the database port.
- Keep `ALLOW_PRIVATE_WEBHOOKS` at `false`.
- Keep dependencies current; Dependabot is configured for Maven, npm, Docker and GitHub Actions.
