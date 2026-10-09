$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080'
$script:results = @()

function Req($uri,$method,$token,$body){
  $h=@{}
  if($token){$h['Authorization']="Bearer $token"}
  $ct=$null
  if($body -ne $null){$ct='application/json'}
  try{
    $r=Invoke-RestMethod -Uri "$base$uri" -Method $method -Headers $h -ContentType $ct -Body $body
    $codeField=$r.code
    return [pscustomobject]@{Code=$codeField; Body=$r; Status=200}
  }catch{
    $resp=$_.Exception.Response
    $status=0
    if($resp){$status=[int]$resp.StatusCode}
    $raw=''
    try{$reader=New-Object System.IO.StreamReader($resp.GetResponseStream()); $raw=$reader.ReadToEnd()}catch{}
    $bodyObj=$null
    try{$bodyObj=$raw|ConvertFrom-Json}catch{$bodyObj=$raw}
    $codeField=$null
    try{if($bodyObj.code){$codeField=$bodyObj.code}}catch{}
    return [pscustomobject]@{Code=$codeField; Body=$bodyObj; Raw=$raw; Status=$status}
  }
}

function Add-Result($id,$rule,$scene,$expect,$actual,$conclusion){
  $script:results+=[pscustomobject]@{Id=$id;Rule=$rule;Scene=$scene;Expect=$expect;Actual=$actual;Conclusion=$conclusion}
}

