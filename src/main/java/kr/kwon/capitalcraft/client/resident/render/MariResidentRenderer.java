package kr.kwon.capitalcraft.client.resident.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

public final class MariResidentRenderer
    extends MobRenderer<Villager, ResidentVillagerRenderState, MariResidentModel> {

    public MariResidentRenderer(EntityRendererProvider.Context context) {
        super(context, new MariResidentModel(MariMeshLoader.load(context.getResourceManager())), 0.38F);
    }

    @Override
    public Identifier getTextureLocation(ResidentVillagerRenderState state) {
        return MariMeshLoader.TEXTURE;
    }

    @Override
    public ResidentVillagerRenderState createRenderState() {
        return new ResidentVillagerRenderState();
    }
}
