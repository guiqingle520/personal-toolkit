import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r"\{\{\s*pageData.totalElements\s*\}\} total", r"{{ $t('app.total', { total: pageData.totalElements }) }}"),
    (r"\{\{\s*pendingCount\s*\}\} pending", r"{{ $t('app.pending', { pending: pendingCount }) }}"),
    (r"on this page", r"{{ $t('app.onThisPage') }}"),
]

for old, new_s in replacements:
    content = re.sub(old, new_s, content)

with open('src/components/TodoList.vue', 'w', encoding='utf-8') as f:
    f.write(content)
