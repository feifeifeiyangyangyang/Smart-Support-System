# 交付验证报告

## 1. 本次交付范围

本项目位于 `smart-customer-service/`，包含：

- `server/`：Spring Boot 后端源码、Flyway 迁移、自动化测试、Maven Wrapper。
- `web/`：Vue 3 + TypeScript 前端源码。
- `deploy/`：Docker Compose、后端/前端 Dockerfile、Nginx 配置、模型检查和下载脚本。
- `docs/`：迁移报告、面试说明、重构计划和简历描述。
- `sample-data/knowledge/`：演示知识库 Markdown。
- `.env.example`、`.gitignore`、`README.md`。

## 2. 已验证命令

```powershell
cd server
.\mvnw.cmd -q test
.\mvnw.cmd -q package -DskipTests

cd ..\web
npm ci
npm run build

cd ..
docker compose -f deploy\docker-compose.yml config
```

## 3. 当前实现状态

- 已实现真实后端登录、Spring Security 鉴权、JWT Access Token、Redis Refresh Token、退出黑名单。
- 已实现用户端和管理端角色隔离，前端登录不再是演示状态。
- 已实现知识库文档上传、SHA-256 去重、异步任务、文本切片持久化和 Qdrant 写入。
- 已实现 RAG 检索、低相关拒答、回答来源快照保存。
- 已实现聊天 Redis 限流，超限请求不会继续调用 Embedding、Qdrant 或大模型。
- 已实现工单状态机、管理员处理、操作日志和乐观锁并发保护。
- 已补充 Docker Compose 健康检查、Actuator health/readiness 和 Maven Wrapper。

## 4. 安全说明

- `.env` 已被 `.gitignore` 排除，不会提交到仓库。
- `.env.example` 只保留占位配置和 Mock 默认值。
- 真实 API Key 只能放在本地 `.env`，不能写入源码、模板、README、测试文件、日志或交付内容。
- 当前自动化测试和构建默认可以在 Mock ChatModel 下完成；真实大模型调用需要本地提供有效 `LLM_API_KEY` 后再验证。

## 5. 尚未完成的生产级能力

- 未补齐完整 Testcontainers 集成测试。
- 未接入 WireMock 外部服务契约测试。
- 未实现生产级密钥管理、审计系统、多租户和消息推送。
- 未接入真实订单、支付、物流系统。
