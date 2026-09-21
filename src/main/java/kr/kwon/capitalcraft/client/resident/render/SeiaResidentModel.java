package kr.kwon.capitalcraft.client.resident.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

public final class SeiaResidentModel extends EntityModel<ResidentVillagerRenderState> {
    private final ModelPart head;
    private final ModelPart hair;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart tail;
    private final ModelPart halo;

    public SeiaResidentModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        head = root.getChild("head");
        hair = root.getChild("hair");
        leftArm = root.getChild("left_arm");
        rightArm = root.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        tail = root.getChild("tail");
        halo = root.getChild("halo");
    }

    @Override
    public void setupAnim(ResidentVillagerRenderState state) {
        super.setupAnim(state);
        float headX = Mth.clamp(state.xRot, -18, 18) * Mth.DEG_TO_RAD;
        float headY = Mth.clamp(state.yRot, -40, 40) * Mth.DEG_TO_RAD;
        head.xRot = headX;
        head.yRot = headY;
        hair.xRot = headX * 0.72F;
        hair.yRot = headY * 0.72F;
        float walk = state.walkAnimationPos * 0.6662F;
        float speed = Mth.clamp(state.walkAnimationSpeed, 0, 1);
        rightLeg.xRot = Mth.cos(walk) * 0.46F * speed;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.46F * speed;
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.11F * speed;
        leftArm.xRot = Mth.cos(walk) * 0.11F * speed;
        tail.yRot = Mth.sin(state.ageInTicks * 0.055F) * 0.10F;
        tail.xRot = Mth.sin(state.ageInTicks * 0.038F) * 0.025F;
        halo.yRot = state.ageInTicks * 0.006F;
        halo.y += Mth.sin(state.ageInTicks * 0.035F) * 0.11F;
    }
}
