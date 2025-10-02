import { formatBytes, formatNumber, formatPercent, toFixed, formatMillis } from '@/lib/format';

describe('formatBytes', () => {
  it('returns dash for nullish or non-finite', () => {
    expect(formatBytes(null)).toBe('–');
    expect(formatBytes(undefined)).toBe('–');
    expect(formatBytes(Number.NaN)).toBe('–');
    expect(formatBytes(Number.POSITIVE_INFINITY)).toBe('–');
  });

  it('formats bytes and scales units', () => {
    expect(formatBytes(0)).toBe('0.0 B');
    expect(formatBytes(512)).toBe('512.0 B');
    expect(formatBytes(1024)).toBe('1.0 KB');
    expect(formatBytes(1536)).toBe('1.5 KB');
    expect(formatBytes(1048576)).toBe('1.0 MB');
  });

  it('respects fractionDigits', () => {
    expect(formatBytes(1536, 2)).toBe('1.50 KB');
  });
});

describe('formatNumber', () => {
  const normalize = (s: string) => s.replace(/[^\d-]/g, '');

  it('returns dash for nullish or non-finite', () => {
    expect(formatNumber(null)).toBe('–');
    expect(formatNumber(undefined)).toBe('–');
    expect(formatNumber(Number.NaN)).toBe('–');
    expect(formatNumber(Number.NEGATIVE_INFINITY)).toBe('–');
  });

  it('formats integers with possible grouping', () => {
    const out = formatNumber(1000);
    expect(normalize(out)).toBe('1000');
  });

  it('rounds to 0 fraction digits', () => {
    expect(normalize(formatNumber(42.4))).toBe('42');
    expect(normalize(formatNumber(42.5))).toBe('43');
  });
});

describe('formatPercent', () => {
  it('returns dash for nullish or non-finite', () => {
    expect(formatPercent(null)).toBe('–');
    expect(formatPercent(undefined)).toBe('–');
    expect(formatPercent(Number.NaN)).toBe('–');
  });

  it('formats percentages with default digits', () => {
    expect(formatPercent(0)).toBe('0%');
    expect(formatPercent(0.123)).toBe('12%');
    expect(formatPercent(-0.5)).toBe('-50%');
  });

  it('respects fractionDigits', () => {
    expect(formatPercent(0.1234, 1)).toBe('12.3%');
    expect(formatPercent(0.129, 2)).toBe('12.90%');
  });
});

describe('toFixed', () => {
  it('returns dash for nullish or non-finite', () => {
    expect(toFixed(null)).toBe('–');
    expect(toFixed(undefined)).toBe('–');
    expect(toFixed(Number.NaN)).toBe('–');
  });

  it('formats with fixed digits', () => {
    expect(toFixed(1.2345, 0)).toBe('1');
    expect(toFixed(1.2345, 2)).toBe('1.23');
  });
});

describe('formatMillis', () => {
  it('returns dash for nullish or non-finite', () => {
    expect(formatMillis(null)).toBe('–');
    expect(formatMillis(undefined)).toBe('–');
    expect(formatMillis(Number.NaN)).toBe('–');
  });

  it('formats milliseconds and seconds', () => {
    expect(formatMillis(500)).toBe('500 ms');
    expect(formatMillis(1500)).toBe('1.50 s');
  });

  it('formats minutes and seconds', () => {
    expect(formatMillis(61000)).toBe('1m 1s');
    expect(formatMillis(125000)).toBe('2m 5s');
  });
});
