param([Parameter(Mandatory=$true)][string]$Scratch)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$allowed=[IO.Path]::GetFullPath('H:/CodexScratch/terraria_modding/.test-runtime')
$work=[IO.Path]::GetFullPath($Scratch)
if([IO.Path]::GetDirectoryName($work) -ne $allowed -or ![IO.Path]::GetFileName($work).StartsWith('metallurgy-report-fix-') -or !(Test-Path -LiteralPath $work -PathType Container)) {throw 'Unexpected owned scratch path'}
if((Get-Item -LiteralPath $work).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Scratch root is a reparse point'}
$slash=$work.Replace('\','/')
$using=@(Get-CimInstance Win32_Process | Where-Object {$_.ProcessId -ne $PID -and $_.CommandLine -and ($_.CommandLine.Contains($work) -or $_.CommandLine.Contains($slash))})
if($using.Count) {throw 'Owned scratch is still in use by a process'}
$inspection=Get-Content -LiteralPath (Join-Path $root 'evidence/report-fixes-inspection.json') -Raw | ConvertFrom-Json -AsHashtable
if($inspection.gameLaunched -or $inspection.testsRun) {throw 'Unexpected scope of inspection receipt'}
$children=@(Get-ChildItem -LiteralPath $work -Recurse -Force)
if($children | Where-Object {$_.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)}) {throw 'Unexpected linked scratch entry'}
$files=@($children | Where-Object {!$_.PSIsContainer})
foreach($file in $files) {
    if(![IO.Path]::GetFullPath($file.FullName).StartsWith($work+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Unexpected scratch child path'}
    $handle=[IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
    $handle.Dispose()
}
Remove-Item -LiteralPath $work -Recurse -Force
if(Test-Path -LiteralPath $work) {throw 'Owned scratch cleanup incomplete'}
$receipt=[ordered]@{schema=1;recordedAt=[DateTimeOffset]::Now.ToString('o');ownedScratch=$work;removed=$true;filesRemoved=$files.Count;processesUsingScratch=0;allFilesUnlocked=$true;retained='Maintained source, ordinary binary/source deliverables and concise compiler/package/inspection evidence';gameLaunched=$false;testsRun=$false}
$receipt | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'evidence/report-fixes-cleanup.json') -Encoding utf8
$releasePath=Join-Path $root 'evidence/report-fixes-release.json'
$release=Get-Content -LiteralPath $releasePath -Raw | ConvertFrom-Json -AsHashtable
$release.scratchCleanupComplete=$true
$release.scratchCleanupReceipt='evidence/report-fixes-cleanup.json'
$release | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $releasePath -Encoding utf8
$manifestPath=Join-Path $root 'artifacts/manifest.json'
$manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -AsHashtable
if($manifest.latestPackagingReceipt -ne 'evidence/report-fixes-release.json') {throw 'Another release owns the current artifact manifest'}
$manifest.currentReleaseScratchRemoved=$true
$manifest | ConvertTo-Json -Depth 60 | Set-Content -LiteralPath $manifestPath -Encoding utf8
$receipt | ConvertTo-Json
