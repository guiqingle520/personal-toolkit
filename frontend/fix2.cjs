const fs = require('fs');
let code = fs.readFileSync('src/components/TodoList.test.ts', 'utf8');

const mockImpl = `function mockFetchImplementation(url, options) {
  if (url.includes('/api/todos/stats/overview')) return { ok: true, json: async () => createStatsOverviewResponse() }
  if (url.includes('/api/todos/stats/by-category')) return { ok: true, json: async () => createStatsCategoryResponse() }
  if (url.includes('/api/todos/stats/trend')) return { ok: true, json: async () => createStatsTrendResponse() }
  if (url.includes('/api/todos/options')) return { ok: true, json: async () => createOptionsResponse() }
  
  if (url.includes('/api/todos/1/sub-items') && options?.method === 'POST') return { ok: true, json: async () => createSuccessResponse({ id: 102, todoId: 1, title: 'Ship checklist UI', status: 'PENDING', sortOrder: 1, createTime: '2026-04-07T00:00:00', updateTime: '2026-04-07T00:00:00' }) }
  if (url.includes('/api/todos/1/sub-items')) return { ok: true, json: async () => createSubItemsResponse() }
  if (url.includes('/api/todos/1/restore')) return { ok: true, json: async () => createSuccessResponse({ id: 1 }) }
  
  if (url.includes('/api/todos/') && options?.method === 'PUT') return { ok: true, json: async () => createSuccessResponse({ id: 1 }) }
  if (url.includes('/api/todos') && options?.method === 'POST') {
    if (options?.body?.includes('Trigger validation envelope')) return { ok: false, json: async () => createValidationErrorResponse() }
    return { ok: true, json: async () => createSuccessResponse({ id: 2 }) }
  }
  
  // Default to page response
  return { ok: true, json: async () => createPageResponse() }
}

describe('TodoList reset behavior', () => {`;

if (!code.includes('function mockFetchImplementation')) {
  code = code.replace(/describe\('TodoList reset behavior', \(\) => \{/, mockImpl);
  
  code = code.replace(/fetchMock\s*\n\s*\.mockReset\(\)/g, 'fetchMock.mockReset(); fetchMock.mockImplementation(mockFetchImplementation)');
  code = code.replace(/fetchMock\.mockReset\(\)/g, 'fetchMock.mockReset(); fetchMock.mockImplementation(mockFetchImplementation)');
  
  // Also clean up any lingering `.mockResolvedValueOnce`
  code = code.replace(/\.mockResolvedValueOnce\(.*?\)/g, '');
  code = code.replace(/\.mockImplementationOnce\(.*?\)/g, '');
  
  fs.writeFileSync('src/components/TodoList.test.ts', code);
}
console.log('done');