package dev.chise.chisetweaks.feature.rendering;

import java.util.Arrays;

/** 壁越し解析結果を固定上限のdouble-bufferでRendererへ受け渡す共通snapshot。 */
class ThroughWallPositionSnapshot {
    private final long[][] positions;
    private final int[] counts = new int[2];

    private volatile int activeSlot;
    private volatile long renderRevision;
    private double eyeX;
    private double eyeY;
    private double eyeZ;

    ThroughWallPositionSnapshot(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
        positions = new long[][] {new long[capacity], new long[capacity]};
    }

    int capacity() { return positions[0].length; }
    boolean isEmpty() { return counts[activeSlot] == 0; }
    long renderRevision() { return renderRevision; }

    boolean publish(long[] sortedPositions, int count, double eyeX, double eyeY, double eyeZ) {
        validateInput(sortedPositions, count);
        int currentSlot = activeSlot;
        boolean changed = !matches(currentSlot, sortedPositions, count);
        if (changed) {
            int nextSlot = currentSlot ^ 1;
            System.arraycopy(sortedPositions, 0, positions[nextSlot], 0, count);
            counts[nextSlot] = count;
            activeSlot = nextSlot;
        }
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.eyeZ = eyeZ;
        renderRevision++;
        return changed;
    }

    boolean clear() {
        int currentSlot = activeSlot;
        if (counts[currentSlot] == 0) return false;
        int nextSlot = currentSlot ^ 1;
        counts[nextSlot] = 0;
        activeSlot = nextSlot;
        renderRevision++;
        return true;
    }

    void captureInto(Capture target) {
        if (target == null) throw new IllegalArgumentException("target must not be null");
        if (target.positions.length < capacity()) {
            throw new IllegalArgumentException("capture capacity is smaller than snapshot capacity");
        }
        while (true) {
            long before = renderRevision;
            int slot = activeSlot;
            int count = counts[slot];
            double capturedEyeX = eyeX;
            double capturedEyeY = eyeY;
            double capturedEyeZ = eyeZ;
            System.arraycopy(positions[slot], 0, target.positions, 0, count);
            long after = renderRevision;
            if (before == after) {
                target.count = count;
                target.eyeX = capturedEyeX;
                target.eyeY = capturedEyeY;
                target.eyeZ = capturedEyeZ;
                target.revision = after;
                return;
            }
        }
    }

    private boolean matches(int slot, long[] candidate, int count) {
        if (counts[slot] != count) return false;
        return Arrays.equals(positions[slot], 0, count, candidate, 0, count);
    }

    private void validateInput(long[] candidate, int count) {
        if (candidate == null) throw new IllegalArgumentException("positions must not be null");
        if (count < 0 || count > capacity() || count > candidate.length) {
            throw new IllegalArgumentException("invalid position count: " + count);
        }
    }

    static final class Capture {
        private final long[] positions;
        private int count;
        private double eyeX;
        private double eyeY;
        private double eyeZ;
        private long revision = Long.MIN_VALUE;

        Capture(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity must be positive");
            positions = new long[capacity];
        }

        int count() { return count; }
        long positionAt(int index) {
            if (index < 0 || index >= count) throw new IndexOutOfBoundsException(index);
            return positions[index];
        }
        double eyeX() { return eyeX; }
        double eyeY() { return eyeY; }
        double eyeZ() { return eyeZ; }
        long revision() { return revision; }
    }
}
