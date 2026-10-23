param([Parameter(Mandatory=$true)][string]$Scratch)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$storageHelper=Join-Path $projectRoot '../tools/scratch-storage.ps1'
if(!(Test-Path -LiteralPath $storageHelper)) {$storageHelper=Join-Path $projectRoot 'tools/workspace-scratch-storage.ps1'}
. $storageHelper
$allowedStorage=Get-TerrariaScratchRoot
$baseline=Get-Content -LiteralPath (Join-Path $projectRoot 'docs/source-baseline.json') -Raw | ConvertFrom-Json
$root=[IO.Path]::GetFullPath($Scratch)
if($root -ine [IO.Path]::GetFullPath($baseline.scratch) -or $root -notmatch '^H:\\CodexScratch\\terraria_modding\\\.test-runtime\\metallurgy-port-[0-9a-f]{32}$') {throw 'Not the exact owned H: scratch child'}
if((Get-Item -LiteralPath $root).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Reparse scratch root'}
$manifest=Get-Content -LiteralPath (Join-Path $projectRoot 'artifacts/manifest.json') -Raw | ConvertFrom-Json
foreach($artifact in $manifest.artifacts) {if((Get-FileHash -LiteralPath (Join-Path $projectRoot ('artifacts/'+$artifact.file)) -Algorithm SHA256).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Deliverable hash mismatch; preserve scratch'}}
$receipt=Get-Content -LiteralPath (Join-Path $projectRoot 'evidence/production-package-checkpoint.json') -Raw | ConvertFrom-Json
foreach($target in @('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2')) {if(!$receipt.targets.$target.buildSucceeded -or $receipt.targets.$target.maintainedSourceDifferences.Count) {throw 'Missing exact successful production source receipt'}}
$lock=[IO.File]::Open((Join-Path $root '.operation.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
    $active=Get-CimInstance Win32_Process | Where-Object {$_.ProcessId -ne $PID -and $_.Name -match '^(javaw?|pythonw?|git|git-.*)\.exe$' -and $_.CommandLine.Replace('/','\') -like ('*'+$root+'*')}
    if($active) {throw 'An owned build/reference/recording process is still using scratch'}
    $rows=@()
    foreach($child in @('builds','upstream','chests','dependency-repos','dependency-source','gradle-home','process-temp')) {
        $path=[IO.Path]::GetFullPath((Join-Path $root $child))
        if(!$path.StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Cleanup escaped exact owned directory'}
        if(!(Test-Path -LiteralPath $path)) {continue}
        $item=Get-Item -LiteralPath $path -Force
        if($item.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or (Get-ChildItem -LiteralPath $path -Recurse -Force -Attributes ReparsePoint)) {throw 'Reparse point in exact cleanup target'}
        $files=@(Get-ChildItem -LiteralPath $path -Recurse -Force -File);$bytes=0L
        foreach($file in $files) {$stream=[IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None);$stream.Dispose();$bytes+=$file.Length}
        $rows+=[pscustomobject]@{path=$path;files=$files.Count;bytes=$bytes}
    }
    # All exact children are resolved, contained, checked for use and opened exclusively before removal.
    foreach($row in $rows) {Remove-Item -LiteralPath $row.path -Recurse -Force;if(Test-Path -LiteralPath $row.path) {throw 'Cleanup target remains'}}
    $logs=@(Get-ChildItem -LiteralPath $root -File -Filter '*.log');foreach($log in $logs) {Remove-Item -LiteralPath $log.FullName -Force}
    $result=[pscustomobject]@{completedAt=[DateTime]::UtcNow.ToString('o');scope='Completed production staging, copied original references, extracted dependencies, owned Gradle caches and raw logs';removed=$rows;rawLogsRemoved=$logs.Count;deliverableHashesChecked=$true;sourceReceiptsChecked=$true;processUseChecked=$true;exclusiveAccessChecked=$true;containmentChecked=$true;gameLaunched=$false;mainProfileModified=$false;originalUserJarsAndSavesPreserved=$true}
    $result | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $projectRoot 'evidence/release-scratch-cleanup.json') -Encoding utf8
    $result | ConvertTo-Json -Depth 6 -Compress
} finally {$lock.Dispose()}
Remove-Item -LiteralPath (Join-Path $root '.operation.lock') -Force
if(@(Get-ChildItem -LiteralPath $root -Force).Count -eq 0) {Remove-Item -LiteralPath $root}
