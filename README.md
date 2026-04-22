# personal-toolkit

个人工具箱项目，包含：

- `backend`：Spring Boot 3 + Oracle + Redis 的 Todo API，现已接入 JWT 登录鉴权
- `frontend`：Vue 3 + TypeScript + Vite 的 Todo 页面，现已接入登录 / 注册 / 退出

## 目录结构

```text
personal-toolkit
├─ backend
│  ├─ pom.xml
│  ├─ sql
│  │  └─ create_todo_item.sql
│  └─ src
│     └─ main
│        ├─ java/com/personal/toolkit
│        │  ├─ PersonalToolkitApplication.java
│        │  ├─ config/RedisConfig.java
│        │  └─ todo
│        │     ├─ controller/TodoController.java
│        │     ├─ dto/TodoItemRequest.java
│        │     ├─ entity/TodoItem.java
│        │     ├─ repository/TodoRepository.java
│        │     └─ service/TodoService.java
│        └─ resources/application.yml
└─ frontend
   ├─ index.html
   ├─ package.json
   ├─ tsconfig.json
   ├─ vite.config.ts
   └─ src
      ├─ App.vue
      ├─ main.ts
      └─ components/TodoList.vue
```

## 开发环境说明

当前项目默认通过 `dev` profile 连接你现有的中间件环境：

- Oracle 19.3：`192.168.240.128:1521/gdb`
- 用户名：`gql`
- 密码：`glaway123`
- Redis：`192.168.240.128:6379`
- Redis 密码：`glaway123`

对应配置文件：

- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application.yml`

### JWT 配置说明

当前后端通过以下配置签发和校验 JWT：

- `APP_AUTH_JWT_SECRET`：JWT 对称签名密钥，**推荐使用 Base64 编码后的 32 字节及以上随机密钥**
- `APP_AUTH_JWT_ISSUER`：JWT 签发方，默认值为 `personal-toolkit-backend`
- `APP_AUTH_JWT_EXPIRATION`：JWT 过期时间，默认值为 `PT12H`

项目已内置一个仅用于本地开发的 Base64 默认密钥；在测试、联调、生产环境中，**强烈建议通过环境变量覆盖**。

如果 `APP_AUTH_JWT_SECRET` 被显式配置为空字符串，当前实现也会回退到默认开发密钥，以避免本地启动失败；但**生产环境不要依赖这个兜底行为**，应始终显式提供你自己的安全密钥。

PowerShell 示例：

```powershell
$env:APP_AUTH_JWT_SECRET = "your-base64-secret"
$env:APP_AUTH_JWT_ISSUER = "personal-toolkit-backend"
$env:APP_AUTH_JWT_EXPIRATION = "PT12H"
```

如果你需要生成新的 Base64 密钥，可以使用下面这条 PowerShell 命令：

```powershell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

## 启动方式

### 数据库初始化与旧数据迁移

当前版本已从单用户 Todo 升级为“账号 + Todo 数据归属”模型，数据库初始化与迁移建议按以下顺序执行：

1. `backend/sql/create_app_user.sql`
2. `backend/sql/create_todo_item.sql`
3. `backend/sql/alter_todo_item_add_user_id_phase1.sql`（仅老库升级时需要）
4. `backend/sql/alter_todo_item_add_user_id_phase2.sql`（仅老库升级时需要）
5. `backend/sql/alter_todo_item_phase4_reminder.sql`（已存在旧 Todo 表时需要）
6. `backend/sql/alter_todo_item_phase5_notes_attachments.sql`（已存在旧 Todo 表时需要）
7. `backend/sql/alter_todo_item_phase6_collaboration_placeholders.sql`（已存在旧 Todo 表时需要）
8. `backend/sql/alter_todo_item_phase7_reminder_events.sql`（启用站内提醒闭环时需要）

其中：

- `create_app_user.sql`：创建登录用户表与序列
- `create_todo_item.sql`：创建 Todo 主表
- `alter_todo_item_add_user_id_phase1.sql`：先给历史 Todo 表增加可空 `user_id` 字段
- `alter_todo_item_add_user_id_phase2.sql`：在旧数据回填完成后，补齐 `NOT NULL`、外键和索引

> 如果是全新建库，`create_todo_item.sql` 已经直接包含 `user_id`、外键和索引，不需要再执行这两个升级脚本。

### 旧数据归属回填说明

如果数据库里已经存在历史 Todo 数据，需要先确定一个“接管旧数据”的账号。推荐做法是：**先通过前端注册或调用 `/api/auth/register` 创建目标账号，再执行 SQL 回填**。

