# Remove only obsolete generated duplicates after the new pack files match byte-for-byte.
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$catalog=Get-Content -LiteralPath (Join-Path $projectRoot 'content/legacy-catalog.json') -Raw | ConvertFrom-Json
$plans=@()
foreach($target in @('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2')) {
    $base=Join-Path $projectRoot ($target+'/src/main/resources')
    $relative=@($catalog.materials | Where-Object {$_.PSObject.Properties['tool_stats'] -or $_.PSObject.Properties['armor_stats']} | ForEach-Object {'data/metallurgy/silentgear_materials/'+$_.name+'.json'})
    foreach($file in $relative) {$plans+=@{base=$base;file=$file;pack='silentgear'}}
    if($target -eq 'forge-1.20.1') {
        $relative=@('data/tetra/tiers/metallurgy.json')+@($catalog.materials | Where-Object {$_.PSObject.Properties['tool_stats']} | ForEach-Object {'data/tetra/materials/metallurgy/'+$_.name+'.json'})
        foreach($file in $relative) {$plans+=@{base=$base;file=$file;pack='tetra'}}
    }
}
$removals=@()
foreach($plan in $plans) {
    $old=[IO.Path]::GetFullPath((Join-Path $plan.base $plan.file))
    $new=[IO.Path]::GetFullPath((Join-Path $plan.base ('resourcepacks/'+$plan.pack+'/'+$plan.file)))
    foreach($path in @($old,$new)) {if(!$path.StartsWith($projectRoot+'\',[StringComparison]::OrdinalIgnoreCase)) {throw 'Escaped maintained project root'}}
    if(!(Test-Path -LiteralPath $old)) {continue}
    if(!(Test-Path -LiteralPath $new)) {throw ('Missing generated destination '+$new)}
    $hash=(Get-FileHash -LiteralPath $old -Algorithm SHA256).Hash
    if($hash -ne (Get-FileHash -LiteralPath $new -Algorithm SHA256).Hash) {throw ('Maintained resource differs; review before removing '+$old)}
    $removals+=[pscustomobject]@{old=$old;new=$new;sha256=$hash}
}
foreach($item in $removals) {Remove-Item -LiteralPath $item.old -Force}
[pscustomobject]@{scope='Generated optional data moved behind dependency/config-gated native built-in packs';files=$removals;profilesModified=$false;gamesLaunched=$false} | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $projectRoot 'evidence/modular-pack-layout-migration.json') -Encoding utf8
Write-Output ('Removed '+$removals.Count+' obsolete generated root resources after exact hash comparison')
