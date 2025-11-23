#!/bin/bash

# Docker容器化部署脚本
# 用于快速部署聊天服务到远程服务器

set -e

# 创建必要的目录
echo "📁 创建数据目录..."
mkdir -p data/chat-history
mkdir -p logs/backend

# 设置目录权限
echo "🔒 设置目录权限..."
chmod 755 data/
chmod 755 logs/

# 构建镜像
echo "🔨 构建镜像..."
docker-compose build --no-cache

# 启动服务
echo "🌟 启动服务..."
docker-compose up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo "🔍 检查服务状态..."
if docker-compose ps | grep -q "Up"; then
    echo "✅ 服务启动成功！"
    echo ""
    echo "🌐 服务地址："
    echo "   前端: http://localhost"
    echo "   后端: http://localhost:8080"
    echo "   健康检查: http://localhost:8080/actuator/health"
    echo ""
    echo "📊 查看日志："
    echo "   docker-compose logs -f"
    echo ""
    echo "🛑 停止服务："
    echo "   docker-compose down"
else
    echo "❌ 服务启动失败，请检查日志："
    echo "   docker-compose logs"
    exit 1
fi