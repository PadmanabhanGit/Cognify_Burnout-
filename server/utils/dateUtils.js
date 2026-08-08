function getLocalDateString(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function normalizeDateValue(value) {
  if (!value) return getLocalDateString();

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;

    const parsed = new Date(trimmed);
    if (!Number.isNaN(parsed.getTime())) return getLocalDateString(parsed);

    return getLocalDateString();
  }

  if (value instanceof Date) return getLocalDateString(value);

  return getLocalDateString();
}

function isSameLocalDate(value, targetDate) {
  return normalizeDateValue(value) === normalizeDateValue(targetDate);
}

module.exports = { getLocalDateString, normalizeDateValue, isSameLocalDate };
