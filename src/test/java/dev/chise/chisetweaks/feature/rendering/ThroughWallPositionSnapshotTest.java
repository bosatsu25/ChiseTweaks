package dev.chise.chisetweaks.feature.rendering;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ThroughWallPositionSnapshotTest {
    @Test
    void publishesPackedPositionsAndRefreshesDistanceReferenceWithoutChangingTheSet() {
        ThroughWallPositionSnapshot snapshot = new ThroughWallPositionSnapshot(4);
        ThroughWallPositionSnapshot.Capture capture = new ThroughWallPositionSnapshot.Capture(4);
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
        ThroughWallPositionSnapshot snapshot = new ThroughWallPositionSnapshot(3);
        ThroughWallPositionSnapshot.Capture capture = new ThroughWallPositionSnapshot.Capture(3);
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

    @Test
    void mutationAndCaptureUseOneSerializationBoundary() throws NoSuchMethodException {
        Class<ThroughWallPositionSnapshot> type = ThroughWallPositionSnapshot.class;
        assertTrue(Modifier.isSynchronized(type.getDeclaredMethod(
                "publish", long[].class, int.class, double.class, double.class, double.class).getModifiers()));
        assertTrue(Modifier.isSynchronized(type.getDeclaredMethod("clear").getModifiers()));
        assertTrue(Modifier.isSynchronized(type.getDeclaredMethod(
                "captureInto", ThroughWallPositionSnapshot.Capture.class).getModifiers()));
    }

    @Test
    void rejectsInvalidCapacitiesAndCaptureRanges() {
        assertThrows(IllegalArgumentException.class, () -> new ThroughWallPositionSnapshot(0));
        assertThrows(IllegalArgumentException.class, () -> new ThroughWallPositionSnapshot.Capture(0));

        ThroughWallPositionSnapshot snapshot = new ThroughWallPositionSnapshot(2);
        assertThrows(IllegalArgumentException.class, () -> snapshot.publish(null, 0, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> snapshot.publish(new long[1], 2, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot.captureInto(new ThroughWallPositionSnapshot.Capture(1)));
    }
}
