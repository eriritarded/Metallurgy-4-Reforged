param([Parameter(Mandatory=$true)][string]$Scratch)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$parent=[IO.Path]::GetFullPath('H:/CodexScratch/terraria_modding/.test-runtime')
$work=[IO.Path]::GetFullPath($Scratch)
if([IO.Path]::GetDirectoryName($work) -ne $parent -or ![IO.Path]::GetFileName($work).StartsWith('metallurgy-hephaestus-traits-') -or !(Test-Path -LiteralPath $work -PathType Container)) {throw 'Unexpected owned scratch path'}
if((Get-Item -LiteralPath $work).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Scratch root is linked'}
$build=Get-Content -LiteralPath (Join-Path $root 'docs/hephaestus-traits-build.json') -Raw | ConvertFrom-Json -AsHashtable
$inspection=Get-Content -LiteralPath (Join-Path $root 'docs/hephaestus-traits-inspection.json') -Raw | ConvertFrom-Json -AsHashtable
if(!$build.packaged -or $build.sourceCommit -ne $inspection.sourceCommit) {throw 'Matching package and inspection records required'}
$slash=$work.Replace('\','/')
$using=@(Get-CimInstance Win32_Process | Where-Object {$_.ProcessId -ne $PID -and $_.CommandLine -and ($_.CommandLine.Contains($work) -or $_.CommandLine.Contains($slash))})
if($using.Count) {throw 'Scratch is still in use'}
$children=@(Get-ChildItem -LiteralPath $work -Recurse -Force)
if($children | Where-Object {$_.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)}) {throw 'Linked scratch entry'}
$files=@($children | Where-Object {!$_.PSIsContainer})
foreach($file in $files) {
    if(![IO.Path]::GetFullPath($file.FullName).StartsWith($work+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Unexpected child path'}
    $handle=[IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
    $handle.Dispose()
}
Remove-Item -LiteralPath $work -Recurse -Force
if(Test-Path -LiteralPath $work) {throw 'Scratch cleanup incomplete'}
$receipt=[ordered]@{schema=1;recordedAt=[DateTimeOffset]::Now.ToString('o');sourceCommit=$build.sourceCommit;ownedScratchName=[IO.Path]::GetFileName($work);removed=$true;filesRemoved=$files.Count;processesUsingScratch=0;allFilesUnlocked=$true;retained='Maintained source, concise source/build/inspection records, ordinary production/source packages and required unreleased-candidate backups';testsRun=$false;gameLaunched=$false}
$receipt | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $root 'docs/hephaestus-traits-cleanup.json') -Encoding utf8
$receipt | ConvertTo-Json
