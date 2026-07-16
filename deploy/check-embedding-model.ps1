$ErrorActionPreference = "Stop"
$modelPath = $env:EMBEDDING_MODEL_PATH
$tokenizerPath = $env:EMBEDDING_TOKENIZER_PATH
if ([string]::IsNullOrWhiteSpace($modelPath)) { $modelPath = ".\models\embedding\model.onnx" }
if ([string]::IsNullOrWhiteSpace($tokenizerPath)) { $tokenizerPath = ".\models\embedding\tokenizer.json" }

Write-Output "Checking embedding model files..."
Write-Output "Model: $modelPath"
Write-Output "Tokenizer: $tokenizerPath"

if (!(Test-Path -LiteralPath $modelPath)) { throw "Missing model file: $modelPath" }
if (!(Test-Path -LiteralPath $tokenizerPath)) { throw "Missing tokenizer file: $tokenizerPath" }

Write-Output "Embedding model files exist."
