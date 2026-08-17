package dev.chise.chisetweaks.performance;

record PerformanceRun(
        Double startupMs,
        Double p50FrametimeMs,
        Double p95FrametimeMs,
        Double p99FrametimeMs,
        Double heapMiB,
        Double allocationMiBS,
        Double renderThreadCpuPct,
        Double averageFps) {

    PerformanceRun {
        validate("startupMs", startupMs);
        validate("p50FrametimeMs", p50FrametimeMs);
        validate("p95FrametimeMs", p95FrametimeMs);
        validate("p99FrametimeMs", p99FrametimeMs);
        validate("heapMiB", heapMiB);
        validate("allocationMiBS", allocationMiBS);
        validate("renderThreadCpuPct", renderThreadCpuPct);
        validate("averageFps", averageFps);
    }

    private static void validate(String name, Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0.0)) {
            throw new IllegalArgumentException(name + " must be null or a finite non-negative value");
        }
    }
}
