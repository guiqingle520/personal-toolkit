const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'frontend/src/components/TodoList.vue');
let content = fs.readFileSync(filePath, 'utf8');

// Imports
content = content.replace(
  "import { computed, onMounted, ref } from 'vue'",
  "import { computed, onMounted, ref, watch } from 'vue'"
);

// State additions
const stateAdditions = `
const viewMode = ref<'ACTIVE' | 'RECYCLE_BIN'>('ACTIVE')
const selectedIds = ref<number[]>([])
const options = ref({ categories: [], tags: [] })
const showOptionsPanel = ref(false)

const isAllSelected = computed({
  get: () => todos.value.length > 0 && selectedIds.value.length === todos.value.length,
  set: (val) => {
    if (val) selectedIds.value = todos.value.map(t => t.id)
    else selectedIds.value = []
  }
})

watch(viewMode, () => {
  selectedIds.value = []
  filters.value.page = 0
  loadTodos()
})
`;
content = content.replace(
  "const pendingCount = computed(() => todos.value.filter(t => t.status !== 'DONE').length)",
  "const pendingCount = computed(() => todos.value.filter(t => t.status !== 'DONE').length)\n" + stateAdditions
);

// loadTodos update
content = content.replace(
  "const response = await fetchApi<PageData<TodoItem>>(`/api/todos?${params.toString()}`)",
  "const endpoint = viewMode.value === 'RECYCLE_BIN' ? '/api/todos/recycle-bin' : '/api/todos'\n    const response = await fetchApi<PageData<TodoItem>>(`${endpoint}?${params.toString()}`)"
);

// loadOptions and batch actions
const batchActions = `
async function loadOptions() {
  try {
    const response = await fetchApi<any>('/api/todos/options')
    if (response.data) {
      options.value = response.data
    }
  } catch (e) {
    // ignore
  }
}

async function batchComplete() {
  if (!selectedIds.value.length) return
  submitting.value = true
  try {
    await fetchApi('/api/todos/batch/complete', {
      method: 'POST',
      body: JSON.stringify({ ids: selectedIds.value })
    })
    selectedIds.value = []
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function batchDelete() {
  if (!selectedIds.value.length) return
  if (!confirm('Delete selected tasks?')) return
  submitting.value = true
  try {
    await fetchApi('/api/todos/batch/delete', {
      method: 'POST',
      body: JSON.stringify({ ids: selectedIds.value })
    })
    selectedIds.value = []
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function batchRestore() {
  if (!selectedIds.value.length) return
  submitting.value = true
  try {
    await fetchApi('/api/todos/batch/restore', {
      method: 'POST',
      body: JSON.stringify({ ids: selectedIds.value })
    })
    selectedIds.value = []
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}

async function restoreTodo(id: number) {
  submitting.value = true
  try {
    await fetchApi(\`/api/todos/\${id}/restore\`, { method: 'PUT' })
    await loadTodos()
  } catch(e) { handleError(e) } finally { submitting.value = false }
}
`;
content = content.replace("onMounted(loadTodos)", "onMounted(() => { loadTodos(); loadOptions(); })\n" + batchActions);

// Template modifications
const datalists = `
      <datalist id="category-options">
        <option v-for="c in options.categories" :key="c" :value="c"></option>
      </datalist>
      <datalist id="tag-options">
        <option v-for="t in options.tags" :key="t" :value="t"></option>
      </datalist>
`;
const viewToggle = `
      <!-- VIEW TOGGLE -->
      <div class="view-toggle-bar">
        <button :class="['btn btn-sm', viewMode === 'ACTIVE' ? 'btn-primary' : 'btn-outline']" @click="viewMode = 'ACTIVE'">Active Tasks</button>
        <button :class="['btn btn-sm', viewMode === 'RECYCLE_BIN' ? 'btn-primary' : 'btn-outline']" @click="viewMode = 'RECYCLE_BIN'">Recycle Bin</button>
        <button class="btn btn-sm btn-outline" @click="showOptionsPanel = !showOptionsPanel">Manage Categories/Tags</button>
      </div>

      <!-- OPTIONS PANEL -->
      <div v-if="showOptionsPanel" class="options-panel">
        <h3>Known Categories</h3>
        <div class="options-list">
          <span v-for="c in options.categories" :key="c" class="badge badge-category">{{ c }}</span>
          <span v-if="!options.categories.length">No categories found.</span>
        </div>
        <h3>Known Tags</h3>
        <div class="options-list">
          <span v-for="t in options.tags" :key="t" class="badge badge-tag">#{{ t }}</span>
          <span v-if="!options.tags.length">No tags found.</span>
        </div>
      </div>
`;
content = content.replace("<!-- FILTER SECTION -->", datalists + viewToggle + "\n      <!-- FILTER SECTION -->");

