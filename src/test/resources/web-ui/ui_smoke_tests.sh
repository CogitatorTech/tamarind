#!/usr/bin/env bash
set -euo pipefail

# UI smoke tests (moved from tests/web-ui/ui_smoke_tests.sh)
ROOT_DIR="$(cd "$(dirname "$0")/../../.." && pwd)"
RES_DIR="$ROOT_DIR/src/main/resources/META-INF/resources"

fail() { echo "[FAIL] $1"; exit 1; }
ok() { echo "[OK] $1"; }

NOTEBOOK="$RES_DIR/notebook.html"
[ -f "$NOTEBOOK" ] || fail "notebook.html not found"

grep -q "Dashboard</button>" "$NOTEBOOK" && ok "Dashboard button present in notebook" || fail "Dashboard button missing in notebook"

grep -q "/api/v1/datasources/tables" "$NOTEBOOK" && ok "Notebook points to /api/v1/datasources/tables" || fail "Notebook tables endpoint incorrect"

(grep -q "tamarind_query_template" "$NOTEBOOK" && grep -q "localStorage.removeItem('tamarind_query_template')" "$NOTEBOOK") \
  && ok "Notebook applies and clears query template" \
  || fail "Notebook missing query template handling"

(grep -q "content-type" "$NOTEBOOK" && grep -q "Response parse error" "$NOTEBOOK" && grep -q "Unexpected response" "$NOTEBOOK") \
  && ok "Notebook robust result parsing present" \
  || fail "Notebook robust parsing not detected"

DASHBOARD="$RES_DIR/dashboard.html"
[ -f "$DASHBOARD" ] || fail "dashboard.html not found"

grep -q "showPage('overview', this)" "$DASHBOARD" && ok "Dashboard nav uses explicit element param" || fail "Dashboard nav still uses implicit event"

grep -q "onclick=\"logout()\"" "$DASHBOARD" && ok "Dashboard logout handler fixed" || fail "Dashboard logout handler broken"

(grep -q "badges.push('cached')" "$NOTEBOOK" && grep -q "badges.push('truncated')" "$NOTEBOOK") \
  && ok "Notebook displays metadata badges" \
  || fail "Notebook missing metadata badges"

exit 0

