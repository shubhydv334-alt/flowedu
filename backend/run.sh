#!/usr/bin/env bash
# Starts the backend for local development, loading ../.env first.
set -eu
cd "$(dirname "$0")"

ENV_FILE="../.env"
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    case "$line" in ''|'#'*) continue ;; esac
    case "$line" in *=*) ;; *) continue ;; esac
    key="${line%%=*}"; val="${line#*=}"
    key="$(echo "$key" | tr -d '[:space:]')"
    [ -z "$key" ] && continue
    case "$val" in
      \"*\") val="${val#\"}"; val="${val%\"}" ;;
      \'*\') val="${val#\'}"; val="${val%\'}" ;;
    esac
    export "$key=$val"
  done < "$ENV_FILE"
fi

# Without SMTP credentials the email action runs in SIMULATED mode.
if [ -z "${MAIL_USER:-}" ]; then unset MAIL_HOST; fi

exec mvn spring-boot:run
