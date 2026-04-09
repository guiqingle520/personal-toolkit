import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

# Manually replace action button properties since regex failed due to indentation or specifics
text = text.replace('background: rgba(239, 68, 68, 0.1);', 'background: var(--color-danger-bg);')
text = text.replace('color: #ef4444;', 'color: var(--color-danger);')
text = text.replace('background: rgba(255, 255, 255, 0.1);', 'background: var(--color-surface-active);')
text = text.replace('color: #e4e4e7;', 'color: var(--color-text-bright);')
text = text.replace('color: #71717a;', 'color: var(--color-text-muted);')
text = text.replace('transition: all 0.2s;', 'transition: all var(--transition-fast);')
text = text.replace('border-radius: 8px;', 'border-radius: var(--radius-md);')
text = text.replace('color: #a1a1aa;', 'color: var(--color-text-normal);')
text = text.replace('border: 1px solid #52525b;', 'border: 1px solid var(--color-text-muted);')
text = text.replace('border-radius: 4px;', 'border-radius: var(--radius-sm);')
text = text.replace('box-shadow: inset 1em 1em #38bdf8;', 'box-shadow: inset 1em 1em var(--color-primary);')
text = text.replace('background-color: #38bdf8;', 'background-color: var(--color-primary);')
text = text.replace('color: #38bdf8;', 'color: var(--color-primary);')

with open('src/components/TodoList.vue', 'w', encoding='utf-8') as f:
    f.write(text)
