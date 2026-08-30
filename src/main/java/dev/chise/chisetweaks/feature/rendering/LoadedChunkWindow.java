package dev.chise.chisetweaks.feature.rendering;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Reusable bounded view of already-loaded chunks for local block scans.
 * Loading never force-loads a chunk; missing chunks remain null.
 */
public final class LoadedChunkWindow {
    private final LevelChunk[] chunks;
    private int minChunkX;
    private int minChunkZ;
    private int spanX;
    private int spanZ;
    private int used;

    public LoadedChunkWindow(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        chunks = new LevelChunk[capacity];
    }

    public boolean load(
            ClientLevel level,
            int minBlockX,
            int maxBlockX,
            int minBlockZ,
            int maxBlockZ) {
        clear();
        if (level == null || minBlockX > maxBlockX || minBlockZ > maxBlockZ) return false;

        int resolvedMinChunkX = minBlockX >> 4;
        int resolvedMaxChunkX = maxBlockX >> 4;
        int resolvedMinChunkZ = minBlockZ >> 4;
        int resolvedMaxChunkZ = maxBlockZ >> 4;
        int resolvedSpanX = resolvedMaxChunkX - resolvedMinChunkX + 1;
        int resolvedSpanZ = resolvedMaxChunkZ - resolvedMinChunkZ + 1;
        long required = (long) resolvedSpanX * resolvedSpanZ;
        if (required > chunks.length) return false;

        minChunkX = resolvedMinChunkX;
        minChunkZ = resolvedMinChunkZ;
        spanX = resolvedSpanX;
        spanZ = resolvedSpanZ;
        int index = 0;
        for (int chunkZ = resolvedMinChunkZ; chunkZ <= resolvedMaxChunkZ; chunkZ++) {
            for (int chunkX = resolvedMinChunkX; chunkX <= resolvedMaxChunkX; chunkX++) {
                chunks[index++] = level.getChunkSource().getChunkNow(chunkX, chunkZ);
            }
        }
        used = (int) required;
        return true;
    }

    public LevelChunk atBlock(int blockX, int blockZ) {
        int offsetX = (blockX >> 4) - minChunkX;
        int offsetZ = (blockZ >> 4) - minChunkZ;
        if (offsetX < 0 || offsetX >= spanX || offsetZ < 0 || offsetZ >= spanZ) return null;
        return chunks[offsetZ * spanX + offsetX];
    }

    public void clear() {
        for (int index = 0; index < used; index++) chunks[index] = null;
        used = 0;
        minChunkX = 0;
        minChunkZ = 0;
        spanX = 0;
        spanZ = 0;
    }
}
