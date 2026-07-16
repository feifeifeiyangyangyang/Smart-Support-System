#!/usr/bin/env sh
set -eu

MODEL_PATH="${EMBEDDING_MODEL_PATH:-./models/embedding/model.onnx}"
TOKENIZER_PATH="${EMBEDDING_TOKENIZER_PATH:-./models/embedding/tokenizer.json}"

echo "Checking embedding model files..."
echo "Model: $MODEL_PATH"
echo "Tokenizer: $TOKENIZER_PATH"

test -f "$MODEL_PATH" || { echo "Missing model file: $MODEL_PATH" >&2; exit 1; }
test -f "$TOKENIZER_PATH" || { echo "Missing tokenizer file: $TOKENIZER_PATH" >&2; exit 1; }

echo "Embedding model files exist."
