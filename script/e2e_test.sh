#!/bin/sh

set -eu

# End-to-end API test script for Auth Service
# - Covers: signup, duplicate signup, login (ok/fail), /me, renew, logout, revoked token check,
#           forgot-password, fetch reset token from DB, reset-password, login with new password
# - Requires: curl; optional: jq or python3; optional: docker (for DB token fetch) or local psql

BASE_URL="${BASE_URL:-http://localhost:8080}"
COMPOSE_FILE="script/docker-compose.yml"
POSTGRES_CONTAINER="auth-postgres"
DB_NAME="auth_db"
DB_USER="postgres"

PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

bold() { printf "\033[1m%s\033[0m\n" "$*"; }
green() { printf "\033[32m%s\033[0m\n" "$*"; }
red() { printf "\033[31m%s\033[0m\n" "$*"; }
yellow() { printf "\033[33m%s\033[0m\n" "$*"; }

die() { red "ERROR: $*"; exit 1; }

section() {
  echo ""
  bold "== $* =="
}

pass() { green "PASS: $*"; PASS_COUNT=$((PASS_COUNT+1)); }
fail() { red "FAIL: $*"; FAIL_COUNT=$((FAIL_COUNT+1)); }
warn() { yellow "WARN: $*"; WARN_COUNT=$((WARN_COUNT+1)); }

# If STRICT=1, turn warnings into failures
strict_warn() {
  if [ "${STRICT-}" = "1" ]; then
    fail "$*"
  else
    warn "$*"
  fi
}

status_in() {
  # status_in "200 400 401" "$HTTP_STATUS"
  expected="$1"; actual="$2"
  for s in $expected; do
    [ "$s" = "$actual" ] && return 0
  done
  return 1
}

have_cmd() { command -v "$1" >/dev/null 2>&1; }

json_get() {
  # json_get '<json string>' 'path.to.field'
  json="$1"; shift
  path="$1"
  if have_cmd jq; then
    echo "$json" | jq -r ".${path}" 2>/dev/null
  elif have_cmd python3; then
    python3 - "$path" <<'PY' 2>/dev/null || true
import sys, json
path = sys.argv[1]
obj = json.load(sys.stdin)
cur = obj
try:
    for part in path.split('.'):
        cur = cur[part]
    if cur is None:
        print('')
    else:
        print(cur)
except Exception:
    pass
PY
  else
    # naive fallback: extract last path segment and grep using BSD sed character classes
    key=$(echo "$path" | awk -F. '{print $NF}')
    echo "$json" | sed -E -n 's/.*"'"$key"'"[[:space:]]*:[[:space:]]*"([^"]*)".*/\1/p' | head -n1
  fi
}

request() {
  # request METHOD PATH BODY_JSON TOKEN
  method="$1"
  path="$2"
  body="${3-}"
  token="${4-}"

  url="${BASE_URL}${path}"

  if [ -n "${body}" ] && [ -n "${token}" ]; then
    response=$(curl -sS -X "$method" "$url" -H "Authorization: Bearer ${token}" -H "Content-Type: application/json" --data "$body" -w "\n%{http_code}") || true
  elif [ -n "${body}" ]; then
    response=$(curl -sS -X "$method" "$url" -H "Content-Type: application/json" --data "$body" -w "\n%{http_code}") || true
  elif [ -n "${token}" ]; then
    response=$(curl -sS -X "$method" "$url" -H "Authorization: Bearer ${token}" -w "\n%{http_code}") || true
  else
    response=$(curl -sS -X "$method" "$url" -w "\n%{http_code}") || true
  fi

  HTTP_STATUS=$(printf "%s" "$response" | tail -n1)
  HTTP_BODY=$(printf "%s" "$response" | sed '$d')
}

wait_for_service() {
  section "Waiting for service at ${BASE_URL}"
  retries=60
  ok=0
  while [ "$retries" -gt 0 ]; do
    # Prefer an authenticated endpoint that returns 401 when up
    if curl -sS -o /dev/null -w "%{http_code}" -X GET "${BASE_URL}/api/auth/me" | grep -E "^(200|401|403)$" >/dev/null; then
      ok=1; break
    fi
    # Fallback: POST /login with a minimal JSON body
    if curl -sS -o /dev/null -w "%{http_code}" -X POST -H "Content-Type: application/json" --data '{"username":"x","password":"y"}' "${BASE_URL}/api/auth/login" | grep -E "^(200|400|401|405|415|500)$" >/dev/null; then
      ok=1; break
    fi
    sleep 2
    retries=$((retries-1))
  done
  if [ "$ok" -eq 1 ]; then pass "Service is responding"; else fail "Service did not respond in time"; fi
}

compose_up_if_requested() {
  if [ "${COMPOSE_UP-}" = "1" ]; then
    section "Starting docker-compose"
    have_cmd docker-compose || have_cmd docker || die "Docker is required for compose-up"
    (cd script && docker compose up -d) || (cd script && docker-compose up -d)
  fi
}

