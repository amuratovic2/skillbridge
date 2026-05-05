param(
    [string]$Url = "http://localhost:3001/api/diagnostics/instance",
    [int]$Requests = 100
)

$counts = @{}
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Requests; $i++) {
    try {
        $response = Invoke-RestMethod -Method Get -Uri $Url -TimeoutSec 10
        $instanceId = $response.data.instanceId
        if (-not $instanceId) {
            $instanceId = "unknown"
        }
        if (-not $counts.ContainsKey($instanceId)) {
            $counts[$instanceId] = 0
        }
        $counts[$instanceId]++
    } catch {
        if (-not $counts.ContainsKey("failed")) {
            $counts["failed"] = 0
        }
        $counts["failed"]++
    }
}

$stopwatch.Stop()

"Total requests: $Requests"
"Elapsed ms: $($stopwatch.ElapsedMilliseconds)"
"Distribution:"
$counts.GetEnumerator() | Sort-Object Name | ForEach-Object {
    "$($_.Name): $($_.Value)"
}
