$ErrorActionPreference = 'Stop'
$backendDirectory = Join-Path $PSScriptRoot 'backend'
$runtimeDirectory = Join-Path $env:TEMP 'bbau-library-api'
$promptedForMongoUri = $false
$mongoUriPointer = [IntPtr]::Zero
$launcherExitCode = 1

try {
    if ([string]::IsNullOrWhiteSpace($env:MONGODB_URI)) {
        $promptedForMongoUri = $true
        $secureMongoUri = Read-Host 'MongoDB Atlas URI (leave blank to use local MongoDB)' -AsSecureString
        $mongoUriPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureMongoUri)
        $mongoUri = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($mongoUriPointer)
        if (-not [string]::IsNullOrWhiteSpace($mongoUri)) { $env:MONGODB_URI = $mongoUri }
    }
    if ([string]::IsNullOrWhiteSpace($env:MONGODB_DATABASE)) { $env:MONGODB_DATABASE = 'bbau_library' }

    $runtimeSourceDirectory = Join-Path $runtimeDirectory 'src\main'
    New-Item -ItemType Directory -Force -Path $runtimeSourceDirectory | Out-Null
    Copy-Item -LiteralPath (Join-Path $backendDirectory 'pom.xml') -Destination $runtimeDirectory -Force
    Copy-Item -LiteralPath (Join-Path $backendDirectory 'mvnw.cmd') -Destination $runtimeDirectory -Force
    Copy-Item -LiteralPath (Join-Path $backendDirectory '.mvn') -Destination $runtimeDirectory -Recurse -Force
    Get-ChildItem -LiteralPath (Join-Path $backendDirectory 'src\main') -Force | Copy-Item -Destination $runtimeSourceDirectory -Recurse -Force

    Push-Location $runtimeDirectory
    try {
        & '.\mvnw.cmd' spring-boot:run
        $launcherExitCode = $LASTEXITCODE
    }
    finally { Pop-Location }
}
finally {
    if ($mongoUriPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($mongoUriPointer) }
    if ($promptedForMongoUri) { $env:MONGODB_URI = $null }
    $mongoUri = $null
    if ($secureMongoUri) { $secureMongoUri.Dispose() }
}

exit $launcherExitCode
