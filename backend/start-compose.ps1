$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Test-Path '.env')) { throw 'Create .env using python scripts/init_env.py or copy .env.example and set secrets.' }
docker compose --env-file .env -f backend/docker-compose.yml up -d --build --wait
if ($LASTEXITCODE -ne 0) { throw 'Docker startup failed. Check docker compose logs.' }
