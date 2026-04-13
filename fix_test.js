const fs = require('fs');
let code = fs.readFileSync('frontend/src/components/TodoList.test.ts', 'utf8');

code = code.replace(/\.mockImplementationOnce\(\(\) => \(\{ ok: false, json: async \(\) => createValidationErrorResponse\(\) \}\)\)/g, 
  `.mockImplementation(async (url, options) => {
    if (url.includes('/api/todos') && options?.method === 'POST') return { ok: false, json: async () => createValidationErrorResponse() }
    if (url.includes('/api/todos/options')) return { ok: true, json: async () => createOptionsResponse() }
    if (url.includes('/api/todos/stats/overview')) return { ok: true, json: async () => createStatsOverviewResponse() }
    if (url.includes('/api/todos/stats/by-category')) return { ok: true, json: async () => createStatsCategoryResponse() }
    if (url.includes('/api/todos/stats/trend')) return { ok: true, json: async () => createStatsTrendResponse() }
    return { ok: true, json: async () => createPageResponse() }
  })`);

// Handle trailing dots from deleted mockResolvedValueOnce
code = code.replace(/fetchMock[\s\n]*\.mockReset\(\)[\s\n]*\./g, 'fetchMock.mockReset();\n    ');
code = code.replace(/fetchMock[\s\n]*\.mockReset\(\)[\s\n]*/g, 'fetchMock.mockReset();\n    ');

fs.writeFileSync('frontend/src/components/TodoList.test.ts', code);
console.log('done');