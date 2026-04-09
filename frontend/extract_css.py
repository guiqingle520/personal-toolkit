import json
import urllib.request
with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

# Let's just create tokens.css and ui.css, then remove them from TodoList.vue
# Then we rewrite TodoList.vue

import re
match = re.search(r'<style scoped>(.*?)</style>', text, re.DOTALL)
if not match:
    print("NO MATCH")
else:
    css = match.group(1)
    with open('extracted_css.txt', 'w', encoding='utf-8') as f:
        f.write(css)
    print("WROTE extracted_css.txt")
