# 智服通面试版重构计划

## 当前真实实现

- 后端是 Spring Boot 3.3.5 模块化单体应用，包名为 `com.zhifutong.customer`。
- 前端是 Vue 3 + TypeScript + Element Plus。
- 已有能力包括：文档上传、PDF/DOCX/Markdown/TXT 解析、文本切分、ONNX Embedding、Qdrant 检索、OpenAI-compatible ChatModelClient、低相关度拒答、会话、人工工单、用户端和管理端页面。
- 旧版本登录主要由前端本地判断账号密码，不是真正安全认证。
- 旧版本文档上传在同步流程中完成保存、解析、Embedding 和 Qdrant 写入，事务边界不够清晰。
- 旧版本关键词检索会读取原始知识文档，不适合作为正式检索数据源。

## 当前问题

- 缺少后端强制认证与角色权限边界。
- 缺少 Access Token、Refresh Token、Redis 黑名单和退出登录后的立即失效机制。
- 缺少用户会话、工单与登录用户的强归属约束。
- 文档处理不是可恢复任务模型，服务重启后任务恢复能力不足。
- RAG 来源主要通过 JSON 快照返回，尚未形成结构化来源表。
- 工单状态流转、操作日志和并发冲突控制还不完整。
- README、面试讲解文档、Docker Compose 健康检查和测试覆盖需要补强。

## 准备修改的模块

- `auth` / `security`：Spring Security、JWT、Redis Refresh Token、黑名单、注册、登录、刷新、退出、当前用户。
- `conversation`：会话归属到用户，普通用户只能访问自己的会话。
- `ticket`：工单归属、状态机、处理人、处理结果、操作日志和乐观锁。
- `document` / `task`：上传事务、文档处理任务表、异步执行、重试、恢复和 Qdrant 幂等写入。
- `rag`：持久化知识片段、结构化消息来源、避免提问时重新解析原始文档。
- `web`：真实登录、Token 刷新、401 处理、用户端工单状态展示、管理端处理工单。
- `deploy`：Redis、健康检查、模型只读挂载、Docker Compose 配置验证。
- `docs`：README、面试指南和简历描述。

## 数据库迁移方案

- 不修改已有 `V1__init_schema.sql`。
- 所有结构变化通过新的 Flyway migration 追加。
- 阶段 1 增加用户认证表、会话归属字段和基础工单归属字段。
- 阶段 2 增加文档处理任务表、文档 SHA-256、知识片段表。
- 阶段 3 增加消息来源表、工单操作日志和乐观锁字段。

## 测试计划

- 阶段 1：认证服务单元测试、MockMvc 安全测试、前端 build、docker compose config。
- 阶段 2：文档 SHA-256、任务抢占、重试、失败补偿和 Qdrant 失败测试。
- 阶段 3：RAG 低相关度不调用大模型、来源持久化、工单状态机和并发冲突测试。
- 阶段 4：Testcontainers、WireMock、Docker Compose、README 和面试文档验证。

## 不能虚构的功能边界

- 不声称已经支持微服务、网关、Nacos、Seata、Kubernetes 或 RabbitMQ。
- 不声称系统已达到生产级高并发或大量用户验证。
- 不声称模型准确率、业务收益或延迟优化比例，除非有真实测试数据。
- 不把检索分数描述为模型准确率，只使用 retrievalScore 和 confidenceLevel。
- 不把本地演示账号描述为正式生产账号体系。
- 不在仓库中保存真实 API Key、真实 `.env`、模型大文件、上传文件或数据库数据。
