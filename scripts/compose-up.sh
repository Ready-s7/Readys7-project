#!/bin/bash
# ================================================================
# Day 17 - 풀스택 기동 스크립트 (프론트엔드 포함)
# ================================================================
set -e

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== [1/3] Gradle bootJar 빌드 시작 ==="
cd "$PROJECT_ROOT/backend" && ./gradlew bootJar --no-daemon

echo "=== [2/3] Docker Compose 풀스택 기동 ==="
cd "$PROJECT_ROOT"
docker compose up -d --build

echo "=== [3/3] 프론트엔드 개발 서버 기동 ==="
cd "$PROJECT_ROOT/frontend"
npm run dev &
FRONTEND_PID=$!
echo "프론트엔드 PID: $FRONTEND_PID"
echo $FRONTEND_PID > "$PROJECT_ROOT/.frontend.pid"

cd "$PROJECT_ROOT"

echo "=== 기동 완료 — 백엔드 로그 출력 시작 (중지: Ctrl+C) ==="

trap "echo '=== 종료 중 ===' ; kill $FRONTEND_PID 2>/dev/null; docker compose down; exit" INT TERM

docker compose logs -f backend