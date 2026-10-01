#!/bin/bash
# =============================================================================
# BrowserStack SDK Pipeline Script
# Replicates the Azure DevOps pipeline locally / on CI:
#   1. Fetch 5 matched test cases from Test Management by TC ID
#   2. Replace TC IDs in feature files
#   3. Update credentials + projectName in browserstack.yml
#   4. Trigger Maven build (BrowserStack SDK)
#   5. Print TRA / Test Management report links
# =============================================================================

set -e

# ── Load credentials from .env or Environment Variables ───────────────────────
SCRIPT_DIR_EARLY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR_EARLY/.env" ]; then
  set -a
  source "$SCRIPT_DIR_EARLY/.env"
  set +a
  echo "  Loaded credentials from .env"
fi

# Automatically fallback to BROWSERSTACK_* variables exported by the BrowserStack task
BS_USERNAME="${BS_USERNAME:-$BROWSERSTACK_USERNAME}"
BS_ACCESS_KEY="${BS_ACCESS_KEY:-$BROWSERSTACK_ACCESS_KEY}"

# ── DIAGNOSTICS: Print all expected env vars (mask secrets) ───────────────────
echo ""
echo "============================================================"
echo " 🔍 ENVIRONMENT DIAGNOSTICS"
echo "============================================================"
echo ""
echo "── Credentials ──────────────────────────────────────────────"
if [ -n "$BS_USERNAME" ]; then
  echo "  BS_USERNAME              = $BS_USERNAME  ✅"
else
  echo "  BS_USERNAME              = (NOT SET)  ❌"
fi
if [ -n "$BS_ACCESS_KEY" ]; then
  echo "  BS_ACCESS_KEY            = ${BS_ACCESS_KEY:0:4}****${BS_ACCESS_KEY: -4}  ✅"
else
  echo "  BS_ACCESS_KEY            = (NOT SET)  ❌"
fi
if [ -n "$BROWSERSTACK_USERNAME" ]; then
  echo "  BROWSERSTACK_USERNAME    = $BROWSERSTACK_USERNAME  ✅ (fallback source)"
else
  echo "  BROWSERSTACK_USERNAME    = (NOT SET)"
fi
if [ -n "$BROWSERSTACK_ACCESS_KEY" ]; then
  echo "  BROWSERSTACK_ACCESS_KEY  = ${BROWSERSTACK_ACCESS_KEY:0:4}****${BROWSERSTACK_ACCESS_KEY: -4}  ✅ (fallback source)"
else
  echo "  BROWSERSTACK_ACCESS_KEY  = (NOT SET)"
fi
echo ""
echo "── Test Management ──────────────────────────────────────────"
if [ -n "$TM_PROJECT_IDENTIFIER" ]; then
  echo "  TM_PROJECT_IDENTIFIER    = $TM_PROJECT_IDENTIFIER  ✅"
else
  echo "  TM_PROJECT_IDENTIFIER    = (NOT SET)  ⚠️  (preferred: PR-XX format)"
fi
if [ -n "$TM_PROJECT_ID" ]; then
  echo "  TM_PROJECT_ID            = $TM_PROJECT_ID  ✅"
else
  echo "  TM_PROJECT_ID            = (NOT SET)"
fi
if [ -n "$TM_PROJECT_NAME" ]; then
  echo "  TM_PROJECT_NAME          = $TM_PROJECT_NAME  ✅"
else
  echo "  TM_PROJECT_NAME          = (NOT SET)  ⚠️  (used in browserstack.yml projectName)"
