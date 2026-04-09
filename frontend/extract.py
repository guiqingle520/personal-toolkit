with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

import re
match = re.search(r'<style scoped>(.*?)</style>', text, re.DOTALL)
if match:
    with open('styles.css', 'w', encoding='utf-8') as f:
        f.write(match.group(1))
    print(f"Extracted {len(match.group(1).splitlines())} lines of CSS.")
