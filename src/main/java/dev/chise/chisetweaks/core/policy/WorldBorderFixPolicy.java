package dev.chise.chisetweaks.core.policy;

/** Pure hysteresis policy for optional Nvidium suppression near risky world locations. */
public final class WorldBorderFixPolicy {
    public static final double BORDER_EXIT_MARGIN = 32.0;
    public static final double COORD_EXIT_MARGIN = 256.0;

    private WorldBorderFixPolicy() {}

    public static boolean shouldSuppress(
            double distanceInsideBorder,
            double maxAbsoluteCoordinate,
            boolean currentlySuppressed,
            boolean xrayFixEnabled,
            int borderDistance,
            boolean farCoordFixEnabled,
            int coordThreshold) {
        if (xrayFixEnabled) {
            double threshold = Math.max(1, borderDistance)
                    + (currentlySuppressed ? BORDER_EXIT_MARGIN : 0.0);
            if (distanceInsideBorder < threshold) return true;
        }
        if (farCoordFixEnabled) {
            double threshold = Math.max(1000, coordThreshold)
                    - (currentlySuppressed ? COORD_EXIT_MARGIN : 0.0);
            if (maxAbsoluteCoordinate >= threshold) return true;
        }
        return false;
    }

    public static double distanceInsideSquareBorder(
            double playerX,
            double playerZ,
            double centerX,
            double centerZ,
            double size) {
        double half = Math.max(0.0, size) * 0.5;
        double xDistance = half - Math.abs(playerX - centerX);
        double zDistance = half - Math.abs(playerZ - centerZ);
        return Math.min(xDistance, zDistance);
    }
}
