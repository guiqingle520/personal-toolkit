const fs = require('fs');
let content = fs.readFileSync('frontend/src/components/todo/TodoToolbar.vue', 'utf8');

content = content.replace(
  "defineProps<{",
  "defineProps<{\n  displayMode?: 'LIST' | 'KANBAN'"
);

content = content.replace(
  "defineEmits<{",
  "defineEmits<{\n  (e: 'update:displayMode', val: 'LIST' | 'KANBAN'): void"
);

// Add the buttons
const viewButtons = `
      <div class="view-toggle" v-if="viewMode === 'ACTIVE'">
        <button 
          class="btn btn-sm"
          :class="displayMode === 'LIST' ? 'btn-primary' : 'btn-outline'"
          @click="$emit('update:displayMode', 'LIST')"
          title="List View"
        >
          <i class="icon">list</i>
        </button>
        <button 
          class="btn btn-sm"
          :class="displayMode === 'KANBAN' ? 'btn-primary' : 'btn-outline'"
          @click="$emit('update:displayMode', 'KANBAN')"
          title="Kanban View"
        >
          <i class="icon">dashboard</i>
        </button>
      </div>
`;

content = content.replace(
  '<div class="toolbar-right">',
  '<div class="toolbar-right">' + viewButtons
);

fs.writeFileSync('frontend/src/components/todo/TodoToolbar.vue', content);
console.log('TodoToolbar.vue patched successfully');
