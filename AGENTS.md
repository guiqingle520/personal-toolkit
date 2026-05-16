# AGENTS.md

## 先看这些
- `README.md` 是仓库级的主要安装、运行与迁移事实来源。
- `docs/code-style.md` 中的规则属于仓库约束，应按强制要求执行，而不是按建议理解。

## 仓库结构
- `backend/` 为基于 Spring Boot 3.3.5 + Java 17 的后端服务。
- `frontend/` 为基于 Vue 3 + TypeScript + Vite 的前端应用。
- 当前仓库根目录没有 monorepo 任务编排器、CI 工作流，也没有 repo-local 的 OpenCode 配置文件。

## 精确开发命令
- 前端安装依赖：`npm install`（在 `frontend/` 目录执行）
- 前端开发服务器：`npm run dev`
- 前端测试：`npm test`
- 单个前端测试文件：`npx vitest run src/components/TodoList.test.ts`
- 前端构建：`npm run build`
- 后端本地启动：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`（在 `backend/` 目录执行）
- 后端测试：`mvn test`
- 后端打包校验：`mvn package`
- 使用容器启动整套前后端：`docker compose up --build`

## 环境与基础设施注意点
- `backend/src/main/resources/application.yml` 默认启用 Spring 的 `dev` profile。
- `backend/src/main/resources/application-dev.yml` 默认连接共享 Oracle/Redis 主机 `192.168.240.128`。除非显式覆盖环境变量，否则不要默认数据库和 Redis 运行在本地。
- `docker-compose.yml` 仅启动前端和后端容器，**不会** 启动 Oracle 或 Redis。
- JPA 配置为 `ddl-auto: none`。表结构变更应通过 `backend/sql/` 下的 SQL 脚本管理，不应依赖 Hibernate 自动建表。

## 会影响改动方向的架构事实
- 后端鉴权采用无状态 JWT。`SecurityConfig` 仅放行 `/api/auth/register`、`/api/auth/login`、`/api/auth/captcha`、`/api/auth/login-policy`、健康检查以及 `OPTIONS`；其余接口均要求认证。
- 所有后端控制器统一返回 `ApiResponse<T>` 响应包裹结构。
- 认证相关逻辑集中在 `backend/src/main/java/com/personal/toolkit/auth/`；Todo 相关能力拆分在 `.../todo/` 下的 `TodoController`、`TodoReminderController`、`TodoSavedViewController`、`TodoSubItemController`。
- `PersonalToolkitApplication` 启用了 `@EnableScheduling`，提醒相关能力依赖定时任务驱动。
- 前端启动入口为 `frontend/src/main.ts`，其中完成样式加载、i18n 语言同步、主题初始化以及 router 挂载。
- `frontend/src/App.vue` 通过 token 是否存在决定是否渲染已登录壳层；未登录时直接显示 `AuthScreen`，而不是依赖统一路由守卫拦截。
- 前端认证状态从 `localStorage` 恢复；当前已核对的启动链路不会在启动阶段通过 `/api/auth/me` 重新校准 session。
- `frontend/src/composables/useAuth.ts` 使用 `personal-toolkit-auth-token` 与 `personal-toolkit-auth-user` 两个 `localStorage` key 持久化 token 和用户信息。
- `frontend/src/api.ts` 会自动附带 Bearer Token，并且仅在 `401` 时清理本地 session；`403` **不会** 自动触发登出，需要由页面调用方自行处理。
- `frontend/src/router/index.ts` 会将 `passwordChangeRequired` 用户强制引导至 `/change-password`，并将不需要改密的用户从该路由重定向离开。
- 前端 API 默认使用相对路径 `/api`。本地开发时，Vite 会将 `/api` 代理到 `VITE_API_PROXY_TARGET`，默认值为 `http://localhost:8080`；在 `docker-compose.yml` 中，该目标被改写为 `http://backend:8080`。

## 登录 / 验证码 / 安全策略相关细节
- 登录并非只有用户名+密码。后端还暴露了 `/api/auth/login-policy` 和 `/api/auth/captcha`，并且登录标识既可以是用户名，也可以是邮箱。
- README 已明确说明两种登录模式：固定验证码模式与自适应验证码模式。除非先核对后端设置，否则不要将验证码前端逻辑误判为“无用代码”并删除。
- 安全设置属于运行时策略接口 `/api/auth/security-policy`。访问控制依赖 `APP_AUTH_SECURITY_POLICY_BOOTSTRAP_ALLOWLIST`，其默认值为空，并按当前用户名或邮箱进行不区分大小写匹配。
- 密码过期用户不是通过服务端 session 特殊处理，而是通过 `passwordChangeRequired` 机制作为“受限已登录用户”处理。
- 当密码策略拦截一个原本有效的 token 时，`JwtAuthenticationFilter` 仅对 `/api/auth/change-password` 和 `/api/auth/logout` 放行。

## 测试相关提示
- 前端测试配置定义在 `frontend/vite.config.ts` 中（`vitest`、`jsdom`、`globals`），仓库内没有单独的 Vitest 配置文件。
- `frontend/tsconfig.json` 排除了 `src/**/*.test.ts`，因此 `npm run build` 会检查应用代码类型，但不会覆盖测试文件；测试仍需单独执行。
- 后端控制器测试采用 `@WebMvcTest` + mock 的安全/认证依赖。如果调整了控制器契约或安全行为，应优先同步这些测试，而不是依赖完整集成环境兜底。

## 仓库特有编码规则
- 严格遵守 `docs/code-style.md`，其中明确要求实体类和实现方法必须具备有信息量的注释。
- 所有前端用户可见文案都必须同时接入 `zh-CN` 与 `en` 国际化资源。
- 该 i18n 规则同样覆盖 tooltip、`title`、`aria-label` 等可见或可读描述，不应直接硬编码在组件中。
- 正式方案或计划文档应归档到 `docs/`，并采用版本化命名（如 `*-v1.md`、`*-v2.md`），不要仅保留在对话中。

## 数据库迁移顺序很重要
- `README.md` 记录了基础迁移与回填流程，尤其是必须按顺序执行的两阶段 `user_id` 迁移；不要随意发明新顺序。
- 当前仓库的 `backend/sql/` 中已经包含超出旧版 README 主流程的特性脚本，包括安全策略、密码状态、重复任务、checklist/子项、保存视图、提醒、备注/附件、协作占位等。开始改动前，应先确认本次功能实际依赖哪些脚本，不要默认 README 已覆盖全部现状。
