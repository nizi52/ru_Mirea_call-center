$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven не найден. Установите Maven 3.9+ и добавьте его в PATH.'
}
$javaVersion = cmd /c "java -version 2>&1" | Select-Object -First 1
if ($javaVersion -notmatch '"21') {
    Write-Warning "Нужна Java 21, найдено: $javaVersion. Проверьте JAVA_HOME."
}

# Если адрес БД не задан переменной окружения, используется localhost:5432 из database.properties.
if (-not $env:CALLCENTER_DB_URL) {
    $client = New-Object System.Net.Sockets.TcpClient
    try { $client.Connect('localhost', 5432); $dbUp = $true } catch { $dbUp = $false } finally { $client.Dispose() }
    if (-not $dbUp) {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
            throw 'PostgreSQL не отвечает на localhost:5432, а Docker не найден. Запустите PostgreSQL или задайте CALLCENTER_DB_URL.'
        }
        Write-Host 'PostgreSQL не отвечает, запускаю контейнер из docker-compose.yml...'
        docker compose up -d --wait
        if ($LASTEXITCODE -ne 0) { throw 'Не удалось запустить PostgreSQL через Docker. Проверьте, что Docker Desktop запущен.' }
    }
}

$env:JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
mvn javafx:run
