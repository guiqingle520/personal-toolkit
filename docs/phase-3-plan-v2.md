# Phase 3 Plan v2

## 文档说明

- 文档主题：Todo 3.0 Phase 3 后续功能规划（统计面板 + 看板视图）
- 文档版本：v2
- 归档目录：`docs/`
- 适用范围：统计面板 / 看板视图
- 前置状态：
  - Sprint 3.1 子任务 / Checklist 已完成
  - Sprint 3.2 重复任务已完成

---

## 相对 v1 的变化说明

### v1 已覆盖内容

- 子任务 / Checklist DTO 与实施计划
- 重复任务 DTO 草案
- 统计 DTO 草案
- Todo 主任务扩展字段草案

### v2 的新增与调整

- 明确确认 Sprint 3.1（子任务）与 Sprint 3.2（重复任务）已进入已完成状态
- 将 Phase 3 后续实施重心收敛为：
  - Sprint 3.3：统计面板
  - Sprint 3.4：看板视图
- 将统计部分从 DTO 草案升级为正式实施方案
- 明确看板视图首版为静态分列，不纳入拖拽改状态

---

## 1. Phase 3 当前阶段结论

结合当前仓库状态，Phase 3 前两段已经具备闭环能力：

### 1.1 已完成能力

- 子任务 / Checklist
  - 子任务 CRUD
  - 子任务完成状态切换
  - 主任务进度汇总
  - 前端 checklist 展示与交互

- 重复任务
  - 支持 `NONE` / `DAILY` / `WEEKLY` / `MONTHLY`
  - 支持重复间隔
  - 支持重复截止时间
  - 支持 `completedAt`
  - 当前执行模型为：**完成当前重复任务时生成下一条实例**

### 1.2 Phase 3 后续实施重点

后续阶段聚焦两个方向：

1. **Sprint 3.3：统计面板**
2. **Sprint 3.4：看板视图**

建议按上述顺序推进，不交叉并行实施。

---

## 2. Sprint 3.3 目标

在当前 Todo 模块基础上，补齐第一版统计面板能力，实现：

1. **概览统计**
   - 今日完成数
   - 本周完成数
   - 逾期任务数
   - 活动任务数

2. **分类统计**
   - 按分类统计待办数量
   - 按分类统计已完成数量
   - 支持无分类任务归并展示

3. **最近 7 天完成趋势**
   - 按天统计完成数
   - 用于前端趋势展示

---

## 3. 设计原则

### 3.1 首版只做只读统计

Sprint 3.3 只负责统计展示，不引入新的编辑能力，不改变现有 Todo 主流程。

### 3.2 优先复用现有字段

首版统计优先基于当前已经落地的数据能力实现，不引入新的行为型实体。

优先使用：

- `status`
- `category`
- `dueDate`
- `completedAt`
- `deletedAt`

### 3.3 回收站数据默认不纳入统计

逻辑删除任务不进入统计结果，保持统计面板与活动任务视图口径一致。

### 3.4 先保证口径，再增强表现

首版重点保证统计口径清晰、接口稳定、前后端一致；图表美化、钻取和筛选联动放到后续版本。

---

## 4. 统计口径定义

### 4.1 今日完成数

定义：

- `completedAt` 落在“今天 00:00:00 ~ 今天 23:59:59”的任务数量
- 仅统计 `deletedAt is null` 的任务

### 4.2 本周完成数

定义：

- `completedAt` 落在当前自然周范围内的任务数量
- 建议按**周一为一周开始**
- 仅统计 `deletedAt is null` 的任务

### 4.3 逾期任务数

定义：

- `status != DONE`
- `dueDate < 当前时间`
- `deletedAt is null`

### 4.4 活动任务数

定义：

- `status != DONE`
- `deletedAt is null`

### 4.5 分类统计

定义：

- 按 `category` 分组
- 输出每个分类下：
  - `activeCount`
  - `completedCount`

特殊处理：

- `category` 为空或空白时，统一归并为“未分类”

### 4.6 最近 7 天完成趋势

