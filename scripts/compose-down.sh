#!/bin/bash
# ================================================================
# Day 17 - 풀스택 종료 스크립트 (프론트엔드 포함)
# ================================================================
set -e

echo "=== 프론트엔드 개발 서버 종료 ==="
if [ -f .frontend.pid ]; then
  FRONTEND_PID=$(cat .frontend.pid)
  kill $FRONTEND_PID 2>/dev/null && echo "프론트 PID $FRONTEND_PID 종료됨" || echo "이미 종료된 상태"
  rm .frontend.pid
else
  # PID 파일 없으면 포트로 찾아서 종료
  lsof -ti:3000 | xargs kill -9 2>/dev/null || echo "3000번 포트 프로세스 없음"
fi

echo "=== Docker Compose 풀스택 종료 ==="
docker compose down

echo "=== 종료 완료 — 컨테이너 상태 ==="
docker compose ps