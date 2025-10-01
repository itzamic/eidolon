"use client";

import { useMemo } from "react";
import type { HeapPoint } from "@/lib/types";
import { formatBytes } from "@/lib/format";

type Props = {
  points: HeapPoint[];
  maxBytesHint?: number | null;
  height?: number;
};

export default function HeapChart({ points, maxBytesHint, height = 220 }: Props) {
  const { pathUsed, lastUsed, yMax } = useMemo(() => {
    const n = points.length;
    const w = 600; // viewBox width; svg will scale to container width
    const h = height;

    const usedValues = points.map((p) => p.used);
    const maxCandidate = Math.max(1, ...usedValues);
    const cap = Math.max(maxCandidate, (maxBytesHint ?? 0) || 0);
    const yMax = cap <= 0 ? 1 : cap;

    const toX = (i: number) => (n <= 1 ? 0 : (i / (n - 1)) * w);
    const toY = (v: number) => h - (Math.min(v, yMax) / yMax) * h;

    let d = "";
    points.forEach((p, i) => {
      const x = toX(i);
      const y = toY(p.used);
      d += i === 0 ? `M ${x} ${y}` : ` L ${x} ${y}`;
    });

    const lastUsed = points.length ? points[points.length - 1].used : 0;
    return { pathUsed: d, lastUsed, yMax };
  }, [points, maxBytesHint, height]);

  return (
    <div>
      <svg className="chart" viewBox={`0 0 600 ${height}`} role="img" aria-label="Heap usage chart">
        {/* grid lines */}
        <g opacity="0.25" stroke="var(--border)" strokeWidth="1">
          <line x1="0" y1="0.5" x2="600" y2="0.5" />
          <line x1="0" y1={height / 2} x2="600" y2={height / 2} />
          <line x1="0" y1={height - 0.5} x2="600" y2={height - 0.5} />
        </g>

        {/* path line */}
        <path d={pathUsed} fill="none" stroke="var(--accent)" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />
      </svg>
      <div className="legend">
        <div className="swatch used" />
        <div>Used: {formatBytes(lastUsed)}</div>
        {typeof maxBytesHint === "number" && maxBytesHint > 0 && (
          <>
            <div style={{ width: 12, height: 3, borderRadius: 2, background: "var(--muted)", opacity: 0.5 }} />
            <div>Max: {formatBytes(maxBytesHint)}</div>
          </>
        )}
      </div>
    </div>
  );
}
