const fs = require('fs');
let content = fs.readFileSync('frontend/src/components/TodoList.vue', 'utf8');

// 1. Add import
content = content.replace(
  "import TodoItemsList from './todo/TodoItemsList.vue'",
  "import TodoItemsList from './todo/TodoItemsList.vue'\nimport TodoKanbanView from './todo/TodoKanbanView.vue'"
);

// 2. Add state
content = content.replace(
  "const viewMode = ref<'ACTIVE' | 'RECYCLE_BIN'>('ACTIVE')",
  "const viewMode = ref<'ACTIVE' | 'RECYCLE_BIN'>('ACTIVE')\nconst displayMode = ref<'LIST' | 'KANBAN'>('LIST')"
);

let itemsListPos = content.indexOf('<TodoItemsList');
let itemsListEnd = content.indexOf('/>', itemsListPos) + 2;
let itemsListStr = content.substring(itemsListPos, itemsListEnd);

let kanbanStr = itemsListStr.replace('<TodoItemsList', '<TodoKanbanView\n        v-if="displayMode === \'KANBAN\'"');
itemsListStr = itemsListStr.replace('<TodoItemsList', '<TodoItemsList\n        v-if="displayMode === \'LIST\'"');

content = content.substring(0, itemsListPos) + itemsListStr + '\n' + kanbanStr + content.substring(itemsListEnd);

content = content.replace(
  '<TodoToolbar',
  '<TodoToolbar\n        :displayMode="displayMode"\n        @update:displayMode="displayMode = $event"'
);

content = content.replace('<TodoPagination', '<TodoPagination \n        v-show="displayMode === \'LIST\'"');
content = content.replace('<TodoFilters', '<TodoFilters \n        v-show="displayMode === \'LIST\'"');

content = content.replace(
  'filters.value.page = 0',
  'filters.value.page = 0\n  if (viewMode.value !== \'ACTIVE\') displayMode.value = \'LIST\''
);

fs.writeFileSync('frontend/src/components/TodoList.vue', content);
console.log('TodoList.vue patched successfully');
