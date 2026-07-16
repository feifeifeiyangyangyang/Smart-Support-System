# 智服通 - 企业智能客服问答与人工协同系统

智服通是从现有 Python 企业知识库问答项目重构而来的 Java + Vue 前后端分离项目，面向电商售前、物流、退款、退货、账号和售后服务场景。

## 系统架构

```text
web(Vue 3 + Element Plus)
        |
        v
server(Spring Boot)
        |
        +-- MySQL: 文档、会话、消息、工单
        +-- Qdrant: 文本片段向量
        +-- Local ONNX Embedding: 本地生成向量
        +-- OpenAI-compatible Chat API: 生成最终回答
```

核心链路：

上传文件 -> 校验文件 -> 保存原始文件 -> MySQL 记录文档 -> 解析文本 -> 清洗切分 -> 生成 Embedding -> 写入 Qdrant -> 用户提问 -> 问题 Embedding -> Qdrant 检索 -> 过滤低相关结果 -> 拼接 Prompt -> 调用 Chat API -> 保存消息 -> 返回答案和来源。

## 技术栈

- 后端：Java 17、Spring Boot 3、Maven、MyBatis-Plus、MySQL、Flyway、Qdrant、ONNX Runtime、DJL HuggingFace Tokenizers、PDFBox、POI、CommonMark、JUnit 5、Mockito、Springdoc OpenAPI
- 前端：Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios、Element Plus
- 基础设施：Docker Compose、MySQL 8、Qdrant

## 安全与配置

复制 `.env.example` 为本地 `.env`，但不要提交 `.env`。

真实调用大模型前，必须由用户提供新的 `LLM_API_KEY`。旧项目中的 API Key 已删除并失效，不读取、不恢复、不迁移。用户提供的新 Key 只能写入本地 `.env`，不得写入源码、配置模板、README、日志、测试文件或最终交付内容。

`.env.example` 默认使用 Mock ChatModel，不需要 API Key 也能完成编译、启动和基础演示。需要真实调用时，在本地 `.env` 中设置 `LLM_MOCK_ENABLED=false` 并填写自己的 `LLM_API_KEY`。

当前默认 Chat 模型名为 `deepseek-v4-flash`，通过 OpenAI-compatible 接口调用。

## 模型准备

默认 embedding 文件路径：

```text
./models/embedding/model.onnx
./models/embedding/tokenizer.json
```

可用脚本下载一个 384 维、最大长度 256 的本地 ONNX embedding 模型：

```powershell
.\deploy\download-embedding-model.ps1
```

```bash
sh deploy/download-embedding-model.sh
```

说明：脚本默认下载 `sentence-transformers/all-MiniLM-L6-v2` 的 ONNX 文件，已用于本项目本地推理验证。它偏英文，正式中文客服场景建议替换为中文或多语言 embedding 模型，并同步调整 `EMBEDDING_DIMENSION`。

检查模型文件：

```powershell
.\deploy\check-embedding-model.ps1
```

```bash
sh deploy/check-embedding-model.sh
```

## 启动依赖

```bash
docker compose -f deploy/docker-compose.yml up -d mysql qdrant
```

本机如果已有 MySQL 占用 `3306`，Docker Compose 默认把容器 MySQL 映射到本机 `3307`。

## 启动后端

推荐在项目根目录使用本地启动脚本，它会读取 `.env` 并把相对路径按项目根目录解析：

```powershell
.\deploy\run-server-local.ps1
```

接口文档：

```text
http://127.0.0.1:18080/swagger-ui.html
```

## 启动前端

```bash
cd web
npm install
npm run dev
```

前端开发代理默认指向 `http://127.0.0.1:18080`。如需改后端地址，可设置 `VITE_API_TARGET`。

访问：

```text
用户端登录：http://127.0.0.1:5173/user/login
管理端登录：http://127.0.0.1:5173/admin/login
```

演示账号：

- 用户端：`user / 123456`
- 管理端：`admin / admin123`

## 测试与构建

```bash
cd server
mvn clean test
mvn clean package
```

```bash
cd web
npm run build
```

## 演示步骤

1. 启动 MySQL 和 Qdrant。
2. 配置本地 `.env`。
3. 准备 embedding 模型，或开发测试时设置 `EMBEDDING_MOCK_ENABLED=true`。
4. 启动后端和前端。
5. 在管理后台上传 `sample-data/knowledge/` 中的演示 Markdown。
6. 在客服端提问。
7. 检索资料不足时，系统会拒答并建议转人工工单。

演示知识库已内置 3 个商品资料：

- 暖风杯 H100：小家电，通常 48 小时内发货。
- 轻氧洗面巾 C20：个护耗材，拆封后通常不支持无理由退货。
- 云感靠枕 P9：居家纺织品，未清洗未使用且包装完整时可提交退货申请。

## 已完成状态

已完成：

- Spring Boot 后端、Flyway 表结构、统一响应和异常处理。
- 文档上传、解析、切分、状态管理、下载、删除、失败重试。
- 本地 ONNX embedding 推理，向量归一化，写入 Qdrant。
- 会话、消息和工单持久化。
- RAG 检索、低相关拒答、来源返回、真实 Chat API 调用。
- Vue 客服端和管理后台，前端调用真实 Java API。
- Docker Compose、Dockerfile、Nginx 配置、模型检查和下载脚本。
- 后端测试、前端构建、真实 MySQL + Qdrant + ONNX + dsv4 端到端验证。

未实现：

- OCR、真实订单系统、支付系统、权限系统、复杂报表、多租户、消息推送。这些不属于本次 MVP 范围。

## 常见问题

- 没有 `LLM_API_KEY`：系统使用 Mock ChatModel 完成编译和自动化测试，真实大模型调用不验证。
- 没有本地 ONNX 模型：文档向量化会明确失败，不会自行生成随机向量。
- Qdrant 未启动：上传文档写向量会失败，文档状态会更新为 `FAILED`。
- PowerShell 显示中文响应乱码：这是终端编码显示问题，不代表接口 JSON 内容错误。

## 原项目迁移关系

见 `docs/MIGRATION_REPORT.md`。
