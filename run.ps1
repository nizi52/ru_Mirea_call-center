$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkHome = Join-Path $projectRoot '.tools\jdk17\jdk-17.0.20.1+1'
$mavenBin = Join-Path $projectRoot '.tools\apache-maven-3.9.11\bin'
$mavenRepo = Join-Path $projectRoot '.tools\m2'
$mavenHome = Join-Path $projectRoot '.tools\home'

if (-not (Test-Path $jdkHome)) {
    throw "JDK 17 not found at $jdkHome"
}

if (-not (Test-Path $mavenBin)) {
    throw "Maven not found at $mavenBin"
}

$env:JAVA_HOME = $jdkHome
$env:PATH = "$jdkHome\bin;$mavenBin;$env:PATH"
$env:MAVEN_OPTS = "-Duser.home=$mavenHome"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null

mvn "-Dmaven.repo.local=$mavenRepo" exec:java
