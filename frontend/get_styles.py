import re
with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()
match = re.search(r'<style scoped>(.*?)</style>', text, re.DOTALL)
if match:
    print(match.group(1)[:1500]) # print first 1500 chars to see what it looks like
