"""Bake the user-edited Mari geometry and packed atlas into generator inputs.

blender -b mari-seia-textured-faces.blend --python tools/import-mari-blend.py
"""
import hashlib
import json
from pathlib import Path

import bpy

base = Path(__file__).resolve().parent.parent
targets = (
    'fitted_bodice', 'flat_face', 'fox_ear_inner_highlight_-1',
    'fox_ear_inner_highlight_1', 'left_fox_ear_inner', 'neck', 'right_fox_ear_inner',
)
depsgraph = bpy.context.evaluated_depsgraph_get()
root = bpy.data.objects['mari_display_root']
overrides = {}
for name in targets:
    matches = [obj for obj in bpy.data.objects
               if obj.name.startswith('mari_') and obj.get('runtime_object') == name]
    if len(matches) != 1:
        raise ValueError(f'Expected exactly one Mari object for {name}, found {len(matches)}')
    obj = matches[0]
    evaluated = obj.evaluated_get(depsgraph)
    mesh = evaluated.to_mesh(preserve_all_data_layers=True, depsgraph=depsgraph)
    relative = root.matrix_world.inverted() @ evaluated.matrix_world
    vertices = []
    for vertex in mesh.vertices:
        x, y, z = relative @ vertex.co
        vertices.append([round(x / .75, 5), round(z / .75, 5), round(-y / .75, 5)])
    faces = []
    for polygon in mesh.polygons:
        if len(polygon.vertices) not in (3, 4):
            raise ValueError(f'Unsupported polygon in {name}')
        material = obj.material_slots[polygon.material_index].material.name
        face = {'indices': list(polygon.vertices), 'material': material.removeprefix('mari_')}
        if material == 'mari_painted_face':
            face['material'] = 'skin'
            if not mesh.uv_layers.active:
                raise ValueError('Painted face requires UVs')
            face['uv'] = [[round(uv[0], 8), round(1 - uv[1], 8)]
                          for uv in (mesh.uv_layers.active.data[index].uv
                                     for index in polygon.loop_indices)]
        faces.append(face)
    overrides[name] = {'vertices': vertices, 'faces': faces}
    evaluated.to_mesh_clear()

attachment = Path(bpy.data.filepath)
data = {
    'source': attachment.name,
    'sourceSha256': hashlib.sha256(attachment.read_bytes()).hexdigest(),
    'coordinateSystem': 'runtime blocks: Y up, +Z front',
    'objects': overrides,
}
(base / 'tools/mari-blender-geometry-overrides.json').write_text(json.dumps(data, indent=2) + '\n')
image = bpy.data.images['mari-atlas.png']
if not image.packed_file or tuple(image.size) != (256, 256):
    raise ValueError('Expected a packed 256 x 256 Mari atlas')
(base / 'tools/mari-blender-atlas.png').write_bytes(bytes(image.packed_file.data))

# Blender stores image rows bottom-up; the 128px face tile starts at (96,16)
# in the top-down Minecraft atlas. Export the same pixels as an editing preview.
pixels = list(image.pixels)
tile = bpy.data.images.new('MariImportedFaceTile', width=128, height=128, alpha=True)
tile_pixels = []
for y in range(128):
    offset = ((112 + y) * 256 + 96) * 4
    tile_pixels.extend(pixels[offset:offset + 128 * 4])
tile.pixels = tile_pixels
tile.file_format = 'PNG'
tile.filepath_raw = str(base / 'tools/mari-blender-face.png')
tile.save()
print(f'PASS: exported {len(overrides)} evaluated Mari objects, face UVs and packed atlas')
