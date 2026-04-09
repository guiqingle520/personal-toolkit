const en = {
  app: {
    title: 'Tasks',
    total: '{total} total',
    pending: '{pending} pending',
    onThisPage: 'on this page',
    syncing: 'Syncing...',
    refresh: 'Refresh',
    activeTasks: 'Active Tasks',
    recycleBin: 'Recycle Bin',
    manageCategories: 'Manage Categories/Tags',
    localeLabel: 'Language',
  },
  locale: {
    en: 'English',
    zhCN: 'Chinese'
  },
  options: {
    knownCategories: 'Known Categories',
    noCategories: 'No categories found.',
    knownTags: 'Known Tags',
    noTags: 'No tags found.',
  },
  filter: {
    search: 'Search keyword...',
    allStatus: 'All Status',
    pending: 'Pending',
    done: 'Done',
    allPriorities: 'All Priorities',
    category: 'Category',
    tag: 'Tag',
    reset: 'Reset'
  },
  form: {
    whatNeedsToBeDone: 'What needs to be done?',
    tagsCsv: 'Tags (comma separated)',
    addTask: 'Add Task',
    addSubtask: 'Add Subtask',
    title: 'Title',
    tagsCsvEdit: 'Tags (comma separated)',
    save: 'Save',
    cancel: 'Cancel'
  },
  priority: {
    critical: 'Critical',
    high: 'High',
    medium: 'Medium',
    low: 'Low',
    backlog: 'Backlog',
    na: 'N/A'
  },
  status: {
    initiating: 'Initiating neuro-link...',
    empty: 'No tasks found. The grid is quiet.',
    markAsPending: 'Mark as Pending',
    markAsDone: 'Mark as Done',
    error: 'Error:',
  },
  batch: {
    selectAll: 'Select All',
    selected: '{count} selected',
    complete: 'Complete Selected',
    delete: 'Delete Selected',
    restore: 'Restore Selected'
  },
  action: {
    edit: 'Edit',
    delete: 'Delete',
    restore: 'Restore',
    showChecklist: 'Show checklist',
    hideChecklist: 'Hide checklist',
    confirmDeleteSingle: 'Are you sure you want to delete this task?',
    confirmDeleteBatch: 'Delete selected tasks?'
  },
  checklist: {
    title: 'Checklist',
    empty: 'No checklist items yet.',
    progress: '{completed}/{total} completed',
    untitled: 'Untitled subtask'
  },
  feedback: {
    genericError: 'An error occurred',
    unexpectedError: 'An unexpected error occurred',
    httpError: 'HTTP Error {status}'
  },
  pagination: {
    prev: '← Prev',
    next: 'Next →',
    pageInfo: 'Page {page} of {total}'
  },
  recurrence: {
    none: 'None',
    daily: 'Daily',
    weekly: 'Weekly',
    monthly: 'Monthly',
    interval: 'Interval',
    endTime: 'End Date',
    nextTrigger: 'Scheduled: {time}',
    completedAt: 'Completed: {time}'
  }
}

export default en
export type MessageSchema = typeof en
