import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

# find text nodes in template
template = re.search(r'<template>(.*?)</template>', text, re.DOTALL)
if template:
    html = template.group(1)
    # remove html comments
    html = re.sub(r'<!--.*?-->', '', html, flags=re.DOTALL)
    # find all text
    for line in html.split('\n'):
        # strip tags
        clean = re.sub(r'<[^>]+>', '', line).strip()
        if clean and not re.match(r'^\{\{.*\}\}$', clean) and any(c.isalpha() for c in clean):
            print(clean)
