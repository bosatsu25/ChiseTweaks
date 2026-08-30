package dev.chise.chisetweaks.integration.compat;

import dev.chise.chisetweaks.ChiseTweaksClient;
import dev.chise.chisetweaks.config.CompatibilityIntegrationConfig;
import dev.chise.chisetweaks.core.policy.WorldBorderFixPolicy;
import dev.chise.chisetweaks.runtime.SessionAwareRuntimeComponent;
import dev.chise.chisetweaks.runtime.TickingRuntimeComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec3;

/**
 * Temporarily suppresses Nvidium near world-border/far-coordinate rendering risk zones.
 * The integration is disabled by default and becomes a complete no-op when Nvidium is absent.
 */
public final class WorldBorderFixComponent
        implements TickingRuntimeComponent, SessionAwareRuntimeComponent {
    static final int CHECK_INTERVAL_TICKS = 10;
    static final int STABLE_CHECKS_REQUIRED = 3;
    static final int RELOAD_COOLDOWN_TICKS = 200;

    private int ticksUntilCheck;
    private int stableCount;
    private boolean lastDesired;
    private int reloadCooldown;

    @Override
    public String getId() {
        return "nvidium-world-border-fix";
    }

    @Override
    public void init() {
        NvidiumCompatibility.init();
    }

    @Override
    public void tick(Minecraft client) {
        if (reloadCooldown > 0) reloadCooldown--;

        CompatibilityIntegrationConfig config = CompatibilityIntegrationConfig.getInstance();
        if (!config.worldBorderFixEnabled) {
            releaseSuppression(false, client);
            resetDecisionState();
            return;
        }
        if (!NvidiumCompatibility.isAvailable()) return;
        if (client == null || client.player == null || client.level == null) {
            releaseSuppression(false, client);
            resetDecisionState();
            return;
        }
        if (ticksUntilCheck > 0) {
            ticksUntilCheck--;
            return;
        }
        ticksUntilCheck = CHECK_INTERVAL_TICKS - 1;

        boolean suppressed = NvidiumCompatibility.isSuppressed();
        boolean desired = shouldSuppress(client, config, suppressed);
        if (desired == suppressed) {
            stableCount = 0;
            lastDesired = desired;
            return;
        }
        if (desired != lastDesired) {
            lastDesired = desired;
            stableCount = 1;
            return;
        }
        stableCount++;
        if (stableCount < STABLE_CHECKS_REQUIRED) return;

        boolean needsReload = desired
                ? NvidiumCompatibility.isCurrentlyEnabled()
                : config.worldBorderFixAutoReenable;
        if (needsReload && reloadCooldown > 0) return;

        boolean changed = desired
                ? NvidiumCompatibility.suppress()
                : NvidiumCompatibility.restore();
        stableCount = 0;
        if (!changed) return;

        if (needsReload) {
            client.levelRenderer.allChanged();
            reloadCooldown = RELOAD_COOLDOWN_TICKS;
        }
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal(desired
                            ? "ChiseTweaks: Nvidiumを一時停止しました (World Border Fix)"
                            : needsReload
                                    ? "ChiseTweaks: Nvidiumを復帰しました"
                                    : "ChiseTweaks: Nvidium復帰フラグを戻しました。次回の自然な描画再読込で復帰します"),
                    true);
        }
        ChiseTweaksClient.LOGGER.info(
                "World Border Fix changed Nvidium suppression to {} (reload={})",
                desired,
                needsReload);
    }

    private static boolean shouldSuppress(
            Minecraft client,
            CompatibilityIntegrationConfig config,
            boolean currentlySuppressed) {
        Vec3 position = client.player.position();
        WorldBorder border = client.level.getWorldBorder();
        double distanceInside = WorldBorderFixPolicy.distanceInsideSquareBorder(
                position.x,
                position.z,
                border.getCenterX(),
                border.getCenterZ(),
                border.getSize());
        double maxAbsoluteCoordinate = Math.max(Math.abs(position.x), Math.abs(position.z));
        return WorldBorderFixPolicy.shouldSuppress(
                distanceInside,
                maxAbsoluteCoordinate,
                currentlySuppressed,
                config.worldBorderFixXray,
                config.worldBorderFixDistance,
                config.worldBorderFixFarCoords,
                config.worldBorderFixCoordThreshold);
    }

    @Override
    public void resetSession(Minecraft client) {
        releaseSuppression(false, client);
        resetDecisionState();
        reloadCooldown = 0;
    }

    @Override
    public void onQuarantined(Minecraft client) {
        releaseSuppression(false, client);
        resetDecisionState();
    }

    private static void releaseSuppression(boolean reload, Minecraft client) {
        if (!NvidiumCompatibility.restore()) return;
        if (reload && client != null && client.levelRenderer != null) {
            client.levelRenderer.allChanged();
        }
    }

    private void resetDecisionState() {
        ticksUntilCheck = 0;
        stableCount = 0;
        lastDesired = false;
    }
}
