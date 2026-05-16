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
  trendRange?: string
}>()

const emit = defineEmits<{
  (e: 'update:trendRange', range: string): void
  (e: 'click:due', bucketKey: string): void
  (e: 'click:priority', priority: number): void
  (e: 'click:recurrence', recurrenceType: string): void
}>()

const { t, locale } = useI18n()
const sectionIdPrefix = `todo-stats-${Math.random().toString(36).slice(2, 8)}`

function sectionHeadingId(sectionKey: string) {
  return `${sectionIdPrefix}-${sectionKey}-title`
}

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

const dashboardTrend = computed(() => {
  const baseTrend = buildDashboardTrend(props.trend, (date) => {
    return intlDateLabel.value.format(new Date(`${date}T00:00:00`))
  })
  
  const len = baseTrend.length
  if (len === 0) return []
  
  let interval = 1
  const range = props.trendRange || '7d'
  
  if (range === '90d' || len > 31) {
    interval = 14
  } else if (range === '30d' || len > 14) {
    interval = 7
  }
  
  return baseTrend.map((day, i) => {
    const distance = len - 1 - i
    return {
      ...day,
      showLabel: distance % interval === 0
    }
  })
})

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
        <div class="dashboard-card trend-section" data-testid="stats-trend-section" role="region" :aria-labelledby="sectionHeadingId('trend')">
          <div class="trend-header">
            <h3 :id="sectionHeadingId('trend')">{{ t('stats.trendTitle') }}</h3>
            <select
              class="trend-range-select"
              :value="trendRange || '7d'"
              :aria-labelledby="sectionHeadingId('trend')"
              @change="emit('update:trendRange', ($event.target as HTMLSelectElement).value)"
            >
              <option value="7d">{{ t('stats.trend7d') }}</option>
              <option value="30d">{{ t('stats.trend30d') }}</option>
              <option value="90d">{{ t('stats.trend90d') }}</option>
            </select>
          </div>
          <div v-if="!dashboardTrend.length" class="empty-stats">{{ t('stats.empty') }}</div>
          <div v-else class="trend-chart-wrapper" :class="`range-${trendRange || '7d'}`">
            <div class="trend-chart trend-chart-lg">
              <div
                v-for="day in dashboardTrend"
                :key="day.date"
                class="trend-day trend-day-lg"
              data-testid="stats-trend-bar"
              :data-date="day.date"
              :data-peak="day.isPeak ? '1' : '0'"
            >
               <div class="bars bars-lg">
                 <div
                   class="bar bar-lg created-bar"
                   :style="{ height: maxTrendValue ? `${(day.createdCount / maxTrendValue) * 100}%` : '0' }"
                   :title="t('stats.createdOnlyLabel', { count: day.createdCount })"
                 >
                   <span v-if="day.createdCount > 0" class="bar-value text-muted">{{ day.createdCount }}</span>
                 </div>
                 <div
                   class="bar bar-lg completed-bar"
                   :class="{ 'is-peak': day.isPeak }"
                   :style="{ height: maxTrendValue ? `${(day.completedCount / maxTrendValue) * 100}%` : '0' }"
                   :title="t('stats.completedOnlyLabel', { count: day.completedCount })"
                 >
                   <span v-if="day.completedCount > 0" class="bar-value">{{ day.completedCount }}</span>
                 </div>
               </div>
               <div class="day-label day-label-lg" v-show="day.showLabel">{{ day.label }}</div>
             </div>
           </div>
         </div>
        </div>

        <div class="dashboard-card snapshot-section" data-testid="stats-trend-snapshot" role="region" :aria-labelledby="sectionHeadingId('snapshot')">
          <h3 :id="sectionHeadingId('snapshot')">{{ t('stats.snapshotTitle') }}</h3>
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

        <div v-if="dashboardReminderSummary.length" class="dashboard-card reminder-summary-section" data-testid="stats-reminder-section" role="region" :aria-labelledby="sectionHeadingId('reminder')">
          <h3 :id="sectionHeadingId('reminder')">{{ t('stats.reminderSummaryTitle') }}</h3>
          <div class="snapshot-grid">
            <div v-for="item in dashboardReminderSummary" :key="item.key" class="snapshot-item">
              <span class="snapshot-label">{{ t(`stats.${item.key}`) }}</span>
              <span class="snapshot-value" :class="item.toneClass">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dashboard-col-side">
        <div v-if="dashboardAging.length" class="dashboard-card aging-section" data-testid="stats-aging-section" role="region" :aria-labelledby="sectionHeadingId('aging')">
          <h3 :id="sectionHeadingId('aging')">{{ t('stats.agingDistribution') }}</h3>
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

        <div v-if="dashboardDueBuckets.length" class="dashboard-card due-section" data-testid="stats-due-section" role="region" :aria-labelledby="sectionHeadingId('due')">
          <h3 :id="sectionHeadingId('due')">{{ t('stats.dueBuckets') }}</h3>
          <ul class="dist-list">
            <li
              v-for="bucket in dashboardDueBuckets"
              :key="bucket.key"
            >
              <component
                :is="bucket.key !== 'bucketNoDate' ? 'button' : 'div'"
                class="dist-item"
                :class="{ clickable: bucket.key !== 'bucketNoDate' }"
                @click="bucket.key !== 'bucketNoDate' && emit('click:due', bucket.key)"
              >
                <div class="dist-header">
                  <span class="dist-name">{{ t(`stats.${bucket.key}`) }}</span>
                  <span class="dist-count" :class="bucket.toneClass">{{ bucket.count }}</span>
                </div>
                <div class="dist-progress-bar">
                  <div class="dist-progress-fill" :class="bucket.toneClass" :style="{ width: `${bucket.percentage}%` }"></div>
                </div>
              </component>
            </li>
          </ul>
        </div>

        <div v-if="dashboardPriorityDist.length" class="dashboard-card priority-section" data-testid="stats-priority-section" role="region" :aria-labelledby="sectionHeadingId('priority')">
          <h3 :id="sectionHeadingId('priority')">{{ t('stats.priorityDist') }}</h3>
          <ul class="dist-list">
            <li
              v-for="p in dashboardPriorityDist"
              :key="p.priority"
            >
              <button
                class="dist-item clickable"
                @click="emit('click:priority', p.priority)"
              >
                <div class="dist-header">
                  <span class="dist-name">{{ p.labelKey.includes('.') ? t(p.labelKey) : t(`stats.${p.labelKey}`) }}</span>
                  <span class="dist-count" :class="p.toneClass">{{ p.count }}</span>
                </div>
                <div class="dist-progress-bar">
                  <div class="dist-progress-fill" :class="p.toneClass" :style="{ width: `${p.percentage}%` }"></div>
                </div>
              </button>
            </li>
          </ul>
        </div>

        <div v-if="dashboardRecurrence.length" class="dashboard-card recurrence-section" data-testid="stats-recurrence-section" role="region" :aria-labelledby="sectionHeadingId('recurrence')">
          <h3 :id="sectionHeadingId('recurrence')">{{ t('stats.recurrenceDistribution') }}</h3>
          <ul class="dist-list">
            <li
              v-for="item in dashboardRecurrence"
              :key="item.recurrenceType"
            >
              <button
                class="dist-item clickable"
                @click="emit('click:recurrence', item.recurrenceType)"
              >
                <div class="dist-header">
                  <span class="dist-name">{{ t(item.labelKey) }}</span>
                  <span class="dist-count">{{ item.count }}</span>
                </div>
                <div class="dist-progress-bar">
                  <div class="dist-progress-fill text-primary" :style="{ width: `${item.percentage}%` }"></div>
                </div>
              </button>
            </li>
          </ul>
        </div>

        <div class="dashboard-card category-section" data-testid="stats-categories-section" role="region" :aria-labelledby="sectionHeadingId('categories')">
          <h3 :id="sectionHeadingId('categories')">{{ t('stats.categoryStats') }}</h3>
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
  padding: 24px;
  background: rgba(12, 12, 12, 0.7);
  border: 1px solid rgba(212, 175, 55, 0.15);
  border-radius: var(--radius-lg, 12px);
  backdrop-filter: blur(8px);
}

