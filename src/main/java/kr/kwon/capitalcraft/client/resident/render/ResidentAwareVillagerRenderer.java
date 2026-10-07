package kr.kwon.capitalcraft.client.resident.render;

import com.mojang.blaze3d.vertex.PoseStack;
import kr.kwon.capitalcraft.client.resident.ResidentClientState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.network.chat.Component;

public final class ResidentAwareVillagerRenderer
    extends EntityRenderer<Villager, ResidentVillagerRenderState> {
    private final VillagerRenderer vanillaRenderer;
    private final MariResidentRenderer mariRenderer;
    private final SeiaResidentRenderer seiaRenderer;

    public ResidentAwareVillagerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.vanillaRenderer = new VillagerRenderer(context);
        this.mariRenderer = new MariResidentRenderer(context);
        this.seiaRenderer = new SeiaResidentRenderer(context);
    }

    @Override
    public ResidentVillagerRenderState createRenderState() {
        return new ResidentVillagerRenderState();
    }

    @Override
    public void extractRenderState(Villager villager, ResidentVillagerRenderState state, float partialTick) {
        vanillaRenderer.extractRenderState(villager, state, partialTick);
        state.appearance = ResidentClientState.appearance(villager.getUUID());
        String residentName = ResidentClientState.name(villager.getUUID());
        if (residentName != null && state.nameTag != null) state.nameTag = Component.literal(residentName);
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
        } else if (state.appearance.equals("seia")) {
            seiaRenderer.submit(state, poseStack, collector, cameraState);
        } else {
            vanillaRenderer.submit(state, poseStack, collector, cameraState);
        }
    }
}
