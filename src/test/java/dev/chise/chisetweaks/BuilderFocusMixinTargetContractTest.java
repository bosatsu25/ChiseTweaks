package dev.chise.chisetweaks;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BuilderFocusMixinTargetContractTest {
    @Test
    void minecraft26TargetsStillHaveExpectedMethodShapes() throws Exception {
        assertEquals(
                RenderShape.class,
                BlockBehaviour.BlockStateBase.class.getDeclaredMethod("getRenderShape").getReturnType());
        assertEquals(
                EntityRenderState.class,
                LevelRenderer.class.getDeclaredMethod("extractEntity", Entity.class, float.class).getReturnType());
    }
}
