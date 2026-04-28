<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import type { 
  TodoStatsCategoryItem, 
  TodoStatsOverview, 
  TodoStatsTrendItem,
  TodoStatsTrendSummary,
  TodoStatsDueBuckets,
  TodoStatsPriorityDistribution,
  TodoStatsAging,
  TodoReminderSummary,
  TodoStatsRecurrenceDistribution
} from './types'
import {
  buildDashboardCategories,
  buildDashboardKpis,
  buildDashboardSnapshot,
  buildDashboardTrend,
  buildDashboardDueBuckets,
  buildDashboardPriorities,
  buildDashboardAging,
  buildDashboardReminderSummary,
  buildDashboardRecurrence
} from './todoStatsDashboard'

const props = defineProps<{
  overview: TodoStatsOverview | null
  categories: TodoStatsCategoryItem[]
  trend: TodoStatsTrendItem[]
  trendSummary?: TodoStatsTrendSummary
  dueBuckets?: TodoStatsDueBuckets | null
  priorityDistribution?: TodoStatsPriorityDistribution | null
  aging?: TodoStatsAging | null
  reminderSummary?: TodoReminderSummary | null
  recurrenceDistribution?: TodoStatsRecurrenceDistribution | null
  pageMode?: boolean
}>()

const { t, locale } = useI18n()

const displayCategories = computed(() => {
  return props.categories.map((categoryItem) => ({
    ...categoryItem,
    displayName: categoryItem.category === '__UNCLASSIFIED__' ? t('stats.uncategorized') : categoryItem.category,
  }))
})

const maxTrendValue = computed(() => {
  if (!props.trend.length) return 0
  return Math.max(...props.trend.map((trendItem) => Math.max(trendItem.completedCount, trendItem.createdCount ?? 0)))
})

const intlDateLabel = computed(() => new Intl.DateTimeFormat(locale.value.startsWith('zh') ? 'zh-CN' : 'en-US', {
  month: '2-digit',
  day: '2-digit',
}))

const dashboardKpis = computed(() => props.overview ? buildDashboardKpis(props.overview) : [])

const dashboardTrend = computed(() => buildDashboardTrend(props.trend, (date) => {
  return intlDateLabel.value.format(new Date(`${date}T00:00:00`))
}))

const dashboardSnapshot = computed(() => buildDashboardSnapshot(dashboardTrend.value, props.trendSummary))

const dashboardCategories = computed(() => buildDashboardCategories(props.categories, (category) => {
  return category === '__UNCLASSIFIED__' ? t('stats.uncategorized') : category
}))

const dashboardDueBuckets = computed(() => props.dueBuckets ? buildDashboardDueBuckets(props.dueBuckets) : [])

const dashboardPriorityDist = computed(() => props.priorityDistribution ? buildDashboardPriorities(props.priorityDistribution) : [])

const dashboardAging = computed(() => props.aging ? buildDashboardAging(props.aging) : [])

const dashboardReminderSummary = computed(() => props.reminderSummary ? buildDashboardReminderSummary(props.reminderSummary) : [])

const dashboardRecurrence = computed(() => props.recurrenceDistribution ? buildDashboardRecurrence(props.recurrenceDistribution) : [])
</script>

