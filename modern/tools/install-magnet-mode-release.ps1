$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$receiptPath=Join-Path $projectRoot 'evidence/magnet-mode-release.json'
$receipt=Get-Content -LiteralPath $receiptPath -Raw | ConvertFrom-Json
if (!$receipt.buildSucceeded -or $receipt.installed) {throw 'Missing uninstalled package receipt'}
$artifact=@($receipt.artifacts | Where-Object {$_.target -eq 'neoforge-26.1.2' -and $_.kind -eq 'playable-mod'})
if ($artifact.Count -ne 1) {throw 'Missing unique native26 package'}
$artifact=$artifact[0]
$mods='C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged/minecraft/mods'
$oldPath=Join-Path $mods 'Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.9.jar'
$source=Join-Path $projectRoot ('artifacts/'+$artifact.file)
$destination=Join-Path $mods $artifact.file
$expectedOld='80d07c04640d79ece38c8eb289ff358bf12d58b624f5d4dea3c08c7c249ea18f'
if ((Get-FileHash -LiteralPath $source).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Deliverable hash mismatch'}
if ((Get-FileHash -LiteralPath $oldPath).Hash.ToLowerInvariant() -ne $expectedOld -or (Test-Path -LiteralPath $destination)) {throw 'Unexpected installed package state'}
$lock=[IO.File]::Open($oldPath,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
$lock.Dispose()
$others=@(Get-ChildItem -LiteralPath $mods -Recurse -File | Where-Object {$_.FullName -ine [IO.Path]::GetFullPath($oldPath)} | ForEach-Object {
    [pscustomobject]@{path=[IO.Path]::GetRelativePath($mods,$_.FullName);sha256=(Get-FileHash -LiteralPath $_.FullName).Hash.ToLowerInvariant()}
})
$backup=Join-Path $projectRoot ('artifacts/backups/magnet-mode-port9-'+$expectedOld)
New-Item -ItemType Directory -Path $backup -Force | Out-Null
$backupPath=Join-Path $backup ([IO.Path]::GetFileName($oldPath))
if (Test-Path -LiteralPath $backupPath) {
    if ((Get-FileHash -LiteralPath $backupPath).Hash.ToLowerInvariant() -ne $expectedOld) {throw 'Backup identity mismatch'}
    Remove-Item -LiteralPath $oldPath
} else {Move-Item -LiteralPath $oldPath -Destination $backupPath}
try {
    Copy-Item -LiteralPath $source -Destination $destination
    if ((Get-FileHash -LiteralPath $destination).Hash.ToLowerInvariant() -ne $artifact.sha256) {throw 'Installed hash mismatch'}
} catch {
    if (Test-Path -LiteralPath $destination) {Remove-Item -LiteralPath $destination}
    Copy-Item -LiteralPath $backupPath -Destination $oldPath
    throw
}
$now=@(Get-ChildItem -LiteralPath $mods -Recurse -File | Where-Object {$_.FullName -ine [IO.Path]::GetFullPath($destination)})
if ($now.Count -ne $others.Count) {throw 'Other mods-folder file count changed'}
foreach ($other in $others) {
    if ((Get-FileHash -LiteralPath (Join-Path $mods $other.path)).Hash.ToLowerInvariant() -ne $other.sha256) {throw 'Other mod file changed'}
}
$receipt.installed=$true
$receipt | Add-Member -Force NoteProperty installedAt ([DateTime]::UtcNow.ToString('o'))
$receipt | Add-Member -Force NoteProperty installedPath $destination
$receipt | Add-Member -Force NoteProperty installedSha256 $artifact.sha256
$receipt | Add-Member -Force NoteProperty previousBackup $backupPath
$receipt | Add-Member -Force NoteProperty previousInstalledSha256 $expectedOld
$receipt | Add-Member -Force NoteProperty otherModFilesPreserved $others
$receipt | Add-Member -Force NoteProperty gameReloaded $false
$receipt | ConvertTo-Json -Depth 18 | Set-Content -LiteralPath $receiptPath -Encoding utf8
$manifestPath=Join-Path $projectRoot 'artifacts/manifest.json'
$manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$manifest.state='All three magnet mode feedback packages compiled and inspected. Native26 port.10 installed in Simply NeoForged. Other mods preserved; no game/runtime check.'
$manifest.mainProfileModified=$true
$manifest.installationReceipt='evidence/magnet-mode-release.json'
$manifest.installationHistory+=@('evidence/magnet-mode-release.json')
$manifest | Add-Member -Force NoteProperty currentInstallationEvidence 'evidence/magnet-mode-release.json'
$manifest | ConvertTo-Json -Depth 18 | Set-Content -LiteralPath $manifestPath -Encoding utf8
[pscustomobject]@{installed=$destination;sha256=$artifact.sha256;previousBackup=$backupPath;otherFilesPreserved=$others.Count;gameLaunched=$false;gameReloaded=$false} | ConvertTo-Json
