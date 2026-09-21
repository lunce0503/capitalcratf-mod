package kr.kwon.capitalcraft.client.resident.render;

import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class MariResidentModel extends EntityModel<ResidentVillagerRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath(CapitalCraftClientMod.RESOURCE_NAMESPACE, "mari_resident"),
        "main"
    );

    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart halo;

    public MariResidentModel(ModelPart root) {
        super(root, RenderTypes::entityCutoutNoCull);
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.halo = root.getChild("halo");
    }

    private static CubeListBuilder cube(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder head = cube(0, 64)
            .addBox(-4, -6, -4, 8, 8, 8)
            .texOffs(192, 64).addBox(-4.4F, -6.5F, 2.8F, 8.8F, 8.5F, 1.4F)
            .texOffs(192, 64).addBox(-4.5F, -6.4F, -3.8F, 1.5F, 9.8F, 7.4F)
            .texOffs(192, 64).addBox(3.0F, -6.4F, -3.8F, 1.5F, 9.8F, 7.4F)
            .texOffs(192, 64).addBox(-3.8F, -6.6F, -4.4F, 2.8F, 3.8F, 0.8F)
            .texOffs(192, 64).addBox(-1.4F, -6.8F, -4.45F, 2.8F, 4.2F, 0.8F)
            .texOffs(192, 64).addBox(1.0F, -6.6F, -4.4F, 2.8F, 3.8F, 0.8F)
            .texOffs(0, 128).addBox(-2.9F, -1.8F, -4.35F, 2.2F, 1.2F, 0.45F)
            .texOffs(0, 128).addBox(0.7F, -1.8F, -4.35F, 2.2F, 1.2F, 0.45F)
            .texOffs(64, 128).addBox(-2.2F, -1.7F, -4.62F, 0.7F, 1.0F, 0.25F)
            .texOffs(64, 128).addBox(1.5F, -1.7F, -4.62F, 0.7F, 1.0F, 0.25F)
            .texOffs(128, 64).addBox(-0.5F, 0.5F, -4.35F, 1.0F, 0.25F, 0.3F)
            .texOffs(192, 128).addBox(-4.5F, -5.0F, -4.65F, 9.0F, 1.7F, 0.8F)
            .texOffs(64, 64).addBox(-3.7F, 0.0F, -4.32F, 1.0F, 0.3F, 0.25F)
            .texOffs(64, 64).addBox(2.7F, 0.0F, -4.32F, 1.0F, 0.3F, 0.25F)
            // Long stepped fox ears integrated into the veil.
            .texOffs(64, 0).addBox(-5.0F, -9.0F, -2.5F, 3.2F, 4.0F, 5.0F)
            .texOffs(64, 0).addBox(-5.7F, -12.0F, -2.0F, 2.2F, 3.5F, 4.0F)
            .texOffs(64, 0).addBox(-6.0F, -14.0F, -1.4F, 1.2F, 2.5F, 2.8F)
            .texOffs(64, 0).addBox(1.8F, -9.0F, -2.5F, 3.2F, 4.0F, 5.0F)
            .texOffs(64, 0).addBox(3.5F, -12.0F, -2.0F, 2.2F, 3.5F, 4.0F)
            .texOffs(64, 0).addBox(4.8F, -14.0F, -1.4F, 1.2F, 2.5F, 2.8F)
            .texOffs(64, 192).addBox(-5.65F, -11.4F, -2.15F, 1.5F, 2.5F, 0.5F)
            .texOffs(64, 192).addBox(4.15F, -11.4F, -2.15F, 1.5F, 2.5F, 0.5F)
            // Flower ornament and leaf.
            .texOffs(128, 192).addBox(-5.0F, -5.1F, -4.9F, 1.1F, 1.1F, 0.5F)
            .texOffs(192, 0).addBox(-5.7F, -5.3F, -4.85F, 0.8F, 1.5F, 0.45F)
            .texOffs(192, 0).addBox(-4.1F, -5.3F, -4.85F, 0.8F, 1.5F, 0.45F)
            .texOffs(192, 0).addBox(-4.9F, -6.0F, -4.85F, 1.2F, 0.8F, 0.45F)
            .texOffs(128, 128).addBox(-6.1F, -4.0F, -4.8F, 1.8F, 0.7F, 0.4F);
        root.addOrReplaceChild("head", head, PartPose.offset(0, 4, 0));

        CubeListBuilder dress = cube(0, 0)
            .addBox(-5, 6, -3, 10, 8, 6)
            .texOffs(0, 0).addBox(-7, 13, -4, 14, 9, 8)
            .texOffs(192, 0).addBox(-7.2F, 21, -4.2F, 14.4F, 3, 8.4F)
            .texOffs(192, 0).addBox(-4.1F, 12.8F, -4.35F, 8.2F, 9.2F, 0.7F)
            .texOffs(192, 0).addBox(-3.4F, 6.0F, -3.5F, 6.8F, 3.2F, 0.6F)
            .texOffs(128, 128).addBox(-2.8F, 8.5F, -3.8F, 2.8F, 2.7F, 0.7F)
            .texOffs(128, 128).addBox(0, 8.5F, -3.8F, 2.8F, 2.7F, 0.7F)
            .texOffs(192, 128).addBox(-0.8F, 8.3F, -4.0F, 1.6F, 1.6F, 0.8F)
            .texOffs(192, 128).addBox(-6.8F, 6.8F, -3.3F, 2.0F, 3.4F, 6.6F)
            .texOffs(192, 128).addBox(4.8F, 6.8F, -3.3F, 2.0F, 3.4F, 6.6F);
        root.addOrReplaceChild("dress", dress, PartPose.ZERO);

        CubeListBuilder leftArm = cube(0, 0)
            .addBox(-2.0F, 0, -2.5F, 4, 9, 5)
            .texOffs(192, 128).addBox(-2.1F, 7.2F, -2.6F, 4.2F, 2, 5.2F)
            .texOffs(192, 0).addBox(-2.0F, 8.8F, -3.0F, 4, 3, 4);
        root.addOrReplaceChild("left_arm", leftArm, PartPose.offsetAndRotation(6, 7, 0, -0.55F, 0, -0.16F));

        CubeListBuilder rightArm = cube(0, 0)
            .addBox(-2.0F, 0, -2.5F, 4, 9, 5)
            .texOffs(192, 128).addBox(-2.1F, 7.2F, -2.6F, 4.2F, 2, 5.2F)
            .texOffs(192, 0).addBox(-2.0F, 8.8F, -3.0F, 4, 3, 4);
        root.addOrReplaceChild("right_arm", rightArm, PartPose.offsetAndRotation(-6, 7, 0, -0.55F, 0, 0.16F));

        CubeListBuilder leg = cube(192, 0)
            .addBox(-1.7F, 0, -1.8F, 3.4F, 3.2F, 3.6F)
            .texOffs(128, 0).addBox(-2.0F, 2.5F, -2.6F, 4, 2.5F, 5.2F);
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(2.2F, 19, 0));
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-2.2F, 19, 0));

        CubeListBuilder halo = cube(0, 192)
            .addBox(-5, -0.5F, -6, 10, 1, 1)
            .addBox(-5, -0.5F, 5, 10, 1, 1)
            .addBox(-6, -0.5F, -5, 1, 1, 10)
            .addBox(5, -0.5F, -5, 1, 1, 10)
            .addBox(-1.5F, -0.4F, -1.5F, 3, 0.8F, 3)
            .addBox(-7, -0.5F, -1, 2, 1, 2)
            .addBox(5, -0.5F, -1, 2, 1, 2)
            .addBox(-1, -0.5F, -7, 2, 1, 2)
            .addBox(-1, -0.5F, 5, 2, 1, 2);
        root.addOrReplaceChild("halo", halo, PartPose.offsetAndRotation(0, -11, 0, 0, Mth.PI / 4, 0));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(ResidentVillagerRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float walk = state.walkAnimationPos;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        rightLeg.xRot = Mth.cos(walk * 0.6662F) * 0.65F * speed;
        leftLeg.xRot = Mth.cos(walk * 0.6662F + Mth.PI) * 0.65F * speed;
        rightArm.xRot = -0.55F + Mth.cos(walk * 0.6662F + Mth.PI) * 0.08F * speed;
        leftArm.xRot = -0.55F + Mth.cos(walk * 0.6662F) * 0.08F * speed;
        halo.yRot = state.ageInTicks * 0.015F;
    }
}
