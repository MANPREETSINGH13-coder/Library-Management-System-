@echo off
setlocal
set "MAVEN_VERSION=3.9.16"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\apache-maven-%MAVEN_VERSION%"
set "MAVEN_EXE=%MAVEN_HOME%\bin\mvn.cmd"
set "MAVEN_ARCHIVE=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_EXE%" (
  echo Downloading Apache Maven %MAVEN_VERSION% for Windows...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; New-Item -ItemType Directory -Force -Path '%USERPROFILE%\.m2\wrapper' | Out-Null; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ARCHIVE%'; Expand-Archive -LiteralPath '%MAVEN_ARCHIVE%' -DestinationPath '%USERPROFILE%\.m2\wrapper' -Force; Remove-Item -LiteralPath '%MAVEN_ARCHIVE%' -Force"
  if errorlevel 1 exit /b 1
)

call "%MAVEN_EXE%" %*
exit /b %ERRORLEVEL%