get_db_password() {
  pw="your_password_here"
  if [ -f "$COMPOSE_FILE" ]; then
    line=$(grep -E "POSTGRES_PASSWORD\s*:" -n "$COMPOSE_FILE" || true)
    if [ -n "${line}" ]; then
      pw=$(echo "$line" | sed -E 's/.*POSTGRES_PASSWORD\s*:\s*"?([^"[:space:]]+)"?.*/\1/')
    fi
  fi
  if [ -f "src/main/resources/application.properties" ]; then
    prop=$(grep -E "^spring.datasource.password=" -n src/main/resources/application.properties || true)
    if [ -n "${prop}" ]; then
      pw=$(echo "$prop" | sed -E 's/.*spring.datasource.password=//')
    fi
  fi
  echo "$pw"
}

fetch_reset_token_from_db() {
  email="$1"
  token=""
  sql="SELECT prt.token FROM password_reset_tokens prt JOIN users u ON prt.user_id = u.id WHERE u.email = '${email}' ORDER BY prt.created_at DESC LIMIT 1;"

  if have_cmd docker && docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
    token=$(docker exec -i "$POSTGRES_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "$sql" 2>/dev/null | tr -d '\r' | head -n1)
  elif have_cmd psql; then
    pw=$(get_db_password)
    token=$(PGPASSWORD="$pw" psql -h 127.0.0.1 -U "$DB_USER" -d "$DB_NAME" -t -A -c "$sql" 2>/dev/null | tr -d '\r' | head -n1)
  else
    warn "Neither docker (with ${POSTGRES_CONTAINER}) nor local psql found; cannot fetch reset token"
  fi

  echo "$token"
}

main() {
  if [ "${1-}" = "--compose-up" ]; then COMPOSE_UP=1; fi
  compose_up_if_requested
  wait_for_service

  section "Prepare unique test data"
  suffix="$(date +%s)$RANDOM"
  NAME="Test User ${suffix}"
  EMAIL="test_${suffix}@example.com"
  USERNAME="test_${suffix}"
  PASSWORD="Secret123!"
  PASSWORD_NEW="Secret456!"
  pass "Using email=${EMAIL}, username=${USERNAME}"

  section "Signup (201)"
  request POST "/api/auth/signup" "{\"name\":\"${NAME}\",\"email\":\"${EMAIL}\",\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}"
  if [ "$HTTP_STATUS" = "201" ]; then pass "Signup created user"; else fail "Signup expected 201, got ${HTTP_STATUS}: ${HTTP_BODY}"; fi

  section "Signup invalid payloads (400)"
  request POST "/api/auth/signup" "{\"email\":\"x@example.com\",\"username\":\"${USERNAME}_x\",\"password\":\"${PASSWORD}\"}"
  if [ "$HTTP_STATUS" = "400" ]; then pass "Signup missing name rejected"; else warn "Signup missing name got ${HTTP_STATUS}"; fi
  request POST "/api/auth/signup" "{\"name\":\"A\",\"email\":\"${USERNAME}_a@example.com\",\"username\":\"${USERNAME}_a\",\"password\":\"123\"}"
  if [ "$HTTP_STATUS" = "400" ]; then pass "Signup weak password rejected"; else warn "Signup weak password got ${HTTP_STATUS}"; fi
  request POST "/api/auth/signup" "{\"name\":\"Admin\",\"email\":\"${USERNAME}_admin@example.com\",\"username\":\"${USERNAME}_admin\",\"password\":\"${PASSWORD}\",\"role\":\"ADMIN\"}"
  if [ "$HTTP_STATUS" = "201" ]; then warn "Role escalation possible (client set role)"; else pass "Role escalation prevented (${HTTP_STATUS})"; fi

  section "Duplicate Signup (400)"
  request POST "/api/auth/signup" "{\"name\":\"${NAME}\",\"email\":\"${EMAIL}\",\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}"
  if [ "$HTTP_STATUS" = "400" ]; then pass "Duplicate signup rejected"; else fail "Duplicate signup expected 400, got ${HTTP_STATUS}"; fi

  section "Login success (200)"
  request POST "/api/auth/login" "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}"
  if [ "$HTTP_STATUS" != "200" ]; then fail "Login expected 200, got ${HTTP_STATUS}: ${HTTP_BODY}"; else pass "Login ok"; fi
  ACCESS_TOKEN=
  ACCESS_TOKEN=$(json_get "$HTTP_BODY" "data.accessToken")
  if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then fail "Missing accessToken in login response"; else pass "Got accessToken"; fi

  section "/me (200)"
  request GET "/api/auth/me" "" "$ACCESS_TOKEN"
  if [ "$HTTP_STATUS" = "200" ]; then pass "/me returned profile"; else fail "/me expected 200, got ${HTTP_STATUS}"; fi

  section "Login failure (401)"
  request POST "/api/auth/login" "{\"username\":\"${USERNAME}\",\"password\":\"wrong\"}"
  if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "400" ]; then pass "Invalid credentials rejected"; else fail "Expected 401/400 for wrong password, got ${HTTP_STATUS}"; fi

  section "Login invalid requests (401)"
  request POST "/api/auth/login" "{\"password\":\"${PASSWORD}\"}"
  if [ "$HTTP_STATUS" = "401" ]; then pass "Login missing username/email rejected"; else warn "Login missing username/email got ${HTTP_STATUS}"; fi

  section "Token renew (200)"
  request POST "/api/auth/token/renew" "" "$ACCESS_TOKEN"
  if [ "$HTTP_STATUS" = "200" ]; then pass "Token renewed"; else fail "Renew expected 200, got ${HTTP_STATUS}"; fi
  ACCESS_TOKEN_NEW=
  ACCESS_TOKEN_NEW=$(json_get "$HTTP_BODY" "data.accessToken")
  if [ -z "$ACCESS_TOKEN_NEW" ] || [ "$ACCESS_TOKEN_NEW" = "null" ]; then fail "Missing accessToken in renew response"; else pass "Got renewed token"; fi
  if [ "$ACCESS_TOKEN_NEW" = "$ACCESS_TOKEN" ]; then warn "Renewed token equals old token (policy?)"; fi

  section "Logout (200)"
  request POST "/api/auth/logout" "" "$ACCESS_TOKEN_NEW"
  if [ "$HTTP_STATUS" = "200" ]; then pass "Logout ok"; else fail "Logout expected 200, got ${HTTP_STATUS}"; fi

  section "/me with revoked token (should be rejected)"
  request GET "/api/auth/me" "" "$ACCESS_TOKEN_NEW"
  if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "403" ]; then pass "Revoked token rejected by /me (${HTTP_STATUS})"; else fail "Expected 401/403 for revoked token, got ${HTTP_STATUS}"; fi

  section "Protected endpoint without token (should be rejected)"
  request GET "/api/auth/me"
  if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "403" ]; then pass "Missing token rejected"; else fail "Expected 401/403 without token, got ${HTTP_STATUS}"; fi

  section "Forgot password and fetch reset token"
  request POST "/api/auth/forgot-password" "{\"email\":\"${EMAIL}\"}"
  FORGOT_STATUS="$HTTP_STATUS"
  RESET_TOKEN="$(fetch_reset_token_from_db "$EMAIL")"
  if [ -n "$RESET_TOKEN" ] && [ "$FORGOT_STATUS" != "200" ]; then
    warn "Forgot-password returned ${FORGOT_STATUS} but reset token exists (email sending likely failed)"
  fi
  if [ -z "$RESET_TOKEN" ] && [ "$FORGOT_STATUS" != "200" ]; then
    fail "Forgot-password expected 200 and to create a token; got ${FORGOT_STATUS} and no token"
  elif [ -z "$RESET_TOKEN" ] && [ "$FORGOT_STATUS" = "200" ]; then
    fail "Forgot-password returned 200 but no reset token found"
  else
    pass "Reset token available"
    section "Reset password (200)"
    request POST "/api/auth/reset-password" "{\"token\":\"${RESET_TOKEN}\",\"newPassword\":\"${PASSWORD_NEW}\"}"
    if [ "$HTTP_STATUS" = "200" ]; then pass "Password reset ok"; else fail "Reset-password expected 200, got ${HTTP_STATUS}: ${HTTP_BODY}"; fi

    section "Reset password reuse (should fail)"
    request POST "/api/auth/reset-password" "{\"token\":\"${RESET_TOKEN}\",\"newPassword\":\"${PASSWORD_NEW}\"}"
    if status_in "400 401 409" "$HTTP_STATUS"; then pass "Used reset token rejected (${HTTP_STATUS})"; else warn "Used reset token unexpected status ${HTTP_STATUS}"; fi

    section "Login old password (should fail)"
    request POST "/api/auth/login" "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}"
    if [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "400" ]; then pass "Old password rejected"; else fail "Expected 401/400 for old password, got ${HTTP_STATUS}"; fi

    section "Login new password (200)"
    request POST "/api/auth/login" "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD_NEW}\"}"
    if [ "$HTTP_STATUS" = "200" ]; then pass "New password works"; else fail "New password login expected 200, got ${HTTP_STATUS}"; fi

    section "Renew without token (should fail)"
    request POST "/api/auth/token/renew"
    if status_in "400 401 403" "$HTTP_STATUS"; then pass "Renew without token rejected (${HTTP_STATUS})"; else warn "Renew without token unexpected ${HTTP_STATUS}"; fi
  fi

  echo ""
  bold "==== TEST SUMMARY ===="
  echo "Passed: ${PASS_COUNT}"
  echo "Failed: ${FAIL_COUNT}"
  echo "Warnings: ${WARN_COUNT}"
  echo "======================="
  if [ "$FAIL_COUNT" -ne 0 ]; then exit 1; fi
}

main "$@"


