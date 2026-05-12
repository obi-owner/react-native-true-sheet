#!/usr/bin/env bash
# Produce a slim source tree suitable as the "modified" side of a yarn patch.
#
# Output: ./dist/  with package.json + src/ + android/ + ios/ + common/ + podspec.
# Excluded: lib/ (we ship src/ directly), example/, docs/, CHANGELOG, sourcemaps,
# tests, mocks fixtures.
#
# Usage:
#   yarn build:patch                 # writes ./dist/
#   yarn build:patch path/to/out     # writes to a different folder
#
# Then in dionysus:
#   yarn patch @lodev09/react-native-true-sheet@npm:<version>
#   # yarn prints a temp folder path; replace its contents with ./dist/
#   yarn patch-commit -s <temp-folder>

set -euo pipefail

DEST="${1:-dist}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

rm -rf "$DEST"
mkdir -p "$DEST"

npm pack --ignore-scripts >/dev/null
TGZ="$(ls lodev09-react-native-true-sheet-*.tgz | head -n 1)"
tar -xzf "$TGZ" -C "$DEST" --strip-components=1
rm "$TGZ"

# Strip anything the consumer doesn't need at runtime.
rm -rf \
  "$DEST/example" \
  "$DEST/docs" \
  "$DEST/__tests__" \
  "$DEST/__mocks__" \
  "$DEST/CHANGELOG.md"
find "$DEST" -name "*.map" -delete

# Pull lib/ and README.md verbatim from the published npm tarball. We ship
# src/ as the entry point so lib/ is unused at runtime, and README diffs are
# noise. Restoring both keeps the patch limited to intentional source edits
# and avoids yarn-patch generating deletion entries for lib/.
PKG_VERSION="$(node -p "require('./package.json').version")"
PRISTINE_DIR="$(mktemp -d)"
( cd "$PRISTINE_DIR" && npm pack "@lodev09/react-native-true-sheet@${PKG_VERSION}" >/dev/null )
PRISTINE_TGZ="$(ls "$PRISTINE_DIR"/lodev09-react-native-true-sheet-*.tgz | head -n 1)"
tar -xzf "$PRISTINE_TGZ" -C "$PRISTINE_DIR" --strip-components=1 package/lib package/README.md
rm -rf "$DEST/lib"
mv "$PRISTINE_DIR/lib" "$DEST/lib"
cp "$PRISTINE_DIR/README.md" "$DEST/README.md"
rm -rf "$PRISTINE_DIR"

echo
echo "Patch source ready at: $DEST"
echo
echo "Next steps in the consumer (e.g. apps/dionysus):"
echo "  yarn patch @lodev09/react-native-true-sheet@npm:<version>"
echo "  # yarn will print a temp folder path. Replace its contents with $DEST:"
echo "  #   rm -rf <temp-folder>/* && cp -R $DEST/. <temp-folder>/"
echo "  yarn patch-commit -s <temp-folder>"
