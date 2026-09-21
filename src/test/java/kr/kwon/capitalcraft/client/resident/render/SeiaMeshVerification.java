package kr.kwon.capitalcraft.client.resident.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import net.minecraft.client.model.geom.ModelPart;

/** Checks Seia's resources, signature geometry, animation reset and native submission. */
public final class SeiaMeshVerification {
    public static void main(String[] args) throws Exception {
        String meshPath = "/assets/" + SeiaMeshLoader.MESH.getNamespace() + "/" + SeiaMeshLoader.MESH.getPath();
        JsonObject data;
        ModelPart root;
        try (var input = new InputStreamReader(Objects.requireNonNull(
            SeiaMeshVerification.class.getResourceAsStream(meshPath)), StandardCharsets.UTF_8)) {
            data = JsonParser.parseReader(input).getAsJsonObject();
        }
        try (var input = new java.io.StringReader(data.toString())) { root = SeiaMeshLoader.bake(input); }
        require(data.get("texture").getAsString().equals(SeiaMeshLoader.TEXTURE.toString()), "Texture ID mismatch");
        String texturePath = "/assets/" + SeiaMeshLoader.TEXTURE.getNamespace() + "/" + SeiaMeshLoader.TEXTURE.getPath();
        BufferedImage texture = ImageIO.read(Objects.requireNonNull(SeiaMeshVerification.class.getResourceAsStream(texturePath)));
        require(texture.getWidth() == 64 && texture.getHeight() == 64, "Palette dimensions");

        int faces = 0;
        boolean leftEar = false, rightEar = false, tail = false, bird = false, halo = false;
        for (var value : data.getAsJsonArray("objects")) {
            var object = value.getAsJsonObject();
            String name = object.get("name").getAsString();
            leftEar |= name.equals("left_fox_ear");
            rightEar |= name.equals("right_fox_ear");
            tail |= name.equals("large_fox_tail");
            bird |= name.equals("shoulder_bird");
            halo |= name.equals("halo_crown_0");
            faces += object.getAsJsonArray("faces").size();
        }
        require(leftEar && rightEar && tail && bird && halo, "Seia signature parts");
        root.visit(new PoseStack(), (pose, path, index, cube) -> {
            var vertices = cube.polygons[0].vertices();
            float area = (vertices[1].u()-vertices[0].u())*(vertices[2].v()-vertices[0].v())
                - (vertices[2].u()-vertices[0].u())*(vertices[1].v()-vertices[0].v());
            require(Math.abs(area)>1e-7, "Shader tangent UV area must be non-zero");
        });

        boolean compatibility = Boolean.getBoolean("capitalcraft.verifyMariCompatibility");
        List<Class<?>> interfaces = new ArrayList<>(List.of(VertexConsumer.class));
        if (compatibility) interfaces.add(Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter"));
        int[] submitted = {0}, fastPathCalls = {0};
        VertexConsumer consumer = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
            interfaces.toArray(Class<?>[]::new), (proxy, method, values) -> {
                if (method.getName().equals("canUseIntrinsics")) return true;
                if (method.getName().equals("push")) { fastPathCalls[0]++; return null; }
                if (method.getName().equals("addVertex") && values.length == 11) {
                    require((int)values[3] == -1 && (int)values[6] == 0 && (int)values[7] == 0xf000f0,
                        "Preserve colour, overlay and light");
                    for (int i : new int[]{0,1,2,4,5,8,9,10}) require(Float.isFinite((float)values[i]), "Finite vertex");
                    submitted[0]++;
                    return null;
                }
                return proxy;
            });
        root.render(new PoseStack(), consumer, 0xf000f0, 0);
        require(submitted[0] == faces*4, "All Seia faces submitted");
        require(fastPathCalls[0] == 0, "Seia must bypass the cached cuboid fast path");

        SeiaResidentModel model = new SeiaResidentModel(root);
        ResidentVillagerRenderState state = new ResidentVillagerRenderState();
        state.ageInTicks=37; state.walkAnimationPos=3; state.walkAnimationSpeed=.7F; state.xRot=90; state.yRot=120;
        model.setupAnim(state);
        float haloY=root.getChild("halo").y, tailYRot=root.getChild("tail").yRot;
        for(int i=0;i<200;i++) model.setupAnim(state);
        require(Math.abs(root.getChild("halo").y-haloY)<1e-6, "Animation must not accumulate offsets");
        require(Math.abs(root.getChild("tail").yRot-tailYRot)<1e-6, "Tail animation must reset");
        require(Math.abs(root.getChild("head").yRot)<.7F, "Head rotation limited");
        root.render(new PoseStack(), consumer, 0xf000f0, 0);
        require(fastPathCalls[0] == 0, "Animated Seia must bypass the cuboid cache");
        try (var bad = new java.io.StringReader("{\"format\":99}")) {
            try { SeiaMeshLoader.bake(bad); throw new AssertionError("Invalid format accepted"); }
            catch(IllegalArgumentException expectedException) { /* intended */ }
        }
        System.out.println("PASS: Seia " + faces + " faces / " + faces*4 + " native vertices; fox ears, tail, bird, halo, UVs and animation reset");
    }

    private static void require(boolean result, String reason) {
        if (!result) throw new AssertionError(reason);
    }
}
