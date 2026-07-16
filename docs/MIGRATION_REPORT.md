# 旧 Python 项目迁移审计报告

## 1. 原项目整体结构

原项目是一个 FastAPI 企业知识库问答系统，主要目录如下：

```text
实训/
├── app/main.py
├── app/backend/config.py
├── app/backend/model/models.py
├── app/backend/service/ChatService.py
├── app/backend/service/DocumentService.py
├── app/backend/service/VectorStoreService.py
├── app/web/templates/index.html
├── app/web/static/js/app.js
├── app/web/static/css/styles.css
├── pyproject.toml
└── test_app.py
```

审计时没有读取旧 `.env` 内容；旧项目中的真实密钥不应迁移、恢复或复用。

## 2. 原项目调用链

1. `app/main.py` 提供 FastAPI 路由。
2. 上传接口校验扩展名后，将文件保存到配置中的本地目录。
3. `DocumentService` 使用 PyPDF2、python-docx、markdown 提取文本。
4. `VectorStoreService` 使用 LangChain text splitter 切分文本。
5. `VectorStoreService` 使用 ChromaDB SentenceTransformer embedding 写入向量库，并维护内存 BM25 索引。
6. `/api/chat` 接收用户问题和 `thread_id`。
7. `ChatService` 调用 `VectorStoreService.search` 检索相关片段。
8. `ChatService` 拼接参考文档上下文，用 LangChain `ChatOpenAI` 调用 OpenAI-compatible API。
9. 接口返回回答和来源，但会话消息没有真正持久化。

## 3. Python 到 Java 模块映射

| Python 模块 | Java 模块 |
|---|---|
| `app/main.py` | `controller/*Controller.java` |
| `config.py` | `application.yml` + `AppProperties.java` |
| `DocumentService.py` | `DocumentApplicationService.java` + `DocumentParser.java` + `FileValidator.java` |
| `VectorStoreService.py` | `TextChunker.java` + `EmbeddingClient.java` + `QdrantVectorStore.java` |
| `ChatService.py` | `ChatApplicationService.java` + `PromptBuilder.java` + `ChatModelClient.java` |
| `models.py` | `entity/*` + `dto/*` + `vo/*` |
| `index.html` / `app.js` | Vue 3 `web/src/views/*` |
| SQLite 初始化 | Flyway `V1__init_schema.sql` |

## 4. 原项目已识别问题

- `config.py` 中存在硬编码 API Key；报告中已做掩码，未记录完整 Key。
- 配置中存在 `C:/Users/23180/...` 这类开发者本机绝对路径。
- 同时使用 SQLite 原生访问和 SQLAlchemy 建模，数据访问方式混用。
- 启动时打印 API Key 前缀，存在泄露风险。
- 前端下载功能存在模拟下载文本和未定义变量风险。
- 前端来源字段与后端字段命名不统一。
- `thread_id` 没有保存为会话记录。
- 文件大小配置存在，但上传校验不完整。
- 删除文档时没有完整确认数据库、向量库和磁盘文件状态一致。
- 上传和向量化失败时缺少可靠补偿。
- `.venv`、`.idea`、缓存、本地数据库被打进压缩包。
- 原压缩包体积过大，不适合作为源码仓库。

## 5. 复用与重写边界

可复用的是业务思路：上传文档、提取文本、切分、检索、基于知识库回答。

必须重写的是实现方式：配置安全、数据库设计、会话持久化、工单、前端架构、向量库、异常处理、测试和部署配置。新项目不依赖旧 Python 项目运行。

## 6. 安全处理

新项目只提交 `.env.example`，真实 `.env` 被 `.gitignore` 排除。首次真实调用大模型前必须由用户提供新的 `LLM_API_KEY`；旧 Key 已失效，不读取、不恢复、不迁移。