fi
echo ""
echo "── Project / Build ──────────────────────────────────────────"
echo "  JAVA_HOME                = ${JAVA_HOME:-(NOT SET)}"
echo "  MAVEN_HOME               = ${MAVEN_HOME:-(NOT SET)}"
echo "  PATH (java)              = $(command -v java 2>/dev/null || echo 'java not found ❌')"
echo "  PATH (mvn)               = $(command -v mvn 2>/dev/null || echo 'mvn not found ❌')"
echo "  PATH (python3)           = $(command -v python3 2>/dev/null || echo 'python3 not found ❌')"
echo "  PATH (curl)              = $(command -v curl 2>/dev/null || echo 'curl not found ❌')"
echo ""
echo "── .env file ────────────────────────────────────────────────"
if [ -f "$SCRIPT_DIR_EARLY/.env" ]; then
  echo "  .env file                = FOUND at $SCRIPT_DIR_EARLY/.env  ✅"
else
  echo "  .env file                = NOT FOUND (relying on pipeline env vars)"
fi
echo ""
echo "── API Connectivity Check ───────────────────────────────────"
if [ -n "$BS_USERNAME" ] && [ -n "$BS_ACCESS_KEY" ]; then
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$BS_USERNAME:$BS_ACCESS_KEY" \
    "https://api.browserstack.com/automate/plan.json")
  if [ "$HTTP_STATUS" = "200" ]; then
    echo "  BrowserStack Automate API = HTTP $HTTP_STATUS  ✅"
  else
    echo "  BrowserStack Automate API = HTTP $HTTP_STATUS  ❌ (check credentials / network)"
  fi
  TM_HTTP=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$BS_USERNAME:$BS_ACCESS_KEY" \
    "https://test-management.browserstack.com/api/v2/projects")
  if [ "$TM_HTTP" = "200" ]; then
    echo "  Test Management API       = HTTP $TM_HTTP  ✅"
  else
    echo "  Test Management API       = HTTP $TM_HTTP  ❌ (check credentials / network)"
  fi
else
  echo "  API checks SKIPPED — credentials not set"
fi
echo "============================================================"
echo ""

if [ -z "$BS_USERNAME" ] || [ -z "$BS_ACCESS_KEY" ]; then
  echo "ERROR: Missing BrowserStack credentials. Provide a .env file or ensure BS_USERNAME/BS_ACCESS_KEY (or BROWSERSTACK_USERNAME/BROWSERSTACK_ACCESS_KEY) environment variables are set."
  exit 1
else
  echo "  BrowserStack credentials initialized successfully for: $BS_USERNAME"
fi

# ── Paths ─────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FEATURES_DIR="$SCRIPT_DIR/src/test/resources/Features"
BS_YML="$SCRIPT_DIR/browserstack.yml"

echo "============================================================"
echo " BrowserStack SDK Pipeline — Run"
echo "============================================================"

# ── STEP 1: Fetch 5 random manual (not_automated) test cases from TM ─────────
echo ""

# Use TM_PROJECT_IDENTIFIER (PR-XX format) if set, otherwise use TM_PROJECT_ID
# In Azure DevOps, set TM_PROJECT_IDENTIFIER=PR-23 as a pipeline variable
TM_API_PROJECT="${TM_PROJECT_IDENTIFIER:-$TM_PROJECT_ID}"
if echo "$TM_API_PROJECT" | grep -qE '^[0-9]+$'; then
  echo "  WARNING: TM_API_PROJECT '$TM_API_PROJECT' looks like a numeric ID."
  echo "  The TM API requires PR-XX format. Set TM_PROJECT_IDENTIFIER=PR-23 in your pipeline variables."
  echo "  Attempting to use as-is (may fail)..."
fi

echo "[1/4] Fetching 5 random manual test cases from Test Management (project: $TM_API_PROJECT)..."

TM_RESPONSE=$(curl -s \
  -u "$BS_USERNAME:$BS_ACCESS_KEY" \
  "https://test-management.browserstack.com/api/v2/projects/$TM_API_PROJECT/test-cases?automation_status=not_automated&per_page=100")