定义：

- 统计最近连续 7 天内每日完成的任务数量
- 无数据日期也要补 0
- 返回结果按日期升序排列

说明：

- 使用 `completedAt` 作为统计基准
- 首版只做 7 天，不开放动态范围切换

---

## 5. 接口设计

### 5.1 概览统计接口

#### GET `/api/todos/stats/overview`

返回建议：

```json
{
  "success": true,
  "message": "Todo stats overview fetched successfully",
  "data": {
    "todayCompleted": 3,
    "weekCompleted": 12,
    "overdueCount": 4,
    "activeCount": 18
  },
  "timestamp": "2026-04-10T10:00:00+08:00"
}
```

### 5.2 分类统计接口

#### GET `/api/todos/stats/by-category`

返回建议：

```json
{
  "success": true,
  "message": "Todo stats by category fetched successfully",
  "data": [
    {
      "category": "Work",
      "activeCount": 6,
      "completedCount": 10
    },
    {
      "category": "Personal",
      "activeCount": 3,
      "completedCount": 5
    },
    {
      "category": "未分类",
      "activeCount": 2,
      "completedCount": 1
    }
  ],
  "timestamp": "2026-04-10T10:00:00+08:00"
}
```

### 5.3 最近 7 天趋势接口

#### GET `/api/todos/stats/trend?range=7d`

首版虽然只支持 7 天，但建议保留 `range` 参数入口，便于后续扩展。

返回建议：

```json
{
  "success": true,
  "message": "Todo stats trend fetched successfully",
  "data": {
    "range": "7d",
    "items": [
      { "date": "2026-04-04", "completedCount": 1 },
      { "date": "2026-04-05", "completedCount": 0 },
      { "date": "2026-04-06", "completedCount": 2 },
      { "date": "2026-04-07", "completedCount": 4 },
      { "date": "2026-04-08", "completedCount": 1 },
      { "date": "2026-04-09", "completedCount": 3 },
      { "date": "2026-04-10", "completedCount": 2 }
    ]
  },
  "timestamp": "2026-04-10T10:00:00+08:00"
}
```

---

## 6. 后端实施方案

### 6.1 DTO 新增

建议新增：

- `TodoStatsOverviewResponse`
- `TodoStatsCategoryItemResponse`
- `TodoStatsTrendItemResponse`
- `TodoStatsTrendResponse`

### 6.2 Repository 能力扩展

建议在 `TodoRepository` 中补充统计查询能力。

#### 概览统计

建议支持：

- 今日完成数统计
- 本周完成数统计
- 逾期任务数统计
- 活动任务数统计

#### 分类统计

建议支持：

- 按 `category` 聚合 `status != DONE`
- 按 `category` 聚合 `status = DONE`

#### 趋势统计

建议支持：

- 按 `completedAt` 日期聚合最近 7 天完成数

实现说明：

- 数据库查询只返回有值日期
- Service 层负责补齐缺失日期为 0

### 6.3 Service 设计

建议新增：

- `TodoStatsService`

建议方法：

- `getOverview()`
- `getCategoryStats()`
- `getTrend(String range)`

职责：

- 统一处理统计口径
- 统一处理时间边界
- 统一处理无分类映射
- 统一处理趋势补零逻辑
- 为后续统计缓存预留扩展点

### 6.4 是否需要缓存

首版建议：

- **可以先不单独引入统计缓存**
- 先以正确性和可维护性为主

### 6.5 Controller 设计

建议新增：

- `TodoStatsController`

推荐路径：

- `/api/todos/stats/overview`
- `/api/todos/stats/by-category`
- `/api/todos/stats/trend`

---

## 7. 前端实施方案

### 7.1 页面接入方式

建议在当前 `TodoList` 主页面中新增一个**统计面板区域**，优先采用“列表页上方展示”的方式，而不是单独开新页面。

### 7.2 前端组件拆分建议

建议新增组件：

- `TodoStatsPanel.vue`
- `TodoStatsOverviewCards.vue`
- `TodoStatsCategoryList.vue`
- `TodoStatsTrendChart.vue`

