param(
    [string]$Url = "http://localhost:3000/api/diagnostics/instance",
    [int]$Requests = 100,
    [int]$TimeoutSec = 3
)

$counts = @{}
$failures = 0
$failureMessages = @{}
$timer = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 1; $i -le $Requests; $i++) {
    try {
        $response = Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec $TimeoutSec
        $instance = $response.data.instanceId
        if (-not $instance) {
            $instance = $response.data.port
        }
        if (-not $instance) {
            $instance = "unknown"
        }
        if (-not $counts.ContainsKey($instance)) {
            $counts[$instance] = 0
        }
        $counts[$instance]++
    } catch {
        $failures++
        $message = $_.Exception.Message
        if (-not $failureMessages.ContainsKey($message)) {
            $failureMessages[$message] = 0
        }
        $failureMessages[$message]++
    }
}

$timer.Stop()

[pscustomobject]@{
    url = $Url
    requests = $Requests
    failures = $failures
    totalMilliseconds = $timer.ElapsedMilliseconds
    distribution = $counts
    failureMessages = $failureMessages
} | ConvertTo-Json -Depth 5
