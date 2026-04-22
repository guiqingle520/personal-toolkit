# Personal Toolkit 操作手册

## 📋 目录

- [项目概述](#项目概述)
- [技术架构](#技术架构)
- [环境准备](#环境准备)
- [快速开始](#快速开始)
- [功能说明](#功能说明)
- [API 接口文档](#api-接口文档)
- [数据库设计](#数据库设计)
- [缓存策略](#缓存策略)
- [部署指南](#部署指南)
- [开发规范](#开发规范)
- [常见问题](#常见问题)
- [扩展建议](#扩展建议)

---

## 项目概述

**Personal Toolkit** 是一个个人待办事项管理系统，采用前后端分离架构，提供简洁高效的任務管理功能。

### 核心特性

- ✅ JWT 登录 / 注册 / 退出
- ✅ 登录随机验证码（SVG）
- ✅ 登录失败按 IP / 账号标识限流
- ✅ Todo 数据按账号隔离
- ✅ Todo 提醒时间
- ✅ Todo 站内提醒列表 / 已读 / 全部已读
- ✅ Todo Saved Views（保存筛选 / 设默认 / 重命名 / 删除）
- ✅ 完整的 CRUD 操作（创建、查询、更新、删除）
- ✅ 任务状态管理（待处理/已完成）
- ✅ 分页、筛选与回收站
- ✅ 时间预设筛选（今日到期 / 逾期 / 即将提醒）
- ✅ 重复类型筛选与活动筛选 chips
- ✅ 长备注与附件链接
- ✅ 协作能力预留（负责人 / 协作人 / 观察者占位字段）
- ✅ Checklist / 子任务管理
- ✅ 重复任务（完成后生成下一条实例）
- ✅ 统计面板（概览 / 分类 / 最近 7 天趋势）
- ✅ 列表视图 / 静态看板视图切换
- ✅ Redis 缓存加速查询性能
- ✅ 统一响应格式和异常处理
- ✅ 赛博朋克风格的现代化 UI
- ✅ Docker 容器化部署支持
- ✅ TypeScript 类型安全保障

### 当前阶段状态

当前项目已完成并验收通过：

- Phase 3 / Sprint 3.1：子任务 / Checklist
- Phase 3 / Sprint 3.2：重复任务 / Recurrence
- Phase 3 / Sprint 3.3：统计面板 / Stats Panel
- Phase 3 / Sprint 3.4：看板视图 / Static Kanban View

建议配套阅读：

- `docs/phase-3-plan-v3.md`
- `docs/phase-4-plan-v1.md`

### 适用场景

- 个人日常任务管理
- Spring Boot 3 + Vue 3 学习实践
- Cache-Aside 缓存模式参考实现
- 小型项目脚手架

---

## 技术架构

### 整体架构图

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Browser   │ ◄─────► │  Vue 3 Front │ ◄─────► │Spring Boot  │
│             │         │  (Port 5173) │         │ Backend     │
└─────────────┘         └──────────────┘         │ (Port 8080) │
                                                  └──────┬──────┘
                                                         │
                                          ┌──────────────┴──────────────┐
                                          │                             │
                                   ┌──────▼──────┐            ┌────────▼────────┐
                                   │   Oracle    │            │     Redis       │
                                   │   Database  │            │     Cache       │
                                   └─────────────┘            └─────────────────┘
```

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 3.3.5 | Web 框架 |
| Spring Security | - | JWT 鉴权 |
| Spring Data JPA | - | ORM 数据访问 |
| Oracle JDBC | ojdbc11 | 数据库驱动 |
| Spring Data Redis | - | 缓存集成 |
| JJWT | 0.12.6 | JWT 签发与校验 |
| Maven | - | 依赖管理和构建 |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.13 | 渐进式框架 |
| TypeScript | 5.7.2 | 类型系统 |
| Vite | 6.0.1 | 构建工具 |
| Vue TSC | 2.1.10 | 类型检查 |

---

## 环境准备

### 必需软件

1. **JDK 17+**
   ```powershell
   java -version
   ```

2. **Node.js 18+**
   ```powershell
   node -v
   npm -v
   ```

3. **Maven 3.6+**
   ```powershell
   mvn -version
   ```

4. **Docker & Docker Compose**（可选，用于容器化部署）
   ```powershell
   docker --version
   docker compose version
   ```

### 中间件依赖

#### Oracle 数据库

- **版本**: 19.3
- **连接信息**:
  - Host: `192.168.240.128`
  - Port: `1521`
  - SID: `gdb`
  - Username: `gql`
  - Password: `glaway123`

#### Redis 缓存

- **版本**: 6.x+
- **连接信息**:
  - Host: `192.168.240.128`
  - Port: `6379`
  - Password: `glaway123`
  - Database: `0`

> **注意**: 如果使用本地 Oracle/Redis，请修改 `backend/src/main/resources/application-dev.yml` 中的配置。

---

## 快速开始

### 方式一：本地开发模式（推荐）

#### 1. 初始化数据库

连接到 Oracle 数据库后，按以下顺序执行当前脚本集：

1. `backend/sql/create_app_user.sql`
2. `backend/sql/create_todo_item.sql`
3. `backend/sql/alter_todo_item_add_user_id_phase1.sql`（仅老库升级时需要）
4. `backend/sql/alter_todo_item_add_user_id_phase2.sql`（仅老库升级时需要）

其中：

- `create_app_user.sql` 用于创建登录用户表
- `create_todo_item.sql` 用于创建 Todo 主表
- `alter_todo_item_add_user_id_phase1.sql` 用于给历史 Todo 增加可空账号归属字段
- `alter_todo_item_add_user_id_phase2.sql` 用于在回填完成后补齐约束与索引

> 对于全新初始化数据库，不需要再执行 Phase 1 / Phase 2，因为 `create_todo_item.sql` 已经直接带了 `user_id`、外键与索引。

#### 1.1 旧数据绑定到指定账号

如果库里已经存在登录改造前的旧 Todo，需要先把这些旧数据统一绑定到一个指定账号。

推荐流程如下：

1. 先启动前后端，注册一个用于接管旧数据的账号，例如：`legacy_owner`
2. 回到 Oracle，执行以下 SQL 查询该账号主键：

```sql
SELECT id, username, email
FROM app_user
WHERE username = 'legacy_owner';
```

3. 检查待回填的旧数据量：

```sql
SELECT COUNT(*) AS unbound_todo_count
FROM todo_item
WHERE user_id IS NULL;
```

4. 先执行第一阶段扩字段脚本：

```sql
@backend/sql/alter_todo_item_add_user_id_phase1.sql
```

5. 执行回填：

```sql
UPDATE todo_item
SET user_id = (
    SELECT id
    FROM app_user
    WHERE username = 'legacy_owner'
)
WHERE user_id IS NULL;

COMMIT;
```

6. 执行回填后的校验：

```sql
SELECT COUNT(*) AS remaining_unbound_count
FROM todo_item
WHERE user_id IS NULL;

SELECT user_id, COUNT(*) AS todo_count
FROM todo_item
GROUP BY user_id
ORDER BY user_id;
```

当 `remaining_unbound_count = 0` 后，说明旧数据已经全部完成绑定。

7. 执行第二阶段收口脚本：

```sql
-- 先将 :legacy_user_id 替换成真实用户主键，
-- 或在 SQL 工具中显式绑定该变量后再执行
@backend/sql/alter_todo_item_add_user_id_phase2.sql
```

> `alter_todo_item_add_user_id.sql` 已改为弃用说明文件，仅用于兼容历史引用，不建议继续使用。

以下示例仅展示 `create_todo_item.sql` 的基础建表片段：

```sql
-- 文件位置: backend/sql/create_todo_item.sql
-- 创建序列
CREATE SEQUENCE todo_item_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- 创建表
CREATE TABLE todo_item (
    id NUMBER(19) PRIMARY KEY,
    title VARCHAR2(200 CHAR) NOT NULL,
    status VARCHAR2(32 CHAR) DEFAULT 'PENDING' NOT NULL,
    create_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    update_time TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT ck_todo_item_status CHECK (status IN ('PENDING', 'DONE'))
);
```

#### 2. 启动后端服务

```powershell
# 进入后端目录
cd backend

# 使用 Maven 启动（dev 环境）
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动成功后，控制台会显示：
```
Started PersonalToolkitApplication in X.XXX seconds
```

后端服务运行在: `http://localhost:8080`

#### 3. 启动前端服务

新开一个终端窗口：

```powershell
# 进入前端目录
cd frontend

# 首次需要安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务运行在: `http://localhost:5173`

#### 4. 注册与登录

首次访问前端页面后，会先进入登录 / 注册页。

**首次使用建议顺序：**

1. 打开 `http://localhost:5173`
2. 点击注册，填写：
   - 用户名
   - 邮箱
   - 密码（8~100 位）
3. 注册成功后，前端会自动进入已登录态
4. 后续登录时可使用**用户名或邮箱**配合密码，并输入当前随机验证码
5. 顶部工具栏可执行退出操作，退出后将清除本地登录态

#### 4.1 登录验证码流程

登录页会自动请求 `GET /api/auth/captcha` 获取验证码图片与 `captchaId`。

用户登录时需要提交：

- 用户名或邮箱
- 密码
- 验证码答案
- 当前验证码对应的 `captchaId`

验证码策略：

- 验证码有效期：120 秒
- 单个验证码最多尝试：3 次
- 登录失败过多会触发限流
- 验证码刷新过快会触发限流

当登录失败（400/401/429）时，前端会自动刷新验证码。

#### 5. 访问应用

打开浏览器访问: `http://localhost:5173`

---

### 方式二：Docker Compose 部署

#### 1. 构建并启动所有服务

```powershell
# 在项目根目录执行
docker compose up --build
```

#### 2. 后台运行

```powershell
docker compose up -d --build
```

#### 3. 查看日志

```powershell
# 查看所有服务日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f backend
docker compose logs -f frontend
```

#### 4. 停止服务

```powershell
docker compose down
```

#### 5. 清理资源

```powershell
# 停止并删除容器、网络
docker compose down

# 同时删除镜像
docker compose down --rmi all

# 删除数据卷（如果有）
docker compose down -v
```

---

## 功能说明

### 1. 任务列表展示

- **自动加载**: 登录成功后自动从后端加载当前账号下的待办事项
- **排序规则**: 默认按创建时间倒序排列，可按筛选条件分页查询
- **统计信息**: 显示总任务数、待处理任务数，并在活动任务页展示统计面板
- **刷新功能**: 点击“刷新 / Refresh”按钮手动同步最新数据

### 1.5 登录态与账号隔离

- Todo、统计面板、回收站、Checklist 都只返回当前账号自己的数据
- 前端会在本地保存 JWT，用于刷新页面后恢复登录态
- 如果后端返回 `401 Unauthorized`，前端会自动清理本地登录态并回到登录页
- 当前版本的退出是无状态退出：前端清除本地 Token，服务端不做已签发 Token 撤销

### 1.1 Checklist / 子任务

- 支持在主任务下展开 checklist
- 支持子任务新增、更新、删除、状态切换
- 支持显示 checklist 完成进度汇总

### 1.2 重复任务

- 支持每日 / 每周 / 每月重复任务
- 当前执行模型为：完成当前任务后生成下一条实例
- 支持重复间隔、结束时间、完成时间展示

### 1.3 统计面板

- 仅在活动任务页展示
- 支持概览统计、分类统计、最近 7 天完成趋势
- 概览卡片额外展示未来 24 小时即将提醒数与未读提醒数

### 1.4 看板视图

- 活动任务页支持列表视图 / 静态看板视图切换
- 当前看板首版按“待办 / 已完成”静态分列
- 当前不支持拖拽改状态

### 1.6 站内提醒

- Todo 设置 `remindAt` 后，后端会自动同步对应提醒事件
- 后端默认每 60 秒扫描一次到点提醒，并将其投递到站内未读列表
- 活动任务页左侧会显示提醒面板
- 支持单条已读、全部已读和跳转回对应任务
- 当前不支持邮件、短信或企业 IM 推送

### 1.7 Saved Views

- 支持将当前筛选条件保存为命名视图
- 支持应用、设为默认、重命名、删除
- 页面初始化时会优先尝试应用默认视图
- 当前只保存白名单筛选字段，不保存分页参数
- 当前不支持共享视图

### 2. 创建任务

**操作步骤**:
1. 在输入框中输入任务标题
2. 按回车键或点击 "Add Task" 按钮
3. 新任务会立即出现在列表顶部

**业务规则**:
- 标题不能为空
- 默认状态为 `PENDING`（待处理）
- 自动记录创建时间

### 3. 编辑任务

**操作步骤**:
1. 将鼠标悬停在任务上，点击右上角的编辑图标（✎）
2. 在输入框中修改任务标题
3. 按回车键或点击 "Save" 保存
4. 按 ESC 键或点击 "Cancel" 取消编辑

**业务规则**:
- 如果标题未变化，自动取消编辑
- 标题不能为空
- 更新后自动刷新更新时间字段

### 4. 切换任务状态

**操作步骤**:
- 点击任务左侧的状态切换按钮
- `PENDING` → `DONE`: 标记为已完成
- `DONE` → `PENDING`: 重新标记为待处理

**视觉效果**:
- 已完成的任务会显示删除线
- 已完成的任务透明度降低
- 状态按钮颜色变化（灰色 ↔ 绿色）

### 5. 删除任务

**操作步骤**:
1. 将鼠标悬停在任务上
2. 点击右上角的删除按钮（×）
3. 在确认对话框中点击 "确定"

**业务规则**:
- 删除前需要二次确认
- 删除后从列表中移除
- 同时清除相关缓存

### 6. 错误处理

- **网络错误**: 显示红色错误横幅
- **参数校验失败**: 显示字段级错误详情
- **加载状态**: 显示旋转动画和提示文字
- **空状态**: 列表为空时显示友好提示

---

## API 接口文档

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/login-policy` | 获取登录页验证码策略（固定验证码或自适应验证码） |
| GET | `/api/auth/captcha` | 获取登录验证码（返回 captchaId、图片和过期时间） |
| POST | `/api/auth/register` | 注册账号并返回 JWT 与当前用户信息 |
| POST | `/api/auth/login` | 支持用户名或邮箱登录；固定模式要求验证码，自适应模式按风险要求验证码 |
| GET | `/api/auth/me` | 获取当前登录用户信息 |
| POST | `/api/auth/logout` | 退出当前登录态（前端清理 Token） |

### 登录策略接口返回

```json
{
  "success": true,
  "message": "Login policy fetched successfully",
  "data": {
    "captchaEnabled": true,
    "adaptiveCaptcha": false,
    "adaptiveTriggerThreshold": 2
  },
  "timestamp": "2026-04-17T10:00:00Z"
}
```

### 登录接口请求体

```json
{
  "username": "alice@example.com",
  "password": "password123",
  "captchaId": "<captcha-id>",
  "captchaCode": "AB12C"
}
```

### 登录常见错误

- `400 CAPTCHA_REQUIRED`
- `400 Captcha expired or invalid`
- `400 Captcha verification failed`
- `401 Invalid username or password`
- `429 Too many captcha requests, please try again later`
- `429 Too many failed login attempts, please try again later`

### 登录模式说明

- 固定验证码模式：登录页进入后先请求 `/api/auth/captcha`，再提交登录请求。
- 自适应验证码模式：登录页进入后先请求 `/api/auth/login-policy`；默认连续失败达到 2 次后，服务端开始要求验证码。收到 `CAPTCHA_REQUIRED` 后，再请求 `/api/auth/captcha` 并补交验证码。

### Todo 接口认证要求

除 `/api/auth/register`、`/api/auth/login` 与健康检查外，其余接口都需要在请求头中携带：

```http
Authorization: Bearer <jwt-token>
```

### 基础信息

- **Base URL**: `http://localhost:8080/api`
- **Content-Type**: `application/json`
- **字符编码**: `UTF-8`

### 统一响应格式

#### 成功响应

```json
{
  "success": true,
  "message": "操作成功提示信息",
  "data": { ... },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

#### 错误响应

```json
{
  "success": false,
  "message": "错误描述",
  "validation": {
    "title": ["标题不能为空"]
  },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

### 接口列表

#### 1. 分页查询待办事项

**请求**
```http
GET /api/todos
```

**响应** (200 OK)
```json
{
  "success": true,
  "message": "Todo list fetched successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "学习 Spring Boot",
        "status": "PENDING",
        "priority": 3,
        "category": "Work",
        "tags": "backend",
        "createTime": "2024-01-01T10:00:00",
        "updateTime": "2024-01-01T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "page": 0,
    "size": 10,
    "first": true,
    "last": true
  },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

**cURL 示例**
```bash
curl http://localhost:8080/api/todos
```

---

#### 2. 查询待办筛选候选项

**请求**
```http
GET /api/todos/options
```

**响应** (200 OK)
```json
{
  "success": true,
  "message": "Todo options fetched successfully",
  "data": {
    "categories": ["Work", "Personal"],
    "tags": ["backend", "urgent"]
  },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

---

#### 2.1 筛选增强说明

`GET /api/todos` 当前支持以下增强筛选字段：

- `recurrenceType`：按重复类型筛选，取值 `NONE / DAILY / WEEKLY / MONTHLY`
- `timePreset`：按时间预设筛选，取值：
  - `DUE_TODAY`：今日到期
  - `OVERDUE`：逾期未完成
  - `UPCOMING_REMINDER`：未来 24 小时内即将提醒

前端列表页会展示活动筛选 chips，可直接点击移除对应筛选条件。

当前协作能力仍为占位实现：

- `ownerLabel`：负责人显示名
- `collaborators`：协作人，逗号分隔
- `watchers`：观察者，逗号分隔

这些字段当前用于保存和展示协作上下文，尚未接入真实的多人权限与共享流。

**示例**

```http
GET /api/todos?recurrenceType=DAILY&timePreset=OVERDUE&page=0&size=10
```

---

#### 3. 查询统计概览

**请求**
```http
GET /api/todos/stats/overview
```

**补充说明**

- 当前响应除 `todayCompleted / weekCompleted / overdueCount / activeCount / upcomingReminderCount` 外，还包含 `unreadReminderCount`

#### 4. 查询分类统计

**请求**
```http
GET /api/todos/stats/by-category
```

#### 5. 查询最近 7 天趋势

**请求**
```http
GET /api/todos/stats/trend?range=7d
```

---

#### 5.1 查询站内提醒列表

**请求**
```http
GET /api/todo-reminders?status=SENT&page=0&size=10
```

**说明**

- `status` 可选，当前支持 `PENDING / SENT / READ / CANCELLED`
- 前端当前默认查询 `SENT`，即未读提醒列表

#### 5.2 查询提醒统计

**请求**
```http
GET /api/todo-reminders/stats
```

**响应字段**

- `unreadCount`：当前用户未读提醒数量

#### 5.3 标记单条提醒已读

**请求**
```http
POST /api/todo-reminders/{id}/read
```

#### 5.4 全部提醒标记已读

**请求**
```http
POST /api/todo-reminders/read-all
```

---

#### 5.5 查询保存视图列表

**请求**
```http
GET /api/todo-saved-views
```

#### 5.6 创建保存视图

**请求**
```http
POST /api/todo-saved-views
Content-Type: application/json
```

**请求体示例**
```json
{
  "name": "Ops Focus",
  "isDefault": true,
  "filters": {
    "status": "PENDING",
    "keyword": "release",
    "sortBy": "createTime",
    "sortDir": "DESC"
  }
}
```

#### 5.7 更新保存视图

**请求**
```http
PUT /api/todo-saved-views/{id}
```

#### 5.8 删除保存视图

**请求**
```http
DELETE /api/todo-saved-views/{id}
```

#### 5.9 设为默认视图

**请求**
```http
POST /api/todo-saved-views/{id}/default
```

---

#### 6. 查询单个待办事项

**请求**
```http
GET /api/todos/{id}
```

**路径参数**
- `id`: 待办事项主键（Long 类型）

**响应** (200 OK)
```json
{
  "success": true,
  "message": "Todo item fetched successfully",
  "data": {
    "id": 1,
    "title": "学习 Spring Boot",
    "status": "PENDING",
    "notes": "整理认证接口说明",
    "attachmentLinks": "https://example.com/spec\nhttps://example.com/mock",
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00"
  },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

**错误响应** (404 Not Found)
```json
{
  "success": false,
  "message": "Todo item not found: 999",
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

**cURL 示例**
```bash
curl http://localhost:8080/api/todos/1
```

---

#### 7. 创建待办事项

**请求**
```http
POST /api/todos
Content-Type: application/json
```

**请求体**
```json
{
  "title": "学习 Vue 3",
  "status": "PENDING"
}
```

**字段说明**
- `title`: 任务标题（必填，最大 200 字符）
- `status`: 任务状态（必填，只能是 `PENDING` 或 `DONE`）
- `remindAt`: 可选，设置后会参与站内提醒事件生成

**响应** (201 Created)
```json
{
  "success": true,
  "message": "Todo item created successfully",
  "data": {
    "id": 2,
    "title": "学习 Vue 3",
    "status": "PENDING",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:00:00"
  },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

**错误响应** (400 Bad Request)
```json
{
  "success": false,
  "message": "Validation failed",
  "validation": {
    "title": ["标题不能为空"]
  },
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

**cURL 示例**
```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"title": "学习 Vue 3", "status": "PENDING"}'
```

---

#### 8. 更新待办事项

**请求**
```http
PUT /api/todos/{id}
Content-Type: application/json
```

**路径参数**
- `id`: 待办事项主键

**请求体**
```json
{
  "title": "深入学习 Vue 3 Composition API",
  "status": "DONE"
}
```

**响应** (200 OK)
```json
{
  "success": true,
  "message": "Todo item updated successfully",
  "data": {
    "id": 2,
    "title": "深入学习 Vue 3 Composition API",
    "status": "DONE",
    "createTime": "2024-01-01T12:00:00",
    "updateTime": "2024-01-01T12:30:00"
  },
  "timestamp": "2024-01-01T12:30:00+08:00"
}
```

**cURL 示例**
```bash
curl -X PUT http://localhost:8080/api/todos/2 \
  -H "Content-Type: application/json" \
  -d '{"title": "深入学习 Vue 3 Composition API", "status": "DONE"}'
```

---

#### 9. 删除待办事项

**请求**
```http
DELETE /api/todos/{id}
```

**路径参数**
- `id`: 待办事项主键

**响应** (200 OK)
```json
{
  "success": true,
  "message": "Todo item deleted successfully",
  "data": null,
  "timestamp": "2024-01-01T12:30:00+08:00"
}
```

**错误响应** (404 Not Found)
```json
{
  "success": false,
  "message": "Todo item not found: 999",
  "timestamp": "2024-01-01T12:30:00+08:00"
}
```

**cURL 示例**
```bash
curl -X DELETE http://localhost:8080/api/todos/2
```

---

#### 10. 查询指定主任务下的 Checklist

**请求**
```http
GET /api/todos/{todoId}/sub-items
```

#### 11. 创建 Checklist 子任务

**请求**
```http
POST /api/todos/{todoId}/sub-items
```

#### 12. 更新 Checklist 子任务

**请求**
```http
PUT /api/todos/{todoId}/sub-items/{subItemId}
```

#### 13. 删除 Checklist 子任务

**请求**
```http
DELETE /api/todos/{todoId}/sub-items/{subItemId}
```

#### 14. 查询 Checklist 汇总

**请求**
```http
GET /api/todos/{todoId}/sub-items/summary
```

---

## 数据库设计

### 表结构：todo_item

| 字段名 | 数据类型 | 约束 | 说明 |
|--------|---------|------|------|
| id | NUMBER(19) | PRIMARY KEY | 主键，由序列生成 |
| title | VARCHAR2(200 CHAR) | NOT NULL | 待办事项标题 |
| status | VARCHAR2(32 CHAR) | NOT NULL, DEFAULT 'PENDING' | 状态：PENDING/DONE |
| priority | NUMBER(2) | NOT NULL, DEFAULT 3 | 优先级，1~5 |
| due_date | TIMESTAMP | NULL | 截止时间 |
| category | VARCHAR2(100 CHAR) | NULL | 分类 |
| tags | VARCHAR2(500 CHAR) | NULL | 标签，逗号分隔 |
| deleted_at | TIMESTAMP | NULL | 软删除时间 |
| recurrence_type | VARCHAR2(32 CHAR) | NOT NULL, DEFAULT 'NONE' | 重复类型 |
| recurrence_interval | NUMBER(4) | NOT NULL, DEFAULT 1 | 重复间隔 |
| recurrence_end_time | TIMESTAMP | NULL | 重复截止时间 |
| next_trigger_time | TIMESTAMP | NULL | 重复任务计划触发时间 |
| completed_at | TIMESTAMP | NULL | 完成时间 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT SYSTIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | NOT NULL, DEFAULT SYSTIMESTAMP | 最后更新时间 |

### 表结构：todo_sub_item

| 字段名 | 数据类型 | 约束 | 说明 |
|--------|---------|------|------|
| id | NUMBER(19) | PRIMARY KEY | 子任务主键 |
| todo_id | NUMBER(19) | NOT NULL | 所属主任务主键 |
| title | VARCHAR2(200) | NOT NULL | 子任务标题 |
| status | VARCHAR2(32) | NOT NULL, DEFAULT 'PENDING' | 子任务状态 |
| sort_order | NUMBER(10) | NOT NULL, DEFAULT 0 | 排序序号 |
| create_time | TIMESTAMP | NOT NULL, DEFAULT SYSTIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | NOT NULL, DEFAULT SYSTIMESTAMP | 更新时间 |

### 当前数据库脚本

- `backend/sql/create_todo_item.sql`
- `backend/sql/alter_todo_item_phase1.sql`
- `backend/sql/alter_todo_item_phase2.sql`
- `backend/sql/alter_todo_item_phase3_recurrence.sql`
- `backend/sql/create_todo_sub_item.sql`

### 约束条件

1. **主键约束**: `PRIMARY KEY (id)`
2. **非空约束**: `title`, `status`, `create_time`, `update_time` 不能为空
3. **检查约束**: `CHECK (status IN ('PENDING', 'DONE'))`
4. **优先级约束**: `priority BETWEEN 1 AND 5`
5. **重复任务约束**:
   - `recurrence_type IN ('NONE', 'DAILY', 'WEEKLY', 'MONTHLY')`
   - `recurrence_interval >= 1`

### 序列：todo_item_seq

```sql
CREATE SEQUENCE todo_item_seq
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;
```

### 索引建议

当前表较小，主键索引已足够。如果数据量增长，可考虑：

```sql
-- 按状态查询优化
CREATE INDEX idx_todo_status ON todo_item(status);

-- 按创建时间排序优化
CREATE INDEX idx_todo_create_time ON todo_item(create_time DESC);
```

---

## 缓存策略

### Cache-Aside 模式

本项目采用 **Cache-Aside（旁路缓存）** 模式，也称为 Lazy Loading 模式。

### 缓存键设计

| 缓存键 | 类型 | TTL | 说明 |
|--------|------|-----|------|
| `todo:list` | String | 10分钟 | 待办列表缓存 |
| `todo:item:{id}` | String | 10分钟 | 单个待办对象缓存 |

### 读操作流程

```
1. 接收查询请求
   ↓
2. 尝试从 Redis 读取缓存
   ↓
3a. 缓存命中 → 直接返回缓存数据
   ↓
3b. 缓存未命中 → 查询 Oracle 数据库
   ↓
4. 将数据库结果写入 Redis 缓存
   ↓
5. 返回数据给客户端
```

### 写操作流程

```
1. 接收写请求（创建/更新/删除）
   ↓
2. 开启数据库事务
   ↓
3. 执行数据库操作
   ↓
4. 注册事务提交后回调
   ↓
5. 提交事务
   ↓
6. 事务提交成功后执行回调：
   - 创建/更新: 更新对象缓存 + 失效列表缓存
   - 删除: 删除对象缓存 + 失效列表缓存
```

### 容错机制

- **Redis 故障降级**: 所有 Redis 操作用 try-catch 包裹，失败仅记录日志
- **不影响主流程**: 缓存读写失败不会中断业务流程
- **自动恢复**: Redis 恢复后自动重新开始缓存

### 关键代码位置

- `backend/src/main/java/com/personal/toolkit/todo/service/TodoService.java`

```java
// 事务提交后更新缓存
runAfterCommit(() -> {
    cacheTodoItemSafely(savedTodoItem);
    evictTodoListCacheSafely();
});
```

### 缓存一致性保证

1. **先更新数据库，后更新缓存**
2. **使用事务提交后回调**，确保只有数据库提交成功才更新缓存
3. **失效列表缓存**，下次查询时重新加载最新数据
4. **设置合理 TTL**（10分钟），即使出现不一致也能自动恢复

---

## 部署指南

### 生产环境配置

#### 1. 修改配置文件

创建 `backend/src/main/resources/application-prod.yml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:oracle:thin:@//your-oracle-host:1521/yoursid
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      database: 0

logging:
  level:
    org.hibernate.SQL: warn
    com.personal.toolkit: info
```

#### 2. 环境变量配置

```powershell
# 数据库配置
$env:SPRING_DATASOURCE_URL="jdbc:oracle:thin:@//host:1521/sid"
$env:SPRING_DATASOURCE_USERNAME="username"
$env:SPRING_DATASOURCE_PASSWORD="password"

# Redis 配置
$env:SPRING_DATA_REDIS_HOST="redis-host"
$env:SPRING_DATA_REDIS_PORT="6379"
$env:SPRING_DATA_REDIS_PASSWORD="password"

# 激活生产环境
$env:SPRING_PROFILES_ACTIVE="prod"
```

#### 3. Docker 生产部署

修改 `docker-compose.yml`:

```yaml
services:
  backend:
    build:
      context: ./backend
    container_name: personal-toolkit-backend
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: ${DB_URL}
      SPRING_DATASOURCE_USERNAME: ${DB_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: ${REDIS_HOST}
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
    ports:
      - "8080:8080"
    restart: always
    networks:
      - app-network

  frontend:
    build:
      context: ./frontend
    container_name: personal-toolkit-frontend
    environment:
      VITE_API_PROXY_TARGET: http://backend:8080
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: always
    networks:
      - app-network

networks:
  app-network:
    driver: bridge
```

创建 `.env` 文件：

```env
DB_URL=jdbc:oracle:thin:@//your-oracle-host:1521/yoursid
DB_USERNAME=your_username
DB_PASSWORD=your_password
REDIS_HOST=your-redis-host
REDIS_PASSWORD=your_redis_password
```

启动服务：

```powershell
docker compose --env-file .env up -d --build
```

### Nginx 反向代理（可选）

如果需要统一的域名访问，可以配置 Nginx：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        proxy_pass http://localhost:5173;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 后端 API
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 健康检查

Spring Boot Actuator 提供健康检查端点：

```bash
curl http://localhost:8080/actuator/health
```

响应：
```json
{
  "status": "UP"
}
```

---

## 开发规范

### 代码注释规范

详细规范请参考: `docs/code-style.md`

#### 核心原则

1. **实体类必须编写具体注释**
   - 说明业务对象含义
   - 说明与数据库表的映射关系
   
2. **实现方法必须编写具体注释**
   - 说明方法职责
   - 说明参数和返回值
   - 说明副作用（缓存更新、状态变化等）

3. **禁止无意义的重复代码注释**
   - ❌ `获取标题`
   - ✅ `返回待办事项标题，用于前端展示`

4. **注释风格**
   - Java: 使用 Javadoc
   - TypeScript/Vue: 使用块注释

### 命名规范

#### 后端 Java

- **类名**: PascalCase（如 `TodoController`）
- **方法名**: camelCase（如 `findAll`）
- **常量**: UPPER_SNAKE_CASE（如 `TODO_LIST_CACHE_KEY`）
- **包名**: 全小写（如 `com.personal.toolkit.todo`）

#### 前端 TypeScript

- **接口/类型**: PascalCase（如 `TodoItem`）
- **变量/函数**: camelCase（如 `loadTodos`）
- **组件文件**: PascalCase（如 `TodoList.vue`）

### Git 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 类型**:
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具链相关

**示例**:
```
feat(todo): 添加任务优先级字段

- 在 TodoItem 实体中添加 priority 字段
- 更新前端表单支持优先级选择
- 添加数据库迁移脚本

Closes #123
```

---

## 常见问题

### Q1: 后端启动失败，提示数据库连接错误

**原因**: Oracle 数据库无法连接

**解决方案**:
1. 检查 Oracle 服务是否启动
2. 验证网络连接: `telnet 192.168.240.128 1521`
3. 确认用户名密码正确
4. 检查 `application-dev.yml` 配置

```powershell
# 测试 Oracle 连接
sqlplus gql/glaway123@192.168.240.128:1521/gdb
```

---

### Q2: Redis 连接失败

**原因**: Redis 服务不可用或配置错误

**解决方案**:
1. 检查 Redis 服务状态
2. 验证网络连接: `telnet 192.168.240.128 6379`
3. 确认密码正确
4. 查看后端日志中的 Redis 相关警告

```powershell
# 测试 Redis 连接
redis-cli -h 192.168.240.128 -p 6379 -a glaway123 ping
```

---

### Q3: 前端无法调用后端 API

**原因**: 跨域问题或代理配置错误

**解决方案**:
1. 确认后端已启动在 8080 端口
2. 检查 `vite.config.ts` 中的代理配置
3. 浏览器控制台查看 Network 面板的错误信息

```typescript
// vite.config.ts 示例
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

---

### Q4: 缓存不生效

**原因**: Redis 连接失败或缓存键错误

**排查步骤**:
1. 检查后端日志是否有 Redis 相关警告
2. 使用 Redis CLI 验证缓存键是否存在:
   ```bash
   redis-cli -h 192.168.240.128 -p 6379 -a glaway123
   > KEYS todo:*
   > GET todo:list
   ```
3. 确认缓存 TTL 设置正确

---

### Q5: 数据库表不存在

**原因**: 未执行建表脚本

**解决方案**:
1. 连接到 Oracle 数据库
2. 按顺序执行以下脚本：
   - `backend/sql/create_todo_item.sql`
   - `backend/sql/alter_todo_item_phase1.sql`
   - `backend/sql/alter_todo_item_phase2.sql`
   - `backend/sql/alter_todo_item_phase3_recurrence.sql`
   - `backend/sql/create_todo_sub_item.sql`
3. 验证表和序列是否创建成功:
   ```sql
   SELECT table_name FROM user_tables WHERE table_name = 'TODO_ITEM';
   SELECT sequence_name FROM user_sequences WHERE sequence_name = 'TODO_ITEM_SEQ';
   ```

---

### Q6: Maven 依赖下载失败

**原因**: 网络问题或 Maven 仓库配置错误

**解决方案**:
1. 检查网络连接
2. 配置国内 Maven 镜像（如阿里云）
3. 清理本地仓库后重新下载:
   ```powershell
   mvn clean
   mvn dependency:purge-local-repository
   mvn install
   ```

---

### Q7: npm install 失败

**原因**: Node.js 版本不兼容或网络问题

**解决方案**:
1. 确认 Node.js 版本 >= 18
2. 配置 npm 国内镜像:
   ```powershell
   npm config set registry https://registry.npmmirror.com
   ```
3. 删除 `node_modules` 和 `package-lock.json` 后重试:
   ```powershell
   Remove-Item -Recurse -Force node_modules
   Remove-Item package-lock.json
   npm install
   ```

---

### Q8: Docker 构建失败

**原因**: Docker 资源不足或网络问题

**解决方案**:
1. 增加 Docker 内存限制（建议 >= 4GB）
2. 清理 Docker 缓存:
   ```powershell
   docker system prune -a
   ```
3. 检查 Dockerfile 中的基础镜像是否可访问

---

## 扩展建议

### 功能扩展

#### 1. 任务优先级

**数据库变更**:
```sql
ALTER TABLE todo_item ADD priority NUMBER(1) DEFAULT 3 
  CHECK (priority IN (1, 2, 3));
COMMENT ON COLUMN todo_item.priority IS '优先级：1-高，2-中，3-低';
```

**后端实现**:
- 在 `TodoItem` 实体中添加 `priority` 字段
- 更新 `TodoItemRequest` DTO
- 修改 Service 层支持优先级排序

**前端实现**:
- 在表单中添加优先级选择器
- 列表中显示优先级标识（不同颜色）

---

#### 2. 任务分类/标签

**数据库设计**:
```sql
CREATE TABLE todo_category (
    id NUMBER(19) PRIMARY KEY,
    name VARCHAR2(50) NOT NULL UNIQUE
);

ALTER TABLE todo_item ADD category_id NUMBER(19);
ALTER TABLE todo_item ADD CONSTRAINT fk_todo_category 
  FOREIGN KEY (category_id) REFERENCES todo_category(id);
```

---

#### 3. 截止日期提醒

**数据库变更**:
```sql
ALTER TABLE todo_item ADD due_date TIMESTAMP;
ALTER TABLE todo_item ADD reminded NUMBER(1) DEFAULT 0;
```

**后端实现**:
- 定时任务扫描即将到期的任务
- 发送通知（邮件/站内信）

---

#### 4. 分页查询

**后端实现**:
```java
@GetMapping
public ResponseEntity<ApiResponse<Page<TodoItem>>> findAll(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    
    Page<TodoItem> todos = todoService.findAll(PageRequest.of(page, size));
    return ResponseEntity.ok(ApiResponse.success("Success", todos));
}
```

**前端实现**:
- 添加分页组件
- 支持页码跳转和每页数量选择

---

### 安全性增强

#### 1. JWT 身份认证

**依赖添加** (`pom.xml`):
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

**实现要点**:
- 用户注册/登录接口
- JWT Token 生成和验证
- 拦截器保护受保护的 API
- 前端存储 Token（localStorage/HttpOnly Cookie）

> 当前项目对 `APP_AUTH_JWT_SECRET` 做了开发期兜底：如果它被显式配置为空字符串，系统会回退到默认开发密钥以避免本地启动失败；但**生产环境不要依赖这个行为**，应始终显式配置你自己的安全密钥。

---

#### 2. API 限流

使用 Spring Cloud Gateway 或自定义拦截器：

```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        String ip = request.getRemoteAddr();
        RateLimiter limiter = limiters.computeIfAbsent(ip, 
            k -> RateLimiter.create(10.0)); // 每秒10次
        
        if (!limiter.tryAcquire()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }
}
```

---

### 测试覆盖

#### 1. 单元测试（Service 层）

```java
@SpringBootTest
class TodoServiceTest {
    
    @Autowired
    private TodoService todoService;
    
    @Test
    void testCreateTodo() {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Test Task");
        request.setStatus("PENDING");
        
        TodoItem todo = todoService.create(request);
        
        assertNotNull(todo.getId());
        assertEquals("Test Task", todo.getTitle());
        assertEquals("PENDING", todo.getStatus());
    }
}
```

#### 2. 集成测试（Controller 层）

```java
@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetAllTodos() throws Exception {
        mockMvc.perform(get("/api/todos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray());
    }
}
```

#### 3. 前端 E2E 测试

使用 Cypress 或 Playwright：

```typescript
// cypress/e2e/todo.cy.ts
describe('Todo List', () => {
  it('should create a new todo', () => {
    cy.visit('/')
    cy.get('.cyber-input').type('Test Task')
    cy.get('.btn-primary').click()
    cy.contains('Test Task').should('be.visible')
  })
})
```

---

### 监控与日志

#### 1. 启用更多 Actuator 端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      probes:
        enabled: true
  metrics:
    export:
      prometheus:
        enabled: true
```

#### 2. Prometheus + Grafana 监控

**docker-compose.yml**:
```yaml
services:
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
```

---

### 性能优化

#### 1. 数据库连接池调优

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### 2. Redis 连接池配置

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 3000ms
```

#### 3. 前端性能优化

- 路由懒加载
- 组件按需引入
- 图片压缩和 CDN
- 开启 Gzip 压缩

---

## 附录

### 项目结构速查

```
personal-toolkit/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/personal/toolkit/
│   │       ├── common/        # 通用组件
│   │       ├── config/        # 配置类
│   │       └── todo/          # Todo 业务模块
│   ├── src/main/resources/
│   │   ├── application.yml    # 主配置文件
│   │   └── application-dev.yml # 开发环境配置
│   ├── sql/                   # 数据库脚本
│   ├── pom.xml                # Maven 配置
│   └── Dockerfile
│
├── frontend/                  # Vue 3 前端
│   ├── src/
│   │   ├── components/        # Vue 组件
│   │   ├── App.vue            # 根组件
│   │   └── main.ts            # 入口文件
│   ├── package.json           # NPM 配置
│   ├── vite.config.ts         # Vite 配置
│   └── Dockerfile
│
├── docs/                      # 文档
│   └── code-style.md          # 代码规范
│
├── docker-compose.yml         # Docker 编排
└── README.md                  # 项目说明
```

### 常用命令速查

```powershell
# === 后端 ===
cd backend
mvn spring-boot:run                          # 启动后端
mvn clean package                            # 打包
mvn test                                     # 运行测试

# === 前端 ===
cd frontend
npm run dev                                  # 开发模式
npm run build                                # 生产构建
npm run preview                              # 预览构建结果

# === Docker ===
docker compose up --build                    # 启动所有服务
docker compose up -d --build                 # 后台启动
docker compose down                          # 停止服务
docker compose logs -f                       # 查看日志
docker compose ps                            # 查看容器状态

# === 数据库 ===
sqlplus gql/glaway123@192.168.240.128:1521/gdb  # 连接 Oracle
redis-cli -h 192.168.240.128 -p 6379 -a glaway123 # 连接 Redis
```

### 端口占用速查

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端开发服务器 | 5173 | Vite Dev Server |
| 后端 API | 8080 | Spring Boot |
| Oracle 数据库 | 1521 | 外部中间件 |
| Redis 缓存 | 6379 | 外部中间件 |
| Prometheus | 9090 | 监控（可选） |
| Grafana | 3000 | 可视化（可选） |

---

## 联系方式与支持

如有问题或建议，请通过以下方式联系：

- 📧 Email: [your-email@example.com]
- 🐛 Issues: [GitHub Issues]
- 📖 文档: `docs/code-style.md`

---

**最后更新**: 2024-01-01  
**版本**: v0.0.1-SNAPSHOT
