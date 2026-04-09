import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

replacements = [
    (r'color: #ef4444;', r'color: var(--color-danger);'),
    (r'background: rgba\(239, 68, 68, 0.1\);', r'background: var(--color-danger-bg);'),
    (r'color: #e4e4e7;', r'color: var(--color-text-bright);'),
    (r'background: rgba\(255, 255, 255, 0.1\);', r'background: var(--color-surface-active);'),
    (r'color: #71717a;', r'color: var(--color-text-muted);'),
    (r'transition: all 0.2s;', r'transition: all var(--transition-fast);'),
    (r'border-radius: 8px;', r'border-radius: var(--radius-md);'),
    (r'color: #a1a1aa;', r'color: var(--color-text-normal);'),
    (r'border: 1px solid #52525b;', r'border: 1px solid var(--color-text-muted);'),
    (r'border-radius: 4px;', r'border-radius: var(--radius-sm);'),
    (r'box-shadow: inset 1em 1em #38bdf8;', r'box-shadow: inset 1em 1em var(--color-primary);'),
    (r'background-color: #38bdf8;', r'background-color: var(--color-primary);'),
    (r'color: #38bdf8;', r'color: var(--color-primary);')
]

for old, new_s in replacements:
    text = re.sub(old, new_s, text)

with open('src/components/TodoList.vue', 'w', encoding='utf-8') as f:
    f.write(text)
