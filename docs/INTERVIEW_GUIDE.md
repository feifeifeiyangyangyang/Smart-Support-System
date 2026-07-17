# 面试讲解指南

## 1. 为什么选择单体而不是微服务？

回答：这个项目是实习面试展示项目，业务边界集中在商品订单、物流、知识库、客服会话、RAG 和工单。模块化单体可以减少部署和链路复杂度，把重点放在认证、事务边界、异步任务和检索可靠性上。

代码路径：`server/src/main/java/com/zhifutong/customer`

可深入：什么时候才需要拆微服务？

## 1.1 为什么加入订单和物流？

回答：真实客服不只回答知识库规则，还要查业务数据。比如“什么时候发货”不能只靠 RAG 猜，必须查订单状态、预计发货时间和物流轨迹。所以聊天链路会先判断是否命中订单/商品/物流问题，命中后直接查业务表，未命中再走知识库检索。

代码路径：`application/CommerceApplicationService.java`、`controller/CommerceController.java`

## 2. JWT 为什么还需要 Redis？

回答：JWT 负责证明“用户是谁”，Redis 负责控制“这次登录现在是否还有效”。退出登录时可以删除 Refresh Token，并把 Access Token 的 jti 写入黑名单，实现立即失效。

代码路径：`auth/JwtTokenService.java`、`auth/AuthSessionService.java`

可深入：Redis 故障时认证策略如何降级？

## 3. Access Token 和 Refresh Token 如何分工？

回答：Access Token 放在 Authorization Header，短有效期，用于访问接口。Refresh Token 放在 HttpOnly Cookie，较长有效期，只用于换新 Access Token。

代码路径：`controller/AuthController.java`

可深入：Refresh Token 为什么要轮换？

## 4. 401 和 403 有什么区别？

回答：401 表示没有认证或登录过期；403 表示已经认证，但角色权限不够，例如 CUSTOMER 访问 `/api/v1/admin/**`。

代码路径：`security/SecurityConfig.java`

## 5. 为什么数据库事务不能回滚本地文件和 Qdrant？

回答：数据库事务只能管理数据库连接里的修改，不能自动回滚文件系统和外部向量库。因此上传流程拆成“保存文件 + 数据库任务表 + 异步处理 + 失败补偿”。

代码路径：`application/DocumentApplicationService.java`、`application/DocumentProcessingService.java`

## 6. 文档任务如何防止重复执行？

回答：任务开始前用数据库条件更新把状态从 PENDING/RETRY_WAIT 改成 RUNNING，并检查影响行数。只有更新成功的线程能处理任务。

代码路径：`DocumentProcessingService.claim`

## 7. 为什么低相关度时不调用大模型？

回答：大模型不知道企业内部政策，低相关度时强行调用容易编造答案。系统直接拒答并建议转人工，降低错误回答风险和模型调用成本。

代码路径：`application/ChatApplicationService.java`

## 8. 为什么要保存回答来源快照？

回答：文档未来可能删除或重建，如果只保存 documentId，历史回答无法复盘。`chat_message_source` 保存当时实际使用的片段快照。

代码路径：`entity/ChatMessageSource.java`、`mapper/ChatMessageSourceMapper.java`

## 9. Redis 限流如何保证原子性？

回答：使用 Lua 脚本把 INCR 和首次设置 TTL 放在 Redis 服务端一次执行，避免并发下计数和过期时间不一致。

代码路径：`ratelimit/ChatRateLimiter.java`

## 10. 工单并发修改如何处理？

回答：管理员更新工单时必须携带 lockVersion，SQL 条件包含旧版本号。更新不到一行说明已被别人修改，返回 409。

代码路径：`application/TicketApplicationService.java`

## 11. Testcontainers 和 Mockito 分别测什么？

回答：Mockito 测单个服务的业务分支和异常路径；Testcontainers 更适合验证真实 MySQL、Redis、Flyway 迁移和唯一约束。目前项目没有把 Testcontainers 作为已完成能力写入依赖，后续如果补集成测试，应再加入对应依赖。

## 12. 项目还有哪些不足？

回答：WireMock 外部服务测试、完整 Testcontainers 覆盖还没补齐。前端 Access Token 目前放 localStorage，存在 XSS 风险，生产环境应进一步收敛。
