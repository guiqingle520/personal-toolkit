import re
with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

m = re.search(r'<style[^>]*>(.*?)</style>', text, re.DOTALL)
if m:
    with open('extracted_css.txt', 'w', encoding='utf-8') as f:
        f.write(m.group(1))
    print(f"Match found. Length: {len(m.group(1))}")
else:
    print("NO MATCH")
