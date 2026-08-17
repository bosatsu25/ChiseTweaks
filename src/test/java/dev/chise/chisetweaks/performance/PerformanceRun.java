package dev.chise.chisetweaks.performance;

record PerformanceRun(
        double startupMs,
        double p50FrametimeMs,
        double p95FrametimeMs,
        double p99FrametimeMs,
        double heapMiB,
        double allocationMiBS,
        double renderThreadCpuPct,
        double averageFps) {

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

    private static void validate(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be a finite non-negative value");
        }
    }
}
