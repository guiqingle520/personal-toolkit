const zhCN = {
  app: {
    title: '任务列表',
    total: '共 {total} 项',
    pending: '{pending} 项待办',
    onThisPage: '本页',
    syncing: '同步中...',
    refresh: '刷新',
    activeTasks: '活动任务',
    recycleBin: '回收站',
    listView: '列表视图',
    kanbanView: '看板视图',
    calendarView: '日历视图',
    manageCategories: '管理分类/标签',
    localeLabel: '语言',
  },
  locale: {
    en: '英文',
    zhCN: '中文'
  },
  theme: {
    label: '主题',
    current: '当前：{theme}',
    system: '跟随系统',
    light: '浅色',
    dark: '深色'
  },
  account: {
    buttonLabel: '账号',
    menuLabel: '打开账号菜单',
    management: '账号管理',
    themeSettings: '主题设置',
    signedInAs: '当前账号：{value}'
  },
  options: {
    knownCategories: '已知分类',
    noCategories: '未找到分类。',
    knownTags: '已知标签',
    noTags: '未找到标签。',
  },
  savedViews: {
    title: '保存视图',
    saveCurrent: '保存当前视图',
    defaultBadge: '默认',
    setDefault: '设为默认',
    rename: '重命名',
    delete: '删除',
    promptName: '请输入视图名称',
    promptRename: '请输入新的视图名称',
    confirmDelete: '确定删除这个保存视图吗？'
  },
  filter: {
    search: '搜索关键字...',
    allStatus: '所有状态',
    pending: '待办',
    done: '已完成',
    allPriorities: '所有优先级',
    allRecurrence: '所有重复',
    category: '分类',
    tag: '标签',
    presetToday: '今日到期',
    presetOverdue: '逾期任务',
    presetUpcoming: '即将提醒',
    statusChip: '状态：{value}',
    priorityChip: '优先级：{value}',
    categoryChip: '分类：{value}',
    tagChip: '标签：{value}',
    recurrenceChip: '重复：{value}',
    presetChip: '预设：{value}',
    dueFromChip: '截止开始：{value}',
    dueToChip: '截止结束：{value}',
    remindFromChip: '提醒开始：{value}',
    remindToChip: '提醒结束：{value}',
    reset: '重置'
  },
  form: {
    whatNeedsToBeDone: '需要完成什么？',
    tagsCsv: '标签（逗号分隔）',
    notes: '备注',
    attachmentLinks: '附件链接（每行一个）',
    attachmentLink: '打开附件',
    ownerLabel: '负责人显示名',
    collaborators: '协作人（逗号分隔）',
    watchers: '观察者（逗号分隔）',
    addTask: '添加任务',
    addSubtask: '添加子任务',
    title: '标题',
    tagsCsvEdit: '标签（逗号分隔）',
    save: '保存',
    cancel: '取消'
  },
  priority: {
    critical: '紧急',
    high: '高',
    medium: '中',
    low: '低',
    backlog: '积压',
    na: '无'
  },
  status: {
    PENDING: '待办',
    IN_PROGRESS: '进行中',
    DONE: '已完成',
    initiating: '正在初始化神经连接...',
    empty: '未找到任务。网格很安静。',
    markAsPending: '标记为待办',
    markAsDone: '标记为已完成',
    error: '错误：',
  },
  batch: {
    selectAll: '全选',
    selected: '已选择 {count} 项',
    complete: '完成所选',
    delete: '删除所选',
    restore: '恢复所选'
  },
  action: {
    edit: '编辑',
    delete: '删除',
    restore: '恢复',
    closeInfo: '关闭提示',
    showChecklist: '显示清单',
    hideChecklist: '隐藏清单',
    confirmDeleteSingle: '您确定要删除此任务吗？',
    confirmDeleteBatch: '删除所选任务？'
  },
  checklist: {
    title: '清单',
    empty: '还没有子任务。',
    progress: '已完成 {completed}/{total}',
    untitled: '未命名子任务'
  },
  feedback: {
    genericError: '发生错误',
    unexpectedError: '发生了未预期的错误',
    httpError: 'HTTP 错误 {status}',
    reminderAfterDueDate: '提醒日期不能晚于截止日期',
    reminderAlignedToDueDate: '提醒日期已同步调整为截止日期',
    createReminderAlignedToDueDate: '新任务的提醒日期已同步调整为截止日期',
    editReminderAlignedToDueDate: '编辑中的提醒日期已同步调整为截止日期',
    createdTodoHiddenByFilters: '任务已创建，但被当前筛选条件隐藏。你可以重置筛选来查看它。'
  },
  pagination: {
    prev: '← 上一页',
    next: '下一页 →',
    pageInfo: '第 {page} 页，共 {total} 页'
  },
  recurrence: {
    none: '无',
    daily: '每天',
    weekly: '每周',
    monthly: '每月',
    interval: '间隔',
    endTime: '结束日期',
    nextTrigger: '计划触发：{time}',
    completedAt: '完成于：{time}'
  },
  reminder: {
    panelTitle: '站内提醒',
    remindAt: '提醒日期',
    remindFrom: '提醒开始',
    remindTo: '提醒结束',
    scheduledAt: '提醒：{time}',
    scheduledAtLabel: '提醒时间：{time}',
    empty: '当前没有未读提醒',
    markRead: '标记已读',
    markAllRead: '全部已读',
    openTodo: '查看任务'
  },
  collaboration: {
    ownerLabel: '负责人：{value}',
    collaboratorsLabel: '协作人：{value}',
    watchersLabel: '观察者：{value}'
  },
  stats: {
    panelTitle: '统计面板',
    todayCompleted: '今日完成',
    weekCompleted: '本周完成',
    overdueCount: '逾期任务',
    activeCount: '活动任务',
    upcomingReminderCount: '即将提醒',
    unreadReminderCount: '未读提醒',
    categoryStats: '分类统计',
    trend7d: '7日趋势',
    uncategorized: '未分类',
    activeLabel: '活动：{count}',
    completedLabel: '完成：{count}',
    completedOnlyLabel: '完成：{count}',
    empty: '暂无数据'
  },
  kanban: {
    pendingColumn: '待办',
    doneColumn: '已完成',
    emptyColumn: '此列暂无任务',
    dropHere: '拖到这里'
  },
  auth: {
    loginTitle: '接入终端',
    registerTitle: '初始化连接',
    username: '用户名',
    loginIdentifier: '用户名或邮箱',
    password: '密码',
    email: '邮箱',
    captcha: '验证码',
    captchaPlaceholder: '输入验证码',
    refreshCaptcha: '刷新验证码',
    loginBtn: '登录',
    registerBtn: '注册',
    noAccount: '没有凭证？',
    registerLink: '创建账号',
    hasAccount: '已注册？',
    loginLink: '在此登录',
    logout: '断开连接'
  },
  calendar: {
    today: '今天',
    mon: '周一',
    tue: '周二',
    wed: '周三',
    thu: '周四',
    fri: '周五',
    sat: '周六',
    sun: '周日',
    more: '还有 {count} 项',
    emptyMonth: '本月没有截止日期任务',
    eventsCount: '共 {count} 项',
    prevMonth: '上个月',
    nextMonth: '下个月'
  }
}

export default zhCN
