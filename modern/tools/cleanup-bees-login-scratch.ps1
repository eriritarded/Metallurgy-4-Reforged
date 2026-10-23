$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $projectRoot '../tools/scratch-storage.ps1')
$allowedParent=Get-TerrariaScratchRoot
$receiptPath=Join-Path $projectRoot 'evidence/bees-login-release.json'
$receipt=Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
$scratch=[IO.Path]::GetFullPath($receipt.scratch)
$marker=(Get-Content -LiteralPath (Join-Path $projectRoot 'evidence/current-bees-login-scratch.txt') -Raw).Trim()
if (!$receipt.buildSucceeded -or $receipt.artifacts.Count -ne 7 -or $scratch -ine [IO.Path]::GetFullPath($marker)) {throw 'Missing completed ordinary package receipt'}
if ([IO.Path]::GetDirectoryName($scratch) -ine $allowedParent -or [IO.Path]::GetFileName($scratch) -notlike 'metallurgy-bees-login-*') {throw 'Not the exact owned scratch child'}
if (!(Test-Path -LiteralPath $scratch) -or (Get-Item -LiteralPath $scratch).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Invalid scratch directory'}
foreach ($artifact in $receipt.artifacts) {
    $deliverable=if($artifact.path) {$artifact.path} else {Join-Path $projectRoot ('artifacts/'+$artifact.file)}
    if ((Get-FileHash -LiteralPath $deliverable).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Deliverable mismatch'}
}
$lock=[IO.File]::Open((Join-Path $scratch '.operation.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
    $active=Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^(javaw?|pythonw?)\.exe$' -and $_.CommandLine -and
        $_.CommandLine.Replace('/','\').IndexOf($scratch,[StringComparison]::OrdinalIgnoreCase) -ge 0
    }
    if ($active) {throw 'Owned build/recording process remains active'}
    $children=@(Get-ChildItem -LiteralPath $scratch -Force | Where-Object {$_.Name -ne '.operation.lock'})
    $allowed=@('packages','process-temp','sources','classes','empty-sourcepath','javac.args','genindustry-codec-baseline.txt','genindustry-codec-corrected.txt','genindustry-compile.log')
    $rows=@()
    foreach ($child in $children) {
        $exact=[IO.Path]::GetFullPath($child.FullName)
        if ($child.Name -notin $allowed -or !$exact.StartsWith($scratch+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Unexpected cleanup child'}
        if ($child.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Child is a reparse point'}
        if ($child.PSIsContainer -and @(Get-ChildItem -LiteralPath $exact -Recurse -Force -Attributes ReparsePoint).Count) {throw 'Nested reparse point'}
        if ($child.Name -eq 'hsperfdata_MIles') {
            foreach ($perf in Get-ChildItem -LiteralPath $exact -Force) {
                if ($perf.Name -match '^\d+$' -and (Get-Process -Id ([int]$perf.Name) -ErrorAction SilentlyContinue)) {throw 'JVM performance data in use'}
            }
        }
        $files=if ($child.PSIsContainer) {@(Get-ChildItem -LiteralPath $exact -Recurse -File -Force)} else {@($child)}
        $bytes=0L
        foreach ($file in $files) {
            $handle=[IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
            $handle.Dispose();$bytes+=$file.Length
        }
        $rows+=[pscustomobject]@{path=$exact;files=$files.Count;bytes=$bytes;directory=$child.PSIsContainer}
    }
    foreach ($row in $rows) {
        if ($row.directory) {Remove-Item -LiteralPath $row.path -Recurse -Force}
        else {Remove-Item -LiteralPath $row.path -Force}
    }
    $result=[ordered]@{completedAt=[DateTime]::UtcNow.ToString('o');target=$scratch;removed=$rows;containmentChecked=$true;processUseChecked=$true;exclusiveAccessChecked=$true;deliverableHashesChecked=$true;gameLaunched=$false;testsRun=$false;rootRemoved=$false}
} finally {$lock.Dispose()}
Remove-Item -LiteralPath (Join-Path $scratch '.operation.lock') -Force
if (@(Get-ChildItem -LiteralPath $scratch -Force).Count -ne 0) {throw 'Owned scratch is not empty'}
Remove-Item -LiteralPath $scratch
$result.rootRemoved=!(Test-Path -LiteralPath $scratch)
$result | ConvertTo-Json -Depth 7 | Set-Content -LiteralPath (Join-Path $projectRoot 'evidence/bees-login-cleanup.json') -Encoding utf8
$receipt.scratchCleanupComplete=$true
$receipt | ConvertTo-Json -Depth 18 | Set-Content -LiteralPath $receiptPath -Encoding utf8
Remove-Item -LiteralPath (Join-Path $projectRoot 'evidence/current-bees-login-scratch.txt')
$result | ConvertTo-Json -Depth 7 -Compress
