param([Parameter(Mandatory=$true)][switch]$UserRequested)
$ErrorActionPreference='Stop'
if(!$UserRequested) {throw 'This replacement requires the user request'}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$profile='C:/Users/USER/AppData/Roaming/PrismLauncher/instances/Simply NeoForged'
$mods=(Resolve-Path -LiteralPath (Join-Path $profile 'minecraft/mods')).Path
$packPath=Join-Path $profile 'mmc-pack.json'
$pack=Get-Content -LiteralPath $packPath -Raw | ConvertFrom-Json -AsHashtable
if(@($pack.components | Where-Object {$_.uid -eq 'net.minecraft' -and $_.version -eq '26.1.2'}).Count -ne 1 -or @($pack.components | Where-Object {$_.uid -eq 'net.neoforged' -and $_.version -eq '26.1.2.122'}).Count -ne 1) {throw 'Unexpected Minecraft/NeoForge target'}
$packHash=(Get-FileHash -LiteralPath $packPath -Algorithm SHA256).Hash
$releasePath=Join-Path $root 'evidence/report-fixes-release.json'
$release=Get-Content -LiteralPath $releasePath -Raw | ConvertFrom-Json -AsHashtable
$rows=@($release.records | Where-Object {$_.target -eq 'neoforge-26.1.2' -and $_.artifact.kind -eq 'playable-mod'})
if(!$release.packaged -or $rows.Count -ne 1 -or $rows[0].artifact.version -ne '1.4.1-port.15') {throw 'Missing native26 production release'}
$row=$rows[0]
$oldName='Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.14.jar'
$newName='Metallurgy-Reforged-MC-26.1.2-neoforge-1.4.1-port.15.jar'
$oldHash='0318dd200a81adb854afb24d9da80ca45c875d25d7bb04be9449f9d4ad24c501'
$newHash='0267ceaebed785679ae692d342ef448105c9f19421dc29bc26bbea434a6862ea'
$old=Join-Path $mods $oldName
$new=Join-Path $mods $newName
$source=Join-Path $root ('artifacts/'+$newName)
if($row.artifact.file -ne $newName -or $row.artifact.sha256 -ne $newHash -or $row.baseline.file -ne $oldName -or $row.baseline.sha256 -ne $oldHash -or (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash -ne $newHash) {throw 'Unexpected artifact identity'}
$receiptPath=Join-Path $root 'evidence/report-fixes-install-26.1.2.json'
if(Test-Path -LiteralPath $receiptPath) {
    if((Test-Path -LiteralPath $new) -and (Get-FileHash -LiteralPath $new -Algorithm SHA256).Hash -eq $newHash -and !(Test-Path -LiteralPath $old)) {Write-Output 'port.15 is already installed with the expected hash';return}
    throw 'Prior install receipt exists but live state differs; inspect before proceeding'
}
if((Test-Path -LiteralPath $new) -or !(Test-Path -LiteralPath $old)) {throw 'Unexpected existing Metallurgy files'}
if(@(Get-ChildItem -LiteralPath $mods -File -Filter 'Metallurgy*.jar').Count -ne 1) {throw 'Multiple Metallurgy packages require inspection'}
foreach($target in @($old,$new)) {if([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($target)) -ne $mods) {throw 'Replacement target is outside mods'}}
if((Get-FileHash -LiteralPath $old -Algorithm SHA256).Hash -ne $oldHash) {throw 'Installed port.14 differs from the reviewed baseline'}
try {$check=[IO.File]::Open($old,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None);$check.Dispose()}
catch {throw 'The installed Metallurgy JAR is locked. Close Simply NeoForged before replacement.'}
function Other-Files([string]$Excluded) {
    $map=@{}
    foreach($file in Get-ChildItem -LiteralPath $mods -Recurse -File) {
        if($file.FullName -eq $Excluded) {continue}
        $map[[IO.Path]::GetRelativePath($mods,$file.FullName)]=(Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    return $map
}
$before=Other-Files $old
$backupDir=Join-Path $root ('artifacts/backups/report-fixes-port14/'+$oldHash)
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$backup=Join-Path $backupDir $oldName
if(Test-Path -LiteralPath $backup) {if((Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash -ne $oldHash) {throw 'Existing rollback backup differs'}}
else {Copy-Item -LiteralPath $old -Destination $backup}
if((Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash -ne $oldHash) {throw 'Rollback backup hash mismatch'}
$check=[IO.File]::Open($old,[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None);$check.Dispose()
if((Get-FileHash -LiteralPath $old -Algorithm SHA256).Hash -ne $oldHash) {throw 'Installed file changed during preflight'}
try {
    Remove-Item -LiteralPath $old
    Copy-Item -LiteralPath $source -Destination $new
    if((Get-FileHash -LiteralPath $new -Algorithm SHA256).Hash -ne $newHash) {throw 'Installed copy hash mismatch'}
    $after=Other-Files $new
    if($before.Count -ne $after.Count) {throw 'Other mods-folder file count changed'}
    foreach($key in $before.Keys) {if(!$after.ContainsKey($key) -or $after[$key] -ne $before[$key]) {throw ('Other mod file changed: '+$key)}}
    if((Get-FileHash -LiteralPath $packPath -Algorithm SHA256).Hash -ne $packHash) {throw 'Launcher configuration changed concurrently'}
} catch {
    if(Test-Path -LiteralPath $new) {
        if((Get-FileHash -LiteralPath $new -Algorithm SHA256).Hash -ne $newHash) {throw 'Replacement changed unexpectedly; preserving it for manual review'}
        Remove-Item -LiteralPath $new
    }
    if(!(Test-Path -LiteralPath $old)) {Copy-Item -LiteralPath $backup -Destination $old}
    throw
}
$preserved=@($before.GetEnumerator() | Sort-Object Name | ForEach-Object {[ordered]@{path=$_.Key;sha256=$_.Value}})
$receipt=[ordered]@{schema=1;installedAt=[DateTimeOffset]::Now.ToString('o');profile=$profile;minecraft='26.1.2';neoforge='26.1.2.122';version=$row.artifact.version;sourceCommit=$release.sourceCommit;artifact=$newName;installedPath=$new;sha256=$newHash;previousBackup=$backup;previousSha256=$oldHash;otherModFilesPreserved=$preserved;launcherConfigurationSha256=$packHash.ToLowerInvariant();launcherConfigurationChanged=$false;savesChanged=$false;gameLaunched=$false;gameReloaded=$false;runtimeConfirmed=$false}
$receipt | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $receiptPath -Encoding utf8
# This three-target release has only its native26 playable package installed.
$row.installed=$true
$row.installationReceipt='evidence/report-fixes-install-26.1.2.json'
$release.installedTargets=@('neoforge-26.1.2')
$release.installationScope='Native26 playable package only; older targets and source JARs are not installed'
$release.installationReceipts=@('evidence/report-fixes-install-26.1.2.json')
$release | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $releasePath -Encoding utf8
$manifestPath=Join-Path $root 'artifacts/manifest.json'
$manifest=Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json -AsHashtable
$manifest.state='Native26 port.15 installed in Simply NeoForged; older-target port.8 packages remain built only. No game/reload/runtime confirmation.'
$manifest.currentReleaseInstalled=$false
$manifest.currentReleaseInstalledTargets=@('neoforge-26.1.2')
$manifest.mainProfileModified=$true
$manifest.updatedAt=$receipt.installedAt
$manifest.currentInstallationEvidence='evidence/report-fixes-install-26.1.2.json'
$manifest.installationReceipt='evidence/report-fixes-install-26.1.2.json'
$history=@($manifest.installationHistory | Where-Object {$_})
if('evidence/report-fixes-install-26.1.2.json' -notin $history) {$history+='evidence/report-fixes-install-26.1.2.json'}
$manifest.installationHistory=$history
$manifest | ConvertTo-Json -Depth 60 | Set-Content -LiteralPath $manifestPath -Encoding utf8
[ordered]@{installed=$newName;sha256=$newHash;otherModFilesPreserved=$before.Count;gameLaunched=$false;gameReloaded=$false} | ConvertTo-Json
