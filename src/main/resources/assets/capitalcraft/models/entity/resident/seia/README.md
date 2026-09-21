# Seia resident model

`seia-mesh.json` is the single source used by the Fabric runtime and the
four-view software preview. The model deliberately uses long tapered fox ears,
a large curled fox tail, blonde hair, a layered white/blue/gold dress, the
shoulder bird and an ornate halo so it remains distinct from Mari at Minecraft
viewing distances.

- `seia-mesh.json`: runtime bone, polygon and palette data
- `seia-resident.obj` / `.mtl`: Blockbench or Blender interchange files
- `seia-resident-model.json`: counts and signature-part metadata
- `seia-preview.png`: four views rendered from the exact runtime mesh
- `textures/entity/resident/seia.png`: solid-colour palette texture

Regenerate every derived asset with:

```bash
node tools/generate-seia-resident-model.mjs
node tools/render-seia-preview.mjs
```
