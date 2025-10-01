export function formatBytes(value: number | null | undefined, fractionDigits = 1): string {
  if (value == null || !isFinite(value)) return "–";
  const units = ["B", "KB", "MB", "GB", "TB"];
  let v = value;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024;
    i++;
  }
  return `${v.toFixed(fractionDigits)} ${units[i]}`;
}

export function formatNumber(n: number | null | undefined): string {
  if (n == null || !isFinite(n)) return "–";
  return new Intl.NumberFormat(undefined, { maximumFractionDigits: 0 }).format(n);
}

export function formatPercent(n: number | null | undefined, fractionDigits = 0): string {
  if (n == null || !isFinite(n)) return "–";
  return `${(n * 100).toFixed(fractionDigits)}%`;
}

export function toFixed(n: number | null | undefined, digits = 0): string {
  if (n == null || !isFinite(n)) return "–";
  return n.toFixed(digits);
}

export function formatMillis(millis: number | null | undefined): string {
  if (millis == null || !isFinite(millis)) return "–";
  if (millis < 1000) return `${millis} ms`;
  const s = millis / 1000;
  if (s < 60) return `${s.toFixed(2)} s`;
  const m = Math.floor(s / 60);
  const sec = Math.floor(s % 60);
  return `${m}m ${sec}s`;
}
