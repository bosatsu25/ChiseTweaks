package dev.chise.chisetweaks.gui;

import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.Comparator;
import java.util.List;

/** 明示選択されたBlockStateだけを正本として、近傍の同一Block IDを上限付きで比較する。 */
public final class PatternConsistencyInspector
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    static final int HORIZONTAL_RADIUS = 8;
    static final int VERTICAL_RADIUS = 4;
    static final int MAX_BLOCKS_PER_TICK = 256;
    static final int MAX_RETAINED_MISMATCHES = 64;
    static final int RESCAN_INTERVAL_TICKS = 20;
    static final int MAX_PROPERTIES = 32;
    static final int TOTAL_BLOCKS = (HORIZONTAL_RADIUS * 2 + 1)
            * (HORIZONTAL_RADIUS * 2 + 1)
            * (VERTICAL_RADIUS * 2 + 1);

    private static volatile PatternConsistencyInspector active;

    private final BlockPos.MutableBlockPos cursorPos = new BlockPos.MutableBlockPos();
    private final long[] retainedMismatchPositions = new long[MAX_RETAINED_MISMATCHES];
    private Level referenceLevel;
    private BlockPos referencePos;
    private BlockState referenceState;
    private List<Property<?>> comparedProperties = List.of();
    private int[] mismatchCounts = new int[0];
    private int cursor;
    private int cooldown;
    private int compared;
    private int matches;
    private int mismatchTotal;
    private int retainedMismatchCount;
    private long revision;

    @Override
    public String getId() {
        return "pattern-consistency";
    }

    @Override
    public void init() {
        active = this;
    }

    @Override
    public void tick(Minecraft client) {
        scanTick(client == null ? null : client.level);
    }

    @Override
    public void resetSession(Minecraft client) {
        clear();
    }

    @Override
    public void onQuarantined(Minecraft client) {
        clear();
        if (active == this) active = null;
    }

    static PatternConsistencyInspector activeInspector() {
        return active;
    }

    static long currentRevision() {
        PatternConsistencyInspector inspector = active;
        return inspector == null ? 0L : inspector.revision;
    }

    static boolean selectReference(Minecraft client) {
        PatternConsistencyInspector inspector = active;
        if (inspector == null || client == null || client.level == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return false;
        return inspector.selectReference(client.level, hit.getBlockPos());
    }

    static void clearReference() {
        PatternConsistencyInspector inspector = active;
        if (inspector != null) inspector.clear();
    }

    boolean selectReference(Level level, BlockPos pos) {
        if (level == null || pos == null
                || !level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return false;
        BlockState state = level.getBlockState(pos);
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return false;

        referenceLevel = level;
        referencePos = pos.immutable();
        referenceState = state;
        comparedProperties = state.getProperties().stream()
                .sorted(Comparator.comparing(Property::getName))
                .limit(MAX_PROPERTIES)
                .toList();
        mismatchCounts = new int[comparedProperties.size()];
        cursor = 0;
        cooldown = 0;
        compared = 0;
        matches = 0;
        mismatchTotal = 0;
        retainedMismatchCount = 0;
        revision++;
        return true;
    }

    void scanTick(Level level) {
        if (referenceState == null) return;
        if (level == null || level != referenceLevel) {
            clear();
            return;
        }
        int referenceChunkX = referencePos.getX() >> 4;
        int referenceChunkZ = referencePos.getZ() >> 4;
        if (!level.getChunkSource().hasChunk(referenceChunkX, referenceChunkZ)) return;
        if (!level.getBlockState(referencePos).equals(referenceState)) {
            clear();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (cursor == 0) beginScan();

        int processed = 0;
        while (cursor < TOTAL_BLOCKS && processed++ < MAX_BLOCKS_PER_TICK) {
            int index = cursor++;
            int height = VERTICAL_RADIUS * 2 + 1;
            int width = HORIZONTAL_RADIUS * 2 + 1;
            int y = index % height;
            int plane = index / height;
            int x = plane % width;
            int z = plane / width;
            cursorPos.set(
                    referencePos.getX() + x - HORIZONTAL_RADIUS,
                    referencePos.getY() + y - VERTICAL_RADIUS,
                    referencePos.getZ() + z - HORIZONTAL_RADIUS);
            if (cursorPos.equals(referencePos)
                    || !level.getChunkSource().hasChunk(
                            cursorPos.getX() >> 4, cursorPos.getZ() >> 4)) continue;
            compare(level.getBlockState(cursorPos));
        }
        if (cursor >= TOTAL_BLOCKS) {
            revision++;
            cursor = 0;
            cooldown = RESCAN_INTERVAL_TICKS;
        }
    }

    private void beginScan() {
        compared = 0;
        matches = 0;
        mismatchTotal = 0;
        retainedMismatchCount = 0;
        java.util.Arrays.fill(mismatchCounts, 0);
        revision++;
    }

    private void compare(BlockState candidate) {
        if (candidate.getBlock() != referenceState.getBlock()) return;
        compared++;
        boolean mismatch = false;
        for (int index = 0; index < comparedProperties.size(); index++) {
            Property<?> property = comparedProperties.get(index);
            if (!candidate.hasProperty(property)
                    || !sameValue(referenceState, candidate, property)) {
                mismatchCounts[index]++;
                mismatch = true;
            }
        }
        if (!mismatch) {
            matches++;
            return;
        }
        mismatchTotal++;
        if (retainedMismatchCount < retainedMismatchPositions.length) {
            retainedMismatchPositions[retainedMismatchCount++] = cursorPos.asLong();
        }
    }

    private static <T extends Comparable<T>> boolean sameValue(
            BlockState reference,
            BlockState candidate,
            Property<T> property) {
        return reference.getValue(property).equals(candidate.getValue(property));
    }

    private void clear() {
        if (referenceState == null) return;
        referenceLevel = null;
        referencePos = null;
        referenceState = null;
        comparedProperties = List.of();
        mismatchCounts = new int[0];
        cursor = 0;
        cooldown = 0;
        compared = 0;
        matches = 0;
        mismatchTotal = 0;
        retainedMismatchCount = 0;
        revision++;
    }

    int scanCursor() {
        return cursor;
    }

    boolean hasReference() {
        return referenceState != null;
    }

    String referenceId() {
        Identifier id = BuiltInRegistries.BLOCK.getKey(referenceState.getBlock());
        return id == null ? "" : id.toString();
    }

    List<String> referenceProperties() {
        return CrosshairInspector.stateProperties(referenceState);
    }

    int compared() {
        return compared;
    }

    int matches() {
        return matches;
    }

    int mismatchTotal() {
        return mismatchTotal;
    }

    int retainedMismatches() {
        return retainedMismatchCount;
    }

    String mismatchSummary(String group) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < mismatchCounts.length; index++) {
            if (mismatchCounts[index] == 0) continue;
            String property = comparedProperties.get(index).getName();
            if (!group.equals(ChiseTweaksSettingsCatalog.semanticPropertyGroup(property))) continue;
            if (!result.isEmpty()) result.append('\n');
            result.append(ChiseTweaksSettingsCatalog.humanize(property))
                    .append("  ")
                    .append(mismatchCounts[index]);
        }
        return result.toString();
    }
}
