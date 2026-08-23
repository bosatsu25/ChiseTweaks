package dev.chise.chisetweaks.feature.resource;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VisibilityPackTest {
    @Test
    void managedPacksHaveStableDistinctRepositoryIds() {
        assertEquals(2, VisibilityPack.values().length);
        Set<String> ids = Set.of(
                VisibilityPack.CHEST.repositoryPackId(),
                VisibilityPack.WHITE_CONCRETE.repositoryPackId());
        assertEquals(2, ids.size());
        assertEquals("chisetweaks:chise_chest_visibility", VisibilityPack.CHEST.repositoryPackId());
        assertEquals("chisetweaks:chise_white_concrete_visibility", VisibilityPack.WHITE_CONCRETE.repositoryPackId());
    }

    @Test
    void packMetadataIsNonBlank() {
        for (VisibilityPack pack : VisibilityPack.values()) {
            assertTrue(!pack.path().isBlank());
            assertTrue(!pack.displayName().isBlank());
        }
    }
}
