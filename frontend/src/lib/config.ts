export const EIDOLON_BASE = process.env.NEXT_PUBLIC_EIDOLON_BASE ?? "/eidolon";
export const EIDOLON_HTTP_BASE = EIDOLON_BASE; // Proxied by next.config.ts rewrites during dev
export const EIDOLON_WS_URL =
  process.env.NEXT_PUBLIC_EIDOLON_WS_URL ?? `ws://localhost:7090${EIDOLON_BASE}/ws/metrics`;

export const SNAPSHOT_HTTP_URL = `${EIDOLON_HTTP_BASE}/api/metrics/snapshot`;
export const RUNTIME_HTTP_URL = `${EIDOLON_HTTP_BASE}/api/runtime`;
