param([Parameter(Mandatory=$true)][string]$Scratch,[string]$ReceiptName='core-slice-cleanup.json',
    [ValidateSet('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2')][string[]]$Targets=@('forge-1.20.1'))
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if($ReceiptName -notmatch '^[a-z0-9-]+\.json$') { throw 'Invalid receipt filename' }
$baseline=Get-Content -LiteralPath (Join-Path $projectRoot 'docs/source-baseline.json') -Raw | ConvertFrom-Json
$root=[IO.Path]::GetFullPath($Scratch)
$expected=[IO.Path]::GetFullPath($baseline.scratch)
if($root -ine $expected -or $root -notmatch '^H:\\CodexScratch\\terraria_modding\\\.test-runtime\\metallurgy-port-[0-9a-f]{32}$') { throw 'Not this project owned scratch child' }
if((Get-Item -LiteralPath $root).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) { throw 'Reparse root' }
$operationLock=[IO.File]::Open((Join-Path $root '.operation.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
try {
$active=Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object { $_.CommandLine -like ('*'+$root+'*') }
if($active) { throw 'Owned Java processes still running' }
$readers=Get-CimInstance Win32_Process -Filter "Name='python.exe' OR Name='pythonw.exe'" | Where-Object { $_.CommandLine -like ('*'+$root+'*') -and $_.CommandLine -match 'record_checkpoint\.py' }
if($readers) { throw 'The staged-source receipt is still being recorded' }
# The only removable directories are completed fixtures and obsolete MDKs.
# Live source references and the Gradle cache are intentionally not in this list.
$children=@('stage','mdk-1.20.1','mdk-1.21.1','mdk-26.1.2','forge-mdk.zip')+@($Targets | ForEach-Object {'builds/'+$_})
$removals=@()
foreach($child in $children) {
    $path=[IO.Path]::GetFullPath((Join-Path $root $child))
    if(!$path.StartsWith($root+'\',[StringComparison]::OrdinalIgnoreCase)) { throw 'Escaped owned root' }
    if(Test-Path -LiteralPath $path) {
        $item=Get-Item -LiteralPath $path -Force
        $files=if($item.PSIsContainer) { @(Get-ChildItem -LiteralPath $path -Recurse -Force -File) } else { @($item) }
        if($item.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint) -or ($item.PSIsContainer -and (Get-ChildItem -LiteralPath $path -Recurse -Force -Attributes ReparsePoint))) { throw 'Reparse point in exact target' }
        foreach($file in $files) { $handle=[IO.File]::Open($file.FullName,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None);$handle.Dispose() }
        $removals+=[pscustomobject]@{path=$path;directory=$item.PSIsContainer;files=$files.Count}
    }
}
# All exact paths, process ownership, symlinks and exclusive file access have
# been checked before the first removal. Operations stay in native PowerShell.
foreach($target in $removals) {
    if($target.directory) { Remove-Item -LiteralPath $target.path -Recurse -Force }
    else { Remove-Item -LiteralPath $target.path -Force }
    if(Test-Path -LiteralPath $target.path) { throw 'Cleanup target still exists' }
}
$logs=@(Get-ChildItem -LiteralPath $root -File -Filter '*.log')
foreach($log in $logs) { Remove-Item -LiteralPath $log.FullName -Force }
$receipt=[pscustomobject]@{scope='Completed core fixtures, generated worlds, obsolete build outputs, MDK copies and raw logs';targets=$removals;rawLogsRemoved=$logs.Count;processUseChecked=$true;exclusiveAccessChecked=$true;containmentChecked=$true;liveReference='Upstream sources and Gradle dependencies remain in use by the unfinished port';mainProfileModified=$false}
$receipt | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $projectRoot ('evidence/'+$ReceiptName)) -Encoding utf8
$receipt | ConvertTo-Json -Depth 5 -Compress
} finally { $operationLock.Dispose() }

