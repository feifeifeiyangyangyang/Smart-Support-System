$ErrorActionPreference = "Stop"

$root = Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")
$target = Join-Path $root "models\embedding"
New-Item -ItemType Directory -Force -Path $target | Out-Null

$files = @(
    @{
        Url = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/onnx/model.onnx"
        Path = Join-Path $target "model.onnx"
    },
    @{
        Url = "https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/main/tokenizer.json"
        Path = Join-Path $target "tokenizer.json"
    }
)

foreach ($file in $files) {
    Write-Output "Downloading $($file.Url)"
    Invoke-WebRequest -UseBasicParsing -Uri $file.Url -OutFile $file.Path -TimeoutSec 300
}

Write-Output "Embedding model downloaded to $target"
