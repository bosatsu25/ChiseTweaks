package dev.chise.chisetweaks.integration.masa;

import dev.chise.chisetweaks.ChiseTweaksClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Method;

/** Fail-soft reflection boundary for reading Litematica's schematic world without a hard dependency. */
public final class LitematicaSchematicAccess {
    private static volatile boolean lookupAttempted;
    private static volatile Method getSchematicWorld;

    private LitematicaSchematicAccess() {}

    public static BlockState expectedState(BlockPos pos) {
        if (pos == null || !MasaModAvailability.isLoaded(MasaModAvailability.LITEMATICA)) return null;
        try {
            Method getter = schematicWorldGetter();
            if (getter == null) return null;
            Object world = getter.invoke(null);
            return world instanceof Level level ? level.getBlockState(pos) : null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            ChiseTweaksClient.LOGGER.debug(
                    "Litematica schematic inspection unavailable after {}",
                    failure.getClass().getSimpleName());
            return null;
        }
    }

    private static Method schematicWorldGetter() {
        if (lookupAttempted) return getSchematicWorld;
        synchronized (LitematicaSchematicAccess.class) {
            if (lookupAttempted) return getSchematicWorld;
            lookupAttempted = true;
            try {
                Class<?> handler = Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler");
                getSchematicWorld = handler.getMethod("getSchematicWorld");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                getSchematicWorld = null;
            }
            return getSchematicWorld;
        }
    }
}
