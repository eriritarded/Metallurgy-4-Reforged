$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$receiptPath=Join-Path $root 'evidence/bees-login-release.json'
$receipt=Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if(!$receipt.packagingSucceeded -or $receipt.installed) {throw 'Expected an uninstalled inspected release'}
$mods='C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged/minecraft/mods'
$meta=@($receipt.artifacts | Where-Object {$_.target -eq 'neoforge-26.1.2' -and $_.kind -eq 'playable-mod' -and !$_.modId})
$addon=@($receipt.artifacts | Where-Object {$_.modId -eq 'genindustry_bees'})
if($meta.Count -ne 1 -or $addon.Count -ne 1) {throw 'Expected unique Metallurgy and Genetic Industry packages'}
$changes=@(
    [pscustomobject]@{id='metallurgy';artifact=$meta[0];source=(Join-Path $root ('artifacts/'+$meta[0].file));oldName='Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.12.jar';oldSha='1677051c8199604be0f18e6b42f85c9c4fbeaeca7afb23277decf80a5aa1551f';backupRoot=(Join-Path $root 'artifacts/backups/world-login-metallurgy-port12')},
    [pscustomobject]@{id='genindustry_bees';artifact=$addon[0];source=$addon[0].path;oldName='genindustry_bees-neoforge-1.0.0+26.1.2-port.1.jar';oldSha='cdc1866a820436a2c08bbe45ad1a579059d99e42b295f38758f1d08cda23deb3';backupRoot='H:/AI_MC_Modding/ComplicatedBees/dist/backups/genetic-template-sync-port1'}
)
foreach($change in $changes) {
    $old=Join-Path $mods $change.oldName
    $destination=Join-Path $mods $change.artifact.file
    if((Get-FileHash -LiteralPath $change.source).Hash.ToLowerInvariant() -ne $change.artifact.sha256) {throw 'Deliverable hash mismatch'}
    if((Get-FileHash -LiteralPath $old).Hash.ToLowerInvariant() -ne $change.oldSha -or (Test-Path -LiteralPath $destination)) {throw 'Unexpected installed package state'}
    $handle=[IO.File]::Open($old,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
    $handle.Dispose()
}
$oldNames=@($changes | ForEach-Object {$_.oldName})
$others=@(Get-ChildItem -LiteralPath $mods -Recurse -File | Where-Object {[IO.Path]::GetRelativePath($mods,$_.FullName) -notin $oldNames} | ForEach-Object {
    [pscustomobject]@{path=[IO.Path]::GetRelativePath($mods,$_.FullName);sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}
})
$completed=@();$removed=@()
try {
    foreach($change in $changes) {
        $old=Join-Path $mods $change.oldName
        $destination=Join-Path $mods $change.artifact.file
        $backupDirectory=Join-Path $change.backupRoot $change.oldSha
        New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
        $backup=Join-Path $backupDirectory $change.oldName
        if(!(Test-Path -LiteralPath $backup)) {Copy-Item -LiteralPath $old -Destination $backup}
        if((Get-FileHash -LiteralPath $backup).Hash.ToLowerInvariant() -ne $change.oldSha) {throw 'Backup identity mismatch'}
        Remove-Item -LiteralPath $old
        $removed+=[pscustomobject]@{old=$old;backup=$backup;destination=$destination;newSha=$change.artifact.sha256}
        Copy-Item -LiteralPath $change.source -Destination $destination
        if((Get-FileHash -LiteralPath $destination).Hash.ToLowerInvariant() -ne $change.artifact.sha256) {throw 'Installed hash mismatch'}
        $completed+=[pscustomobject]@{id=$change.id;version=$change.artifact.version;path=$destination;sha256=$change.artifact.sha256;previousBackup=$backup;previousSha256=$change.oldSha}
    }
    $newNames=@($changes | ForEach-Object {$_.artifact.file})
    $now=@(Get-ChildItem -LiteralPath $mods -Recurse -File | Where-Object {[IO.Path]::GetRelativePath($mods,$_.FullName) -notin $newNames})
    if($now.Count -ne $others.Count) {throw 'Other mods-folder file count changed'}
    foreach($other in $others) {
        if((Get-FileHash -LiteralPath (Join-Path $mods $other.path)).Hash.ToLowerInvariant() -ne $other.sha256) {throw 'Other mods-folder file changed'}
    }
} catch {
    foreach($step in $removed) {
        if(Test-Path -LiteralPath $step.destination) {Remove-Item -LiteralPath $step.destination}
        Copy-Item -LiteralPath $step.backup -Destination $step.old
    }
    throw
}
$receipt.installed=$true
$receipt | Add-Member -Force NoteProperty installedAt ([DateTime]::UtcNow.ToString('o'))
$receipt | Add-Member -Force NoteProperty installedMods $completed
$receipt | Add-Member -Force NoteProperty otherModFilesPreserved $others
$receipt | Add-Member -Force NoteProperty gameReloaded $false
$receipt | ConvertTo-Json -Depth 24 | Set-Content -LiteralPath $receiptPath -Encoding utf8
$manifestPath=Join-Path $root 'artifacts/manifest.json'
$manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$manifest.state='Metallurgy port.13 advancement resources and Genetic Industry26.1.2 port.2 serializer fix installed in Simply NeoForged. Other mods preserved; no game/tests.'
$manifest | Add-Member -Force NoteProperty currentReleaseInstalled $true
$manifest.installationReceipt='evidence/bees-login-release.json'
$manifest.installationHistory+=@('evidence/bees-login-release.json')
$manifest | Add-Member -Force NoteProperty currentInstallationEvidence 'evidence/bees-login-release.json'
$manifest | ConvertTo-Json -Depth 24 | Set-Content -LiteralPath $manifestPath -Encoding utf8
[pscustomobject]@{installed=$completed;otherFilesPreserved=$others.Count;gameLaunched=$false;gameReloaded=$false} | ConvertTo-Json -Depth 5
