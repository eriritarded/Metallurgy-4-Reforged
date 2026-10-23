$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $projectRoot '../tools/scratch-storage.ps1')
$allowedParent=Get-TerrariaScratchRoot
$scratch=[IO.Path]::GetFullPath('H:/CodexScratch/terraria_modding/.test-runtime/metallurgy-atlas-fix-45c9de6487e346b0af3c96a81b22b413')
if (!$scratch.StartsWith($allowedParent.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Scratch containment failed'}
if (!(Test-Path -LiteralPath $scratch)) {throw 'Expected owned scratch is absent'}
if ((Get-Item -LiteralPath $scratch).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Scratch is a reparse point'}
$receipt=Get-Content -LiteralPath (Join-Path $projectRoot 'evidence/ore-atlas-fix-26.1.2.json') -Raw | ConvertFrom-Json
if (!$receipt.buildSucceeded -or !$receipt.installed -or $receipt.scratch -ine $scratch) {throw 'Missing completed package/install receipt'}
foreach ($artifact in $receipt.artifacts) {
    $deliverable=Join-Path $projectRoot ('artifacts/'+$artifact.file)
    if ((Get-FileHash -LiteralPath $deliverable).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Deliverable mismatch'}
}
if ((Get-FileHash -LiteralPath $receipt.installedPath).Hash.ToLowerInvariant() -ne $receipt.installedSha256) {throw 'Installed artifact mismatch'}
$audit=Get-Content -LiteralPath (Join-Path $projectRoot 'evidence/atlas-subscriber-after-26.1.2.json') -Raw | ConvertFrom-Json
if ($audit.invalidOrUnresolved.Count -ne 0 -or $audit.sha256 -ne $receipt.installedSha256) {throw 'Missing static listener audit'}
$lock=[IO.File]::Open((Join-Path $scratch '.operation.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
    $active=Get-CimInstance Win32_Process | Where-Object {
        $_.ProcessId -ne $PID -and $_.Name -match '^(javaw?|pythonw?)\.exe$' -and $_.CommandLine -and
        $_.CommandLine.Replace('/','\').IndexOf($scratch,[StringComparison]::OrdinalIgnoreCase) -ge 0
    }
    if ($active) {throw 'Owned build or recording process is still active'}
    $children=@(Get-ChildItem -LiteralPath $scratch -Force | Where-Object {$_.Name -ne '.operation.lock'})
    $allowed=@('builds','gradle-home','process-temp','hsperfdata_MIles','atlas-fix-neoforge-26.1.2.log')
    $rows=@()
    foreach ($child in $children) {
        $exact=[IO.Path]::GetFullPath($child.FullName)
        if ($child.Name -notin $allowed -or !$exact.StartsWith($scratch+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Unexpected cleanup child'}
        if ($child.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Child is a reparse point'}
        if ($child.PSIsContainer -and @(Get-ChildItem -LiteralPath $exact -Recurse -Force -Attributes ReparsePoint).Count) {throw 'Nested reparse point'}
        if ($child.Name -eq 'hsperfdata_MIles') {
            foreach ($perf in Get-ChildItem -LiteralPath $exact -Force) {
                if ($perf.Name -match '^\d+$' -and (Get-Process -Id ([int]$perf.Name) -ErrorAction SilentlyContinue)) {throw 'JVM performance data remains in use'}
            }
        }
        $files=if ($child.PSIsContainer) {@(Get-ChildItem -LiteralPath $exact -Recurse -File -Force)} else {@($child)}
        $bytes=0L
        foreach ($file in $files) {
            $handle=[IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
            $handle.Dispose()
            $bytes+=$file.Length
        }
        $rows+=[pscustomobject]@{path=$exact;files=$files.Count;bytes=$bytes;directory=$child.PSIsContainer}
    }
    foreach ($row in $rows) {
        if ($row.directory) {Remove-Item -LiteralPath $row.path -Recurse -Force}
        else {Remove-Item -LiteralPath $row.path -Force}
    }
    $result=[ordered]@{completedAt=[DateTime]::UtcNow.ToString('o');target=$scratch;removed=$rows;containmentChecked=$true;processUseChecked=$true;exclusiveAccessChecked=$true;deliverableAndInstalledHashesChecked=$true;gameLaunched=$false;testsRun=$false;rootRemoved=$false}
} finally {$lock.Dispose()}
Remove-Item -LiteralPath (Join-Path $scratch '.operation.lock') -Force
if (@(Get-ChildItem -LiteralPath $scratch -Force).Count -ne 0) {throw 'Owned scratch is not empty'}
Remove-Item -LiteralPath $scratch
$result.rootRemoved=!(Test-Path -LiteralPath $scratch)
$result | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $projectRoot 'evidence/ore-atlas-fix-cleanup-26.1.2.json') -Encoding utf8
$result | ConvertTo-Json -Depth 6 -Compress
