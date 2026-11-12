#!/usr/bin/env bash
cd "$(dirname "$0")" || exit 1
if [ -f ../.env ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    case "$line" in ''|'#'*) continue ;; esac
    key="${line%%=*}"; val="${line#*=}"
    key="$(echo "$key" | tr -d '[:space:]')"
    [ -z "$key" ] && continue
    case "$val" in
      \"*\") val="${val#\"}"; val="${val%\"}" ;;
      \'*\') val="${val#\'}"; val="${val%\'}" ;;
    esac
    export "$key=$val"
  done < ../.env
fi
if [ -z "$MAIL_USER" ]; then unset MAIL_HOST; fi
mvn spring-boot:run
