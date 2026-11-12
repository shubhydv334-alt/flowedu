# Troubleshooting

## The backend refuses to start

* **"JWT secret must be at least 32 bytes"**: set `JWT_SECRET` to a longer random value, for example `openssl rand -base64 48`.
* **Placeholder secret rejected under the `prod` profile**: replace the example value from `.env.example` with a real secret.
* **Missing `DB_URL`, `DB_USER`, `DB_PASS` or `CORS_ORIGIN`**: the `prod` profile has no defaults for these.
* **Admin bootstrap fails**: `ADMIN_PASSWORD` must be at least 12 characters.

## Login problems

* **429 Too Many Requests**: five failed attempts lock that email and address for 15 minutes. Wait, or restart the backend to clear the in-memory counter.
* **No demo accounts**: demo data is seeded only when `SEED_DEMO_DATA` is true, which is the default outside `prod`.
* **Demo credentials missing from the login page**: they are shown only in development, or when the frontend is built with `VITE_SHOW_DEMO_LOGIN=true`.

## Browser errors

* **"Cannot reach the server"**: the API is down or the URL is wrong. Check that the backend is running and that nginx can reach the `backend` service.
* **CORS errors**: set `CORS_ORIGIN` to the exact origin the browser uses, including the scheme and port.

## Workflows

* **Webhook blocked**: URLs resolving to private, loopback or link-local addresses are rejected. Use a public URL, or set `ALLOW_PRIVATE_WEBHOOKS=true` on a trusted network.
* **Schedule never fires**: the workflow must be enabled, and the cron expression must have six fields.
* **Messages are not sent in a test run**: preview runs never send anything by design. Use a live run.
* **Email fails**: check the SMTP settings in the README configuration table.

## Docker

* **Compose asks for variables**: `DB_PASS` and `JWT_SECRET` are required. Copy `.env.example` to `.env` and fill them in.
* **Port already in use**: change the published port in `docker-compose.yml`.
