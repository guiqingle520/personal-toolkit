const LAST_TASKS_PATH_KEY = 'personal-toolkit-last-tasks-path'

function getStorage() {
  if (typeof window === 'undefined') {
    return null
  }

  return window.sessionStorage
}

function isValidTasksPath(path: string | null | undefined): path is string {
  return typeof path === 'string' && path.startsWith('/tasks')
}

export function saveLastTasksPath(fullPath: string) {
  if (!isValidTasksPath(fullPath)) {
    return
  }

  getStorage()?.setItem(LAST_TASKS_PATH_KEY, fullPath)
}

export function getLastTasksPath() {
  const stored = getStorage()?.getItem(LAST_TASKS_PATH_KEY)
  return isValidTasksPath(stored) ? stored : null
}

export function clearLastTasksPath() {
  getStorage()?.removeItem(LAST_TASKS_PATH_KEY)
}