.todo-dashboard-page {
  padding: 32px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.stats-panel-title {
  margin-bottom: 1.5rem;
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--color-primary);
  text-transform: uppercase;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-box {
  min-width: 0;
  padding: 20px;
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md, 8px);
  text-align: left;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.stat-box::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  opacity: 0.1;
  transition: opacity 0.3s ease;
}

.stat-box:hover {
  transform: translateY(-2px);
  background: rgba(15, 15, 15, 0.6);
  border-color: rgba(212, 175, 55, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}

.stat-box:hover::after {
  opacity: 0.5;
}

.stat-label {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-muted, #999);
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.text-success { color: var(--color-success, #10b981); text-shadow: 0 0 10px rgba(16, 185, 129, 0.2); }
.text-warning { color: var(--color-warning, #f59e0b); text-shadow: 0 0 10px rgba(245, 158, 11, 0.2); }
.text-primary { color: var(--color-primary, #d4af37); text-shadow: 0 0 10px rgba(212, 175, 55, 0.2); }
.text-info { color: var(--color-info, #d4af37); text-shadow: 0 0 10px rgba(212, 175, 55, 0.2); }

.stats-row {
  display: flex;
  gap: 20px;
}

.stats-card {
  flex: 1;
  background: rgba(5, 5, 5, 0.5);
  padding: 20px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid rgba(255, 255, 255, 0.03);
  transition: all 0.3s ease;
}

.stats-card:hover {
  border-color: rgba(255, 255, 255, 0.08);
}

.stats-card h3 {
  margin: 0 0 1.2rem 0;
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-bright, #fff);
  letter-spacing: 0.01em;
}

.dist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dist-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm, 6px);
  transition: background-color 0.2s ease, transform 0.2s ease;
}

button.dist-item {
  width: 100%;
  background: transparent;
  border: none;
  text-align: left;
  font-family: inherit;
  color: inherit;
}

.dist-item.clickable {
  cursor: pointer;
}

.dist-item.clickable:hover {
  background: rgba(255, 255, 255, 0.05);
  transform: translateX(4px);
}

.trend-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 1.2rem;
  margin-bottom: 1rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.trend-header h3 {
  margin: 0;
  font-size: 1.05rem;
}

.trend-range-select {
  background: rgba(0, 0, 0, 0.3);
  color: var(--color-primary);
  border: 1px solid rgba(212, 175, 55, 0.3);
  border-radius: var(--radius-sm, 4px);
  padding: 4px 12px;
  font-size: 0.85rem;
  font-weight: 500;
  outline: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.trend-range-select:hover {
  background: rgba(212, 175, 55, 0.1);
  box-shadow: 0 0 8px rgba(212, 175, 55, 0.2);
}

.dist-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
}

.dist-name {
  color: var(--color-text-bright, #fff);
  font-weight: 500;
}

.dist-count {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.dist-progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.5);
}

.dist-progress-fill {
  height: 100%;
  border-radius: 2px;
  box-shadow: 0 0 8px currentColor;
}

.dist-progress-fill.text-warning { background: var(--color-warning, #f59e0b); }
.dist-progress-fill.text-primary { background: var(--color-primary, #d4af37); }
.dist-progress-fill.text-info { background: var(--color-info, #d4af37); }
.dist-progress-fill.text-muted { background: var(--text-muted, #888); box-shadow: none; }

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-list li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  font-size: 0.9rem;
}

.category-list li:last-child {
  border-bottom: none;
}

.cat-name {
  font-weight: 500;
}

.category-summary {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.cat-count {
  background: rgba(0, 0, 0, 0.4);
  padding: 0.2rem 0.6rem;
  border-radius: 12px;
  font-size: 0.75rem;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.cat-count.active {
  color: var(--color-primary, #d4af37);
  border-color: rgba(212, 175, 55, 0.2);
}

.cat-count.completed {
  color: var(--color-success, #10b981);
  border-color: rgba(16, 185, 129, 0.2);
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 0.5rem;
  height: 140px;
  padding-bottom: 24px;
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
  color: var(--color-text-bright, #fff);
  font-weight: 600;
  margin-bottom: 8px;
}

.bars {
  display: flex;
  height: 120px;
  width: 100%;
  align-items: flex-end;
  justify-content: center;
}

.bar {
  width: 10px;
  min-height: 2px;
  border-radius: 3px 3px 0 0;
  transition: height 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.created-bar {
  background: rgba(255, 255, 255, 0.1);
  margin-right: 4px;
}

.completed-bar {
  background: linear-gradient(180deg, var(--color-primary), rgba(212, 175, 55, 0.4));
  box-shadow: 0 0 8px rgba(212, 175, 55, 0.2);
}

.day-label {
  position: absolute;
  bottom: -24px;
  font-size: 0.75rem;
  color: var(--text-muted, #777);
  white-space: nowrap;
}

.empty-stats {
  color: var(--text-muted, #666);
  font-style: italic;
  text-align: center;
  padding: 2rem 0;
  font-size: 0.9rem;
}

.trend-chart-wrapper {
  width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 12px;
  padding-top: 40px;
}

.trend-chart-wrapper::-webkit-scrollbar {
  height: 4px;
}
.trend-chart-wrapper::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 2px;
}
.trend-chart-wrapper::-webkit-scrollbar-thumb {
  background: rgba(212, 175, 55, 0.3);
  border-radius: 2px;
}
.trend-chart-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(212, 175, 55, 0.6);
}

.trend-chart-wrapper .trend-chart-lg {
  min-width: max-content;
}

.trend-chart-wrapper .trend-day-lg {
  flex: 1 0 auto;
}

/* Page Mode Specifics */
.dashboard-kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.kpi-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: rgba(12, 12, 12, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.kpi-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--color-primary);
  opacity: 0.5;
  transition: opacity 0.3s ease;
}

.kpi-card:hover {
  transform: translateY(-3px);
  border-color: rgba(212, 175, 55, 0.2);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}

.kpi-card:hover::before {
  opacity: 1;
}

.kpi-icon {
  font-size: 2rem;
  opacity: 0.8;
  filter: drop-shadow(0 0 8px currentColor);
}

.kpi-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-label {
  font-size: 0.85rem;
  color: var(--text-muted, #888);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  font-weight: 500;
}

.kpi-value {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.dashboard-main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.dashboard-col-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-col-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dashboard-card {
  background: rgba(12, 12, 12, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-lg);
  padding: 28px;
}

.dashboard-card h3 {
  margin: 0 0 1.5rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--color-primary);
  text-transform: uppercase;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 20px;
}

.snapshot-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.02);
}

.snapshot-label {
  font-size: 0.8rem;
  color: var(--text-muted, #888);
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.snapshot-value {
  font-size: 1.5rem;
  font-weight: 700;
}

.snapshot-value--compact {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.snapshot-value--compact small {
  font-size: 0.9rem;
  color: var(--color-primary);
  opacity: 0.8;
}

.category-dist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.category-dist-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.02);
  transition: transform 0.2s ease;
}

.category-dist-item:hover {
  transform: translateX(4px);
  border-color: rgba(212, 175, 55, 0.2);
}

.cat-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.cat-rank {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-primary);
  opacity: 0.8;
}

.cat-name-lg {
  flex: 1;
  font-size: 1.05rem;
  font-weight: 500;
  color: var(--color-text-bright, #fff);
}

.cat-total {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--color-text-bright, #fff);
}

.cat-progress-bar {
  height: 6px;
  display: flex;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
  overflow: hidden;
  box-shadow: inset 0 1px 2px rgba(0, 0, 0, 0.5);
}

.cat-progress-fill {
  height: 100%;
}

.cat-progress-fill--completed {
  background: linear-gradient(90deg, rgba(16, 185, 129, 0.5), var(--color-success));
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.4);
}

.cat-progress-fill--active {
  background: linear-gradient(90deg, rgba(212, 175, 55, 0.4), var(--color-primary));
}

.cat-details {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.cat-count-sm {
  font-size: 0.8rem;
  padding: 4px 10px;
  border-radius: var(--radius-sm);
  background: rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.cat-count-sm.active { color: var(--color-primary); border-color: rgba(212, 175, 55, 0.2); }
.cat-count-sm.completed { color: var(--color-success); border-color: rgba(16, 185, 129, 0.2); }
.cat-count-sm.share { color: var(--text-muted, #888); }
</style>
