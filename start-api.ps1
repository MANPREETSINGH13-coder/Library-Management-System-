$ErrorActionPreference = 'Stop'

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

& (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
exit $LASTEXITCODE

