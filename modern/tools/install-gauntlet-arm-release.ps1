$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$receiptPath=Join-Path $root 'evidence/gauntlet-arm-release.json'
$receipt=Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if(!$receipt.compiled -or !$receipt.packaged -or $receipt.version -ne '1.4.1-port.14') {throw 'Expected the compiled/inspected native26 port.14 release'}
$instance='C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged'
$pack=Get-Content -LiteralPath (Join-Path $instance 'mmc-pack.json') -Raw | ConvertFrom-Json -AsHashtable
$mc=@($pack.components | Where-Object {$_.uid -eq 'net.minecraft'})
$loader=@($pack.components | Where-Object {$_.uid -eq 'net.neoforged'})
if($mc.Count -ne 1 -or $mc[0].version -ne '26.1.2' -or $loader.Count -ne 1 -or $loader[0].version -ne '26.1.2.122') {throw 'Unexpected live launcher target; no configuration was changed'}
$mods=[IO.Path]::GetFullPath((Join-Path $instance 'minecraft/mods'))
$record=@($receipt.records | Where-Object {$_.artifact.kind -eq 'playable-mod'})
if($record.Count -ne 1) {throw 'Expected one native26 playable artifact'}
$artifact=$record[0].artifact
$baseline=$record[0].baseline
if($artifact.file -ne 'Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.14.jar' -or $baseline.file -ne 'Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.13.jar') {throw 'Unexpected archive names'}
$source=Join-Path $root ('artifacts/'+$artifact.file)
$old=Join-Path $mods $baseline.file
$destination=Join-Path $mods $artifact.file
if((Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Deliverable identity mismatch'}
if($receipt.installed) {
    if((Get-FileHash -LiteralPath $destination).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Installed identity changed since the receipt'}
    Write-Output 'The recorded port.14 artifact is already installed.'
    exit 0
}
if(!(Test-Path -LiteralPath $old) -or (Test-Path -LiteralPath $destination) -or (Get-FileHash -LiteralPath $old).Hash.ToLowerInvariant() -ne $baseline.sha256) {throw 'Unexpected current mod package state'}
foreach($path in @($old,$destination)) {
    if([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($path)) -ne $mods) {throw 'Unexpected replacement path'}
}
try {$handle=[IO.File]::Open($old,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None);$handle.Dispose()}
catch {throw 'The installed Metallurgy JAR is still locked. Close Simply NeoForged before replacement.'}
$others=@(Get-ChildItem -LiteralPath $mods -Recurse -File | Where-Object {[IO.Path]::GetRelativePath($mods,$_.FullName) -ne $baseline.file} | ForEach-Object {
    [pscustomobject]@{path=[IO.Path]::GetRelativePath($mods,$_.FullName);sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}
})
$backupDirectory=Join-Path $root ('artifacts/backups/gauntlet-arm-port13/'+$baseline.sha256)
New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
$backup=Join-Path $backupDirectory $baseline.file
if(!(Test-Path -LiteralPath $backup)) {Copy-Item -LiteralPath $old -Destination $backup}
if((Get-FileHash -LiteralPath $backup).Hash.ToLowerInvariant() -ne $baseline.sha256) {throw 'Rollback backup identity mismatch'}
$removed=$false
try {
    # Repeat the lock/identity checks immediately before replacing the one file.
    $handle=[IO.File]::Open($old,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None);$handle.Dispose()
    if((Get-FileHash -LiteralPath $old).Hash.ToLowerInvariant() -ne $baseline.sha256) {throw 'Installed baseline changed during preparation'}
    Remove-Item -LiteralPath $old
    $removed=$true
    Copy-Item -LiteralPath $source -Destination $destination
    if((Get-FileHash -LiteralPath $destination).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Installed hash mismatch'}
    $now=@(Get-ChildItem -LiteralPath $mods -Recurse -File | Where-Object {[IO.Path]::GetRelativePath($mods,$_.FullName) -ne $artifact.file})
    if($now.Count -ne $others.Count) {throw 'Other mods-folder file count changed'}
    foreach($other in $others) {
        if((Get-FileHash -LiteralPath (Join-Path $mods $other.path)).Hash.ToLowerInvariant() -ne $other.sha256) {throw 'Another mods-folder file changed'}
    }
} catch {
    if($removed) {
        if(Test-Path -LiteralPath $destination) {Remove-Item -LiteralPath $destination}
        Copy-Item -LiteralPath $backup -Destination $old
    }
    throw
}
$installedAt=[DateTimeOffset]::Now.ToString('o')
$installation=[ordered]@{schema=1;installedAt=$installedAt;profile=$instance;version=$artifact.version;sourceCommit=$receipt.sourceCommit;artifact=$artifact.file;sha256=$artifact.sha256;installedPath=$destination;previousBackup=$backup;previousSha256=$baseline.sha256;otherModFilesPreserved=$others;gameLaunched=$false;gameReloaded=$false;renderingObserved=$false;launcherConfigurationChanged=$false;savesChanged=$false}
$installation | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $root 'evidence/gauntlet-arm-install.json') -Encoding utf8
$receipt.installed=$true
$receipt.mainProfileModified=$true
$receipt | Add-Member -Force NoteProperty installedAt $installedAt
$receipt | Add-Member -Force NoteProperty installationReceipt 'evidence/gauntlet-arm-install.json'
$receipt | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $receiptPath -Encoding utf8
$manifestPath=Join-Path $root 'artifacts/manifest.json'
$manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$manifest.state='Native26 port.14 gauntlet arm correction installed in Simply NeoForged; other mod files preserved. No game launch/reload or visual confirmation.'
$manifest.mainProfileModified=$true
$manifest | Add-Member -Force NoteProperty updatedAt $installedAt
$manifest | Add-Member -Force NoteProperty currentReleaseInstalled $true
$manifest | Add-Member -Force NoteProperty installationReceipt 'evidence/gauntlet-arm-install.json'
$manifest | Add-Member -Force NoteProperty currentInstallationEvidence 'evidence/gauntlet-arm-install.json'
$history=@($manifest.installationHistory | Where-Object {$_})
if('evidence/gauntlet-arm-install.json' -notin $history) {$history+=@('evidence/gauntlet-arm-install.json')}
$manifest | Add-Member -Force NoteProperty installationHistory $history
$manifest | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $manifestPath -Encoding utf8
[pscustomobject]@{installed=$artifact.file;sha256=$artifact.sha256;otherModFilesPreserved=$others.Count;gameLaunched=$false;gameReloaded=$false} | ConvertTo-Json