<template>
  <div v-if="overview && pageMode" class="todo-dashboard-page" data-testid="page-stats-dashboard">
    <div class="dashboard-kpi-grid" data-testid="stats-kpi-grid">
      <div
        v-for="kpi in dashboardKpis"
        :key="kpi.key"
        class="kpi-card"
        data-testid="stats-kpi-card"
        :data-metric-key="kpi.key"
      >
        <div class="kpi-icon">{{ kpi.icon }}</div>
        <div class="kpi-info">
          <div class="kpi-label">{{ t(`stats.${kpi.key}`) }}</div>
          <div class="kpi-value" :class="kpi.toneClass">{{ kpi.value }}</div>
        </div>
      </div>
    </div>

    <div class="dashboard-main-grid">
      <div class="dashboard-col-main">
        <div class="dashboard-card trend-section" data-testid="stats-trend-section">
          <h3>{{ t('stats.trend7d') }}</h3>
          <div v-if="!dashboardTrend.length" class="empty-stats">{{ t('stats.empty') }}</div>
          <div v-else class="trend-chart-lg">
            <div
              v-for="day in dashboardTrend"
              :key="day.date"
              class="trend-day-lg"
              data-testid="stats-trend-bar"
              :data-date="day.date"
              :data-peak="day.isPeak ? '1' : '0'"
            >
              <div class="bars-lg">
                <div
                  class="bar-lg created-bar"
                  :style="{ height: maxTrendValue ? `${(day.createdCount / maxTrendValue) * 100}%` : '0' }"
                  :title="t('stats.createdOnlyLabel', { count: day.createdCount })"
                >
                  <span v-if="day.createdCount > 0" class="bar-value text-muted">{{ day.createdCount }}</span>
                </div>
                <div
                  class="bar-lg completed-bar"
                  :class="{ 'is-peak': day.isPeak }"
                  :style="{ height: maxTrendValue ? `${(day.completedCount / maxTrendValue) * 100}%` : '0' }"
                  :title="t('stats.completedOnlyLabel', { count: day.completedCount })"
                >
                  <span v-if="day.completedCount > 0" class="bar-value">{{ day.completedCount }}</span>
                </div>
              </div>
              <div class="day-label-lg">{{ day.label }}</div>
            </div>
          </div>
        </div>

        <div class="dashboard-card snapshot-section" data-testid="stats-trend-snapshot">
          <h3>{{ t('stats.snapshotTitle') }}</h3>
          <div class="snapshot-grid">
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="trendTotalCreated">
              <span class="snapshot-label">{{ t('stats.trendTotalCreated') }}</span>
              <span class="snapshot-value text-primary">{{ dashboardSnapshot.totalCreated }}</span>
            </div>
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="trendTotalCompleted">
              <span class="snapshot-label">{{ t('stats.trendTotalCompleted') }}</span>
              <span class="snapshot-value text-success">{{ dashboardSnapshot.totalCompleted }}</span>
            </div>
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="trendNetChange">
              <span class="snapshot-label">{{ t('stats.trendNetChange') }}</span>
              <span class="snapshot-value" :class="dashboardSnapshot.netChange > 0 ? 'text-warning' : 'text-success'">
                {{ dashboardSnapshot.netChange > 0 ? '+' : '' }}{{ dashboardSnapshot.netChange }}
              </span>
            </div>
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="completionRate">
              <span class="snapshot-label">{{ t('stats.completionRate', { rate: '' }).replace(' %', '').replace('%', '') }}</span>
              <span class="snapshot-value">{{ dashboardSnapshot.completionRate }}%</span>
            </div>
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="averagePerShownDay">
              <span class="snapshot-label">{{ t('stats.averagePerShownDay') }}</span>
              <span class="snapshot-value text-primary">{{ dashboardSnapshot.averagePerShownDay }}</span>
            </div>
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="activeDays">
              <span class="snapshot-label">{{ t('stats.activeDays') }}</span>
              <span class="snapshot-value">{{ dashboardSnapshot.activeDays }}</span>
            </div>
            <div class="snapshot-item" data-testid="stats-snapshot-item" data-snapshot-key="peakDay">
              <span class="snapshot-label">{{ t('stats.peakDay') }}</span>
              <span class="snapshot-value snapshot-value--compact">
                {{ dashboardSnapshot.peakDate || '—' }}
                <small v-if="dashboardSnapshot.peakCompletedCount">{{ dashboardSnapshot.peakCompletedCount }}</small>
              </span>
            </div>
          </div>
        </div>

        <div v-if="dashboardReminderSummary.length" class="dashboard-card reminder-summary-section" data-testid="stats-reminder-section">
          <h3>{{ t('stats.reminderSummaryTitle') }}</h3>
          <div class="snapshot-grid">
            <div v-for="item in dashboardReminderSummary" :key="item.key" class="snapshot-item">
              <span class="snapshot-label">{{ t(`stats.${item.key}`) }}</span>
              <span class="snapshot-value" :class="item.toneClass">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dashboard-col-side">
        <div v-if="dashboardAging.length" class="dashboard-card aging-section" data-testid="stats-aging-section">
          <h3>{{ t('stats.agingDistribution') }}</h3>
          <ul class="dist-list">
            <li v-for="bucket in dashboardAging" :key="bucket.label" class="dist-item">
              <div class="dist-header">
                <span class="dist-name">{{ bucket.label }}</span>
                <span class="dist-count" :class="bucket.toneClass">{{ bucket.count }}</span>
              </div>
              <div class="dist-progress-bar">
                <div class="dist-progress-fill" :class="bucket.toneClass" :style="{ width: `${bucket.percentage}%` }"></div>
              </div>
            </li>
          </ul>
        </div>

        <div v-if="dashboardDueBuckets.length" class="dashboard-card due-section" data-testid="stats-due-section">
          <h3>{{ t('stats.dueBuckets') }}</h3>
          <ul class="dist-list">
            <li v-for="bucket in dashboardDueBuckets" :key="bucket.key" class="dist-item">
              <div class="dist-header">
                <span class="dist-name">{{ t(`stats.${bucket.key}`) }}</span>
                <span class="dist-count" :class="bucket.toneClass">{{ bucket.count }}</span>
              </div>
              <div class="dist-progress-bar">
                <div class="dist-progress-fill" :class="bucket.toneClass" :style="{ width: `${bucket.percentage}%` }"></div>
              </div>
            </li>
          </ul>
        </div>

        <div v-if="dashboardPriorityDist.length" class="dashboard-card priority-section" data-testid="stats-priority-section">
          <h3>{{ t('stats.priorityDist') }}</h3>
          <ul class="dist-list">
            <li v-for="p in dashboardPriorityDist" :key="p.priority" class="dist-item">
              <div class="dist-header">
                <span class="dist-name">{{ p.labelKey.includes('.') ? t(p.labelKey) : t(`stats.${p.labelKey}`) }}</span>
                <span class="dist-count" :class="p.toneClass">{{ p.count }}</span>
              </div>
              <div class="dist-progress-bar">
                <div class="dist-progress-fill" :class="p.toneClass" :style="{ width: `${p.percentage}%` }"></div>
              </div>
            </li>
          </ul>
        </div>

        <div v-if="dashboardRecurrence.length" class="dashboard-card recurrence-section" data-testid="stats-recurrence-section">
          <h3>{{ t('stats.recurrenceDistribution') }}</h3>
          <ul class="dist-list">
            <li v-for="item in dashboardRecurrence" :key="item.recurrenceType" class="dist-item">
              <div class="dist-header">
                <span class="dist-name">{{ t(item.labelKey) }}</span>
                <span class="dist-count">{{ item.count }}</span>
              </div>
              <div class="dist-progress-bar">
                <div class="dist-progress-fill text-primary" :style="{ width: `${item.percentage}%` }"></div>
              </div>
            </li>
          </ul>
        </div>

        <div class="dashboard-card category-section" data-testid="stats-categories-section">
          <h3>{{ t('stats.categoryStats') }}</h3>
          <div v-if="!dashboardCategories.length" class="empty-stats">{{ t('stats.empty') }}</div>
          <ul v-else class="category-dist-list">
            <li
              v-for="(cat, index) in dashboardCategories"
              :key="cat.categoryKey"
              class="category-dist-item"
              data-testid="stats-category-row"
              :data-category-key="cat.categoryKey"
            >
              <div class="cat-header">
                <span class="cat-rank">#{{ index + 1 }}</span>
                <span class="cat-name-lg">{{ cat.displayName }}</span>
                <span class="cat-total">{{ t('stats.totalTasks', { count: cat.totalCount }) }}</span>
              </div>
              <div class="cat-progress-bar" :title="t('stats.completionRate', { rate: cat.completionRate })">
                <div class="cat-progress-fill cat-progress-fill--completed" :style="{ width: `${cat.completionRate}%` }"></div>
                <div class="cat-progress-fill cat-progress-fill--active" :style="{ width: `${Math.max(0, 100 - cat.completionRate)}%` }"></div>
              </div>
              <div class="cat-details">
                <span class="cat-count-sm active">{{ t('stats.activeLabel', { count: cat.activeCount }) }}</span>
                <span class="cat-count-sm completed">{{ t('stats.completedLabel', { count: cat.completedCount }) }}</span>
                <span class="cat-count-sm share">{{ t('stats.shareOfTrackedTotal', { share: cat.shareOfTotal }) }}</span>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>

  <div v-else-if="overview" class="todo-stats-panel" :class="{ 'page-mode': pageMode }">
    <div class="stats-panel-title">{{ t('stats.panelTitle') }}</div>

    <div class="stats-overview">
      <div class="stat-box">
        <div class="stat-label">{{ t('stats.todayCompleted') }}</div>
        <div class="stat-value">{{ overview.todayCompleted }}</div>
      </div>
      <div class="stat-box">
        <div class="stat-label">{{ t('stats.weekCompleted') }}</div>
        <div class="stat-value text-success">{{ overview.weekCompleted }}</div>
      </div>
      <div class="stat-box">
        <div class="stat-label">{{ t('stats.overdueCount') }}</div>
        <div class="stat-value text-warning">{{ overview.overdueCount }}</div>
      </div>
      <div class="stat-box">
        <div class="stat-label">{{ t('stats.activeCount') }}</div>
        <div class="stat-value text-primary">{{ overview.activeCount }}</div>
      </div>
      <div class="stat-box">
        <div class="stat-label">{{ t('stats.upcomingReminderCount') }}</div>
        <div class="stat-value text-info">{{ overview.upcomingReminderCount }}</div>
      </div>
      <div class="stat-box">
        <div class="stat-label">{{ t('stats.unreadReminderCount') }}</div>
        <div class="stat-value text-info">{{ overview.unreadReminderCount }}</div>
      </div>
    </div>

    <div class="stats-row">
      <div class="stats-card categories-card">
        <h3>{{ t('stats.categoryStats') }}</h3>
        <div v-if="!categories.length" class="empty-stats">{{ t('stats.empty') }}</div>
        <ul v-else class="category-list">
          <li v-for="cat in displayCategories" :key="cat.category">
            <span class="cat-name">{{ cat.displayName }}</span>
            <span class="category-summary">
              <span class="cat-count active">{{ t('stats.activeLabel', { count: cat.activeCount }) }}</span>
              <span class="cat-count completed">{{ t('stats.completedLabel', { count: cat.completedCount }) }}</span>
            </span>
          </li>
        </ul>
      </div>

      <div class="stats-card trend-card">
        <h3>{{ t('stats.trend7d') }}</h3>
        <div v-if="!trend.length" class="empty-stats">{{ t('stats.empty') }}</div>
        <div v-else class="trend-chart">
          <div v-for="day in trend" :key="day.date" class="trend-day">
            <div class="trend-value">{{ day.completedCount }}</div>
            <div class="bars">
              <div
                class="bar created-bar"
                :style="{ height: maxTrendValue ? `${(day.createdCount / maxTrendValue) * 100}%` : '0' }"
                :title="t('stats.createdOnlyLabel', { count: day.createdCount })"
              ></div>
              <div
                class="bar completed-bar"
                :style="{ height: maxTrendValue ? `${(day.completedCount / maxTrendValue) * 100}%` : '0' }"
                :title="t('stats.completedOnlyLabel', { count: day.completedCount })"
              ></div>
            </div>
            <div class="day-label">{{ day.date.substring(5) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.todo-stats-panel {
  margin-bottom: 0;
  padding: 18px;
  background: var(--bg-surface, rgba(255, 255, 255, 0.05));
  border: 1px solid var(--border-color, rgba(255, 255, 255, 0.1));
  border-radius: var(--radius-md, 8px);
}

.stats-panel-title {
  margin-bottom: 1rem;
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-bright, #fff);
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.stat-box {
  min-width: 0;
  padding: 12px;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-sm, 4px);
  text-align: left;
}

.stat-label {
  font-size: 0.78rem;
  color: var(--text-muted, #aaa);
  margin-bottom: 0.35rem;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: bold;
}

.text-success { color: var(--success-color, #10b981); }
.text-warning { color: var(--warning-color, #f59e0b); }
.text-primary { color: var(--primary-color, #3b82f6); }
.text-info { color: var(--accent-color, #38bdf8); }

.stats-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stats-card {
  flex: 1;
  background: rgba(0, 0, 0, 0.2);
  padding: 14px;
  border-radius: var(--radius-sm, 4px);
}

.stats-card h3 {
  margin: 0 0 0.9rem 0;
  font-size: 0.95rem;
  color: var(--text-muted, #aaa);
}

.dist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dist-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dist-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.85rem;
}

.dist-name {
  color: var(--text-bright, #fff);
}

.dist-count {
  font-weight: 600;
}

.dist-progress-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.dist-progress-fill {
  height: 100%;
  border-radius: 3px;
}

.dist-progress-fill.text-warning { background: var(--warning-color, #f59e0b); }
.dist-progress-fill.text-primary { background: var(--primary-color, #3b82f6); }
.dist-progress-fill.text-info { background: var(--accent-color, #38bdf8); }
.dist-progress-fill.text-muted { background: var(--text-muted, #aaa); }

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.category-list li:last-child {
  border-bottom: none;
}

.category-summary {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.cat-count {
  background: rgba(255, 255, 255, 0.1);
  padding: 0.1rem 0.5rem;
  border-radius: 12px;
  font-size: 0.8rem;
}

.cat-count.active {
  color: var(--warning-color, #f59e0b);
}

.cat-count.completed {
  color: var(--success-color, #10b981);
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  height: 120px;
  padding-bottom: 20px;
  position: relative;
}

.trend-day {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  position: relative;
}

.trend-value {
  font-size: 0.75rem;
  color: var(--text-muted, #aaa);
  margin-bottom: 6px;
}

.bars {
  display: flex;
  height: 100px;
  width: 100%;
  align-items: flex-end;
  justify-content: center;
}

.bar {
  width: 8px;
  min-height: 2px;
  border-radius: 2px 2px 0 0;
  transition: height 0.3s ease;
}

.created-bar {
  background: rgba(255, 255, 255, 0.2);
  margin-right: 2px;
}

.completed-bar {
  background: var(--success-color, #10b981);
}

.day-label {
  position: absolute;
  bottom: -20px;
  font-size: 0.7rem;
  color: var(--text-muted, #aaa);
  white-space: nowrap;
}

.empty-stats {
  color: var(--text-muted, #aaa);
  font-style: italic;
  text-align: center;
  padding: 1rem 0;
}
</style>
