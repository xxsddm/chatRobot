# 聊天服务Docker容器化部署指南

## 🎯 快速开始

### 一键部署（推荐）

#### Linux/Mac:
```bash
chmod +x deploy.sh
./deploy.sh
```

#### Windows:
```cmd
deploy.bat
```

### 手动部署步骤

1. **构建镜像**
```bash
docker-compose build
```

2. **启动服务**
```bash
docker-compose up -d
```

3. **查看状态**
```bash
docker-compose ps
```

4. **查看日志**
```bash
docker-compose logs -f backend
docker-compose logs -f frontend
```

## 📋 服务配置

### 端口映射
- **前端服务**: 80 → localhost:80
- **后端服务**: 8080 → localhost:8080
- **Ollama服务**: 11434 → localhost:11434

### 资源限制

| 服务 | CPU限制 | 内存限制 | CPU预留 | 内存预留 |
|------|---------|----------|---------|----------|
| 后端 | 2.0核 | 1GB | 0.5核 | 256MB |
| 前端 | 0.5核 | 128MB | 0.1核 | 32MB |
| Ollama | 4.0核 | 4GB | 1.0核 | 512MB |

### 数据持久化

#### 重要数据路径
1. **聊天历史**: `./data/chat-history/` → `/data/chat-history/`
2. **应用日志**: `./logs/backend/` → `/app/logs/`
3. **Ollama模型**: `./data/ollama/` → `/root/.ollama/`

#### 备份建议
```bash
# 创建备份目录
mkdir -p backup

# 备份聊天历史
tar -czf backup/chat-history-$(date +%Y%m%d).tar.gz data/chat-history/

# 备份Ollama模型（如果需要）
tar -czf backup/ollama-models-$(date +%Y%m%d).tar.gz data/ollama/
```

## 🔧 环境变量配置

### 创建 .env 文件（可选）
```bash
# OpenAI API配置
OPENAI_API_KEY=your-api-key-here
OPENAI_BASE_URL=https://open.bigmodel.cn/api/paas/v4
OPENAI_MODEL_NAME=glm-4.5-flash

# Ollama配置
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_MODEL_NAME=qwen3:1.7b

# 应用配置
MAPDB_MAX_SESSIONS=1000
MAPDB_DISK_THRESHOLD_MB=500
MODEL_TEMPERATURE=0.7
MODEL_TIMEOUT_SECOND=30
```

## 🚀 常用命令

### 服务管理
```bash
# 启动服务
docker-compose up -d

# 停止服务
docker-compose down

# 重启服务
docker-compose restart

# 重新构建并启动
docker-compose down && docker-compose build && docker-compose up -d
```

### 日志查看
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f ollama

# 查看最近100行日志
docker-compose logs --tail=100 backend
```

### 容器操作
```bash
# 进入容器
docker-compose exec backend bash
docker-compose exec frontend sh

# 查看容器资源使用
docker stats

# 清理无用镜像和容器
docker system prune -f
```

## 🔍 健康检查

### 服务状态检查
```bash
# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查前端服务
curl http://localhost

# 检查Ollama服务
curl http://localhost:11434/api/tags
```

### 数据库检查
```bash
# 进入后端容器检查数据库
docker-compose exec backend bash
ls -la /data/chat-history/
du -sh /data/chat-history/
```

## 🛠️ 故障排查

### 常见问题

1. **端口冲突**
   - 检查端口是否被占用：`netstat -an | grep :8080`
   - 修改docker-compose.yml中的端口映射

2. **权限问题**
   - 确保数据目录权限：`chmod -R 755 data/`
   - 检查Docker用户权限

3. **内存不足**
   - 调整资源限制
   - 增加系统内存或清理其他容器

4. **构建失败**
   - 检查网络连接
   - 清理Docker缓存：`docker system prune -a`

### 日志分析
```bash
# 查看错误日志
docker-compose logs | grep ERROR
docker-compose logs | grep WARN

# 实时监控
docker-compose logs -f --tail=100
```

## 📊 性能监控

### 资源使用监控
```bash
# 实时资源监控
docker stats

# 查看容器详情
docker inspect chat-service-backend
docker inspect chat-service-frontend
```

### 数据库性能
- 监控磁盘使用：`du -sh data/chat-history/`
- 查看会话数量：通过API接口获取

## 🔒 安全建议

1. **API密钥保护**
   - 使用环境变量传递敏感信息
   - 不要将密钥提交到代码仓库

2. **网络安全**
   - 在生产环境中使用HTTPS
   - 配置防火墙规则

3. **数据安全**
   - 定期备份重要数据
   - 加密敏感数据

## 📞 支持

如果遇到问题，请检查：
1. 服务日志：`docker-compose logs`
2. 系统资源：`docker stats`
3. 网络连接：`curl`测试各个端点
4. 配置文件：检查`docker-compose.yml`和`.env`文件

## 🔄 更新和升级

### 更新代码后重新部署
```bash
# 拉取最新代码
git pull origin main

# 重新构建和部署
./deploy.sh
```

### 零停机更新
```bash
# 滚动更新
docker-compose up -d --no-deps --build backend
docker-compose up -d --no-deps --build frontend
```