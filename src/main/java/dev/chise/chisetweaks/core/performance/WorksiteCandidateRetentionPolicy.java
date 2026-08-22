package dev.chise.chisetweaks.core.performance;

public final class WorksiteCandidateRetentionPolicy {
    private WorksiteCandidateRetentionPolicy() {}

    public static boolean shouldRetain(
            int currentSize,
            int maximumSize,
            int candidatePriority,
            double candidateDistanceSquared,
            int weakestPriority,
            double weakestDistanceSquared) {
        if (maximumSize <= 0) return false;
        if (currentSize < maximumSize) return true;
        return isBetter(candidatePriority, candidateDistanceSquared, weakestPriority, weakestDistanceSquared);
    }

    public static boolean isBetter(
            int candidatePriority,
            double candidateDistanceSquared,
            int existingPriority,
            double existingDistanceSquared) {
        if (candidatePriority != existingPriority) return candidatePriority > existingPriority;
        return candidateDistanceSquared < existingDistanceSquared;
    }
}
