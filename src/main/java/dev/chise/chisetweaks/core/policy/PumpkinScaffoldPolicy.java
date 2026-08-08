package dev.chise.chisetweaks.core.policy;

/** Pure bounds and eligibility policy for Pumpkin Scaffold. */
public final class PumpkinScaffoldPolicy {
    public static final int MIN_PLACEMENT_RANGE = 1;
    public static final int MAX_PLACEMENT_RANGE = 5;
    public static final int DEFAULT_PLACEMENT_RANGE = 4;
    public static final int HOTBAR_SIZE = 9;

    private PumpkinScaffoldPolicy() {
    }

    public static int clampPlacementRange(int requested) {
        return Math.max(MIN_PLACEMENT_RANGE, Math.min(MAX_PLACEMENT_RANGE, requested));
    }

    public static boolean canHandleClick(
            boolean enabled,
            boolean playerPresent,
            boolean levelPresent,
            boolean gameModePresent,
            boolean screenOpen,
            boolean spectator,
            boolean hitPresent) {
        return enabled
                && playerPresent
                && levelPresent
                && gameModePresent
                && !screenOpen
                && !spectator
                && hitPresent;
    }

    public static boolean canAttemptAirPlacement(boolean chunkLoaded, boolean targetIsAir) {
        return chunkLoaded && targetIsAir;
    }
}
