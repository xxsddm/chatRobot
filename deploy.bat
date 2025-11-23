@echo off
REM Docker容器化启动脚本（Windows版本）
REM 用于快速部署聊天服务到远程服务器

echo 🚀 开始部署聊天服务容器...

REM 检查Docker是否安装
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker未安装，请先安装Docker
    exit /b 1
)

REM 检查Docker Compose是否安装
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Docker Compose未安装，请先安装Docker Compose
    exit /b 1
)

REM 创建必要的目录
echo 📁 创建数据目录...
if not exist "data\chat-history" mkdir "data\chat-history"
if not exist "data\ollama" mkdir "data\ollama"
if not exist "logs\backend" mkdir "logs\backend"
if not exist "backup" mkdir "backup"

REM 可选：设置环境变量
if exist ".env" (
    echo 📋 加载环境变量...
    REM 这里可以添加加载环境变量的逻辑
)

REM 构建和启动服务
echo 🏗️  构建Docker镜像...
docker-compose build
if %errorlevel% neq 0 (
    echo ❌ Docker镜像构建失败
    exit /b 1
)

echo 🚀 启动服务...
docker-compose up -d
if %errorlevel% neq 0 (
    echo ❌ 服务启动失败
    exit /b 1
)

REM 等待服务启动
echo ⏳ 等待服务启动...
timeout /t 30 /nobreak >nul

REM 检查服务状态
echo 🔍 检查服务状态...
docker-compose ps

REM 健康检查
echo 🏥 执行健康检查...
curl -f http://localhost:8080/actuator/health >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 后端服务运行正常
) else (
    echo ❌ 后端服务可能未完全启动，请稍后检查日志
)

curl -f http://localhost >nul 2>&1
if %errorlevel% equ 0 (
    echo ✅ 前端服务运行正常
) else (
    echo ❌ 前端服务可能未完全启动，请稍后检查日志
)

echo.
echo 🎉 部署完成！
echo 📋 服务访问地址：
echo    前端: http://localhost
echo    后端: http://localhost:8080
echo    API文档: http://localhost:8080/swagger-ui.html
echo.
echo 📊 查看日志命令：
echo    docker-compose logs -f backend
echo    docker-compose logs -f frontend
echo.
echo 🔄 重启服务命令：
echo    docker-compose restart
echo.
echo ⏹️  停止服务命令：
echo    docker-compose down

pause