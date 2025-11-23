@echo off
REM 聊天服务部署脚本 (Windows)

echo 🚀 开始部署聊天服务容器...

REM 检查Docker是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker未安装，请先安装Docker
    exit /b 1
)

REM 检查Docker Compose是否安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose未安装，请先安装Docker Compose
    exit /b 1
)

REM 检查环境变量文件
if not exist .env (
    echo ⚠️  未找到.env文件，使用默认配置
    echo    如需配置API密钥，请复制.env.example为.env并修改
)

REM 清理旧容器
echo 🧹 清理旧容器...
docker-compose down --volumes --remove-orphans 2>nul

REM 创建必要的目录
echo 📁 创建数据目录...
if not exist data\chat-history mkdir data\chat-history
if not exist logs\backend mkdir logs\backend

REM 构建镜像
echo 🔨 构建镜像...
docker-compose build --no-cache
if errorlevel 1 (
    echo ❌ 镜像构建失败
    exit /b 1
)

REM 启动服务
echo 🌟 启动服务...
docker-compose up -d
if errorlevel 1 (
    echo ❌ 服务启动失败
    exit /b 1
)

REM 等待服务启动
echo ⏳ 等待服务启动...
timeout /t 30 /nobreak >nul

REM 检查服务状态
echo 🔍 检查服务状态...
docker-compose ps | findstr "Up" >nul
if errorlevel 1 (
    echo ❌ 服务启动失败，请检查日志：
    echo    docker-compose logs
    exit /b 1
) else (
    echo ✅ 服务启动成功！
    echo.
    echo 🌐 服务地址：
    echo    前端: http://localhost
    echo    后端: http://localhost:8080
    echo    健康检查: http://localhost:8080/actuator/health
    echo.
    echo 📊 查看日志：
    echo    docker-compose logs -f
    echo.
    echo 🛑 停止服务：
    echo    docker-compose down
)

echo.
echo 🎉 部署完成！
pause