# 聊天服务容器化部署指南

## 概述
本项目包含前端和后端服务，使用Docker和Docker Compose进行容器化部署。

## 项目结构
```
chatService/
├── backend/                 # 后端服务
│   ├── Dockerfile          # 后端Dockerfile
│   └── ...                 # 后端代码
├── frontend/               # 前端服务
│   ├── Dockerfile.frontend # 前端Dockerfile
│   ├── nginx.conf         # Nginx配置
│   └── ...                # 前端代码
├── docker-compose.yml     # Docker Compose配置
├── deploy.sh             # Linux/macOS部署脚本
├── deploy.bat            # Windows部署脚本
└── .env.example          # 环境变量模板
```

## 快速开始

### 1. 环境要求
- Docker 20.10+
- Docker Compose 1.29+

### 2. 配置环境变量
复制环境变量模板并修改：
```bash
cp .env.example .env
# 编辑.env文件，填入你的API密钥
```

### 3. 部署服务

#### Windows
```cmd
deploy.bat
```

#### Linux/macOS
```bash
chmod +x deploy.sh
./deploy.sh
```

#### 手动部署
```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

## 服务说明

### 端口映射
- 前端服务：80 → 容器80
- 后端服务：8080 → 容器8080

### 数据持久化
- 聊天记录：`./data/chat-history`
- 日志文件：`./logs/backend`

### 健康检查
- 后端健康检查：`http://localhost:8080/actuator/health`
- 前端健康检查：`http://localhost`

## 环境变量配置

### OpenAI配置
```env
OPENAI_API_KEY=your_api_key_here
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL_NAME=gpt-3.5-turbo
```

### Ollama配置（可选）
```env
OLLAMA_BASE_URL=http://ollama:11434
OLLAMA_MODEL_NAME=llama2
```

### 模型参数
```env
MODEL_TEMPERATURE=0.7
MODEL_TIMEOUT=30
PRIMARY_MODEL=default
```

## 常用命令

### 查看服务状态
```bash
docker-compose ps
```

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 查看后端日志
docker-compose logs -f backend

# 查看前端日志
docker-compose logs -f frontend
```

### 重启服务
```bash
docker-compose restart
```

### 停止服务
```bash
docker-compose down

# 同时删除数据卷
docker-compose down --volumes
```

### 重新构建
```bash
docker-compose build --no-cache
docker-compose up -d
```

## 故障排查

### 服务启动失败
1. 检查端口是否被占用
2. 查看服务日志：`docker-compose logs`
3. 检查环境变量配置

### 健康检查失败
1. 等待服务完全启动（约30-60秒）
2. 检查服务日志
3. 验证网络连接

### 数据持久化问题
1. 检查目录权限
2. 确保挂载目录存在
3. 检查Docker卷配置

## 安全建议

1. **API密钥安全**：不要将.env文件提交到版本控制
2. **网络安全**：在生产环境中使用HTTPS
3. **访问控制**：配置适当的防火墙规则
4. **日志管理**：定期清理日志文件

## 性能优化

### 资源限制
后端服务默认限制：
- 内存：1GB
- CPU：1.0核

### 前端优化
- 静态资源缓存1年
- Gzip压缩启用
- CDN加速（生产环境）

## 更新维护

### 更新服务
1. 拉取最新代码
2. 重新构建镜像：`docker-compose build --no-cache`
3. 重启服务：`docker-compose up -d`

### 备份数据
```bash
# 备份聊天记录
cp -r data/chat-history backup/chat-history-$(date +%Y%m%d)

# 备份日志
cp -r logs backup/logs-$(date +%Y%m%d)
```

## 支持

如有问题，请检查日志或提交Issue。