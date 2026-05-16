# CLAUDE.md

## 项目概述
Personal Toolkit 是一个个人工具集应用，包含待办事项管理、日历、统计等功能。

## 技术栈
- **前端**: Vue 3 + TypeScript + Vite
- **后端**: Spring Boot 3.3.5 + Java 17
- **数据库**: Oracle
- **缓存**: Redis
- **容器化**: Docker Compose

## 开发环境

### 前端开发
```bash
cd frontend
npm install
npm run dev
```

### 后端开发
```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 完整环境启动
```bash
docker compose up --build
```

## 重要配置文件
- `backend/src/main/resources/application.yml` - 主配置文件
- `backend/src/main/resources/application-dev.yml` - 开发环境配置
- `frontend/vite.config.ts` - Vite 配置
- `docker-compose.yml` - Docker 编排配置

## 数据库迁移
所有数据库变更脚本位于 `backend/sql/` 目录，必须按顺序执行。

## API 文档
- 认证接口: `/api/auth/*`
- Todo 接口: `/api/todo/*`
- 所有响应统一使用 `ApiResponse<T>` 格式

## 测试
- 前端测试: `npm test` (在 frontend 目录)
- 后端测试: `mvn test` (在 backend 目录)

## 代码规范
详细规范请参考 `docs/code-style.md`

## 注意事项
1. 默认数据库和 Redis 连接地址为 `192.168.240.128`
2. JPA 配置为 `ddl-auto: none`，不自动建表
3. 后端采用无状态 JWT 认证
4. 所有用户可见文案需要支持中英文国际化
