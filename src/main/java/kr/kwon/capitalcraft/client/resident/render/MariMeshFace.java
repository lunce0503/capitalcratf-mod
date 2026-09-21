package kr.kwon.capitalcraft.client.resident.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.EnumSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/** An arbitrary mesh face, not a cuboid that Sodium can reconstruct from its bounds. */
final class MariMeshFace extends ModelPart.Cube {
    MariMeshFace(Vector3f min, Vector3f max, ModelPart.Vertex[] vertices, Vector3f normal) {
        super(0, 0, min.x, min.y, min.z, max.x - min.x, max.y - min.y, max.z - min.z,
            0, 0, 0, false, 64, 64, EnumSet.of(Direction.NORTH));
        polygons[0] = new ModelPart.Polygon(vertices, new Vector3f(normal));
    }

    @Override
    public void compile(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay, int color) {
        // Never call super.compile: Sodium replaces that method with a cached cuboid.
        // Keep ModelPart transforms/animation and the active (possibly Iris) consumer.
        ModelPart.Polygon face = polygons[0];
        Vector3f normal = pose.transformNormal(face.normal(), new Vector3f());
        Vector3f position = new Vector3f();
        for (ModelPart.Vertex vertex : face.vertices()) {
            pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), position);
            consumer.addVertex(position.x, position.y, position.z, color, vertex.u(), vertex.v(),
                overlay, light, normal.x, normal.y, normal.z);
        }
    }
}
