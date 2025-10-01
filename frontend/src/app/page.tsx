"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { HeapPoint, MetricsSnapshot } from "@/lib/types";
import { formatBytes, formatMillis, formatNumber, toFixed } from "@/lib/format";
import { EIDOLON_WS_URL, SNAPSHOT_HTTP_URL } from "@/lib/config";
import HeapChart from "@/components/HeapChart";
import StatCard from "@/components/StatCard";

type ConnStatus = "disconnected" | "connecting" | "open";

function pctUsed(used: number, max: number | null): number | null {
  if (max == null || max <= 0) return null;
  return used / max;
}

export default function Dashboard() {
  const [status, setStatus] = useState<ConnStatus>("disconnected");
  const [err, setErr] = useState<string | null>(null);
  const [snap, setSnap] = useState<MetricsSnapshot | null>(null);
  const [points, setPoints] = useState<HeapPoint[]>([]);
  const wsRef = useRef<WebSocket | null>(null);

  // Initial HTTP fetch (in case WS is disabled or connecting)
  useEffect(() => {
    let cancelled = false;
    fetch(SNAPSHOT_HTTP_URL)
      .then(async (r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json() as Promise<MetricsSnapshot>;
      })
      .then((s) => {
        if (cancelled) return;
        setSnap(s);
        setPoints((prev) => [
          ...prev.slice(-299),
          { t: s.timestampMillis, used: s.heap.used, max: s.heap.max }
        ]);
      })
      .catch(() => {
        // ignore; WS may still provide data
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const onMessage = useCallback((ev: MessageEvent) => {
    try {
      // Server may also send "pong" (string), so guard JSON parsing
      const data = JSON.parse(ev.data) as MetricsSnapshot;
      setSnap(data);
      setPoints((prev) => [
        ...prev.slice(-299),
        { t: data.timestampMillis, used: data.heap.used, max: data.heap.max }
      ]);
    } catch {
      // ignore non-JSON
    }
  }, []);

  const connect = useCallback(() => {
    setErr(null);
    setStatus("connecting");
    if (wsRef.current) {
      try {
        wsRef.current.close();
      } catch {}
      wsRef.current = null;
    }
    try {
      const ws = new WebSocket(EIDOLON_WS_URL);
      wsRef.current = ws;
      ws.onopen = () => setStatus("open");
      ws.onmessage = onMessage;
      ws.onerror = () => setErr("WebSocket error");
      ws.onclose = () => setStatus("disconnected");
    } catch (e: any) {
      setStatus("disconnected");
      setErr(e?.message ?? "Failed to open WebSocket");
    }
  }, [onMessage]);

  const disconnect = useCallback(() => {
    setErr(null);
    setStatus("disconnected");
    if (wsRef.current) {
      try {
        wsRef.current.close();
      } catch {}
      wsRef.current = null;
    }
  }, []);

  // Auto-connect on mount
  useEffect(() => {
    connect();
    return () => {
      if (wsRef.current) {
        try {
          wsRef.current.close();
        } catch {}
        wsRef.current = null;
      }
    };
  }, [connect]);

  const sendPing = useCallback(() => {
    try {
      wsRef.current?.send("ping");
    } catch {}
  }, []);

  const requestSnapshot = useCallback(() => {
    try {
      wsRef.current?.send("snapshot");
    } catch {}
  }, []);

  const heapPct = useMemo(
    () => (snap ? pctUsed(snap.heap.used, snap.heap.max) : null),
    [snap]
  );

  const statusBadgeClass = useMemo(() => {
    const ok = status === "open";
    return `badge ${ok ? "" : ""}`;
  }, [status]);

  return (
    <div className="container">
      <header className="header">
        <h1>Eidolon Metrics Dashboard</h1>
        <div className="toolbar">
          <span className={statusBadgeClass} title="WebSocket connection status">
            <span className={`status-dot ${status === "open" ? "ok" : ""}`} />
            {status}
          </span>
          <button className="button" onClick={connect} disabled={status === "connecting"}>
            Connect
          </button>
          <button className="button" onClick={disconnect}>
            Disconnect
          </button>
          <button className="button" onClick={sendPing}>
            Ping
          </button>
          <button className="button" onClick={requestSnapshot}>
            Request WS Snapshot
          </button>
        </div>
      </header>

      {err && (
        <div className="panel" style={{ borderColor: "var(--danger)" }}>
          <div style={{ color: "var(--danger)" }}>{err}</div>
        </div>
      )}

      <div className="grid">
        {/* Left: Heap chart */}
        <div className="panel">
          <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 12 }}>
            <div>
              <div className="label">Heap Usage</div>
              <div className="value">
                {formatBytes(snap?.heap.used ?? null)}{" "}
                <span className="subtle" style={{ fontSize: 14 }}>
                  / {formatBytes(snap?.heap.max ?? null)}
                </span>
              </div>
            </div>
            <div className="badge" title="Last timestamp">
              {snap ? new Date(snap.timestampMillis).toLocaleTimeString() : "—"}
            </div>
          </div>
          <HeapChart
            points={points}
            maxBytesHint={snap?.heap.max ?? null}
            height={220}
          />
        </div>

        {/* Right: summary cards */}
        <div className="panel">
          <div className="cards">
            <StatCard
              label="Heap Used"
              value={formatBytes(snap?.heap.used ?? null)}
              sub={`Committed: ${formatBytes(snap?.heap.committed ?? null)}`}
            />
            <StatCard
              label="Heap Max"
              value={formatBytes(snap?.heap.max ?? null)}
              sub={heapPct != null ? `Usage: ${(heapPct * 100).toFixed(1)}%` : "Usage: –"}
            />
            <StatCard
              label="Threads"
              value={formatNumber(snap?.threads.threadCount ?? null)}
              sub={`Daemon: ${formatNumber(snap?.threads.daemonThreadCount ?? null)}`}
            />
            <StatCard
              label="Classes Loaded"
              value={formatNumber(snap?.classes.loadedClassCount ?? null)}
              sub={`Total: ${formatNumber(snap?.classes.totalLoadedClassCount ?? null)}`}
            />
          </div>
          <div style={{ display: "flex", gap: 12, marginTop: 12 }}>
            <div className="card" style={{ flex: 1 }}>
              <div className="label">Thread States</div>
              <div style={{ marginTop: 8 }}>
                {snap
                  ? Object.entries(snap.threads.stateCounts).map(([k, v]) => (
                      <span key={k} className="badge" style={{ marginRight: 6, marginBottom: 6 }}>
                        {k}: {formatNumber(v)}
                      </span>
                    ))
                  : <span className="subtle">No data</span>}
              </div>
            </div>
            <div className="card" style={{ flex: 1 }}>
              <div className="label">GC Events</div>
              <div className="subtle" style={{ marginTop: 6 }}>
                Last {snap?.recentGcEvents?.length ?? 0} events
              </div>
              <div style={{ maxHeight: 160, overflow: "auto", marginTop: 8 }}>
                <table className="table">
                  <thead>
                    <tr>
                      <th>GC</th>
                      <th>Action</th>
                      <th>Cause</th>
                      <th>Duration</th>
                    </tr>
                  </thead>
                  <tbody>
                    {snap?.recentGcEvents?.slice(-10).reverse().map((e, i) => (
                      <tr key={i}>
                        <td>{e.gcName}</td>
                        <td className="subtle">{e.gcAction}</td>
                        <td className="subtle">{e.gcCause}</td>
                        <td>{formatMillis(e.durationMillis)}</td>
                      </tr>
                    )) ?? (
                      <tr>
                        <td className="subtle" colSpan={4}>No events</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Memory pools table */}
      <div className="panel" style={{ marginTop: 16 }}>
        <div className="label">Memory Pools</div>
        <div style={{ overflowX: "auto", marginTop: 8 }}>
          <table className="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Type</th>
                <th>Used</th>
                <th>Committed</th>
                <th>Max</th>
                <th>Collection Used</th>
              </tr>
            </thead>
            <tbody>
              {snap?.heap.pools?.map((p) => (
                <tr key={p.name}>
                  <td>{p.name}</td>
                  <td className="subtle">{p.type}</td>
                  <td>{formatBytes(p.usage?.used ?? null)}</td>
                  <td>{formatBytes(p.usage?.committed ?? null)}</td>
                  <td>{formatBytes(p.usage?.max ?? null)}</td>
                  <td>{formatBytes(p.collectionUsage?.used ?? null)}</td>
                </tr>
              )) ?? (
                <tr>
                  <td className="subtle" colSpan={6}>No pools</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* String table (optional) */}
      <div className="panel" style={{ marginTop: 16 }}>
        <div className="label">String Table</div>
        {!snap?.stringTable?.available && <div className="subtle" style={{ marginTop: 6 }}>Not available</div>}
        {snap?.stringTable?.available && (
          <div className="cards" style={{ marginTop: 8 }}>
            <StatCard label="Table Size" value={formatNumber(snap.stringTable?.tableSize ?? null)} />
            <StatCard label="Buckets" value={formatNumber(snap.stringTable?.bucketCount ?? null)} />
            <StatCard label="Entries" value={formatNumber(snap.stringTable?.entryCount ?? null)} />
            <StatCard label="Total Memory" value={formatBytes(snap.stringTable?.totalMemoryBytes ?? null)} />
          </div>
        )}
      </div>
    </div>
  );
}
