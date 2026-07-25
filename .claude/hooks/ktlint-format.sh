#!/usr/bin/env bash
set -euo pipefail

file_path=$(python3 -c "import json,sys; print(json.load(sys.stdin).get('tool_input', {}).get('file_path', ''))")

if [[ -z "$file_path" || "$file_path" != *.kt ]]; then
  exit 0
fi

relative_path="${file_path#"$CLAUDE_PROJECT_DIR"/}"
module="${relative_path%%/*}"

case "$module" in
  domain|application|infrastructure|presentation) ;;
  *) exit 0 ;;
esac

cd "$CLAUDE_PROJECT_DIR"
./gradlew ":${module}:ktlintFormat" -q
