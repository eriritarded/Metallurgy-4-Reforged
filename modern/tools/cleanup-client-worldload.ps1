param([Parameter(Mandatory=$true)][string]$Scratch)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $projectRoot '../tools/scratch-storage.ps1')
$allowed=Get-TerrariaScratchRoot
$exact=[IO.Path]::GetFullPath($Scratch)
if (!$exact.StartsWith($allowed.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase) -or
    (Split-Path -Leaf $exact) -notmatch '^metallurgy-worldload-[a-f0-9]{32}$') {throw 'Not an exact owned client-QA child'}
if (!(Test-Path -LiteralPath $exact) -or (Get-Item -LiteralPath $exact).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) {throw 'Missing/reparse scratch target'}
$receiptPath=Join-Path $projectRoot 'evidence/client-worldload-26.1.2.json'
$receipt=Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if ($receipt.result -ne 'passed' -or !$receipt.launch.closed -or $receipt.launch.exitCode -ne 0 -or $receipt.scratch -ine $exact) {throw 'No matching completed pass receipt'}
$installed=Join-Path 'C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged/minecraft/mods' ('Metallurgy-Reforged-MC-26.1.2-neoforge-'+$receipt.version+'.jar')
if ((Get-FileHash -LiteralPath $installed).Hash.ToLowerInvariant() -ne $receipt.productionSha256) {throw 'Installed production hash changed'}
$launch=Get-Content -LiteralPath (Join-Path $exact 'launch.json') -Raw | ConvertFrom-Json
if (!$launch.closed -or (Get-Process -Id $launch.pid -ErrorAction SilentlyContinue)) {throw 'Owned game remains active or PID reused; inspect before cleanup'}
$active=Get-CimInstance Win32_Process | Where-Object {
    $_.ProcessId -ne $PID -and $_.Name -match '^(javaw?|pythonw?)\.exe$' -and $_.CommandLine -and
    $_.CommandLine.Replace('/','\').IndexOf($exact,[StringComparison]::OrdinalIgnoreCase) -ge 0
}
if ($active) {throw 'Owned QA/build/recording process still active'}
if (@(Get-ChildItem -LiteralPath $exact -Recurse -Force -Attributes ReparsePoint).Count) {throw 'Nested reparse point'}
$files=@(Get-ChildItem -LiteralPath $exact -Recurse -File -Force)
$bytes=0L
foreach($file in $files) {
    $resolved=[IO.Path]::GetFullPath($file.FullName)
    if (!$resolved.StartsWith($exact+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'File containment failed'}
    $handle=[IO.File]::Open($resolved,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
    $handle.Dispose(); $bytes+=$file.Length
}
# Native PowerShell only; remove this exact verified owned directory, never the shared root.
Remove-Item -LiteralPath $exact -Recurse -Force
if (Test-Path -LiteralPath $exact) {throw 'Owned scratch remains'}
$result=[ordered]@{
    completedAt=[DateTime]::UtcNow.ToString('o');target=$exact;rootRemoved=$true
    removedFiles=$files.Count;removedBytes=$bytes;containmentChecked=$true;reparseChecked=$true
    processUseChecked=$true;exclusiveAccessChecked=$true;installedProductionHashChecked=$true
    rawLogsWorldsProfileCopiedDependenciesHarnessBinariesAndScreenshotRemoved=$true
}
$result | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $projectRoot 'evidence/client-worldload-cleanup-26.1.2.json') -Encoding utf8
$receipt.cleanupComplete=$true
$receipt | Add-Member -NotePropertyName cleanupReceipt -NotePropertyValue 'evidence/client-worldload-cleanup-26.1.2.json' -Force
$receipt | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $receiptPath -Encoding utf8
$marker=Join-Path $projectRoot 'evidence/current-client-smoke-scratch.txt'
if ((Get-Content -LiteralPath $marker -Raw).Trim().Replace('/','\') -ieq $exact) {Remove-Item -LiteralPath $marker}
$result | ConvertTo-Json -Compress
