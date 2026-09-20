#!/usr/bin/env bash
set -euo pipefail

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [image-tag]" >&2
  exit 64
fi

script_dir="$(cd "$(dirname "$0")" && pwd)"
image_tag="${1:-liteflow-agent-sandbox:node22}"

docker build --pull --tag "$image_tag" "$script_dir"
