#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
test -f .env || { echo 'Create .env using python3 scripts/init_env.py'; exit 1; }
docker compose --env-file .env -f backend/docker-compose.yml up -d --build --wait
