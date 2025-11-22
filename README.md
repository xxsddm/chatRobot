# Chat Service - 智能聊天服务

基于Spring Boot + LangChain4J + Vue 3构建的智能聊天服务应用。

## 项目结构

```
chatService/
├── src/main/java/              # Spring Boot后端代码
├── src/main/resources/         # 后端资源配置
│   └── static/                 # 前端静态资源（构建后）
├── frontend/                   # Vue 3前端项目
│   ├── src/
│   │   ├── components/         # Vue组件
│   │   ├── stores/           # Pinia状态管理
│   │   └── views/            # 页面视图
│   └── package.json          # 前端依赖配置
├── build-frontend.bat        # Windows前端构建脚本
├── build-frontend.sh         # Linux/Mac前端构建脚本
└── pom.xml                   # Maven配置
```

## 技术栈

### 后端
- **Spring Boot 3.5** - 后端框架
- **LangChain4J 1.8** - AI集成框架
- **OpenAI API** - AI模型服务
- **Reactor** - 响应式编程
- **Java 21** - 编程语言

### 前端
- **Vue 3** - 前端框架
- **TypeScript** - 类型安全的JavaScript
- **Element Plus** - UI组件库
- **Pinia** - 状态管理
- **Vite** - 构建工具

## 快速开始

### 1. 启动后端服务

```bash
# 编译项目
mvn clean compile

# 运行Spring Boot应用
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动

### 2. 启动前端开发服务器

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将在 http://localhost:3000 启动，并代理API请求到后端服务

### 3. 构建完整应用

#### 构建前端
```bash
# Windows
build-frontend.bat

# Linux/Mac
bash build-frontend.sh
```

#### 构建整个项目
```bash
# 先构建前端，再构建后端
build-frontend.bat  # 或 .sh
mvn clean package
```

## 功能特性

### 聊天功能
- 💬 智能对话
- 🔄 流式响应
- 💾 会话历史管理
- 📱 响应式界面

### API端点
- `POST /api/chat` - 发送消息（非流式）
- `POST /api/chat/stream` - 发送消息（流式响应）
- `DELETE /api/chat/history/{sessionId}` - 清除会话历史

## 配置说明

### 后端配置
在 `src/main/resources/application.yml` 中配置：
- OpenAI API密钥
- 服务器端口
- 其他应用参数

### 前端配置
在 `frontend/vite.config.ts` 中配置：
- API代理地址
- 开发服务器端口
- 构建选项

## 开发建议

1. **分离开发** - 前后端可以独立开发和测试
2. **API优先** - 先定义好API接口，再并行开发
3. **类型安全** - 前后端都使用TypeScript提高代码质量
4. **组件化** - 前端采用组件化开发模式

## 部署

### 独立部署
- 后端：打包成JAR文件运行
- 前端：构建后的静态文件部署到Nginx或Apache

### 集成部署
使用构建脚本将前端构建到后端的静态资源目录，统一部署

## 贡献指南

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 许可证

MIT License