#!/bin/bash

# Docker容器化启动脚本
# 用于快速部署聊天服务到远程服务器

set -e

echo "🚀 开始部署聊天服务容器..."

# 检查Docker和Docker Compose是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装，请先安装Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 创建必要的目录
echo "📁 创建数据目录..."
mkdir -p data/chat-history
mkdir -p logs/backend

# 设置目录权限
echo "🔒 设置目录权限..."
chmod 755 data/
chmod 755 logs/

# 可选：设置环境变量
if [ -f ".env" ]; then
    echo "📋 加载环境变量..."
    source .env
else
    echo "⚠️  未找到.env文件，使用默认配置"
fi

# 构建和启动服务
echo "🏗️  构建Docker镜像..."
docker-compose build

echo "🚀 启动服务..."
docker-compose up -d

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 30

# 检查服务状态
echo "🔍 检查服务状态..."
docker-compose ps

# 健康检查
echo "🏥 执行健康检查..."
if curl -f http://localhost:8080/actuator/health &> /dev/null; then
    echo "✅ 后端服务运行正常"
else
    echo "❌ 后端服务可能未完全启动，请稍后检查日志"
fi

if curl -f http://localhost &> /dev/null; then
    echo "✅ 前端服务运行正常"
else
    echo "❌ 前端服务可能未完全启动，请稍后检查日志"
fi

echo ""
echo "🎉 部署完成！"
echo ""
echo "📊 查看日志命令："
echo "   docker-compose logs -f backend"
echo "   docker-compose logs -f frontend"
echo ""
echo "🔄 重启服务命令："
echo "   docker-compose restart"
echo ""
echo "⏹️  停止服务命令："
echo "   docker-compose down"