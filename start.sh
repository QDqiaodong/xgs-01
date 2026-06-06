#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "错误：未找到 .env 配置文件"
    exit 1
fi

export $(grep -v '^#' "$ENV_FILE" | xargs)

echo "=========================================="
echo "  闲置好物置换市集 - 项目启动"
echo "=========================================="
echo ""
echo "Docker 镜像源: ${DOCKER_REGISTRY}"
echo "前端端口: ${FRONTEND_PORT}"
echo "后端端口: ${BACKEND_PORT}"
echo "MySQL 端口: ${MYSQL_PORT}"
echo "Redis 端口: ${REDIS_PORT}"
echo ""

echo "[1/4] 正在构建并启动服务..."
docker compose up --build -d

echo ""
echo "[2/4] 等待 MySQL 启动..."
MAX_RETRIES=30
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker exec ${COMPOSE_PROJECT_NAME}-mysql mysqladmin ping -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} --silent 2>/dev/null; then
        echo "  MySQL 已就绪"
        break
    fi
    echo -n "."
    sleep 2
    RETRY=$((RETRY + 1))
done

if [ $RETRY -ge $MAX_RETRIES ]; then
    echo ""
    echo "  警告：MySQL 启动超时，请检查日志"
fi

echo ""
echo "[3/4] 等待 Redis 启动..."
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker exec ${COMPOSE_PROJECT_NAME}-redis redis-cli ping 2>/dev/null | grep -q PONG; then
        echo "  Redis 已就绪"
        break
    fi
    echo -n "."
    sleep 1
    RETRY=$((RETRY + 1))
done

if [ $RETRY -ge $MAX_RETRIES ]; then
    echo ""
    echo "  警告：Redis 启动超时，请检查日志"
fi

echo ""
echo "[4/4] 等待后端服务启动..."
MAX_RETRIES=60
RETRY=0
while [ $RETRY -lt $MAX_RETRIES ]; do
    if docker logs ${COMPOSE_PROJECT_NAME}-backend 2>/dev/null | grep -q "Started SwapMarketApplication"; then
        echo "  后端服务已就绪"
        break
    fi
    echo -n "."
    sleep 2
    RETRY=$((RETRY + 1))
done

if [ $RETRY -ge $MAX_RETRIES ]; then
    echo ""
    echo "  提示：后端服务可能仍在启动中，请稍后访问"
fi

echo ""
echo "=========================================="
echo "  ✅ 项目启动完成！"
echo "=========================================="
echo ""
echo "🌐 前端访问地址: http://localhost:${FRONTEND_PORT}"
echo "🔧 后端 API 地址: http://localhost:${BACKEND_PORT}/api"
echo "🗄️  MySQL 地址: localhost:${MYSQL_PORT}"
echo "🔴 Redis 地址: localhost:${REDIS_PORT}"
echo ""
echo "常用命令："
echo "  查看日志: docker compose logs -f"
echo "  停止服务: docker compose down"
echo "  重启服务: docker compose restart"
echo ""
