package kr.kwon.capitalcraft.client.resident.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.kwon.capitalcraft.client.CapitalCraftClientMod;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;

/** Loads the same faceted geometry used by the OBJ export and model preview. */
public final class MariMeshLoader {
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
        CapitalCraftClientMod.RESOURCE_NAMESPACE, "textures/entity/resident/mari.png"
    );
    public static final Identifier MESH = Identifier.fromNamespaceAndPath(
        CapitalCraftClientMod.RESOURCE_NAMESPACE, "models/entity/resident/mari/mari-mesh.json"
    );

    private MariMeshLoader() {
    }

    public static ModelPart load(ResourceManager resources) {
        // Resource-manager resolution also supports F3+T and resource pack overrides.
        try (Reader reader = resources.openAsReader(MESH)) {
            return bake(reader);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Could not load Mari resident mesh " + MESH, exception);
        }
    }

    static ModelPart bake(Reader reader) {
        JsonObject mesh = JsonParser.parseReader(reader).getAsJsonObject();
        if (mesh.get("format").getAsInt() != 2) {
            throw new IllegalArgumentException("Unsupported Mari mesh version");
        }
        if (!TEXTURE.toString().equals(mesh.get("texture").getAsString())) {
            throw new IllegalArgumentException("Mari mesh texture identifier mismatch");
        }
        float scale = mesh.get("modelScale").getAsFloat();
        List<String> materials = new ArrayList<>(mesh.getAsJsonObject("palette").keySet());
        Map<String, Vector3f> pivots = new LinkedHashMap<>();
        Map<String, List<ModelPart.Cube>> geometry = new LinkedHashMap<>();
        mesh.getAsJsonObject("bones").entrySet().forEach(entry -> {
            pivots.put(entry.getKey(), point(entry.getValue().getAsJsonArray()));
            geometry.put(entry.getKey(), new ArrayList<>());
        });
        for (JsonElement value : mesh.getAsJsonArray("objects")) {
            JsonObject object = value.getAsJsonObject();
            String bone = object.get("bone").getAsString();
            Vector3f pivot = pivots.get(bone);
            if (pivot == null) throw new IllegalArgumentException("Unknown Mari bone: " + bone);
            JsonArray vertices = object.getAsJsonArray("vertices");
            for (JsonElement faceValue : object.getAsJsonArray("faces")) {
                JsonObject face = faceValue.getAsJsonObject();
                JsonArray indices = face.getAsJsonArray("indices");
                int count = indices.size();
                if (count != 3 && count != 4) throw new IllegalArgumentException("Expected triangle/quad");
                int material = materials.indexOf(face.get("material").getAsString());
                if (material < 0 || material >= 64) throw new IllegalArgumentException("Invalid material");
                float u = ((material % 8) * 8 + 4) / 64.0F;
                float v = ((material / 8) * 8 + 4) / 64.0F;
                ModelPart.Vertex[] polygon = new ModelPart.Vertex[4];
                Vector3f[] positions = new Vector3f[4];
                for (int i = 0; i < 4; i++) {
                    int index = indices.get(Math.min(i, count - 1)).getAsInt();
                    Vector3f p = point(vertices.get(index).getAsJsonArray()).sub(pivot);
                    // Authoring is Y-up/+Z-front; ModelParts are Y-down/-Z-front.
                    p.mul(scale, -scale, -scale);
                    positions[i] = p;
                    // A small UV square within the solid palette cell also gives shader
                    // tangent calculations a non-zero UV area. Triangles repeat vertex 3.
                    int corner = Math.min(i, count - 1);
                    float du = (corner == 0 || corner == 3 ? -1 : 1) / 64.0F;
                    float dv = (corner < 2 ? -1 : 1) / 64.0F;
                    polygon[i] = new ModelPart.Vertex(p.x, p.y, p.z, u + du, v + dv);
                }
                Vector3f normal = new Vector3f(positions[1]).sub(positions[0])
                    .cross(new Vector3f(positions[2]).sub(positions[0]));
                if (normal.lengthSquared() < 1.0E-10F) throw new IllegalArgumentException("Degenerate face");
                normal.normalize();
                Vector3f min = new Vector3f(positions[0]), max = new Vector3f(positions[0]);
                for (Vector3f p : positions) { min.min(p); max.max(p); }
                geometry.get(bone).add(new MariMeshFace(min, max, polygon, normal));
            }
        }
        Map<String, ModelPart> children = new LinkedHashMap<>();
        pivots.forEach((name, p) -> {
            ModelPart part = new ModelPart(geometry.get(name), Map.of());
            PartPose pose = PartPose.offset(p.x * scale, 24 - p.y * scale, -p.z * scale);
            part.setInitialPose(pose);
            part.loadPose(pose);
            children.put(name, part);
        });
        return new ModelPart(List.of(), children);
    }

    private static Vector3f point(JsonArray value) {
        Vector3f point = new Vector3f(value.get(0).getAsFloat(), value.get(1).getAsFloat(), value.get(2).getAsFloat());
        if (!point.isFinite()) throw new IllegalArgumentException("Non-finite Mari vertex");
        return point;
    }
}
