param(
    [Parameter(Mandatory=$true)][ValidateSet('forge-1.20.1','neoforge-1.21.1','neoforge-26.1.2')][string]$Target,
    [Parameter(Mandatory=$true)][string]$Scratch,
    [string[]]$Tasks=@('build'),
    [switch]$RefreshSource
)
$ErrorActionPreference='Stop'
if ($Tasks | Where-Object { $_ -match '(?i)client|gametest|server|verification|launch|(^|:)run|test' }) { throw 'Ordinary builds are compilation/packaging only; authorized client checks use the separate isolated QA harness' }
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$holdPath=Join-Path $projectRoot 'docs/launch-hold.json'
if(Test-Path -LiteralPath $holdPath) {
    $hold=Get-Content -LiteralPath $holdPath -Raw | ConvertFrom-Json
    if($hold.active) { throw ('Launch hold: '+$hold.reason+' '+$hold.resumeCondition) }
}
$storageHelper=Join-Path $projectRoot '../tools/scratch-storage.ps1'
if(!(Test-Path -LiteralPath $storageHelper)) {$storageHelper=Join-Path $projectRoot 'tools/workspace-scratch-storage.ps1'}
. $storageHelper
$allowedRoot=Get-TerrariaScratchRoot
$work=[IO.Path]::GetFullPath($Scratch)
if (!$work.StartsWith($allowedRoot.TrimEnd('\')+'\',[StringComparison]::OrdinalIgnoreCase) -or !(Test-Path -LiteralPath $work) -or (Get-Item -LiteralPath $work).Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)) { throw 'Invalid owned H: work directory' }
$processTemp=Join-Path $work 'process-temp'
New-Item -ItemType Directory -Path $processTemp -Force | Out-Null
$env:TEMP=$processTemp
$env:TMP=$processTemp
$env:TMPDIR=$processTemp
$env:GRADLE_USER_HOME=Join-Path $work 'gradle-home'
$javaVersion=switch($Target) { 'forge-1.20.1' {17};'neoforge-1.21.1' {21};'neoforge-26.1.2' {25} }
$env:JAVA_HOME='C:/Program Files/Java/jdk-'+$javaVersion
$env:JAVA_TOOL_OPTIONS='-Djava.awt.headless=true -Djava.io.tmpdir='+$env:TEMP
$operationLock=[IO.File]::Open((Join-Path $work '.operation.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::ReadWrite)
try {
function Copy-ChangedSource([string]$Source,[string]$Destination) {
    if((Get-Item -LiteralPath $Source).PSIsContainer) {
        New-Item -ItemType Directory -Path $Destination -Force | Out-Null
        foreach($file in Get-ChildItem -LiteralPath $Source -Recurse -File) {
            $relative=[IO.Path]::GetRelativePath($Source,$file.FullName)
            $out=Join-Path $Destination $relative
            $old=Get-Item -LiteralPath $out -ErrorAction SilentlyContinue
            if($null -eq $old -or $old.Length -ne $file.Length -or $old.LastWriteTimeUtc -ne $file.LastWriteTimeUtc) {
                New-Item -ItemType Directory -Path (Split-Path -Parent $out) -Force | Out-Null
                Copy-Item -LiteralPath $file.FullName -Destination $out -Force
            }
        }
    } else { Copy-Item -LiteralPath $Source -Destination $Destination -Force }
}
$stage=Join-Path (Join-Path $work 'builds') $Target
New-Item -ItemType Directory -Path $stage -Force | Out-Null
$targetStage=Join-Path $stage $Target
if (!(Test-Path -LiteralPath $targetStage) -or $RefreshSource) {
    New-Item -ItemType Directory -Path $targetStage -Force | Out-Null
    foreach($name in @('build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','gradle','src')) {
        $source=Join-Path (Join-Path $projectRoot $Target) $name
        if (Test-Path -LiteralPath $source) { Copy-ChangedSource $source (Join-Path $targetStage $name) }
    }
    $guard=Join-Path $stage 'tools'
    New-Item -ItemType Directory -Path $guard -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot 'tools/no-game-launch.gradle') -Destination $guard -Force
    $common=Join-Path $projectRoot 'common'
    if (Test-Path -LiteralPath $common) { Copy-ChangedSource $common (Join-Path $stage 'common') }
    $verification=Join-Path $projectRoot 'verification'
    if (($Tasks -contains '-Pverification') -and (Test-Path -LiteralPath $verification)) { Copy-ChangedSource $verification (Join-Path $stage 'verification') }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'LICENSE'),(Join-Path $projectRoot 'NOTICE.md') -Destination $stage -Force
}
Push-Location -LiteralPath $targetStage
try {
    & (Join-Path $env:JAVA_HOME 'bin/java.exe') '-Dorg.gradle.internal.launcher.welcomeMessageEnabled=false' ('-Djava.io.tmpdir='+$processTemp) -classpath 'gradle/wrapper/gradle-wrapper.jar' org.gradle.wrapper.GradleWrapperMain --no-daemon --console=plain -x test @Tasks
    if($LASTEXITCODE -ne 0) { throw "Gradle failed for $Target (exit $LASTEXITCODE)" }
} finally { Pop-Location }
} finally { $operationLock.Dispose() }
