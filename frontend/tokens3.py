import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

replacements = [
    # Button styling
    (r'border-radius: 12px;', r'border-radius: var(--radius-lg);'),
    (r'transition: all 0.2s cubic-bezier\(0.4, 0, 0.2, 1\);', r'transition: all var(--transition-bounce);'),
    (r'background: linear-gradient\(135deg, #3b82f6 0%, #2563eb 100%\);', r'background: var(--color-primary-gradient);'),
    (r'color: #a1a1aa;', r'color: var(--color-text-normal);'),
    (r'border: 1px solid rgba\(255, 255, 255, 0.1\);', r'border: 1px solid var(--color-border);'),
    (r'background: rgba\(255, 255, 255, 0.05\);', r'background: var(--color-surface-hover);'),
    (r'border-radius: 8px;', r'border-radius: var(--radius-md);'),
    (r'background: rgba\(16, 185, 129, 0.1\);', r'background: var(--color-success-bg);'),
    (r'color: #34d399;', r'color: var(--color-success);'),
    (r'border: 1px solid rgba\(16, 185, 129, 0.2\);', r'border: 1px solid var(--color-success-border);'),
    (r'color: #71717a;', r'color: var(--color-text-muted);'),
    (r'color: #e4e4e7;', r'color: var(--color-text-bright);'),
    
    # Status toggle styling
    (r'border-color: #52525b;', r'border-color: var(--color-text-muted);'),
    (r'border-color: #38bdf8;', r'border-color: var(--color-primary);'),
    (r'background: #10b981;', r'background: var(--color-success);'),
    (r'border-color: #10b981;', r'border-color: var(--color-success);'),
    
    # Badge styling
    (r'background: rgba\(52, 211, 153, 0.1\); color: #34d399;', r'background: var(--color-success-bg); color: var(--color-success);'),
    (r'background: rgba\(251, 191, 36, 0.1\); color: #fbbf24;', r'background: var(--color-warning-bg); color: var(--color-warning);'),
    (r'background: rgba\(239, 68, 68, 0.1\); color: #ef4444;', r'background: var(--color-danger-bg); color: var(--color-danger);'),
    (r'background: rgba\(56, 189, 248, 0.1\); color: #38bdf8;', r'background: rgba(56, 189, 248, 0.1); color: var(--color-primary);'),
    (r'background: rgba\(168, 85, 247, 0.1\); color: #a855f7;', r'background: var(--color-purple-bg); color: var(--color-purple);'),
    
    # Action buttons
    (r'background: rgba\(239, 68, 68, 0.1\);', r'background: var(--color-danger-bg);'),
    (r'color: #ef4444;', r'color: var(--color-danger);'),
    (r'background: rgba\(255, 255, 255, 0.1\);', r'background: var(--color-surface-active);'),
    
    # Checkbox
    (r'border: 1px solid #52525b;', r'border: 1px solid var(--color-text-muted);'),
    (r'border-radius: 4px;', r'border-radius: var(--radius-sm);'),
    (r'box-shadow: inset 1em 1em #38bdf8; background-color: #38bdf8;', r'box-shadow: inset 1em 1em var(--color-primary); background-color: var(--color-primary);'),
    
    # Inputs & Highlights
    (r'color: #38bdf8;', r'color: var(--color-primary);'),
    (r'border-color: #38bdf8;', r'border-color: var(--color-primary);'),
    
    # Inline styles
    (r'style="color: #ef4444; border-color: #ef4444"', r'style="color: var(--color-danger); border-color: var(--color-danger)"'),
]

for old, new_s in replacements:
    text = re.sub(old, new_s, text)

with open('src/components/TodoList.vue', 'w', encoding='utf-8') as f:
    f.write(text)