下面给出一套推荐 SQL 流程，假设你要把旧数据统一归到账号 `legacy_owner`。

#### 第一步：确认目标账号已经存在

```sql
SELECT id, username, email
FROM app_user
WHERE username = 'legacy_owner';
```

如果查不到记录，请先注册这个账号，再继续后续步骤。

#### 第二步：检查当前仍未绑定账号的 Todo 数量

```sql
SELECT COUNT(*) AS unbound_todo_count
FROM todo_item
WHERE user_id IS NULL;
```

#### 第三步：先执行 Phase 1 扩字段脚本

```sql
@backend/sql/alter_todo_item_add_user_id_phase1.sql
```

如果你使用的工具不支持 `@`，也可以直接打开并执行脚本内容。

#### 第四步：执行旧数据回填

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

#### 第五步：校验回填结果

```sql
SELECT COUNT(*) AS remaining_unbound_count
FROM todo_item
WHERE user_id IS NULL;

SELECT user_id, COUNT(*) AS todo_count
FROM todo_item
GROUP BY user_id
ORDER BY user_id;
```

#### 第六步：执行 Phase 2 收口脚本

当 `remaining_unbound_count = 0` 后，再执行第二阶段脚本：

```sql
-- 先将脚本里的 :legacy_user_id 替换成真实用户主键，
-- 或在 SQL 工具中绑定变量后再执行
@backend/sql/alter_todo_item_add_user_id_phase2.sql
```

> `backend/sql/alter_todo_item_add_user_id.sql` 已保留为弃用说明文件，不建议继续直接使用。

### 方式一：直接本地启动后端

```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 方式二：使用 Docker Compose 启动前后端

> 当前 `docker-compose.yml` 默认不再启动本地 Oracle/Redis，而是复用现有中间件。

```powershell
docker compose up --build
```

### 前端单独启动

```powershell
cd frontend
npm install
npm run dev
```

### 首次使用

1. 打开前端页面 `http://localhost:5173`
2. 先注册账号（用户名 + 邮箱 + 密码）
3. 注册成功后前端会自动进入已登录状态
4. 后续可使用**用户名或邮箱**配合密码登录，请求会自动携带 JWT Token，Todo 数据只会展示当前账号名下的数据
5. 点击页面右上角的“断开连接 / Disconnect”可退出当前登录态

## 代码注释规范

项目代码注释规范已统一沉淀到以下文件，请以后以该文档为唯一维护入口：

- `docs/code-style.md`

其中明确约束了：

- 实体类注释要求
- 实现方法注释要求
- 前端国际化要求（菜单、按钮、图标说明等用户可见内容需支持中文/英文）
- 方案文档归档要求（正式方案需归档到 `docs/`，变更需按版本迭代）
- Java / Vue / TypeScript 的注释风格
- 注释失真后的同步更新要求

## 当前项目状态

当前 Todo 模块已完成并验收通过的阶段：

- Phase 3 / Sprint 3.1：子任务 / Checklist
- Phase 3 / Sprint 3.2：重复任务 / Recurrence
- Phase 3 / Sprint 3.3：统计面板 / Stats Panel
- Phase 3 / Sprint 3.4：看板视图 / Static Kanban View

当前系统已具备：

- JWT 登录 / 注册 / 退出
- 支持用户名或邮箱登录
- 登录随机验证码（SVG）
- 登录失败按 IP / 标识限流
- 当前用户登录态恢复（前端本地持久化）
- Todo 数据按账号隔离
- Todo 基础 CRUD
- Todo 提醒时间
- Todo 站内提醒列表 / 已读 / 全部已读
- Todo Saved Views（保存筛选 / 设默认 / 重命名 / 删除）
- 筛选 / 分页 / 回收站
- 时间预设筛选（今日到期 / 逾期 / 即将提醒）
- 重复类型筛选与活动筛选 chips
- 长备注与附件链接
- 协作能力预留（负责人 / 协作人 / 观察者占位字段）
- Checklist / 子任务
- 重复任务
- 统计面板
- 列表 / 看板视图切换
- 中英文国际化

## 文档索引

