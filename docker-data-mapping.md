# Docker容器化部署说明

## 项目结构分析

基于项目分析，需要持久化的数据包括：

### 1. 聊天历史数据
- **路径**: `/data/chat-history` (容器内)
- **本地映射**: `./data/chat-history`
- **用途**: 存储MapDB数据库文件，包含所有聊天会话历史
- **重要性**: 高 - 用户聊天数据必须持久化

### 2. 日志文件
- **路径**: `/app/logs` (容器内)
- **本地映射**: `./logs/backend`
- **用途**: 存储应用运行日志
- **重要性**: 中 - 便于故障排查和监控

### 3. Ollama模型数据 (可选)
- **路径**: `/root/.ollama` (容器内)
- **本地映射**: `./data/ollama`
- **用途**: 存储本地AI模型文件
- **重要性**: 低 - 模型可以重新下载

## 磁盘路径映射配置

### docker-compose.yml中的卷映射
```yaml
volumes:
  # 主要数据持久化
  - ./data/chat-history:/data/chat-history
  # 日志文件持久化
  - ./logs/backend:/app/logs
  # Ollama模型数据持久化 (可选)
  - ./data/ollama:/root/.ollama
```

### 本地目录结构
```
chatService/
├── data/
│   ├── chat-history/     # 聊天历史数据库
│   │   └── chat-history.db
│   └── ollama/          # Ollama模型数据 (可选)
├── logs/
│   └── backend/         # 后端日志文件
├── docker-compose.yml
├── Dockerfile.backend
└── Dockerfile.frontend
```

## 数据备份建议

### 1. 聊天历史数据备份
```bash
# 定期备份聊天历史
cp -r data/chat-history backup/chat-history-$(date +%Y%m%d)

# 或者使用tar打包
tar -czf backup/chat-history-$(date +%Y%m%d).tar.gz data/chat-history/
```

### 2. 自动化备份脚本
可以创建定时任务(cron)进行自动备份：
```bash
# 每天凌晨2点备份
0 2 * * * /path/to/backup-script.sh
```

## 数据恢复
```bash
# 恢复聊天历史数据
cp -r backup/chat-history-20240101/* data/chat-history/

# 重启服务
docker-compose restart backend
```

## 注意事项

1. **权限设置**: 确保Docker容器有权限读写映射的本地目录
2. **磁盘空间**: 监控data目录的磁盘使用情况，避免空间不足
3. **定期清理**: 根据配置的磁盘阈值(500MB)自动清理旧数据
4. **数据安全**: 聊天历史包含敏感信息，确保备份数据的安全性