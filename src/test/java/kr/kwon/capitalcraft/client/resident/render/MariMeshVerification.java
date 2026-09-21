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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import net.minecraft.client.model.geom.ModelPart;

/** Verifies resource lookup, native submitted geometry, UV colours and animation reset. */
public final class MariMeshVerification {

    public static void main(String[] args) throws Exception {
        String meshPath = "/assets/" + MariMeshLoader.MESH.getNamespace() + "/" + MariMeshLoader.MESH.getPath();
        JsonObject data;
        ModelPart root;
        try (var input = new InputStreamReader(Objects.requireNonNull(
            MariMeshVerification.class.getResourceAsStream(meshPath)), StandardCharsets.UTF_8)) {
            data = JsonParser.parseReader(input).getAsJsonObject();
        }
        try (var input = new java.io.StringReader(data.toString())) { root = MariMeshLoader.bake(input); }
        require(data.get("texture").getAsString().equals(MariMeshLoader.TEXTURE.toString()), "Texture ID mismatch");
        String texturePath = "/assets/" + MariMeshLoader.TEXTURE.getNamespace() + "/" + MariMeshLoader.TEXTURE.getPath();
        BufferedImage texture = ImageIO.read(Objects.requireNonNull(MariMeshVerification.class.getResourceAsStream(texturePath)));
        require(texture.getWidth() == 64 && texture.getHeight() == 64, "Palette dimensions");
        List<String> palette = new ArrayList<>(data.getAsJsonObject("palette").keySet());
        Map<Integer, List<double[]>> expected = new HashMap<>();
        int faces = 0;
        for (var value : data.getAsJsonArray("objects")) {
            var object = value.getAsJsonObject();
            for (var faceValue : object.getAsJsonArray("faces")) {
                faces++;
                var face = faceValue.getAsJsonObject();
                var indices = face.getAsJsonArray("indices");
                int material = palette.indexOf(face.get("material").getAsString());
                for (int i = 0; i < 4; i++) {
                    var p = object.getAsJsonArray("vertices").get(indices.get(Math.min(i, indices.size() - 1)).getAsInt()).getAsJsonArray();
                    double scale = data.get("modelScale").getAsDouble() / 16;
                    expected.computeIfAbsent(material, key -> new ArrayList<>()).add(new double[]{
                        p.get(0).getAsDouble()*scale,1.5-p.get(1).getAsDouble()*scale,-p.get(2).getAsDouble()*scale});
                }
            }
        }
        int[] submitted = {0};
        boolean[] compareMesh = {true};
        VertexConsumer consumer = (VertexConsumer) Proxy.newProxyInstance(VertexConsumer.class.getClassLoader(),
            new Class<?>[]{VertexConsumer.class}, (proxy, method, values) -> {
                if (method.getName().equals("addVertex") && values.length == 11) {
                    float x=(float) values[0], y=(float) values[1], z=(float) values[2];
                    float u=(float) values[4], v=(float) values[5];
                    require(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z), "Finite position");
                    require(u > 0 && u < 1 && v > 0 && v < 1, "UV in texture");
                    int material = (int)(v*8)*8 + (int)(u*8);
                    require(material < palette.size(), "UV references populated tile");
                    int rgb = texture.getRGB((int)(u*64),(int)(v*64)) & 0xffffff;
                    require(rgb == Integer.parseInt(data.getAsJsonObject("palette").get(palette.get(material)).getAsString().substring(1),16), "Palette matches material");
                    float nx=(float)values[8],ny=(float)values[9],nz=(float)values[10];
                    require(Math.abs(nx*nx+ny*ny+nz*nz-1)<.0001, "Unit normal");
                    if (compareMesh[0]) {
                        List<double[]> candidates = expected.get(material);
                        int match = -1;
                        for(int i=0;i<candidates.size();i++) {
                            double[] p=candidates.get(i);
                            if(Math.abs(p[0]-x)<1e-5 && Math.abs(p[1]-y)<1e-5 && Math.abs(p[2]-z)<1e-5) {
                                match=i; break;
                            }
                        }
                        require(match>=0,"Native vertex/material absent from authoring mesh: "+x+","+y+","+z);
                        candidates.remove(match);
                    }
                    submitted[0]++;
                    return null;
                }
                return proxy;
            });
        root.render(new PoseStack(),consumer,0xf000f0,0);
        require(submitted[0] == faces*4, "All faces submitted");
        require(expected.values().stream().allMatch(List::isEmpty), "All authoring vertices were matched");
        compareMesh[0]=false;
        MariResidentModel model = new MariResidentModel(root);
        ResidentVillagerRenderState state = new ResidentVillagerRenderState();
        state.ageInTicks=37; state.walkAnimationPos=3; state.walkAnimationSpeed=.7F; state.xRot=90;state.yRot=120;
        model.setupAnim(state);
        float haloY=root.getChild("halo").y;
        for(int i=0;i<200;i++) model.setupAnim(state);
        require(Math.abs(root.getChild("halo").y-haloY)<1e-6, "Animation must not accumulate offsets");
        require(Math.abs(root.getChild("head").yRot)<.7F, "Head rotation limited");
        root.render(new PoseStack(),consumer,0xf000f0,0);
        try (var bad = new java.io.StringReader("{\"format\":99}")) {
            try { MariMeshLoader.bake(bad); throw new AssertionError("Invalid format accepted"); }
            catch(IllegalArgumentException expectedException) { /* intended rejection */ }
        }
        System.out.println("PASS: " + faces + " faces / " + faces*4 + " native vertices; mesh, texture, UVs, normals, walking and animation reset");
    }

    private static void require(boolean result,String reason) {
        if(!result) throw new AssertionError(reason);
    }
}
