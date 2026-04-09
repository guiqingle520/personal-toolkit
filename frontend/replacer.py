import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace hardcoded strings
replacements = [
    (r"<h1>Tasks</h1>", r"<h1>{{ $t('app.title') }}</h1>"),
    (r"\{\{ pageData.totalElements \}\} total", r"{{ $t('app.total', { total: pageData.totalElements }) }}"),
    (r"\{\{ pendingCount \}\} pending", r"{{ $t('app.pending', { pending: pendingCount }) }}"),
    (r"on this page", r"{{ $t('app.onThisPage') }}"),
    (r"\{\{ loading \? 'Syncing\.\.\.' : 'Refresh' \}\}", r"{{ loading ? $t('app.syncing') : $t('app.refresh') }}"),
    (r">Active Tasks</button>", r">{{ $t('app.activeTasks') }}</button>"),
    (r">Recycle Bin</button>", r">{{ $t('app.recycleBin') }}</button>"),
    (r">Manage Categories/Tags</button>", r">{{ $t('app.manageCategories') }}</button>"),
    
    (r"<h3>Known Categories</h3>", r"<h3>{{ $t('options.knownCategories') }}</h3>"),
    (r"No categories found\.", r"{{ $t('options.noCategories') }}"),
    (r"<h3>Known Tags</h3>", r"<h3>{{ $t('options.knownTags') }}</h3>"),
    (r"No tags found\.", r"{{ $t('options.noTags') }}"),
    
    (r'placeholder="Search keyword\.\.\."', r':placeholder="$t(\'filter.search\')"'),
    (r'<option value="">All Status</option>', r'<option value="">{{ $t(\'filter.allStatus\') }}</option>'),
    (r'<option value="PENDING">Pending</option>', r'<option value="PENDING">{{ $t(\'filter.pending\') }}</option>'),
    (r'<option value="DONE">Done</option>', r'<option value="DONE">{{ $t(\'filter.done\') }}</option>'),
    (r'<option value="">All Priorities</option>', r'<option value="">{{ $t(\'filter.allPriorities\') }}</option>'),
    (r'<option :value="1">Backlog</option>', r'<option :value="1">{{ $t(\'priority.backlog\') }}</option>'),
    (r'<option :value="2">Low</option>', r'<option :value="2">{{ $t(\'priority.low\') }}</option>'),
    (r'<option :value="3">Medium</option>', r'<option :value="3">{{ $t(\'priority.medium\') }}</option>'),
    (r'<option :value="4">High</option>', r'<option :value="4">{{ $t(\'priority.high\') }}</option>'),
    (r'<option :value="5">Critical</option>', r'<option :value="5">{{ $t(\'priority.critical\') }}</option>'),
    (r'placeholder="Category"', r':placeholder="$t(\'filter.category\')"'),
    (r'placeholder="Tag"', r':placeholder="$t(\'filter.tag\')"'),
    (r'>Reset</button>', r'>{{ $t(\'filter.reset\') }}</button>'),
    
    (r'placeholder="What needs to be done\?"', r':placeholder="$t(\'form.whatNeedsToBeDone\')"'),
    (r'placeholder="Tags \(comma separated\)"', r':placeholder="$t(\'form.tagsCsv\')"'),
    (r'>\s*Add Task\s*</button>', r'>{{ $t(\'form.addTask\') }}</button>'),
    
    (r"<strong>Error:</strong>", r"<strong>{{ $t('status.error') }}</strong>"),
    (r"Initiating neuro-link\.\.\.", r"{{ $t('status.initiating') }}"),
    (r"No tasks found\. The grid is quiet\.", r"{{ $t('status.empty') }}"),
    
    (r"Select All", r"{{ $t('batch.selectAll') }}"),
    (r"\{\{ selectedIds\.length \}\} selected", r"{{ $t('batch.selected', { count: selectedIds.length }) }}"),
    (r">Complete Selected</button>", r">{{ $t('batch.complete') }}</button>"),
    (r">Delete Selected</button>", r">{{ $t('batch.delete') }}</button>"),
    (r">Restore Selected</button>", r">{{ $t('batch.restore') }}</button>"),
    
    (r"Mark as \$\{(.*?) === 'DONE' \? 'Pending' : 'Done'\}", r"{{ $t($1 === \'DONE\' ? \'status.markAsPending\' : \'status.markAsDone\') }}"),
    (r':title="`Mark as \$\{todo\.status === \'DONE\' \? \'Pending\' : \'Done\'\}`"', r':title="todo.status === \'DONE\' ? $t(\'status.markAsPending\') : $t(\'status.markAsDone\')"'),
    
    (r'placeholder="Title"', r':placeholder="$t(\'form.title\')"'),
    (r'placeholder="Tags \(csv\)"', r':placeholder="$t(\'form.tagsCsvEdit\')"'),
    (r">Save</button>", r">{{ $t('form.save') }}</button>"),
    (r">Cancel</button>", r">{{ $t('form.cancel') }}</button>"),
    
    (r'\{\{ formatPriorityLabel\(todo.priority\) \}\}', r'{{ $t(formatPriorityLabel(todo.priority)) }}'),
    
    (r'title="Edit"', r':title="$t(\'action.edit\')"'),
    (r'title="Delete"', r':title="$t(\'action.delete\')"'),
    (r'title="Restore"', r':title="$t(\'action.restore\')"'),
    
    (r'>&larr; Prev</button>', r'>{{ $t(\'pagination.prev\') }}</button>'),
    (r'>Next &rarr;</button>', r'>{{ $t(\'pagination.next\') }}</button>'),
    (r'Page \{\{ pageData\.page \+ 1 \}\} of \{\{ pageData\.totalPages \}\}', r'{{ $t(\'pagination.pageInfo\', { page: pageData.page + 1, total: pageData.totalPages }) }}')
]

for old, new_s in replacements:
    content = re.sub(old, new_s, content, flags=re.MULTILINE)

with open('src/components/TodoList.vue', 'w', encoding='utf-8') as f:
    f.write(content)
