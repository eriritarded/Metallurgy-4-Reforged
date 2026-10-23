# Scope TEMP/TMP to this build process and its children, not the user's apps.
function Get-TerrariaScratchRoot {
    $taskBase = if ($env:TERRARIA_SCRATCH_ROOT) { $env:TERRARIA_SCRATCH_ROOT } else { 'H:/CodexScratch/terraria_modding' }
    if (![IO.Path]::IsPathFullyQualified($taskBase)) { throw 'Scratch storage needs an absolute path' }
    $taskBase = [IO.Path]::GetFullPath($taskBase)
    $taskDrive = [IO.Path]::GetPathRoot($taskBase)
    if ($taskDrive -ieq 'C:\' -or !(Test-Path -LiteralPath $taskDrive)) { throw 'Scratch drive unavailable or C: selected; no C: fallback' }
    if ((Get-PSDrive -Name $taskDrive.Substring(0,1)).Free -lt 20GB) { throw 'Scratch drive has less than 20 GiB free' }
    $taskRoot = Join-Path $taskBase '.test-runtime'
    $taskTemp = Join-Path $taskBase 'temp'
    New-Item -ItemType Directory -Path $taskRoot,$taskTemp -Force | Out-Null
    $env:TEMP = $taskTemp
    $env:TMP = $taskTemp
    $env:TMPDIR = $taskTemp
    $env:PYTHONPYCACHEPREFIX = Join-Path $taskBase 'cache/python'
    $env:DOTNET_CLI_HOME = Join-Path $taskBase 'cache/dotnet'
    $env:NUGET_PACKAGES = Join-Path $taskBase 'cache/nuget'
    return $taskRoot
}
