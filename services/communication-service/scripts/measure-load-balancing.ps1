param(
    [string]$Url = "http://localhost:3004/api/diagnostics/instance",
    [int]$Requests = 100
)

$counts = @{}
$failures = 0
$timer = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Requests; $i++) {
    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 10
        $instance = $response.data.instanceId
        if (-not $instance) {
            $instance = $response.data.port
        }
        if (-not $counts.ContainsKey($instance)) {
            $counts[$instance] = 0
        }
        $counts[$instance]++
    } catch {
        $failures++
    }
}

$timer.Stop()

[pscustomobject]@{
    url = $Url
    requests = $Requests
    failures = $failures
    totalMilliseconds = $timer.ElapsedMilliseconds
    distribution = $counts
} | ConvertTo-Json -Depth 5
