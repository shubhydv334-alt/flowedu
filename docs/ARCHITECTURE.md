# Architecture

```
Browser ── React SPA (Vite) ──▶ /api ──▶ Spring Boot ──▶ Relational database
                                          │
                                          ├── Scheduler (cron and date triggers)
                                          └── Outbound: SMTP, Telegram, webhooks, AI providers
```

In Docker Compose the SPA is served by nginx, which also proxies `/api` to the
backend so the browser sees a single origin.

## Backend (`backend/src/main/java/com/flowedu`)

| Area | Classes |
| --- | --- |
| Web | `AuthController`, `AcademicController`, `ImportController`, `InsightsController`, `TemplateController`, `WorkflowController` |
| Security | `SecurityConfig`, `JwtAuthFilter`, `JwtService`, `LoginAttemptLimiter`, `AdminBootstrap` |
| Errors | `ApiErrors` turns every exception into `{"error": ...}` |
| Persistence | `Domain` (entities), `Repos` (Spring Data repositories), `DataSeeder` |
| Automation | `WorkflowEngine`, `WorkflowService`, `WorkflowSupport`, `Triggers`, `Actions`, `Action`, `UrlGuard` |

Requests are authenticated by a stateless JWT. Authorization is role based and
enforced with `@PreAuthorize` on the controllers.

### Workflow execution

1. A trigger selects enabled workflows and calls `WorkflowEngine.start`.
2. The engine builds a context for each student and applies the conditions.
3. Actions run in order. Each action reads from and writes to the shared context.
4. The outcome of every step is stored as a run, in live or preview mode.

Actions that reach the network are isolated behind the `Action` interface, so
they can be tested without real services.

## Frontend (`frontend/src`)

| Path | Purpose |
| --- | --- |
| `api.js` | Fetch wrapper: token header, JSON errors, network failure message |
| `auth.jsx` | Session context and protected routes |
| `ErrorBoundary.jsx` | Keeps a render error from blanking the app |
| `pages/` | Dashboard, Academics, Import, Templates, Workflows, WorkflowBuilder, Logs, Login |
| `ui.jsx`, `charts.jsx` | Shared components |

## Configuration

Backend settings come from `application.yml`, overridden by environment
variables. The `prod` profile requires the database and secrets to be supplied
and disables demo data. See the README for the full variable list.
