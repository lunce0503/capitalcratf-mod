package kr.kwon.capitalcraft.client.resident.render;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.world.entity.EntityType;

public final class ResidentRendering {
    private ResidentRendering() {
    }

    @SuppressWarnings("deprecation")
    public static void register() {
        EntityRendererRegistry.register(EntityType.VILLAGER, ResidentAwareVillagerRenderer::new);
    }
}
