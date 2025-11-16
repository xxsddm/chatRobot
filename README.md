# Chat Service

这是一个基于Spring Boot和LangChain4J构建的聊天微服务，支持与AI模型进行多轮对话，并提供了简单的前端界面。

## 功能特性

- 支持多轮对话
- 会话管理和历史记录
- 兼容OpenAI API格式
- 可配置的模型参数
- 基于Spring Boot的RESTful API
- 简单的Web前端界面

## 技术栈

- Java 21
- Spring Boot 3.2.0
- LangChain4J 1.8.0
- Maven
- HTML/CSS/JavaScript

## 快速开始

1. 克隆项目并进入目录
2. 配置API密钥（见下文）
3. 运行项目：`mvn spring-boot:run`
4. 访问Web界面：`http://localhost:8080`
5. 或者直接使用API：`http://localhost:8080/api/chat`

## 配置

### 环境变量

- `OPENAI_API_KEY`: OpenAI API密钥（必需）
- `OPENAI_BASE_URL`: API基础URL（可选，默认为https://api.openai.com）
- `MODEL_NAME`: 模型名称（可选，默认为gpt-3.5-turbo）

### application.yml

也可以在`src/main/resources/application.yml`中直接配置：

```yaml
chat:
  model:
    provider: openai
    api-key: your-api-key-here
    base-url: https://api.openai.com
    model-name: gpt-3.5-turbo
    temperature: 0.7
    max-tokens: 1000
    timeout: 30s
```

## API使用

### 发送聊天请求

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下你自己"
  }'
```

### 带会话ID的聊天请求

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "your-session-id",
    "message": "你能详细解释一下吗？"
  }'
```

### 获取会话历史

```bash
curl -X GET http://localhost:8080/api/chat/history/{sessionId}
```

### 清除会话历史

```bash
curl -X DELETE http://localhost:8080/api/chat/history/{sessionId}
```

## Web界面

访问 `http://localhost:8080` 可以使用简单的Web聊天界面。界面功能包括：

- 发送消息给AI助手
- 查看聊天历史
- 清除聊天历史
- 自动管理会话ID

## 扩展支持其他模型

目前默认支持OpenAI格式的API，但可以通过修改`ChatModelConfig`类来支持其他兼容OpenAI格式的模型提供商。

## 会话管理

系统使用内存存储会话历史，每个会话通过唯一的sessionId标识。在生产环境中，您可能需要：

1. 将内存存储替换为数据库存储
2. 添加会话过期机制
3. 实现用户认证和授权