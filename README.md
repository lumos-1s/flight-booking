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
| 知识库管理后台 | 统计 / 分块列表 / 检索测试 / 新增知识 4 个接口，前端管理面板，知识可持久化 |
| MCP 工具调用 | 实时天气 / 空气质量、GitHub 仓库操作，通过 MCP 标准协议挂载外部能力 |
| 角色约束 | 系统提示词限定客服角色，越界请求被拒绝 |

## 项目结构

```
tuling-flight-booking_all/
├── flight-booking/        # 主应用（Spring Boot，智能客服后端）
├── mcp-stdio-server/      # MCP 天气服务器（STDIO 子进程）
└── app/chatgpt-demo/      # 前端（Vue3 + Vite）
```

## 快速开始

1. 后端：IDEA 打开 `flight-booking`，在 Run Configuration 的 Environment variables 配置：

```
TULING_AI_API_KEY=sk-xxx                 # 聊天 + Embedding 模型（阿里云百炼）
GITHUB_PERSONAL_ACCESS_TOKEN=xxx         # GitHub MCP 用（可选）
```

2. 前端：

```bash
cd app/chatgpt-demo
npm install
npm run dev
```

3. 浏览器访问 `http://localhost:5173`，输入 `message` 开始对话

## 知识库管理后台

左侧抽屉内置管理面板，对应后端接口：

| 接口 | 作用 |
|---|---|
| `GET /kb/stats` | 分块数 / 总字符统计 |
| `GET /kb/documents` | 全部分块列表 |
| `GET /kb/search?q=行李超重&topK=3` | RAG 检索，返回命中分块 |
| `POST /kb/add` | 新增知识（入库 + 写入源文件，重启不丢） |

启动日志输出 `RAG知识库加载完成，共 N 个分块`，分块内容与命中结果均可可视化验证。

## 演示效果

- 问"北京天气怎么样" → 调用 MCP 工具返回实时天气
- 问"行李超重怎么收费" → 命中知识库，回答附「信息来源」标注
- 换窗口提问 → 会话相互隔离
- 知识库面板检索"行李超重" → 命中分块原文折叠展示
- 面板新增一条规则 → 分块数 +1，重启后端后仍保留