content = content.replace(/placeholder="Category" class="cyber-input form-sm"/g, 'placeholder="Category" class="cyber-input form-sm" list="category-options"');
content = content.replace(/placeholder="Category" class="cyber-input"/g, 'placeholder="Category" class="cyber-input" list="category-options"');
content = content.replace(/placeholder="Tag" class="cyber-input form-sm"/g, 'placeholder="Tag" class="cyber-input form-sm" list="tag-options"');
content = content.replace(/placeholder="Tags \(comma separated\)" class="cyber-input flex-2"/g, 'placeholder="Tags (comma separated)" class="cyber-input flex-2" list="tag-options"');
content = content.replace(/placeholder="Tags \(csv\)" class="cyber-input form-sm"/g, 'placeholder="Tags (csv)" class="cyber-input form-sm" list="tag-options"');

const batchBar = `
      <div class="batch-actions-bar" v-if="todos.length > 0">
        <label class="checkbox-label">
          <input type="checkbox" v-model="isAllSelected" /> Select All
        </label>
        <span class="selected-count" v-if="selectedIds.length > 0">{{ selectedIds.length }} selected</span>
        
        <div class="batch-buttons" v-if="selectedIds.length > 0">
          <template v-if="viewMode === 'ACTIVE'">
            <button class="btn btn-sm btn-success" @click="batchComplete">Complete Selected</button>
            <button class="btn btn-sm btn-outline" style="color: #ef4444; border-color: #ef4444" @click="batchDelete">Delete Selected</button>
          </template>
          <template v-else>
            <button class="btn btn-sm btn-success" @click="batchRestore">Restore Selected</button>
          </template>
        </div>
      </div>
`;

// Insert batch actions before list
content = content.replace("<!-- LIST -->\n      <ul v-else class=\"todo-list\">", batchBar + "\n      <!-- LIST -->\n      <ul v-else class=\"todo-list\">");

// Inject checkbox
content = content.replace(
  '<div class="todo-actions-left">',
  '<div class="todo-actions-left">\n            <input type="checkbox" v-model="selectedIds" :value="todo.id" class="cyber-checkbox" style="margin-right: 8px;" />'
);

// Inject Restore button for Recycle Bin items
content = content.replace(
  '<button class="action-btn delete-btn" @click="deleteTodo(todo.id)" :disabled="submitting" title="Delete">×</button>',
  `<button v-if="viewMode === 'ACTIVE'" class="action-btn delete-btn" @click="deleteTodo(todo.id)" :disabled="submitting" title="Delete">×</button>
            <button v-else class="action-btn" @click="restoreTodo(todo.id)" :disabled="submitting" title="Restore">↺</button>`
);

// CSS appending
content = content.replace(
  "</style>",
  `
.view-toggle-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.options-panel { background: rgba(255,255,255,0.03); padding: 16px; border-radius: 8px; margin-bottom: 16px; border: 1px solid rgba(255,255,255,0.05); }
.options-panel h3 { margin-top: 0; font-size: 1rem; color: #e4e4e7; }
.options-list { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.options-list:last-child { margin-bottom: 0; }
.batch-actions-bar { display: flex; align-items: center; gap: 16px; padding: 12px; background: rgba(255,255,255,0.02); border-radius: 8px; margin-bottom: 16px; }
.checkbox-label { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #a1a1aa; font-size: 0.9rem; }
.cyber-checkbox { appearance: none; width: 18px; height: 18px; border: 1px solid #52525b; border-radius: 4px; background: transparent; cursor: pointer; display: grid; place-content: center; }
.cyber-checkbox::before { content: ""; width: 10px; height: 10px; transform: scale(0); transition: 120ms transform ease-in-out; box-shadow: inset 1em 1em #38bdf8; background-color: #38bdf8; transform-origin: center; clip-path: polygon(14% 44%, 0 65%, 50% 100%, 100% 16%, 80% 0%, 43% 62%); }
.cyber-checkbox:checked::before { transform: scale(1); }
.selected-count { color: #38bdf8; font-size: 0.9rem; font-weight: 600; }
.batch-buttons { display: flex; gap: 8px; margin-left: auto; }
</style>`
);

fs.writeFileSync(filePath, content);
console.log('Successfully patched TodoList.vue');
