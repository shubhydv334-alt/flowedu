# API Reference

All endpoints are served under `/api` and exchange JSON unless noted. Except for
`POST /api/auth/login` and `GET /actuator/health`, every request needs an
`Authorization: Bearer <token>` header.

Failures use a single shape, whatever the cause:

```json
{ "error": "Human readable message" }
```

| Status | Meaning |
| --- | --- |
| 400 | Validation failed, malformed body or bad parameter |
| 401 | Missing, invalid or expired token |
| 403 | Authenticated, but the role is not allowed |
| 404 | Resource does not exist |
| 405 | Method not supported for the path |
| 409 | Conflict, for example a duplicate email or roll number |
| 429 | Too many failed logins; retry later |
| 500 | Unexpected error. Details are logged, never returned |

Roles are `ADMIN`, `TEACHER` and `STUDENT`. "Write" below means `ADMIN` or `TEACHER`.

## Authentication

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | public | Exchange `{email, password}` for a token and profile |
| GET | `/api/auth/me` | any | Current user's name, email and role |
| GET | `/api/auth/users` | admin | List accounts |
| POST | `/api/auth/users` | admin | Create an account `{email, password, name, role}` |

Passwords must be at least 8 characters. After five failed attempts for the same
email and client address, login returns 429 for 15 minutes.

## Academic data

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/departments`, `/courses`, `/students`, `/exams`, `/assignments`, `/attendance` | any | List records |
| POST | `/api/departments`, `/courses`, `/students`, `/exams`, `/assignments` | write | Create a record |
| PUT | `/api/{resource}/{id}` | write | Update a record |
| DELETE | `/api/{resource}/{id}` | write | Delete a record |
| POST | `/api/assignments/{id}/submit` | any | Mark an assignment submitted |
| GET | `/api/assignments/{id}/submissions` | any | Students who submitted |
| GET | `/api/assignments/{id}/not-submitted` | any | Students who have not |

## Insights

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/dashboard` | Counts, attendance and workflow run statistics |

## Imports

Multipart uploads with a CSV in the `file` part. Teachers and admins only.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/import/{type}?preview=true` | Validate rows and report errors without saving (default) |
| POST | `/api/import/{type}?preview=false` | Save the valid rows |

`{type}` is `students` or `attendance`. An empty file is rejected with 400.

## Templates

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/templates` | Built-in workflow templates |
| POST | `/api/templates/{key}/use` | Create a workflow from a template |

## Workflows

| Method | Path | Description |
| --- | --- | --- |
| GET | `/api/workflows` | List workflows |
| GET | `/api/workflows/{id}` | One workflow |
| POST | `/api/workflows` | Create from `{name, trigger, conditions, steps}` |
| PUT | `/api/workflows/{id}` | Replace the definition |
| DELETE | `/api/workflows/{id}` | Delete (204) |
| POST | `/api/workflows/{id}/toggle` | Enable or disable |
| POST | `/api/workflows/{id}/clone` | Copy as "... (copy)" |
| POST | `/api/workflows/{id}/test` | Preview run: nothing is sent |
| POST | `/api/workflows/{id}/run` | Live run now |
| GET | `/api/workflows/runs` | Run history |
| GET | `/api/workflows/runs/{id}` | One run with its step results |

Validation rejects an invalid cron expression, a workflow with no steps and any
unknown action type.

## Health

`GET /actuator/health` is public and is the only actuator endpoint exposed.
