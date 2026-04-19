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
    listView: 'List View',
    kanbanView: 'Kanban View',
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
    allRecurrence: 'All Recurrence',
    category: 'Category',
    tag: 'Tag',
    presetToday: 'Due Today',
    presetOverdue: 'Overdue',
    presetUpcoming: 'Upcoming Reminder',
    statusChip: 'Status: {value}',
    priorityChip: 'Priority: {value}',
    categoryChip: 'Category: {value}',
    tagChip: 'Tag: {value}',
    recurrenceChip: 'Repeat: {value}',
    presetChip: 'Preset: {value}',
    dueFromChip: 'Due From: {value}',
    dueToChip: 'Due To: {value}',
    remindFromChip: 'Reminder From: {value}',
    remindToChip: 'Reminder To: {value}',
    reset: 'Reset'
  },
  form: {
    whatNeedsToBeDone: 'What needs to be done?',
    tagsCsv: 'Tags (comma separated)',
    notes: 'Notes',
    attachmentLinks: 'Attachment links (one per line)',
    attachmentLink: 'Open attachment',
    ownerLabel: 'Owner Label',
    collaborators: 'Collaborators (comma separated)',
    watchers: 'Watchers (comma separated)',
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
    PENDING: 'Pending',
    IN_PROGRESS: 'In Progress',
    DONE: 'Done',
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
  },
  reminder: {
    remindAt: 'Reminder Date',
    remindFrom: 'Reminder From',
    remindTo: 'Reminder To',
    scheduledAt: 'Reminder: {time}'
  },
  collaboration: {
    ownerLabel: 'Owner: {value}',
    collaboratorsLabel: 'Collaborators: {value}',
    watchersLabel: 'Watchers: {value}'
  },
  stats: {
    panelTitle: 'Stats Panel',
    todayCompleted: 'Today Completed',
    weekCompleted: 'Week Completed',
    overdueCount: 'Overdue Tasks',
    activeCount: 'Active Tasks',
    upcomingReminderCount: 'Upcoming Reminders',
    categoryStats: 'Category Stats',
    trend7d: '7-Day Trend',
    uncategorized: 'Uncategorized',
    activeLabel: 'Active: {count}',
    completedLabel: 'Completed: {count}',
    completedOnlyLabel: 'Completed: {count}',
    empty: 'No stats available'
  },
  kanban: {
    pendingColumn: 'Pending',
    doneColumn: 'Done',
    emptyColumn: 'No tasks in this column',
    dropHere: 'Drop task here'
  },
  auth: {
    loginTitle: 'Access Terminal',
    registerTitle: 'Initialize Connection',
    username: 'Username',
    loginIdentifier: 'Username or Email',
    password: 'Password',
    email: 'Email',
    captcha: 'Captcha',
    captchaPlaceholder: 'Enter captcha',
    refreshCaptcha: 'Refresh Captcha',
    loginBtn: 'Login',
    registerBtn: 'Register',
    noAccount: 'No credentials?',
    registerLink: 'Create account',
    hasAccount: 'Already registered?',
    loginLink: 'Login here',
    logout: 'Disconnect'
  }
}

export default en
export type MessageSchema = typeof en
