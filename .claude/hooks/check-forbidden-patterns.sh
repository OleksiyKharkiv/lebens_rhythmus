#!/usr/bin/env bash
# PostToolUse hook — run after Write/Edit on backend/**/*.java
# Catches LR's known forbidden patterns (CODING_PROTOCOL.md) mechanically.
# Heuristic, not exhaustive — expect some false positives, tune as you go.
#
# What this deliberately does NOT try to catch (needs semantic judgement —
# that's what the architect-reviewer subagent is for, not a grep hook):
#   - business logic accidentally living in a @RestController method
#     (needs semantic read, not grep)
#   - missing @Transactional on a multi-step service write (needs call-graph
#     context)
#   - actual layer/import-direction violations at scale (consider ArchUnit
#     tests in CI instead — see note at bottom)
#
# For real secret-scanning, don't rely on the regex below as your only line
# of defense — run gitleaks or trufflehog in CI as well. This hook is a fast
# local tripwire, not a security boundary.
#
# Prerequisite: jq. Register in .claude/settings.json:
#   "hooks": { "PostToolUse": [ { "matcher": "Write|Edit|MultiEdit",
#     "hooks": [ { "type": "command",
#       "command": "bash .claude/hooks/check-forbidden-patterns.sh" } ] } ] }

set -uo pipefail

# Claude Code passes event data as JSON on stdin, not as a positional arg.
# tool_input.file_path is where Write/Edit/MultiEdit put the touched file.
INPUT=$(cat)
FILE=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

[ -z "$FILE" ] && exit 0         # no file path in this event — nothing to check
[[ "$FILE" != *.java ]] && exit 0 # only care about Java files

fail=0

# 1. Runtime schema mutation instead of a real migration — the exact
#    DatabaseFixConfig anti-pattern (DEBT-1, KNOWN_ISSUES.md). Don't let a
#    new instance of this slip in just because one already exists.
if grep -nE '\.execute\(\s*"?\s*ALTER\s+TABLE' "$FILE" | grep -iv 'test'; then
  echo "❌ $FILE: runtime ALTER TABLE — use a real migration instead (see DEBT-1, KNOWN_ISSUES.md)"
  fail=1
fi

# 2. float/double on a money-shaped field name (if/when a payment domain exists)
if grep -nE '\b(float|double)\b.*(Amount|Total|Price|Betrag|Preis|Summe|Kosten)' "$FILE"; then
  echo "❌ $FILE: float/double on a money field — use BigDecimal"
  fail=1
fi

# 3. Hardcoded secret-looking literal (crude — see note above re: gitleaks)
if grep -nE '(SECRET|PASSWORD|API_KEY|JWT_SECRET)\s*=\s*"[^"]{8,}"' "$FILE"; then
  echo "❌ $FILE: possible hardcoded secret — use env var / application secrets + fail-fast check"
  fail=1
fi

# 4. Raw string-concatenated SQL (possible SQL injection) — crude heuristic
if grep -nE '"(SELECT|INSERT|UPDATE|DELETE)[^"]*"\s*\+' "$FILE"; then
  echo "❌ $FILE: string-concatenated SQL — use @Query with parameters or Spring Data derived queries"
  fail=1
fi

# 5. println/printStackTrace instead of a real logger — easy path for
#    accidentally logging a child's personal data (Datenschutz check §4)
if grep -nE '(System\.out\.println|\.printStackTrace\(\))' "$FILE"; then
  echo "❌ $FILE: System.out/printStackTrace — use SLF4J logger, and mind Datenschutz §4 (don't log personal data at INFO/DEBUG)"
  fail=1
fi

exit $fail

# --- Layer-direction check (better as CI job / ArchUnit test, not a per-edit hook) ---
# Consider adding an ArchUnit test asserting:
#   - classes in ..entity.. must not depend on ..controller..
#   - classes in ..controller.. must not access ..repository.. directly
#     (must go through ..service..)
# This catches architecture drift at build time, not just at edit time.
