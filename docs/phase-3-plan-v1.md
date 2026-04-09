# Phase 3 Plan v1

## 文档说明

- 文档主题：Todo 3.0 下一阶段功能规划
- 文档版本：v1
- 归档目录：`docs/`
- 适用范围：子任务 / 重复任务 / 看板视图 / 统计面板

---

## 1. Phase 3 的接口 DTO 草案

### 1.1 子任务 DTO

#### TodoSubItemRequest

建议字段：

- `title: String`
- `status: String`
- `sortOrder: Integer`

建议说明：

- `title` 必填，最大长度建议 200
- `status` 首版建议支持 `TODO` / `DONE`
- `sortOrder` 用于前端展示顺序控制

#### TodoSubItemResponse

建议字段：

- `id: Long`
- `todoId: Long`
- `title: String`
- `status: String`
- `sortOrder: Integer`
- `createTime: LocalDateTime`
- `updateTime: LocalDateTime`

#### TodoSubItemSummaryResponse

建议字段：

- `totalCount: Integer`
- `completedCount: Integer`
- `progressPercent: Integer`

用途：

- 用于主任务列表页快速显示 checklist 进度

---

### 1.2 重复任务 DTO

#### TodoRecurrenceRequest

建议字段：

- `recurrenceType: String`
- `recurrenceInterval: Integer`
- `recurrenceEndTime: LocalDateTime`

枚举建议：

- `NONE`
- `DAILY`
- `WEEKLY`
- `MONTHLY`

#### TodoRecurrenceResponse

建议字段：

- `recurrenceType: String`
- `recurrenceInterval: Integer`
- `recurrenceEndTime: LocalDateTime`
- `nextTriggerTime: LocalDateTime`

---

### 1.3 统计 DTO

#### TodoStatsOverviewResponse

建议字段：

- `todayCompleted: Integer`
- `weekCompleted: Integer`
- `overdueCount: Integer`
- `activeCount: Integer`

#### TodoStatsCategoryItemResponse

建议字段：

- `category: String`
- `completedCount: Integer`
- `activeCount: Integer`

#### TodoStatsTrendItemResponse

建议字段：

- `date: String`
- `completedCount: Integer`

#### TodoStatsTrendResponse

建议字段：

- `range: String`
- `items: List<TodoStatsTrendItemResponse>`

---

### 1.4 Todo 主任务扩展 DTO

建议在现有 Todo 返回结构中增加：

- `subItemSummary`
- `recurrenceType`
- `recurrenceInterval`
- `recurrenceEndTime`
- `nextTriggerTime`
- `completedAt`

用途：

- 减少前端进入详情页前的二次查询成本

---

## 2. 数据库实体类设计草案

### 2.1 TodoSubItem

建议新增实体：`TodoSubItem`

字段建议：

- `id: Long`
- `todoId: Long`
- `title: String`
- `status: String`
- `sortOrder: Integer`
- `createTime: LocalDateTime`
- `updateTime: LocalDateTime`

职责：

- 表示主任务下的一条子任务记录
- 用于 checklist 展示、完成率计算和执行拆解

关系建议：

- `TodoItem` 1:N `TodoSubItem`

---

### 2.2 TodoItem 扩展字段

建议在现有 `TodoItem` 中补充：

- `recurrenceType: String`
- `recurrenceInterval: Integer`
- `recurrenceEndTime: LocalDateTime`
- `nextTriggerTime: LocalDateTime`
- `completedAt: LocalDateTime`

职责：

- 支撑重复任务能力
- 支撑统计能力中的完成时间分析

建议约束：

- `recurrenceType` 默认 `NONE`
- `recurrenceInterval` 默认 `1`

---

### 2.3 可选实体：TodoActivityLog

建议作为后续增强，不列入 Sprint 3.1 必做范围。

字段建议：

- `id: Long`
- `todoId: Long`
- `actionType: String`
- `beforeStatus: String`
- `afterStatus: String`
- `operateTime: LocalDateTime`

职责：

- 为复杂统计、审计和时间线展示提供数据基础

---

## 3. Sprint 3.1（子任务）详细实施计划

### 3.1 目标

完成 Todo 子任务 / Checklist 能力的第一版闭环，实现：

- 子任务 CRUD
- 子任务完成状态切换
- 主任务进度汇总
- 前端 checklist 展示与交互

---

### 3.2 后端实施步骤

#### Step 1：数据库变更

- 新增 `todo_sub_item` 表
- 新增索引与外键
- 新增序列 `todo_sub_item_seq`

#### Step 2：实体与 Repository

- 新增 `TodoSubItem` 实体类
- 新增 `TodoSubItemRepository`

建议能力：

- 按 `todoId` 查询子任务列表
- 按 `todoId + id` 查询单个子任务
- 按 `todoId` 删除或统计

#### Step 3：DTO 与 Service

- 新增 `TodoSubItemRequest`
- 新增 `TodoSubItemResponse`
- 新增 `TodoSubItemSummaryResponse`
- 新增 `TodoSubItemService`

Service 需要处理：

- 主任务存在性校验
- 标题非空校验
- 排序字段默认值
- 状态切换
- 汇总进度计算

#### Step 4：Controller

新增接口：

- `GET /api/todos/{id}/sub-items`
- `POST /api/todos/{id}/sub-items`
- `PUT /api/todos/{todoId}/sub-items/{subItemId}`
- `DELETE /api/todos/{todoId}/sub-items/{subItemId}`

建议补一个：

- `GET /api/todos/{id}/sub-items/summary`

#### Step 5：后端测试

- Service：
  - 创建子任务
  - 更新子任务
  - 删除子任务
  - 主任务不存在返回 404
  - 汇总统计正确
- Controller：
  - 成功响应壳正确
  - 字段校验错误响应正确

---

### 3.3 前端实施步骤

#### Step 1：API 接入

在 Todo 容器层新增：

- 查询子任务
- 创建子任务
- 更新子任务
- 删除子任务

#### Step 2：组件拆分

建议新增：

- `TodoSubItemList.vue`
- `TodoSubItemRow.vue`

#### Step 3：交互设计

主任务项下展示：

- 子任务列表
- 新增输入框
- 完成勾选
- 删除按钮
- 进度条 / 比例文案

#### Step 4：前端测试

- 子任务列表渲染正确
- 勾选完成会触发正确请求
- 新增与删除行为正确
- 进度显示正确

---

### 3.4 验收标准

#### 功能验收

- 主任务下可以新增子任务
- 子任务可以勾选完成与删除
- 主任务显示已完成数量与总数
- 刷新后子任务数据可正确回显

#### 接口验收

- 所有子任务接口返回统一 `ApiResponse`
- 错误响应符合当前项目统一错误结构

#### 测试验收

- 后端相关测试通过
- 前端相关测试通过
- 前端构建通过

---

### 3.5 风险与约束

- 首版不做子任务拖拽排序，可只保留 `sortOrder` 字段做后续扩展
- 首版不做子任务嵌套，保持单层 checklist
- 首版主任务状态是否自动联动子任务完成率，建议先不自动强绑定，避免语义过重

---

### 3.6 后续版本演进建议

若 Sprint 3.1 落地顺利，下一版可新增：

- 子任务拖拽排序
- 子任务批量完成
- 主任务自动完成规则
- 子任务时间预估

---

## 版本变更说明

### v1

- 首次归档 Phase 3 方案
- 包含接口 DTO 草案
- 包含数据库实体类设计草案
- 包含 Sprint 3.1（子任务）详细实施计划
