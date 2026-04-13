const fs = require('fs');
let en = fs.readFileSync('frontend/src/locales/en.ts', 'utf8');
en = en.replace('status: {', 'status: {\n    PENDING: \'Pending\',\n    IN_PROGRESS: \'In Progress\',\n    DONE: \'Done\',');
fs.writeFileSync('frontend/src/locales/en.ts', en);

let zh = fs.readFileSync('frontend/src/locales/zh-CN.ts', 'utf8');
zh = zh.replace('status: {', 'status: {\n    PENDING: \'待办\',\n    IN_PROGRESS: \'进行中\',\n    DONE: \'已完成\',');
fs.writeFileSync('frontend/src/locales/zh-CN.ts', zh);

console.log('Locales patched');
