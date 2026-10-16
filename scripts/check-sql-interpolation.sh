#!/usr/bin/env bash
# NFR-09: fail the build when SQL text is built from runtime values.
# Concatenating compile-time constants (UPPER_CASE names) is allowed; values
# must go through bind parameters.
set -euo pipefail

root="${1:-api/src/main/java}"
sql='(SELECT|INSERT INTO|UPDATE [a-z_]+ SET|DELETE FROM|WHERE|VALUES|ORDER BY)'

concatenated=$(grep -rnE "\"[^\"]*${sql}[^\"]*\"[[:space:]]*\+[[:space:]]*[a-z_][A-Za-z0-9_.()]*|[a-z_][A-Za-z0-9_.()]*[[:space:]]*\+[[:space:]]*\"[^\"]*${sql}" \
    --include='*.java' "$root" || true)
formatted=$(grep -rnE "\"[^\"]*${sql}[^\"]*\"\.formatted\(|String\.format\([[:space:]]*\"[^\"]*${sql}" \
    --include='*.java' "$root" || true)

findings=$(printf '%s\n%s\n' "$concatenated" "$formatted" | sed '/^$/d')

if [ -n "$findings" ]; then
    echo "SQL built from runtime values. Use bind parameters instead:"
    echo "$findings"
    exit 1
fi

echo "No string-built SQL found in $root"
