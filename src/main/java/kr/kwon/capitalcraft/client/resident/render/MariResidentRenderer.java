package kr.kwon.capitalcraft.client.resident.render;

import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;

public final class MariResidentRenderer
    extends MobRenderer<Villager, ResidentVillagerRenderState, MariResidentModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        CapitalCraftClientMod.RESOURCE_NAMESPACE,
        "textures/entity/resident/mari.png"
    );

    public MariResidentRenderer(EntityRendererProvider.Context context) {
        super(context, new MariResidentModel(context.bakeLayer(MariResidentModel.LAYER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(ResidentVillagerRenderState state) {
        return TEXTURE;
    }

    @Override
    public ResidentVillagerRenderState createRenderState() {
        return new ResidentVillagerRenderState();
    }
}
