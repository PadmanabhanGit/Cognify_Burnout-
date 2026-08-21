/**
 * Single source of truth for the web app's "Total App Usage" figure.
 *
 * Extracted verbatim from the /usage page so that /usage and /dashboard share
 * ONE calculation rather than each deriving a total independently. Nothing here
 * is new logic — the category buckets and the seconds-with-minutes-fallback are
 * exactly what AppUsage.jsx already did.
 *
 * Input is always the `usage` array from GET /api/usage/today, i.e. Android's
 * own per-category totals after they have been synced through the backend. The
 * browser never measures anything itself.
 */

/** Backend classifier may emit "Entertainment", "Others", "Video", etc. */
export function normalizeCategory(cat) {
  if (!cat) return 'Others';
  const c = cat.toLowerCase();
  if (c.includes('social')) return 'Social Media';
  if (c.includes('gaming') || c.includes('game')) return 'Gaming';
  if (c.includes('stream') || c.includes('entertainment') || c.includes('video') || c.includes('media')) return 'Streaming';
  if (c.includes('product') || c.includes('work') || c.includes('study') || c.includes('edu')) return 'Productivity';
  return 'Others';
}

/**
 * Per-category seconds. The current API preserves seconds; older records fall
 * back to stored whole minutes.
 *
 * 'Others' (Maps, Chrome, Calculator, ...) is a real bucket, not dropped:
 * Android's own Total App Usage already includes it (every app counts toward
 * the total regardless of category), so excluding it here made the web
 * total look like a different, smaller number for the same day's usage —
 * confirmed on-device: Android showed 5h10m, web showed 1h29m for the same
 * account, same day, because this map previously had no 'Others' key at all
 * and normalizeCategory's default fell through the `if (norm in buckets)`
 * check silently.
 */
export function bucketUsageSeconds(usage) {
  const buckets = { 'Social Media': 0, 'Gaming': 0, 'Streaming': 0, 'Productivity': 0, 'Others': 0 };
  (usage || []).forEach((u) => {
    const norm = normalizeCategory(u.category);
    if (norm in buckets) {
      buckets[norm] += Number(u.durationSeconds ?? (u.duration || 0) * 60);
    }
  });
  return buckets;
}

/**
 * Total App Usage in seconds = Social Media + Gaming + Streaming +
 * Productivity + Others — every category, matching what "Total App Usage"
 * means on Android (every app counts, not just the four named ones).
 */
export function sumUsageSeconds(usage) {
  return Object.values(bucketUsageSeconds(usage)).reduce((a, b) => a + b, 0);
}

/**
 * Compact Dashboard format: "H.MM", where the digits after the dot are MINUTES,
 * not a decimal fraction of an hour.
 *
 *   4h 10m 40s -> "4.11H"   (40s rounds 10m up to 11m)
 *   1h 30m 00s -> "1.30H"
 *          45m -> "45m"     (under an hour, minutes only)
 *
 * Seconds are rounded to the nearest minute BEFORE splitting, so the underlying
 * total is never truncated on its way to the display.
 */
export function formatCompactUsage(totalSeconds) {
  const secs = Math.max(0, Number(totalSeconds) || 0);
  if (secs <= 0) return '0m';
  const totalMinutes = Math.round(secs / 60);
  if (totalMinutes < 60) return `${totalMinutes}m`;
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${h}.${String(m).padStart(2, '0')}H`;
}
