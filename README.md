# 图灵航空 · AI 智能客服系统

基于 Spring AI 的航空领域智能客服，支持流式对话、多轮会话隔离、RAG 知识库检索与 MCP 外部工具调用。

## 技术栈

- 后端：Spring Boot 4 + Spring AI 2.0（DeepSeek / Qwen 双模型）
- 向量检索：SimpleVectorStore + Embedding 模型（RAG）
- 协议：MCP（天气/空气质量外部工具）、SSE（流式响应）
- 前端：Vue 3 + Vite + Element Plus

## 核心能力

| 能力 | 说明 |
|---|---|
| SSE 流式对话 | 打字机式流式输出，前端 EventSource 实时接收 |
| 多轮会话隔离 | 前端持久化 userId，后端按 conversationId 分槽记忆，用户间上下文隔离 |
| Function Calling | 查票、退票等业务工具，增删改前需用户确认 |
| RAG 检索增强 | 业务规则文档切片（250 token/块）→ 向量化入库 → 检索注入，回答附出处引用 |
| MCP 工具调用 | 实时天气 / 空气质量，通过 MCP 标准协议挂载外部能力 |
| 角色约束 | 系统提示词限定客服角色，越界请求被拒绝 |

## 项目结构

```
tuling-flight-booking_all/
├── flight-booking/        # 主应用（Spring Boot，智能客服后端）
├── mcp-stdio-server/      # MCP 天气服务器（STDIO 子进程）
└── app/chatgpt-demo/      # 前端（Vue3 + Vite）
```

## 快速开始

1. 后端：IDEA 打开 `flight-booking`，配置环境变量 `TULING_AI_API_KEY` 后运行
2. 前端：

```bash
cd app/chatgpt-demo
npm install
npm run dev
```

3. 浏览器访问 `http://localhost:5173`，输入 `message` 开始对话

## 演示效果

- 问"北京天气怎么样" → 调用 MCP 工具返回实时天气
- 问"行李超重怎么收费" → 命中知识库，回答附「信息来源」标注
- 换窗口提问 → 会话相互隔离