function Login($acct){
  $r=Req '/api/auth/login' 'POST' $null ("{`"account`":`"$acct`",`"password`":`"123456`"}")
  return [pscustomobject]@{Token=$r.Body.data.token; Role=$r.Body.data.role; Name=$r.Body.data.name}
}

Write-Host "===== Prep: login ====="
$tTok=(Login 'T2024001').Token
$aTok=(Login 'admin').Token
Write-Host "teacher/admin OK"

# T01 V1.0 regression - student login
$sLogin=Login 'S2024001'
$sTok1=$sLogin.Token
$role=$sLogin.Role
Add-Result 'T01' 'V1.0-login-regression' 'S2024001 login' 'role=STUDENT, token issued' "role=$role,tokenLen=$($sTok1.Length)" $(if($role -eq 'STUDENT' -and $sTok1){'PASS'}else{'FAIL'})

# T02 V1.0 regression - activity list
$list=Req '/api/student/activities?current=1&size=20' 'GET' $sTok1 $null
$total=$list.Body.data.total
Add-Result 'T02' 'V1.0-activity-list-regression' 'student query activity list' 'total=5' "total=$total" $(if($total -eq 5){'PASS'}else{'FAIL'})

# T03 V2.0 grade-allowed (S2024001 grade=2024 register act2 target=2024)
$r3=Req '/api/student/activities/2/register' 'POST' $sTok1 $null
$st3=$r3.Body.data.status
Add-Result 'T03' 'V2.0-grade-qualify-pass' 'S2024001(2024) register act2 target=2024' 'ENROLLED' "status=$st3,msg=$($r3.Body.data.message)" $(if($st3 -eq 'ENROLLED'){'PASS'}else{'FAIL'})

# T04 V2.0 grade-reject (teacher creates target=2025 act, 2024 student rejected)
$actBody='{"title":"V2test-2025only","activityTime":"2026-11-01 09:00:00","location":"test","description":"grade reject","maxParticipants":10,"signupDeadline":"2026-10-30 23:59:59","targetGrade":"2025"}'
$newAct=Req '/api/teacher/activities' 'POST' $tTok $actBody
$gid=$newAct.Body.data
$r4=Req "/api/student/activities/$gid/register" 'POST' $sTok1 $null
$code4=$r4.Body.code
Add-Result 'T04' 'V2.0-grade-qualify-reject' 'S2024001(2024) register 2025-only act' 'code=1010 GRADE_NOT_ALLOWED' "code=$code4,msg=$($r4.Body.message)" $(if($code4 -eq 1010){'PASS'}else{'FAIL'})

# T05 V2.0 waiting-queue (max=1 act: A=03 ENROLLED, B=04 WAITING order1, C=06 WAITING order2)
$actBody2='{"title":"V2test-quota1","activityTime":"2026-11-20 09:00:00","location":"room","description":"waitlist test","maxParticipants":1,"signupDeadline":"2026-11-18 23:59:59","targetGrade":null}'
$newAct2=Req '/api/teacher/activities' 'POST' $tTok $actBody2
$cid=$newAct2.Body.data
$sTok3=(Login 'S2024003').Token; $sTok4=(Login 'S2024004').Token; $sTok6=(Login 'S2024006').Token
$r5a=Req "/api/student/activities/$cid/register" 'POST' $sTok3 $null
$r5b=Req "/api/student/activities/$cid/register" 'POST' $sTok4 $null
$r5c=Req "/api/student/activities/$cid/register" 'POST' $sTok6 $null
Add-Result 'T05' 'V2.0-threebranch-waiting' 'max=1 act: A ENROLLED, B/C wait' 'A=ENROLLED;B=order1;C=order2' "A=$($r5a.Body.data.status);B=$($r5b.Body.data.status)/o$($r5b.Body.data.waitOrder);C=$($r5c.Body.data.status)/o$($r5c.Body.data.waitOrder)" $(if($r5a.Body.data.status -eq 'ENROLLED' -and $r5b.Body.data.waitOrder -eq 1 -and $r5c.Body.data.waitOrder -eq 2){'PASS'}else{'FAIL'})

# T06 V2.0 duplicate-block (A already ENROLLED, register same act)
$r6=Req "/api/student/activities/$cid/register" 'POST' $sTok3 $null
$code6=$r6.Body.code
Add-Result 'T06' 'V2.0-duplicate-signup' 'A re-register same act' 'code=1006 DUPLICATE_SIGNUP' "code=$code6,msg=$($r6.Body.message)" $(if($code6 -eq 1006){'PASS'}else{'FAIL'})

# T10 V2.0 teacher waiting-queue view (should be B,C =2)
$wq=Req "/api/teacher/activities/$cid/waiting?current=1&size=10" 'GET' $tTok $null
$wcount=$wq.Body.data.total
Add-Result 'T10' 'V2.0-teacher-waiting-queue' 'teacher view waiting queue' 'total=2' "total=$wcount" $(if($wcount -eq 2){'PASS'}else{'FAIL'})

# T07 V2.0 withdraw+promote (A withdraw -> B promoted ENROLLED, C order1)
$r7=Req "/api/student/activities/$cid/withdraw" 'POST' $sTok3 $null
$code7=$r7.Body.code
$myB=Req '/api/student/registrations' 'GET' $sTok4 $null
$bReg=$myB.Body.data.records | Where-Object {$_.activityId -eq $cid}
$myC=Req '/api/student/registrations' 'GET' $sTok6 $null
$cReg=$myC.Body.data.records | Where-Object {$_.activityId -eq $cid}
Add-Result 'T07' 'V2.0-withdraw-promote' 'A withdraw -> B promote, C keep order2 (no reorder)' 'A=CANCELLED;B=ENROLLED;C=WAITING/o2' "code=$code7;B=$($bReg.status);C=$($cReg.status)/o$($cReg.waitOrder)" $(if($code7 -eq 200 -and $bReg.status -eq 'ENROLLED' -and $cReg.waitOrder -eq 2){'PASS'}else{'FAIL'})

# T13 V2.0 notifications (A CANCELLED, B PROMOTED, teacher roster-change)
$nA=Req '/api/student/notifications?onlyUnread=true&current=1&size=20' 'GET' $sTok3 $null
$nB=Req '/api/student/notifications?onlyUnread=true&current=1&size=20' 'GET' $sTok4 $null
$nT=Req '/api/teacher/notifications?onlyUnread=true&current=1&size=20' 'GET' $tTok $null
$aTypes=($nA.Body.data.records.type) -join '/'
$bTypes=($nB.Body.data.records.type) -join '/'
$tTypes=($nT.Body.data.records.type) -join '/'
Add-Result 'T13' 'V2.0-auto-notifications' 'check notifications after withdraw+promote' 'A has CANCELLED; B has PROMOTED; T has roster-change' "A=[$aTypes];B=[$bTypes];T=[$tTypes]" $(if($aTypes -match 'CANCELLED' -and $bTypes -match 'PROMOTED' -and ($tTypes -match 'CANCELLED' -or $tTypes -match 'PROMOTED')){'PASS'}else{'FAIL'})

# T08 V2.0 account-disable (admin disable S2024005 -> login fails 1002)
$dis=Req '/api/admin/users/S2024005/disable' 'POST' $aTok $null
$loginBody='{"account":"S2024005","password":"123456"}'
$r8=Req '/api/auth/login' 'POST' $null $loginBody
$code8=$r8.Body.code
Req '/api/admin/users/S2024005/enable' 'POST' $aTok $null | Out-Null
Add-Result 'T08' 'V2.0-account-disable' 'admin disable S2024005 then login' 'code=1002 ACCOUNT_DISABLED' "code=$code8,msg=$($r8.Body.message)" $(if($code8 -eq 1002){'PASS'}else{'FAIL'})

# T09 V2.0 role-forbidden (student calls teacher publish)
$badBody='{"title":"hack","activityTime":"2026-12-01 09:00:00","location":"x","maxParticipants":5,"signupDeadline":"2026-11-29 23:59:59"}'
$r9=Req '/api/teacher/activities' 'POST' $sTok1 $badBody
$code9=$r9.Body.code
Add-Result 'T09' 'V2.0-role-rbac' 'student token call teacher publish' 'code=403 FORBIDDEN' "code=$code9,msg=$($r9.Body.message)" $(if($code9 -eq 403){'PASS'}else{'FAIL'})

# T11 V2.0 admin cannot register (admin call student register)
$r11=Req '/api/student/activities/1/register' 'POST' $aTok $null
$code11=$r11.Body.code
Add-Result 'T11' 'V2.0-admin-cannot-register' 'admin token call student register' 'code=403 FORBIDDEN' "code=$code11,msg=$($r11.Body.message)" $(if($code11 -eq 403){'PASS'}else{'FAIL'})

# T12 V2.0 teacher export CSV
try{
  $csv=Invoke-WebRequest -Uri "$base/api/teacher/activities/$cid/export" -Headers @{Authorization="Bearer $tTok"} -UseBasicParsing
  $csvText=$csv.Content
  $lineCount=($csvText -split "`n").Count
  Add-Result 'T12' 'V2.0-teacher-export-csv' "teacher export act $cid roster" 'CSV with header+rows' "status=$($csv.StatusCode),lines=$lineCount" $(if($csv.StatusCode -eq 200 -and $lineCount -gt 1){'PASS'}else{'FAIL'})
}catch{
  Add-Result 'T12' 'V2.0-teacher-export-csv' "teacher export act $cid" 'CSV' "err:$($_.Exception.Message)" 'FAIL'
}

Write-Host ""
Write-Host "===================== TEST SUMMARY ====================="
foreach($r in $script:results){
  Write-Host ("[{0}] {1}" -f $r.Conclusion,$r.Id)
  Write-Host ("  Scene  : {0}" -f $r.Scene)
  Write-Host ("  Expect : {0}" -f $r.Expect)
  Write-Host ("  Actual : {0}" -f $r.Actual)
}
$pass=($script:results | Where-Object {$_.Conclusion -eq 'PASS'}).Count
Write-Host "PASS $pass / $($script:results.Count)"
