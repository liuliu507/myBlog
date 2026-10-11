# My Blog

基于 Spring Boot 4 + Vue 3 的全栈博客系统，集成 Markdown 编辑、AI 对话、评论互动等功能，Docker 一键部署。

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479a1)
![Redis](https://img.shields.io/badge/Redis-7-red)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)

---

## 功能特性

- **用户体系**：注册 / 登录 / JWT 鉴权 / 忘记密码（邮件验证码）
- **文章管理**：Markdown 编辑器、分类、标签、关键词搜索
- **互动功能**：二级楼中楼评论、点赞、浏览数（防刷）
- **AI 对话**：智谱 GLM 流式输出、对话历史持久化
- **图片上传**：腾讯云 COS（后端转发，密钥不暴露前端）
- **通知系统**：评论 / 点赞通知、未读计数
- **响应式设计**：桌面优先 + 移动端适配

---

## 技术栈

| 层级 | 技术 |
|------|------|
| **后端** | Spring Boot 4.1、Java 21、MyBatis Plus 3.5、Spring Security Crypto、JJWT 0.12、WebFlux（WebClient） |
| **前端** | Vue 3.5、Vite 8、Element Plus 2.14、Pinia 4、Vue Router 5、Axios、md-editor-v3 |
| **存储** | MySQL 8.4、Redis 7、腾讯云 COS |
| **部署** | Docker Compose、Nginx、Maven Wrapper |
| **第三方** | 智谱 GLM API、QQ 邮箱 SMTP |

---

## 项目结构

```
my-blog/
├── my-blog-backend/            # Spring Boot 后端
│   ├── src/main/java/.../
│   │   ├── controller/         # 接口层
│   │   ├── service/            # 业务层
│   │   ├── mapper/             # MyBatis Plus 数据访问层
│   │   ├── entity/             # 数据库实体
│   │   ├── dto/                # 请求 / 响应 VO（敏感字段隔离）
│   │   ├── config/             # 配置类（MyBatis、定时任务等）
│   │   ├── interceptor/        # JWT 鉴权拦截器
│   │   ├── exception/          # 全局异常处理
│   │   └── util/               # 工具类（JWT、Redis、邮件）
│   └── src/main/resources/
│       ├── application.yml          # 公共配置
│       ├── application-dev.yml      # 本地开发配置（不提交）
│       └── application-prod.yml     # 生产配置（环境变量注入）
├── my-blog-frontend/           # Vue 3 前端
│   ├── src/
│   │   ├── api/                # Axios 接口封装
│   │   ├── views/              # 页面组件
│   │   ├── components/         # 通用组件
│   │   ├── stores/             # Pinia 状态管理
│   │   ├── router/             # 路由
│   │   └── utils/              # 请求封装、校验、格式化
│   └── nginx.conf              # Nginx 配置
├── sql/
│   └── init.sql                # 数据库初始化脚本
├── docker/
│   └── mysql/                  # MySQL 自定义镜像（字符集配置）
├── docker-compose.yml          # 一键编排
└── .env.example                # 环境变量模板
```

---

## 核心技术亮点

### 1. JWT 无状态鉴权 + 敏感字段隔离
自定义 `JwtInterceptor` 统一校验 token，响应统一用 `Result<T>` 包装；通过 `UserVO` 等 VO 层隔离密码等敏感字段，密码使用 `BCryptPasswordEncoder` 加密存储。

### 2. Redis 原子操作防刷
- 浏览数：`SET NX EX 86400` 实现同一用户/IP 24 小时内仅计数一次
- 邮箱验证码：发送后写入 60 秒冷却 key，原子操作避免高并发竞态
- 所有限流均用 Redis 原子命令，而非 `hasKey` + `set` 分步操作

### 3. AI 流式输出（SSE）
`WebClient` 转发智谱 GLM 的 SSE 流到前端，对话落库放在 `doOnComplete` 中通过 `Schedulers.boundedElastic()` 异步执行，避免阻塞 Netty 事件循环。对话历史存 MySQL，配合每日定时任务清理 3 天前记录。

### 4. 图片上传安全架构
采用**后端转发**模式（前端 → 后端 → COS），密钥仅存后端环境变量；COS Bucket 设为公有读私有写，图片 URL 可直接被 `<img>` 标签访问。头像 2MB / 文章图 5MB 限制。

### 5. 评论二级楼中楼
评论表通过 `parent_id` + `reply_to_user_id` 实现二级结构，不做无限嵌套；点赞用 `(article_id, user_id)` 唯一索引防重复。

### 6. 敏感配置全链路环境变量化
- `.env` 不提交，模板见 `.env.example`
- `application-prod.yml` 敏感字段无默认值，缺失即 fail-fast
- Docker Compose 统一通过 `${VAR}` 注入，镜像内不硬编码凭证

### 7. 移动端适配兜底
针对 WebKit 内核（iOS Safari / 微信内置浏览器）横向溢出问题：
- flex 子项显式设置 `min-width: 0`
- 全局 `overflow-x: clip`（旧浏览器回退 `hidden`），不破坏 header sticky 吸顶

### 8. Docker 工程化
- MySQL 配置文件 `COPY` 进镜像，规避 Windows Docker Desktop bind mount 权限问题
- `.dockerignore` 排除 `node_modules`，避免 Windows 依赖被复制进 Linux 容器
- 后端容器设置 `SPRING_PROFILES_ACTIVE=prod`，强制使用生产配置

---

## 快速开始（Docker 一键启动）

### 环境要求
- Docker 20.10+
- Docker Compose v2

### 启动步骤

```bash
# 1. 复制环境变量模板
# Linux / macOS
cp .env.example .env
# Windows PowerShell
Copy-Item .env.example .env

# 2. 编辑 .env，填入真实值（见下方环境变量说明）

# 3. 一键构建并启动
docker compose up -d --build

# 4. 访问
前端: http://localhost
```

---

## 环境变量配置

所有敏感值通过 `.env` 文件注入（`.env` 不提交到 Git）。复制 `.env.example` 后填写：

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 自定义强密码 |
| `REDIS_PASSWORD` | Redis 密码 | 自定义强密码 |
| `MAIL_USERNAME` | QQ 邮箱账号 | `your_qq@qq.com` |
| `MAIL_PASSWORD` | QQ 邮箱 SMTP 授权码（非登录密码） | 邮箱后台获取 |
| `JWT_SECRET` | JWT 签名密钥，≥ 32 字节 | 随机生成 |
| `GLM_API_KEY` | 智谱 GLM API Key | [open.bigmodel.cn](https://open.bigmodel.cn) 获取 |
| `COS_SECRET_ID` | 腾讯云 SecretId | 腾讯云控制台获取 |
| `COS_SECRET_KEY` | 腾讯云 SecretKey | 腾讯云控制台获取 |
| `COS_BUCKET` | COS Bucket 名称 | `my-blog-123456` |
| `COS_REGION` | COS 地域 | `ap-guangzhou` |

> **JWT_SECRET 生成**（PowerShell）：
> ```powershell
> [Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Max 256 }))
> ```

---

## 本地开发

### 后端

```bash
cd my-blog-backend

# 1. 复制开发配置模板
Copy-Item src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml

# 2. 编辑 application-dev.yml，填入本地 MySQL / Redis / 邮箱 / JWT / GLM / COS 配置

# 3. 确保本地 MySQL 已创建数据库并执行 sql/init.sql
# 4. 启动
./mvnw spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 前端

```bash
cd my-blog-frontend

# 1. 安装依赖
npm install

# 2. 启动开发服务器
npm run dev
```

前端启动在 `http://localhost:5173`，Vite 已配置 `/api` 代理到后端 `8080` 端口。

---

## 生产部署

### 安全建议
- 腾讯云安全组仅开放 **80**（HTTP）和 **22**（SSH）端口
- MySQL / Redis 端口仅绑定 `127.0.0.1`，不暴露公网
- `JWT_SECRET`、数据库密码等必须使用强随机值

### 部署流程

```bash
# 1. 上传代码到服务器
# 2. 配置 .env
# 3. 构建并启动
docker compose up -d --build
```

### 注意事项

- **重新部署禁止使用 `docker compose down -v`**，否则会删除 MySQL 数据卷导致数据丢失。应使用 `docker compose up -d --build` 重启。
- `sql/init.sql` **仅在数据卷首次初始化时执行**。后续新增的表（如 `chat_message`、`comment`、`article_like`）需手动进入 MySQL 执行建表语句。
- 新增环境变量后，需同时更新 `.env.example`、`docker-compose.yml` 和 `application-prod.yml`，保持链路一致。

---


## API 概览

| 模块 | 接口 | 说明 |
|------|------|------|
| **认证** | `POST /api/auth/register` | 注册 |
| | `POST /api/auth/login` | 登录 |
| | `GET /api/auth/me` | 获取当前用户信息 |
| | `POST /api/auth/avatar` | 上传头像 |
| | `POST /api/auth/send-reset-code` | 发送重置密码验证码 |
| | `POST /api/auth/reset-password` | 重置密码 |
| **文章** | `GET /api/articles` | 文章列表（分页） |
| | `GET /api/articles/search` | 关键词搜索 |
| | `GET /api/articles/{id}` | 文章详情 |
| | `POST /api/articles` | 发布文章 |
| | `PUT /api/articles/{id}` | 编辑文章 |
| | `DELETE /api/articles/{id}` | 删除文章 |
| | `POST /api/articles/{id}/like` | 点赞 / 取消点赞 |
| | `POST /api/articles/{id}/view` | 记录浏览 |
| **评论** | `GET /api/comments?articleId=` | 获取评论树 |
| | `POST /api/comments` | 发表评论 |
| | `DELETE /api/comments/{id}` | 删除评论 |
| **AI 对话** | `POST /api/chat/stream` | 流式对话（SSE） |
| | `GET /api/chat/history` | 对话历史 |
| **上传** | `POST /api/upload/avatar` | 上传头像 |
| | `POST /api/upload/article-image` | 上传文章图片 |
| **分类/标签** | `GET /api/categories` | 分类列表 |
| | `GET /api/tags` | 标签列表 |
| **通知** | `GET /api/notifications` | 通知列表 |
| | `GET /api/notifications/unread-count` | 未读数 |
