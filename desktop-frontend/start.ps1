$ErrorActionPreference = 'Stop'
$moduleDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryDirectory = Resolve-Path (Join-Path $moduleDirectory '..')
$mavenWrapper = Join-Path $repositoryDirectory 'backend\mvnw.cmd'
$runtimeDirectory = Join-Path $env:TEMP 'bbau-library-javafx'
$runtimeSource = Join-Path $runtimeDirectory 'src'

New-Item -ItemType Directory -Force -Path $runtimeDirectory, $runtimeSource | Out-Null
Copy-Item -LiteralPath (Join-Path $moduleDirectory 'pom.xml') -Destination $runtimeDirectory -Force
Get-ChildItem -LiteralPath (Join-Path $moduleDirectory 'src') -Force |
    Copy-Item -Destination $runtimeSource -Recurse -Force

if ([string]::IsNullOrWhiteSpace($env:LIBRARY_API_URL)) {
    $env:LIBRARY_API_URL = 'http://localhost:8080'
}

Push-Location $runtimeDirectory
try {
    & $mavenWrapper -f (Join-Path $runtimeDirectory 'pom.xml') javafx:run
    $launcherExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $launcherExitCode
