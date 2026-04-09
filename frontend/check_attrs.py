import re

with open('src/components/TodoList.vue', 'r', encoding='utf-8') as f:
    text = f.read()

template = re.search(r'<template>(.*?)</template>', text, re.DOTALL)
if template:
    html = template.group(1)
    # find attributes like placeholder="...", title="..."
    for m in re.finditer(r'(placeholder|title)="([^"]+)"', html):
        print(m.group(0))
