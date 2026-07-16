#!/usr/bin/env sh
set -eu

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
TARGET="$ROOT/models/embedding"
mkdir -p "$TARGET"

curl -L --retry 3 --connect-timeout 30 \
  -o "$TARGET/model.onnx" \
  "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx"

curl -L --retry 3 --connect-timeout 30 \
  -o "$TARGET/tokenizer.json" \
  "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json"

echo "Embedding model downloaded to $TARGET"
