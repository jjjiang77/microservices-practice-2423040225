#!/usr/bin/env bash
set -euo pipefail
# 默认检查本地三业务服务。
check() {
  local label="$1" url="$2"
  if ! curl --fail --silent --show-error --location --connect-timeout 5 --max-time 15 "$url" > /dev/null; then
    printf 'FAIL: %s\n' "$label" >&2
    exit 1
  fi
  printf 'PASS: %s\n' "$label"
}
check 'Nacos console' "${NACOS_CONSOLE_URL:-http://127.0.0.1:18080/}"
for url in ${HEALTH_URLS:-http://127.0.0.1:8080/actuator/health http://127.0.0.1:8081/actuator/health http://127.0.0.1:8083/actuator/health}; do
  body=$(curl --fail --silent --show-error --connect-timeout 5 --max-time 15 "$url") || exit 1
  if ! printf '%s' "$body" | python3 -c 'import json,sys; sys.exit(0 if json.load(sys.stdin).get("status") == "UP" else 1)'; then
    printf 'FAIL: application health is not UP\n' >&2
    exit 1
  fi
  printf 'PASS: application health UP\n'
done
printf 'All configured checks passed. Business flow needs separate verification.\n'
