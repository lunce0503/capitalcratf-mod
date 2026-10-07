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
- `seia-atlas.png`: 256×256 texture for the OBJ's painted face material
- `seia-face.png`: 128×128 painted eyes and brows only; no nose or mouth
- `textures/entity/resident/seia.png`: runtime atlas; palette tiles and painted face

The face is one `flat_face` cuboid with eight vertices and six flat quads. There
are no cheek, eye, nose or mouth meshes and no bevels. Only the front quad uses
explicit normalized UVs; all other surfaces retain the palette materials.
The nose and mouth are omitted from the texture too; the lower face is plain skin.
Keep the OBJ, MTL and atlas together when importing into Blender.
Reference proportions scale the complete head/hair/ears/halo assembly by
1.18 in width, 1.06 in height and 1.10 in depth around Y=1.61. Bone pivots
receive the same transform. Eyes use vertical rectangular pupils and thick,
pointed polygonal upper lids, with no round irises, sparkles or lower-lid lines.
The body, limbs, bird and tail are unchanged.

The latest packed Blender project and front/three-quarter/full-body renders are saved in
`client/models/resident-faces/block-eyes/` at the workspace root. Recreate them with
`blender -b --python tools/blender-resident-faces.py -- --output-dir build/blender-faces`.
Blender edits are not automatically exported back to the runtime JSON; also
update the generator when changing the game model.

Regenerate every derived asset with:

```bash
node tools/generate-seia-resident-model.mjs
node tools/render-seia-preview.mjs
```
