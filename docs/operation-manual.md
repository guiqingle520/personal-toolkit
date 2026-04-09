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

- ✅ 完整的 CRUD 操作（创建、查询、更新、删除）
- ✅ 任务状态管理（待处理/已完成）
- ✅ Redis 缓存加速查询性能
- ✅ 统一响应格式和异常处理
- ✅ 赛博朋克风格的现代化 UI
- ✅ Docker 容器化部署支持
- ✅ TypeScript 类型安全保障

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
| Spring Data JPA | - | ORM 数据访问 |
| Oracle JDBC | ojdbc11 | 数据库驱动 |
| Spring Data Redis | - | 缓存集成 |
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

连接到 Oracle 数据库，执行建表脚本：

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

#### 4. 访问应用

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

- **自动加载**: 页面打开时自动从后端加载所有待办事项
- **排序规则**: 按创建时间倒序排列（最新的在前）
- **统计信息**: 显示总任务数和待处理任务数
- **刷新功能**: 点击 "Refresh" 按钮手动同步最新数据

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

#### 1. 查询所有待办事项

**请求**
```http
GET /api/todos
```

**响应** (200 OK)
```json
{
  "success": true,
  "message": "Todo list fetched successfully",
  "data": [
    {
      "id": 1,
      "title": "学习 Spring Boot",
      "status": "PENDING",
      "createTime": "2024-01-01T10:00:00",
      "updateTime": "2024-01-01T10:00:00"
    }
  ],
  "timestamp": "2024-01-01T12:00:00+08:00"
}
```

**cURL 示例**
```bash
curl http://localhost:8080/api/todos
```

---

#### 2. 查询单个待办事项

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

#### 3. 创建待办事项

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

#### 4. 更新待办事项

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

#### 5. 删除待办事项

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

## 数据库设计

### 表结构：todo_item

| 字段名 | 数据类型 | 约束 | 说明 |
|--------|---------|------|------|
| id | NUMBER(19) | PRIMARY KEY | 主键，由序列生成 |
| title | VARCHAR2(200 CHAR) | NOT NULL | 待办事项标题 |
| status | VARCHAR2(32 CHAR) | NOT NULL, DEFAULT 'PENDING' | 状态：PENDING/DONE |
| create_time | TIMESTAMP | NOT NULL, DEFAULT SYSTIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | NOT NULL, DEFAULT SYSTIMESTAMP | 最后更新时间 |

### 约束条件

1. **主键约束**: `PRIMARY KEY (id)`
2. **非空约束**: `title`, `status`, `create_time`, `update_time` 不能为空
3. **检查约束**: `CHECK (status IN ('PENDING', 'DONE'))`

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

[TodoService.java](file:///E:/personalDev/mytodo/personal-toolkit/backend/src/main/java/com/personal/toolkit/todo/service/TodoService.java)

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

详细规范请参考: [docs/code-style.md](file:///E:/personalDev/mytodo/personal-toolkit/docs/code-style.md)

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
2. 执行 `backend/sql/create_todo_item.sql`
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
- 📖 文档: [docs/code-style.md](file:///E:/personalDev/mytodo/personal-toolkit/docs/code-style.md)

---

**最后更新**: 2024-01-01  
**版本**: v0.0.1-SNAPSHOT

