package kr.kwon.capitalcraft.client.resident.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

public final class MariResidentModel extends EntityModel<ResidentVillagerRenderState> {
    private final ModelPart head;
    private final ModelPart veil;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart halo;

    public MariResidentModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        head = root.getChild("head");
        veil = root.getChild("veil");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        halo = root.getChild("halo");
    }

    @Override
    public void setupAnim(ResidentVillagerRenderState state) {
        super.setupAnim(state);
        head.xRot = Mth.clamp(state.xRot, -18, 18) * Mth.DEG_TO_RAD;
        head.yRot = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD;
        veil.yRot = head.yRot * 0.32F;
        float walk = state.walkAnimationPos * 0.6662F;
        float speed = Mth.clamp(state.walkAnimationSpeed, 0, 1);
        rightLeg.xRot = Mth.cos(walk) * 0.48F * speed;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.48F * speed;
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.14F * speed;
        leftArm.xRot = Mth.cos(walk) * 0.14F * speed;
        halo.yRot = state.ageInTicks * 0.008F;
        halo.y += Mth.sin(state.ageInTicks * 0.04F) * 0.13F;
    }
}
