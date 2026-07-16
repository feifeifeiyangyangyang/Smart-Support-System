# 交付验证报告

## 1. 创建和修改的主要内容

新项目位于 `smart-customer-service/`，包含：

- `server/`：Spring Boot 后端源码、Flyway 迁移、自动化测试。
- `web/`：Vue 3 + TypeScript 前端源码。
- `deploy/`：Docker Compose、后端/前端 Dockerfile、Nginx 配置、模型检查和下载脚本。
- `docs/MIGRATION_REPORT.md`：旧 Python 项目迁移审计报告。
- `sample-data/knowledge/`：演示知识库 Markdown。
- `.env.example`、`.gitignore`、`README.md`。

## 2. 已验证命令

```bash
mvn -q test
mvn -q clean package
npm run build
docker compose -f deploy/docker-compose.yml up -d mysql qdrant
```

接口验证：

- `GET /api/v1/health`
- `POST /api/v1/admin/documents`
- `GET /api/v1/admin/documents`
- `GET /api/v1/admin/documents/{id}/download`
- `POST /api/v1/chat`
- `POST /api/v1/tickets`
- `GET /api/v1/admin/tickets`
- `PATCH /api/v1/admin/tickets/{id}/status`

## 3. 真实链路验证结果

已完成真实端到端验证：

- MySQL 8.4 Docker 容器，端口 `3307`。
- Qdrant Docker 容器，端口 `6333`。
- 本地 ONNX embedding，`embeddingMockEnabled=false`。
- 上传 Markdown 文档成功，状态 `COMPLETED`，向量写入 Qdrant。
- `POST /api/v1/chat` 成功返回答案、来源片段、检索分数和置信等级。
- 使用本地 `.env` 中的新 Key 调用真实 DeepSeek/OpenAI-compatible Chat API，模型为 `deepseek-v4-flash`。

验证过程中没有在源码、配置模板、README、日志、测试文件或交付内容中输出 API Key。

## 4. 测试结果

- `mvn -q test`：通过。
- 后端覆盖文本切分、文件校验、文件名安全、置信等级、Prompt 组装、文档状态、工单状态、聊天服务拒答和模型调用分支。
- `mvn -q clean package`：通过。
- `npm run build`：通过。Vite 有 Element Plus chunk size warning，不影响可用性。

## 5. 已完成功能

- MySQL + Flyway 自动建表。
- Qdrant collection 创建、文档向量写入和检索。
- 文档上传、解析、切分、下载、列表、删除、失败重试。
- 会话创建、消息保存、消息清空。
- 客服问答、来源返回、低相关拒答、转人工判断。
- 工单创建、列表、状态处理。
- 前端客服页面和管理后台调用真实 Java API。
- `.env` 已被 `.gitignore` 排除，`.env.example` 只包含占位符。

## 6. 未纳入 MVP 的功能

- OCR。
- 真实订单、支付、权限系统。
- 复杂报表、多租户、消息推送。

## 7. 已知说明

- 本机 `3306` 和 `8080` 被占用，验证时 MySQL 使用 `3307`，后端使用 `18080`。
- PowerShell 显示中文接口响应时可能乱码，这是终端编码显示问题。
- Flyway 提示 MySQL 8.4 新于当前已测试版本，是兼容性警告；迁移已成功执行。
- 本地验证使用 `sentence-transformers/all-MiniLM-L6-v2` 的 ONNX 模型，偏英文；正式中文客服效果建议换中文或多语言 embedding 模型。