# Pick 5 random IDs from the returned list
TC_IDS=$(echo "$TM_RESPONSE" | python3 -c "
import sys, json, random
try:
    data = json.load(sys.stdin)
    if isinstance(data, dict) and 'message' in data:
        print(f'API Error Response: {data.get(\"message\")}', file=sys.stderr)
        sys.exit(1)
    tcs = data.get('test_cases', []) if isinstance(data, dict) else []
    if not tcs:
        print('ERROR: No test cases returned from Test Management API', file=sys.stderr)
        sys.exit(1)
    sample = random.sample(tcs, min(5, len(tcs)))
    ids = [tc['identifier'] for tc in sample]
    print(' '.join(ids))
except Exception as e:
    print(f'Error processing Test Management response: {e}', file=sys.stderr)
    sys.exit(1)
")

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to fetch test cases from Test Management."
  exit 1
fi

echo "  Fetched test case IDs: $TC_IDS"

read -ra TC_ARRAY <<< "$TC_IDS"
TC1="${TC_ARRAY[0]}"
TC2="${TC_ARRAY[1]}"
TC3="${TC_ARRAY[2]}"
TC4="${TC_ARRAY[3]}"
TC5="${TC_ARRAY[4]}"

echo "  TC1=$TC1  TC2=$TC2  TC3=$TC3  TC4=$TC4  TC5=$TC5"

# ── STEP 2: Replace TC IDs in feature files ───────────────────────────────────
echo ""
echo "[2/4] Replacing test case IDs in feature files..."

# Backup originals (only if backup doesn't already exist)
[ -f "$FEATURES_DIR/E2E.feature.bak" ]    || cp "$FEATURES_DIR/E2E.feature"    "$FEATURES_DIR/E2E.feature.bak"
[ -f "$FEATURES_DIR/Users.feature.bak" ]  || cp "$FEATURES_DIR/Users.feature"  "$FEATURES_DIR/Users.feature.bak"
[ -f "$FEATURES_DIR/Offers.feature.bak" ] || cp "$FEATURES_DIR/Offers.feature" "$FEATURES_DIR/Offers.feature.bak"

# E2E.feature → replace existing TC id or inject TC1
if grep -q "TC-[0-9]*" "$FEATURES_DIR/E2E.feature"; then
  EXISTING=$(grep -o "TC-[0-9]*" "$FEATURES_DIR/E2E.feature" | head -1)
  sed -i.tmp "s/$EXISTING/$TC1/g" "$FEATURES_DIR/E2E.feature"
else
  sed -i.tmp "s/Scenario Outline: /Scenario Outline: $TC1 /g" "$FEATURES_DIR/E2E.feature"
fi
rm -f "$FEATURES_DIR/E2E.feature.tmp"

# Users.feature → replace up to 3 existing TC ids with TC2, TC3, TC4
python3 - <<PYEOF
import re

with open('$FEATURES_DIR/Users.feature', 'r') as f:
    content = f.read()

existing = re.findall(r'TC-\d+', content)
new_ids = ['$TC2', '$TC3', '$TC4']

for i, old_id in enumerate(existing[:3]):
    content = content.replace(old_id, new_ids[i], 1)

# If no existing TC ids, inject before each Scenario Outline
if not existing:
    count = [0]
    def replacer(m):
        idx = count[0]
        count[0] += 1
        return 'Scenario Outline: ' + new_ids[idx] + ' ' if idx < len(new_ids) else m.group(0)
    content = re.sub(r'Scenario Outline: ', replacer, content)

with open('$FEATURES_DIR/Users.feature', 'w') as f:
    f.write(content)
print('  Users.feature updated')
PYEOF

# Offers.feature → replace existing TC id or inject TC5
if grep -q "TC-[0-9]*" "$FEATURES_DIR/Offers.feature"; then
  EXISTING=$(grep -o "TC-[0-9]*" "$FEATURES_DIR/Offers.feature" | head -1)
  sed -i.tmp "s/$EXISTING/$TC5/g" "$FEATURES_DIR/Offers.feature"
else
  sed -i.tmp "s/Scenario Outline: /Scenario Outline: $TC5 /g" "$FEATURES_DIR/Offers.feature"
fi
rm -f "$FEATURES_DIR/Offers.feature.tmp"

echo "  E2E.feature    → $TC1"
echo "  Users.feature  → $TC2, $TC3, $TC4"
echo "  Offers.feature → $TC5"

# ── STEP 3: Update browserstack.yml ──────────────────────────────────────────
echo ""
echo "[3/4] Updating browserstack.yml..."

cp "$BS_YML" "$BS_YML.bak"

python3 - <<PYEOF
import re

with open('$BS_YML', 'r') as f:
    content = f.read()

content = re.sub(r'^userName:.*', 'userName: $BS_USERNAME', content, flags=re.MULTILINE)
content = re.sub(r'^accessKey:.*', 'accessKey: $BS_ACCESS_KEY', content, flags=re.MULTILINE)
content = re.sub(r'^projectName:.*', 'projectName: $TM_PROJECT_NAME', content, flags=re.MULTILINE)
content = re.sub(r'^browserstackAutomation:.*', 'browserstackAutomation: false', content, flags=re.MULTILINE)
# Clear platforms so no Automate sessions are attempted (local run only)
content = re.sub(r'^platforms:.*?(?=^\w)', 'platforms: []\n', content, flags=re.MULTILINE | re.DOTALL)

with open('$BS_YML', 'w') as f:
    f.write(content)

print('  browserstack.yml updated successfully')
PYEOF

echo "  userName    → $BS_USERNAME"
echo "  accessKey   → (set)"
echo "  projectName → $TM_PROJECT_NAME"

# ── STEP 4: Trigger Maven build ───────────────────────────────────────────────
echo ""
echo "[4/4] Triggering Maven build on BrowserStack..."
echo "  Command: mvn test -P scenario-onprem"
echo ""

# Force Jira account credentials — unset any shell env vars that would override browserstack.yml
unset BROWSERSTACK_USERNAME
unset BROWSERSTACK_ACCESS_KEY
export BROWSERSTACK_USERNAME="$BS_USERNAME"
export BROWSERSTACK_ACCESS_KEY="$BS_ACCESS_KEY"

export MAVEN_OPTS="--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED \
  --add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED \
  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
  --add-opens=java.base/java.lang=ALL-UNNAMED"

mvn test -P scenario-onprem \
  -Dbrowser-type=chrome \
  -DBROWSERSTACK_USERNAME="$BS_USERNAME" \
  -DBROWSERSTACK_ACCESS_KEY="$BS_ACCESS_KEY"

BUILD_EXIT=$?

# ── Fetch latest TRA build ID ─────────────────────────────────────────────────
echo ""
echo "Fetching latest TRA build ID..."

BUILD_INFO=$(curl -s \
  -u "$BS_USERNAME:$BS_ACCESS_KEY" \
  "https://api.browserstack.com/automate/builds.json?limit=1")

BUILD_ID=$(echo "$BUILD_INFO" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if isinstance(data, list) and data:
        b = data[0].get('automation_build', {})
        print(b.get('hashed_id', 'N/A'))
    else:
        print('N/A')
except:
    print('N/A')
" 2>/dev/null)

BUILD_NAME=$(echo "$BUILD_INFO" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if isinstance(data, list) and data:
        b = data[0].get('automation_build', {})
        print(b.get('name', 'N/A'))
    else:
        print('N/A')
except:
    print('N/A')
" 2>/dev/null)

# ── Report links ──────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
if [ $BUILD_EXIT -eq 0 ]; then
  echo " ✅ Build completed successfully!"
else
  echo " ⚠️  Build finished with exit code $BUILD_EXIT"
fi
echo ""
echo " 🆔 TRA Build ID:   $BUILD_ID"
echo " 📛 TRA Build Name: $BUILD_NAME"
echo ""
echo " 📊 TRA / Observability Report:"
echo "    https://observability.browserstack.com/builds/$BUILD_ID"
echo ""
echo " 📋 Test Management (BStack Jira - JIRA):"
echo "    https://test-management.browserstack.com/projects/3635057"
echo "============================================================"

exit $BUILD_EXIT