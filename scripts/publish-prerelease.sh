#!/usr/bin/env bash
set -euo pipefail

# Manual publishing fallback if GitHub Actions runners are unavailable.
# Run from a clone of the private Sibyl.ad repo with a GitHub-authenticated gh CLI.
cd "$(dirname "${BASH_SOURCE[0]}")/.."
version="v0.1.0-rc.1"
asset="dist/Sibyl.ad-$version.zip"
checksum="$asset.sha256"

command -v gh >/dev/null || { echo "Install GitHub CLI first"; exit 1; }
gh auth status >/dev/null || { echo "Authenticate GitHub CLI interactively, e.g. gh auth login"; exit 1; }
test "$(git branch --show-current)" = test || { echo "Prerelease must be built from test branch"; exit 1; }
test -f "$asset" && test -f "$checksum" || { echo "Run bash scripts/build-release.sh first"; exit 1; }
(cd dist && sha256sum -c "$(basename "$checksum")")
if gh release view "$version" --repo FionaAleksic/Sibyl.ad >/dev/null 2>&1; then
  echo "Refusing to replace existing $version"
  exit 1
fi
gh release create "$version" "$asset" "$checksum" \
  --repo FionaAleksic/Sibyl.ad \
  --target "$(git rev-parse HEAD)" \
  --prerelease \
  --title "Sibyl.ad $version – Read-only LDAPS Connector" \
  --notes-file RELEASE_NOTES.md
echo "Verify at https://github.com/FionaAleksic/Sibyl.ad/releases"
