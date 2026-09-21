package kr.kwon.capitalcraft.client.resident.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

public final class SeiaResidentRenderer
    extends MobRenderer<Villager, ResidentVillagerRenderState, SeiaResidentModel> {

    public SeiaResidentRenderer(EntityRendererProvider.Context context) {
        super(context, new SeiaResidentModel(SeiaMeshLoader.load(context.getResourceManager())), 0.42F);
    }

    @Override
    public Identifier getTextureLocation(ResidentVillagerRenderState state) {
        return SeiaMeshLoader.TEXTURE;
    }

    @Override
    public ResidentVillagerRenderState createRenderState() {
        return new ResidentVillagerRenderState();
    }
}
