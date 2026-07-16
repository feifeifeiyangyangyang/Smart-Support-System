$ErrorActionPreference = "Stop"
$hostName = $env:QDRANT_HOST
$port = $env:QDRANT_PORT
if ([string]::IsNullOrWhiteSpace($hostName)) { $hostName = "localhost" }
if ([string]::IsNullOrWhiteSpace($port)) { $port = "6333" }

$url = "http://$hostName`:$port/collections"
Write-Output "Checking Qdrant: $url"
$response = Invoke-RestMethod -Uri $url -Method Get
$response | ConvertTo-Json -Depth 8