若首版希望控制复杂度，也可以先收敛为一个 `TodoStatsPanel.vue`，内部拆为三个区域。

### 7.3 展示结构建议

#### 概览统计卡片

展示四张卡片：

- 今日完成
- 本周完成
- 逾期任务
- 活动任务

#### 分类统计

展示建议：

- 分类名称
- 活动数量
- 已完成数量

首版可先采用简单列表或轻量占比条，不强依赖第三方图表库。

#### 最近 7 天趋势

首版建议：

- 不强制引入 ECharts / Chart.js
- 可先做简单趋势条 / 柱状块 / 数值序列

### 7.4 前端数据模型建议

建议新增类型：

- `TodoStatsOverview`
- `TodoStatsCategoryItem`
- `TodoStatsTrendItem`
- `TodoStatsTrend`

### 7.5 国际化要求

新增前端展示文案时，必须同步维护：

- `frontend/src/locales/zh-CN.ts`
- `frontend/src/locales/en.ts`

至少包含：

- 统计面板标题
- 今日完成
- 本周完成
- 逾期任务
- 活动任务
- 分类统计
- 最近 7 天
- 未分类
- 加载中 / 暂无数据

---

## 8. 测试方案

### 8.1 后端测试

#### Service 测试

至少覆盖：

- 今日完成数统计正确
- 本周完成数统计正确
- 逾期任务统计正确
- 活动任务统计正确
- 分类统计正确
- 空分类归并正确
- 趋势数据按天补零正确

#### Controller 测试

至少覆盖：

- 三个统计接口成功返回统一响应壳
- 返回字段结构符合约定
- `range=7d` 返回正确结构

### 8.2 前端测试

至少覆盖：

- 统计面板渲染成功
- 概览卡片展示正确
- 分类统计渲染正确
- 趋势数据渲染正确
- 空数据态展示正确
- 国际化文案切换正确

### 8.3 构建验收

必须通过：

- 后端测试
- 前端测试
- 前端构建

---

## 9. 验收标准

### 9.1 功能验收

满足以下条件即视为 Sprint 3.3 完成：

1. Todo 页面可展示统计面板
2. 可正确看到概览统计
3. 可正确看到分类统计
4. 可正确看到最近 7 天完成趋势
5. 中文、英文文案都完整可用

### 9.2 数据口径验收

满足以下条件：

1. 已删除任务不进入统计
2. 逾期任务只统计未完成项
3. 趋势数据缺失日期会补 0
4. 无分类任务会统一归并展示

### 9.3 工程验收

满足以下条件：

1. 后端测试通过
2. 前端测试通过
3. 前端构建通过
4. 新增代码满足注释规范和国际化规范

---

## 10. 风险与约束

### 10.1 首版不引入复杂图表库

首版统计趋势建议先使用轻量展示，不急于接入重型图表依赖。

### 10.2 首版不做统计钻取

首版不支持点击统计卡片跳转筛选结果页，不做联动过滤。

### 10.3 首版不做统计缓存

如后续性能需要，再补统计缓存和失效策略。

---

## 11. Sprint 3.4 规划衔接

Sprint 3.3 完成后，进入：

## Sprint 3.4：看板视图

建议顺序：

1. 先做静态分列
2. 支持按状态分栏展示任务
3. 支持列表视图 / 看板视图切换
4. 拖拽改状态放到后续增强版

---

## 版本变更说明

### v1

- 首次归档 Phase 3 方案
- 包含接口 DTO 草案
- 包含数据库实体类设计草案
- 包含 Sprint 3.1（子任务）详细实施计划

### v2

- 明确确认 Sprint 3.1（子任务）与 Sprint 3.2（重复任务）已完成
- 将 Phase 3 后续实施重点收敛为 Sprint 3.3 统计面板与 Sprint 3.4 看板视图
- 将统计部分从 DTO 草案升级为正式实施方案
- 明确看板视图首版为静态分列，不包含拖拽改状态
