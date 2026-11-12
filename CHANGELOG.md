# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Docker images for the backend and frontend and a Compose stack with PostgreSQL.
- `prod` Spring profile that requires database, JWT and CORS settings at startup.
- `/actuator/health` endpoint for health checks.
- Optional first administrator from `ADMIN_EMAIL` and `ADMIN_PASSWORD`.
- Backend unit and integration tests, frontend lint and unit tests.
- GitHub Actions CI and Dependabot configuration.
- Security policy, contributing guide and error boundary in the UI.
- Frontend component and page tests with coverage reporting, CodeQL scanning and an `.nvmrc`.

### Changed
- Demo data seeding can be disabled with `SEED_DEMO_DATA=false`.
- API errors share one JSON shape and unexpected failures no longer leak details.
- Backend start script fails fast on errors.

### Security
- Reject short or placeholder JWT secrets (placeholders are fatal under `prod`).
- Block webhook calls to loopback, private and link-local addresses.
- Throttle repeated failed sign-ins and validate input when creating users.
- Production frontend bundles no longer prefill or advertise the demo administrator credentials.

### Fixed
- Chart captions now render in uppercase as designed.
- Readable message when the API cannot be reached.
- Lint findings: unescaped quotes in JSX, unstable memo dependency, unused argument.
