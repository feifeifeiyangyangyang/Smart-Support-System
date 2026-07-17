# 简历描述

## 项目标题

智服通：企业知识库客服与人工工单协同平台

## 技术栈

Java 17、Spring Boot 3.3.5、Spring Security、MyBatis-Plus、MySQL、Redis、Flyway、Qdrant、ONNX Runtime、WebClient、Vue 3、TypeScript、Element Plus、Docker Compose

## 项目描述

- 基于 Spring Boot 模块化单体实现企业知识库客服系统，包含用户咨询、知识库管理、RAG 问答、引用溯源和人工工单处理。
- 使用 Spring Security + JWT + Redis 实现登录认证、Refresh Token、退出黑名单和管理员接口权限控制。
- 将文档上传改造为“上传事务 + 任务表 + 异步处理”流程，支持文档 SHA-256 去重、处理失败记录和任务恢复扫描。
- 结合 MySQL 关键词检索与 Qdrant 向量检索构建 RAG 流程，低相关度时不调用大模型并引导转人工。

## 1 分钟介绍

这是一个面向电商售后场景的企业知识库客服系统。管理员上传退换货、物流、商品等知识文档后，系统异步解析并写入向量库。用户提问时，后端会做关键词和向量混合检索，相关度足够才调用大模型生成回答，并保存引用来源快照。系统还实现了 JWT + Redis 登录认证、聊天限流、人工工单状态流转和管理员处理闭环。

## 3 分钟介绍

项目采用 Spring Boot 模块化单体架构，避免为了展示技术栈而拆微服务。认证层使用 Spring Security、JWT 和 Redis，Access Token 负责接口访问，Refresh Token 通过 HttpOnly Cookie 轮换，退出时将 Access Token jti 写入 Redis 黑名单。知识库部分把原来的同步上传改为任务表模型，数据库只记录 PENDING 任务，解析、切片、Embedding 和 Qdrant 写入由异步处理器完成。RAG 部分保留低相关度拒答策略，回答成功后会把来源写入 `chat_message_source`，方便后续追溯。工单部分使用明确状态机和 lockVersion，避免管理员并发处理时互相覆盖。

## 可量化数据如何获得

不要编造 QPS、准确率、用户量或收益。可以通过后续真实压测、接口耗时日志、RAG 命中率测试集、人工标注评估和 Docker 环境集成测试获得真实数据。
