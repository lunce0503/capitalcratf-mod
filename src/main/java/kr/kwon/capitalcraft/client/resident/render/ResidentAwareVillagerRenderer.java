package kr.kwon.capitalcraft.client.resident.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.kwon.capitalcraft.client.resident.ResidentClientState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.npc.villager.Villager;

public final class ResidentAwareVillagerRenderer
    extends EntityRenderer<Villager, ResidentVillagerRenderState> {
    private final VillagerRenderer vanillaRenderer;
    private final MariResidentRenderer mariRenderer;

    public ResidentAwareVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.vanillaRenderer = new VillagerRenderer(context);
        this.mariRenderer = new MariResidentRenderer(context);
    }

    @Override
    public ResidentVillagerRenderState createRenderState() {
        return new ResidentVillagerRenderState();
    }

    @Override
    public void extractRenderState(Villager villager, ResidentVillagerRenderState state, float partialTick) {
        vanillaRenderer.extractRenderState(villager, state, partialTick);
        state.appearance = ResidentClientState.appearance(villager.getUUID());
    }

    @Override
    public void submit(
        ResidentVillagerRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState cameraState
    ) {
        if (state.appearance.equals("mari")) {
            mariRenderer.submit(state, poseStack, collector, cameraState);
        } else {
            vanillaRenderer.submit(state, poseStack, collector, cameraState);
        }
    }
}

