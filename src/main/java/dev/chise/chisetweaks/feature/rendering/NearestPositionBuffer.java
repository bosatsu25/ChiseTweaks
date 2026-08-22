package dev.chise.chisetweaks.feature.rendering;

import java.util.Arrays;

/** Fixed-capacity nearest-N position buffer shared by through-wall analyzers. */
final class NearestPositionBuffer {
    private final long[] positions;
    private final double[] distanceSquared;
    private int count;

    NearestPositionBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        positions = new long[capacity];
        distanceSquared = new double[capacity];
    }

    void clear() {
        count = 0;
    }

    void offer(long packedPosition, double candidateDistanceSquared, int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 0), positions.length);
        if (limit == 0) return;
        if (count < limit) {
            positions[count] = packedPosition;
            distanceSquared[count] = candidateDistanceSquared;
            count++;
            return;
        }

        int farthestIndex = 0;
        double farthestDistance = distanceSquared[0];
        for (int index = 1; index < count; index++) {
            if (distanceSquared[index] > farthestDistance) {
                farthestDistance = distanceSquared[index];
                farthestIndex = index;
            }
        }
        if (candidateDistanceSquared >= farthestDistance) return;
        positions[farthestIndex] = packedPosition;
        distanceSquared[farthestIndex] = candidateDistanceSquared;
    }

    void sortPositions() {
        Arrays.sort(positions, 0, count);
    }

    long[] positions() {
        return positions;
    }

    int count() {
        return count;
    }
}
