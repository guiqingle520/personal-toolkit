# personal-toolkit

个人工具箱项目，包含：

- `backend`：Spring Boot 3 + Oracle + Redis 的 Todo API
- `frontend`：Vue 3 + TypeScript + Vite 的 Todo 页面

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

## 启动方式

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
