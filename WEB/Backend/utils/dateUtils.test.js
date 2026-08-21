const test = require('node:test');
const assert = require('node:assert/strict');
const { normalizeDateValue, isSameLocalDate } = require('./dateUtils');

test('normalizeDateValue converts ISO timestamps to local YYYY-MM-DD', () => {
  const today = new Date('2026-08-08T12:34:56');
  assert.equal(normalizeDateValue(today), '2026-08-08');
});

test('isSameLocalDate compares ISO timestamps and YYYY-MM-DD values', () => {
  assert.equal(isSameLocalDate('2026-08-08T12:30:00', '2026-08-08'), true);
  assert.equal(isSameLocalDate('2026-08-09T00:00:00', '2026-08-08'), false);
});
