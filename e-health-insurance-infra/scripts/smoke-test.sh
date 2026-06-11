#!/usr/bin/env bash
#
# Final smoke test for the e-health-insurance stack.
#
#   ./scripts/smoke-test.sh
#
# Verifies (1) every service reports actuator readiness and (2) a real
# auth round-trip works through the gateway: register -> login -> GET /me.
#
# Env overrides:
#   GATEWAY_URL   gateway base URL              (default http://localhost:8080)
#   HOST          host where service ports live (default localhost)
#   TIMEOUT       per-check curl timeout sec    (default 5)
#   RETRIES       readiness retries per service (default 30, 2s apart)
#
# Exit code 0 = all checks passed, non-zero = at least one failed.

set -uo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
HOST="${HOST:-localhost}"
TIMEOUT="${TIMEOUT:-5}"
RETRIES="${RETRIES:-30}"

# service:port — the eight app services
SERVICES=(
  "gateway:8080"
  "iam:8081"
  "policy:8082"
  "claim:8083"
  "ai:8084"
  "notification:8085"
  "health-record:8086"
  "payment:8087"
)

green() { printf '\033[0;32m%s\033[0m\n' "$1"; }
red()   { printf '\033[0;31m%s\033[0m\n' "$1"; }
info()  { printf '\033[0;36m%s\033[0m\n' "$1"; }

FAILURES=0

# ── 1) Readiness of every service ────────────────────────────────────────────
info "==> Checking actuator readiness for ${#SERVICES[@]} services"
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  url="http://${HOST}:${port}/actuator/health/readiness"
  ok=0
  for ((i = 1; i <= RETRIES; i++)); do
    code=$(curl -fsS -o /dev/null -w '%{http_code}' --max-time "$TIMEOUT" "$url" 2>/dev/null || true)
    if [[ "$code" == "200" ]]; then ok=1; break; fi
    sleep 2
  done
  if [[ "$ok" == "1" ]]; then
    green "  ✓ ${name} ready (${url})"
  else
    red   "  ✗ ${name} NOT ready after $((RETRIES * 2))s (${url})"
    FAILURES=$((FAILURES + 1))
  fi
done

# ── 2) Auth round-trip through the gateway ───────────────────────────────────
info "==> Auth flow via gateway: ${GATEWAY_URL}"
EMAIL="smoke+$(date +%s)@example.com"
PASSWORD="SmokeTest123!"
REG_BODY=$(printf '{"email":"%s","password":"%s","firstName":"Smoke","lastName":"Test"}' "$EMAIL" "$PASSWORD")

reg=$(curl -fsS --max-time "$TIMEOUT" -X POST "${GATEWAY_URL}/api/iam/auth/register" \
        -H 'Content-Type: application/json' -d "$REG_BODY" 2>/dev/null || true)
access=$(printf '%s' "$reg" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

if [[ -z "$access" ]]; then
  # User may already exist on a re-run — fall back to login.
  login=$(curl -fsS --max-time "$TIMEOUT" -X POST "${GATEWAY_URL}/api/iam/auth/login" \
            -H 'Content-Type: application/json' \
            -d "$(printf '{"email":"%s","password":"%s"}' "$EMAIL" "$PASSWORD")" 2>/dev/null || true)
  access=$(printf '%s' "$login" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
fi

if [[ -n "$access" ]]; then
  green "  ✓ register/login returned an access token"
  me=$(curl -fsS --max-time "$TIMEOUT" "${GATEWAY_URL}/api/iam/users/me" \
         -H "Authorization: Bearer ${access}" 2>/dev/null || true)
  if printf '%s' "$me" | grep -q "$EMAIL"; then
    green "  ✓ GET /api/iam/users/me returned the authenticated user"
  else
    red   "  ✗ GET /api/iam/users/me did not return the expected user"
    FAILURES=$((FAILURES + 1))
  fi
else
  red "  ✗ could not obtain an access token (register & login both failed)"
  FAILURES=$((FAILURES + 1))
fi

echo
if [[ "$FAILURES" -eq 0 ]]; then
  green "SMOKE TEST PASSED"
  exit 0
else
  red "SMOKE TEST FAILED — ${FAILURES} check(s) failed"
  exit 1
fi