- `docs/code-style.md`：代码规范与文档归档要求
- `backend/sql/create_app_user.sql`：用户表初始化脚本
- `backend/sql/alter_todo_item_add_user_id_phase1.sql`：Todo 账号归属迁移第一阶段脚本
- `backend/sql/alter_todo_item_add_user_id_phase2.sql`：Todo 账号归属迁移第二阶段脚本
- `backend/sql/alter_todo_item_phase4_reminder.sql`：Todo 提醒时间字段扩展脚本
- `backend/sql/alter_todo_item_phase5_notes_attachments.sql`：Todo 备注与附件链接字段扩展脚本
- `backend/sql/alter_todo_item_phase6_collaboration_placeholders.sql`：Todo 协作占位字段扩展脚本
- `backend/sql/alter_todo_item_phase7_reminder_events.sql`：Todo 站内提醒事件表脚本
- `docs/phase-3-plan-v1.md`：Phase 3 初始规划
- `docs/phase-3-plan-v2.md`：Phase 3 中期实施版
- `docs/phase-3-plan-v3.md`：Phase 3 收尾归档版
- `docs/phase-4-plan-v1.md`：Phase 4 起始规划版
- `docs/operation-manual.md`：操作手册

## 站内提醒说明

当前 Phase 5 / Sprint 5.1 已补齐站内提醒闭环，行为如下：

1. Todo 设置 `remindAt` 后，后端会为该任务同步一条待发送提醒事件。
2. 定时扫描任务默认每 60 秒执行一次，将 `scheduled_at <= 当前时间` 的提醒转为站内未读提醒。
3. 前端活动任务页会显示提醒面板，支持：
   - 查看未读提醒
   - 标记单条已读
   - 全部已读
   - 从提醒回到对应任务
4. 统计面板新增 `未读提醒 / Unread reminders` 指标。

当前提醒闭环仍然是站内能力，尚未接入邮件、短信或 IM 推送。

## Saved Views 说明

当前 Phase 5 / Sprint 5.2 已补齐 Saved Views 的第一版能力，行为如下：

1. 用户可以把当前筛选条件保存成一个命名视图。
2. 保存视图支持：
   - 应用
   - 设为默认
   - 重命名
   - 删除
3. 页面初始化时，如果存在默认视图，会先应用默认视图再请求列表数据。
4. 当前持久化的字段仅包括白名单筛选项，不包含分页参数 `page/size`。

当前首版前端使用轻量交互，不包含独立弹窗和共享视图能力。

## 登录验证码联调说明

当前登录流程支持两种模式：

- 固定验证码模式：先获取验证码，再提交登录请求。
- 自适应验证码模式：首次可直接提交用户名/密码；如果服务端判定当前登录存在风险，会返回 `CAPTCHA_REQUIRED`，前端再补拉验证码并重试。

前端可先调用 `GET /api/auth/login-policy` 获取当前模式。

### 登录策略接口返回

```json
{
  "success": true,
  "message": "Login policy fetched successfully",
  "data": {
    "captchaEnabled": true,
    "adaptiveCaptcha": false,
    "adaptiveTriggerThreshold": 2
  }
}
```

### 固定验证码模式接口顺序

1. `GET /api/auth/captcha`
2. `POST /api/auth/login`

### 验证码接口返回

```json
{
  "success": true,
  "message": "Captcha generated successfully",
  "data": {
    "captchaId": "<captcha-id>",
    "image": "data:image/svg+xml;base64,...",
    "expiresInSeconds": 120
  }
}
```

### 登录请求示例

```json
{
  "username": "alice@example.com",
  "password": "password123",
  "captchaId": "<captcha-id>",
  "captchaCode": "AB12C"
}
```

### 常见错误码

- `400`：`CAPTCHA_REQUIRED`
- `400`：`Captcha expired or invalid`
- `400`：`Captcha verification failed`
- `401`：`Invalid username or password`
- `429`：`Too many captcha requests, please try again later`
- `429`：`Too many failed login attempts, please try again later`

### 安全策略配置

后端支持通过环境变量调整验证码与限流策略：

- `APP_AUTH_CAPTCHA_ENABLED`
- `APP_AUTH_CAPTCHA_ADAPTIVE`
- `APP_AUTH_CAPTCHA_ADAPTIVE_TRIGGER_THRESHOLD`
- `APP_AUTH_CAPTCHA_LENGTH`
- `APP_AUTH_CAPTCHA_MAX_ATTEMPTS`
- `APP_AUTH_CAPTCHA_TTL`
- `APP_AUTH_CAPTCHA_ISSUE_THRESHOLD`
- `APP_AUTH_CAPTCHA_ISSUE_WINDOW`
- `APP_AUTH_CAPTCHA_LOGIN_FAIL_THRESHOLD`
- `APP_AUTH_CAPTCHA_LOGIN_FAIL_WINDOW`

当前默认策略：

- 验证码始终开启
- 自适应验证码默认关闭
- 自适应验证码触发阈值默认 2 次失败
- 有效期 120 秒
- 最多尝试 3 次
- 验证码获取限流：1 分钟 20 次 / IP
- 登录失败限流：15 分钟 5 次 / IP 或标识
