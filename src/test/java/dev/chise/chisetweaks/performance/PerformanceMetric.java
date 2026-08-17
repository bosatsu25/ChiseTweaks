package dev.chise.chisetweaks.performance;

enum PerformanceMetric {
    STARTUP_MS(true),
    P50_FRAMETIME_MS(true),
    P95_FRAMETIME_MS(true),
    P99_FRAMETIME_MS(true),
    HEAP_MIB(true),
    ALLOCATION_MIB_S(true),
    RENDER_THREAD_CPU_PCT(true),
    AVERAGE_FPS(false);

    private final boolean lowerIsBetter;

    PerformanceMetric(boolean lowerIsBetter) {
        this.lowerIsBetter = lowerIsBetter;
    }

    boolean lowerIsBetter() {
        return lowerIsBetter;
    }

    double valueOf(PerformanceRun run) {
        return switch (this) {
            case STARTUP_MS -> run.startupMs();
            case P50_FRAMETIME_MS -> run.p50FrametimeMs();
            case P95_FRAMETIME_MS -> run.p95FrametimeMs();
            case P99_FRAMETIME_MS -> run.p99FrametimeMs();
            case HEAP_MIB -> run.heapMiB();
            case ALLOCATION_MIB_S -> run.allocationMiBS();
            case RENDER_THREAD_CPU_PCT -> run.renderThreadCpuPct();
            case AVERAGE_FPS -> run.averageFps();
        };
    }
}
