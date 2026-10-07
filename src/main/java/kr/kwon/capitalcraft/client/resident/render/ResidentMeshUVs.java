package kr.kwon.capitalcraft.client.resident.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/** Supports painted face UVs alongside the original 8px solid palette tiles. */
final class ResidentMeshUVs {
    final int width;
    final int height;

    ResidentMeshUVs(JsonObject mesh) {
        JsonArray size = mesh.has("textureSize") ? mesh.getAsJsonArray("textureSize") : null;
        if (size != null && size.size() != 2) throw new IllegalArgumentException("Expected texture width/height");
        width = size == null ? 64 : size.get(0).getAsInt();
        height = size == null ? 64 : size.get(1).getAsInt();
        if (width < 64 || height < 64 || width > 4096 || height > 4096) {
            throw new IllegalArgumentException("Invalid resident texture size");
        }
    }

    float[] at(JsonObject face, int corner, int material) {
        if (face.has("uv")) {
            JsonArray coordinates = face.getAsJsonArray("uv");
            if (coordinates.size() != face.getAsJsonArray("indices").size()) {
                throw new IllegalArgumentException("UV count must match face corners");
            }
            JsonArray point = coordinates.get(corner).getAsJsonArray();
            if (point.size() != 2) throw new IllegalArgumentException("Expected UV pair");
            float u = point.get(0).getAsFloat(), v = point.get(1).getAsFloat();
            if (!Float.isFinite(u) || !Float.isFinite(v) || u < 0 || u > 1 || v < 0 || v > 1) {
                throw new IllegalArgumentException("Invalid face UV");
            }
            return new float[]{u, v};
        }
        // Non-zero area inside the existing palette tile keeps shader tangents valid.
        return new float[]{((material % 8) * 8 + (corner == 0 || corner == 3 ? 3 : 5)) / (float) width,
            ((material / 8) * 8 + (corner < 2 ? 3 : 5)) / (float) height};
    }
}
