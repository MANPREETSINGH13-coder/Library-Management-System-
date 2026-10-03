$ErrorActionPreference = 'Stop'
$runtimeDirectory = Join-Path $env:TEMP 'bbau-library-api'

$securePassword = Read-Host 'Supabase database password' -AsSecureString
$passwordPointer = [IntPtr]::Zero

try {
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $encodedPassword = [Uri]::EscapeDataString($password)

    $env:SUPABASE_DB_URL = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?user=postgres.egxaxavjxvamdlujtonc&password=$encodedPassword&sslmode=require"
}
finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $password = $null
    $encodedPassword = $null
}

$runtimeSourceDirectory = Join-Path $runtimeDirectory 'src'
New-Item -ItemType Directory -Force -Path $runtimeSourceDirectory | Out-Null
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'pom.xml') -Destination $runtimeDirectory -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'mvnw.cmd') -Destination $runtimeDirectory -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '.mvn') -Destination $runtimeDirectory -Recurse -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'src\main') -Destination $runtimeSourceDirectory -Recurse -Force

Push-Location $runtimeDirectory
try {
    & '.\mvnw.cmd' spring-boot:run
    $launcherExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $launcherExitCode

