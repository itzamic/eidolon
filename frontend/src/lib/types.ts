export interface MemoryUsage {
  init: number | null;
  used: number;
  committed: number;
  max: number | null;
}

export interface MemoryPool {
  name: string;
  type: "HEAP" | "NON_HEAP" | string;
  usage: MemoryUsage;
  collectionUsage: MemoryUsage | null;
}

export interface HeapMetrics {
  used: number;
  committed: number;
  max: number | null;
  pools: MemoryPool[];
}

export interface ThreadMetrics {
  threadCount: number;
  daemonThreadCount: number;
  peakThreadCount: number;
  totalStartedThreadCount: number;
  stateCounts: Record<string, number>;
}

export interface ClassMetrics {
  loadedClassCount: number;
  totalLoadedClassCount: number;
  unloadedClassCount: number;
}

export interface StringTableMetrics {
  available: boolean;
  tableSize?: number;
  bucketCount?: number;
  entryCount?: number;
  totalMemoryBytes?: number;
  rawAttributes?: Record<string, unknown>;
}

export interface GcEvent {
  gcName: string;
  gcAction: string;
  gcCause: string;
  startTimeMillis: number;
  durationMillis: number;
}

export interface MetricsSnapshot {
  timestampMillis: number;
  heap: HeapMetrics;
  threads: ThreadMetrics;
  classes: ClassMetrics;
  stringTable?: StringTableMetrics;
  recentGcEvents: GcEvent[];
}

export type HeapPoint = { t: number; used: number; max: number | null };

export interface RuntimeInfo {
  jvmName: string;
  jvmVendor: string;
  jvmVersion: string;
  vmName: string;
  vmVendor: string;
  vmVersion: string;

  gcCollectors: string[];
  inputArguments: string[];

  heapInit?: number | null;
  heapMax?: number | null;

  host: string;
  port: number;
  contextPath: string;
  websocketEnabled: boolean;
  websocketIntervalMillis: number;
  gcEventBufferSize: number;
  collectStringTable: boolean;
}
