<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { TodoStatsOverview, TodoStatsCategoryItem, TodoStatsTrendItem } from './types'

const props = defineProps<{
  overview: TodoStatsOverview | null
  categories: TodoStatsCategoryItem[]
  trend: TodoStatsTrendItem[]
}>()

const { t } = useI18n()

const displayCategories = computed(() => {
  return props.categories.map((categoryItem) => ({
    ...categoryItem,
    displayName: categoryItem.category === '__UNCLASSIFIED__' ? t('stats.uncategorized') : categoryItem.category,
  }))
})

const maxTrendValue = computed(() => {
  if (!props.trend.length) return 0
  return Math.max(...props.trend.map((trendItem) => trendItem.completedCount))
})
</script>

<template>
  <div class="todo-stats-panel" v-if="overview">
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
          <div class="trend-day" v-for="day in trend" :key="day.date">
            <div class="trend-value">{{ day.completedCount }}</div>
            <div class="bars">
              <div class="bar completed-bar"
                   :style="{ height: maxTrendValue ? `${(day.completedCount / maxTrendValue) * 100}%` : '0' }"
                   :title="`${t('stats.completedOnlyLabel', { count: day.completedCount })}`"></div>
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
  margin-bottom: var(--spacing-4, 1rem);
  padding: var(--spacing-4, 1rem);
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
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-4, 1rem);
  margin-bottom: var(--spacing-4, 1rem);
}

.stat-box {
  flex: 1;
  min-width: 100px;
  padding: 1rem;
  background: rgba(0, 0, 0, 0.2);
  border-radius: var(--radius-sm, 4px);
  text-align: center;
}

.stat-label {
  font-size: 0.85rem;
  color: var(--text-muted, #aaa);
  margin-bottom: 0.5rem;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: bold;
}

.text-success { color: var(--success-color, #10b981); }
.text-warning { color: var(--warning-color, #f59e0b); }
.text-primary { color: var(--primary-color, #3b82f6); }

.stats-row {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-4, 1rem);
}

.stats-card {
  flex: 1;
  background: rgba(0, 0, 0, 0.2);
  padding: 1rem;
  border-radius: var(--radius-sm, 4px);
}

.stats-card h3 {
  margin: 0 0 1rem 0;
  font-size: 1rem;
  color: var(--text-muted, #aaa);
}

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
