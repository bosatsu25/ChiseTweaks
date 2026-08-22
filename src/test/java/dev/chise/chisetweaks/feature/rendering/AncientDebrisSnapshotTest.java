package dev.chise.chisetweaks.feature.rendering;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AncientDebrisSnapshotTest {
    @Test
    void publishesPackedPositionsAndRefreshesDistanceReferenceWithoutChangingTheSet() {
        AncientDebrisSnapshot snapshot = new AncientDebrisSnapshot(4);
        AncientDebrisSnapshot.Capture capture = new AncientDebrisSnapshot.Capture(4);
        long first = BlockPos.asLong(1, 15, 2);
        long second = BlockPos.asLong(4, 14, 6);
        long[] positions = {Math.min(first, second), Math.max(first, second)};

        assertTrue(snapshot.publish(positions, 2, 1.5, 16.5, 2.5));
        long firstRevision = snapshot.renderRevision();
        snapshot.captureInto(capture);
        assertEquals(2, capture.count());
        assertEquals(positions[0], capture.positionAt(0));
        assertEquals(positions[1], capture.positionAt(1));
        assertEquals(1.5, capture.eyeX());
        assertEquals(16.5, capture.eyeY());
        assertEquals(2.5, capture.eyeZ());
        assertEquals(firstRevision, capture.revision());

        assertFalse(snapshot.publish(positions, 2, 9.5, 18.0, 8.5));
        assertTrue(snapshot.renderRevision() > firstRevision);
        snapshot.captureInto(capture);
        assertEquals(9.5, capture.eyeX());
        assertEquals(18.0, capture.eyeY());
        assertEquals(8.5, capture.eyeZ());
    }

    @Test
    void changedSetAndClearPublishBoundedStateChanges() {
        AncientDebrisSnapshot snapshot = new AncientDebrisSnapshot(3);
        AncientDebrisSnapshot.Capture capture = new AncientDebrisSnapshot.Capture(3);
        long one = BlockPos.asLong(0, 15, 0);
        long two = BlockPos.asLong(1, 15, 0);

        assertTrue(snapshot.publish(new long[] {one}, 1, 0, 16, 0));
        assertTrue(snapshot.publish(new long[] {one, two}, 2, 0, 16, 0));
        snapshot.captureInto(capture);
        assertEquals(2, capture.count());

        assertTrue(snapshot.clear());
        assertTrue(snapshot.isEmpty());
        snapshot.captureInto(capture);
        assertEquals(0, capture.count());
        assertFalse(snapshot.clear());
    }
}
