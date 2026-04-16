#!/bin/bash
# ================================================================
# Day 17 - 풀스택 종료 스크립트
# ================================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== 프론트엔드 개발 서버 종료 ==="

# 방법 1: PID 파일로 시도
if [ -f "$PROJECT_ROOT/.frontend.pid" ]; then
  FRONTEND_PID=$(cat "$PROJECT_ROOT/.frontend.pid")
  kill $FRONTEND_PID 2>/dev/null && echo "PID $FRONTEND_PID 종료" || echo "PID 종료 실패, 포트로 재시도"
  rm "$PROJECT_ROOT/.frontend.pid"
fi

# 방법 2: 5173번 포트 점유 프로세스 강제 종료 (Windows Git Bash)
PORT_PID=$(netstat -ano | grep ":5173 " | grep "LISTENING" | awk '{print $5}' | head -1)
if [ -n "$PORT_PID" ]; then
  echo "포트 5173 점유 PID: $PORT_PID → 종료"
  taskkill //F //PID $PORT_PID 2>/dev/null || kill -9 $PORT_PID 2>/dev/null
  echo "프론트엔드 종료 완료"
else
  echo "5173번 포트 프로세스 없음 (이미 종료됨)"
fi

echo "=== Docker Compose 풀스택 종료 ==="
cd "$PROJECT_ROOT"
docker compose down

echo "=== 종료 완료 — 컨테이너 상태 ==="
docker compose ps