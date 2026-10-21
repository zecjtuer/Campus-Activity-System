$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api'

function Login($acct) {
    $body = @{ account = $acct; password = '123456' } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri ($base + '/auth/login') -Method Post -Body $body -ContentType 'application/json'
    return $r.data.token
}

function GetJson($url, $token) {
    $h = @{ Authorization = "Bearer $token" }
    return Invoke-RestMethod -Uri $url -Headers $h -Method Get
}

function PostJson($url, $token, $body = $null) {
    $h = @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' }
    if ($body) { $b = $body | ConvertTo-Json } else { $b = '{}' }
    try {
        return @{ ok = $true; data = Invoke-RestMethod -Uri $url -Headers $h -Method Post -Body $b }
    } catch {
        $resp = $_.Exception.Response
        $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
        $errBody = $reader.ReadToEnd()
        return @{ ok = $false; err = $errBody }
    }
}

Write-Output '===== V3.0 Verification ====='

$tok = Login 'S2024001'
Write-Output "[1] Login S2024001: token len=$($tok.Length)"

$acts = GetJson ($base + '/student/activities?current=1&size=20') $tok
$list = $acts.data.records
Write-Output "[2] Activities: total=$($acts.data.total), pageCount=$($list.Count)"
$openAct = $list | Where-Object { $_.remaining -gt 0 -and (-not $_.targetGrade -or $_.targetGrade -match '2024') } | Select-Object -First 1
if (-not $openAct) { $openAct = $list | Where-Object { $_.remaining -gt 0 } | Select-Object -First 1 }
Write-Output "    Picked: id=$($openAct.activityId) title=$($openAct.title) remaining=$($openAct.remaining)"

$r3 = PostJson ($base + '/student/activities/' + $openAct.activityId + '/register') $tok
if ($r3.ok) {
    Write-Output "[3] Register: code=$($r3.data.code) status=$($r3.data.data.status) msg=$($r3.data.message)"
} else {
    Write-Output "[3] Register FAIL: $($r3.err)"
}

$r4 = PostJson ($base + '/student/activities/' + $openAct.activityId + '/register') $tok
if ($r4.ok) {
    Write-Output "[4] Duplicate register: UNEXPECTED success code=$($r4.data.code)"
} else {
    $errObj = $r4.err | ConvertFrom-Json
    Write-Output "[4] Duplicate register: code=$($errObj.code) msg=$($errObj.message) (frontend shows this reason)"
}

$unread = GetJson ($base + '/student/notifications/unread-count') $tok
Write-Output "[5] Unread notifications: $($unread.data) (V3.0 polling+badge+desktop notify rely on this)"

$r6 = PostJson ($base + '/student/activities/' + $openAct.activityId + '/withdraw') $tok
if ($r6.ok) {
    Write-Output "[6] Withdraw: code=$($r6.data.code) msg=$($r6.data.message)"
} else {
    Write-Output "[6] Withdraw FAIL: $($r6.err)"
}

$rec = GetJson ($base + '/student/registrations?current=1&size=10') $tok
Write-Output "[7] My registrations: total=$($rec.data.total), latest displayStatus=$($rec.data.records[0].displayStatus)"

$fullAct = $list | Where-Object { $_.remaining -eq 0 } | Select-Object -First 1
if ($fullAct) {
    $r8 = PostJson ($base + '/student/activities/' + $fullAct.activityId + '/register') $tok
    if ($r8.ok) {
        Write-Output "[8] Waitlist(full activity): status=$($r8.data.data.status) msg=$($r8.data.message)"
    } else {
        Write-Output "[8] Waitlist FAIL: $($r8.err)"
    }
} else {
    Write-Output "[8] No full activity, skip waitlist test"
}

Write-Output ''
Write-Output '===== V3.0 Frontend Behaviors (verify in browser) ====='
Write-Output '  - Register button: loading+disabled during request (prevents duplicate click -> 126 duplicates)'
Write-Output '  - On failure: Toast shows backend error message (e.g. DUPLICATE_SIGNUP), no longer silent'
Write-Output '  - Message badge: pulse animation, 30s polling, new notification -> desktop + in-app toast'
Write-Output '  - My registrations: status-changed rows highlighted with "status update" tag'
Write-Output '  - Backend: FOR UPDATE pessimistic lock on activity row during register (prevents oversell)'
