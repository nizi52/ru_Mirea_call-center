$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$localJdk = Get-ChildItem (Join-Path $projectRoot '.tools\jdk17') -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } | Select-Object -First 1
$localMaven = Join-Path $projectRoot '.tools\apache-maven-3.9.11\bin'

if ($localJdk) {
    $env:JAVA_HOME = $localJdk.FullName
    $env:PATH = "$($localJdk.FullName)\bin;$env:PATH"
}
if (Test-Path $localMaven) {
    $env:PATH = "$localMaven;$env:PATH"
    $mavenRepo = Join-Path $projectRoot '.tools\m2'
    $mavenHome = Join-Path $projectRoot '.tools\home'
    $env:MAVEN_OPTS = "-Duser.home=$mavenHome"
}
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven not found. Install Maven 3.9+ or place it in .tools/apache-maven-3.9.11.'
}
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

if (-not $env:CALLCENTER_DB_URL) {
    $postgresBin = Join-Path $env:ProgramFiles 'PostgreSQL\18\bin'
    $pgReady = Join-Path $postgresBin 'pg_isready.exe'
    $pgCtl = Join-Path $postgresBin 'pg_ctl.exe'
    $localData = Join-Path $projectRoot '.tools\pg-check'
    if (Test-Path $pgReady) {
        & $pgReady -h localhost -p 5432 | Out-Null
        if ($LASTEXITCODE -ne 0 -and (Test-Path (Join-Path $localData 'PG_VERSION'))) {
            & $pgCtl -D $localData -l (Join-Path $projectRoot '.tools\pg-check.log') -o '-p 5432 -h localhost' start
            & $pgReady -h localhost -p 5432 | Out-Null
        }
        if ($LASTEXITCODE -ne 0) {
            throw 'PostgreSQL is not available on localhost:5432. Start postgresql-x64-18 as administrator or configure CALLCENTER_DB_URL.'
        }
    }
}

if ($mavenRepo) {
    mvn "-Dmaven.repo.local=$mavenRepo" compile exec:java
} else {
    mvn compile exec:java
}
