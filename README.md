# CapitalCraft Mod

Fabric 1.21.11 client mod for CapitalCraft.

## Features

- Connects to the Paper server through the `capitalcraft:main` custom payload channel.
- Sends a required client handshake with the `finance_ui` and `vehicle_v1` features.
- Opens the finance screen with the `V` key.
- Requests the player's balance from the server.
- Sends transfer requests to the server-side CapitalCraft plugin.
- Opens the trade screen with the `G` key.
- Sends accepted trade requests for money payments and held-item sales.
- Sends server-authoritative compact-sedan driving input and renders a vehicle speed HUD.
- Receives server-authoritative resident appearance snapshots and renders the Mari and Seia resident models.

## Vehicle controls

1. An administrator creates a car with `/vehicle create compact_sedan [owner]`.
2. The owner right-clicks the car body to enter it.
3. Drive with `W/S`, steer with `A/D`, brake with `Space`, and exit with `Shift`.

The Paper plugin renders the MVP car body with vanilla display entities. This Fabric
mod is still required for input, state synchronization, and the driving HUD.

Version 0.7.0 replaces Mari's box-based model with a shared, faceted mesh matching
the reference silhouette: pointed fox ears, long lined veil, braided peach hair,
flared dress, floral ornament, embroidered gold trim and a star-shaped halo.
The in-game model, editable OBJ and software preview use the same geometry.
The `resident_appearance_v1` handshake keeps ordinary villagers on the vanilla
renderer and applies the custom appearance only to residents assigned `mari`.

Version 0.8.0 adds the `seia` appearance using the same runtime-safe faceted mesh pipeline, with tall
fox ears, blonde hair, a layered white/blue/gold dress, a large curled fox tail,
the shoulder bird and an ornate halo. Its runtime JSON, OBJ/MTL and four-view
preview are all generated from one source.

The server remains authoritative. This mod provides UI, input transport and rendering.

Version 0.8.1 rounds Mari's and Seia's cheeks and jawlines, fills the temple gaps
between fringe and side hair, and embeds Seia's fox ears in the crown with fur
at the roots. Seia's hair follows the same head rotation so the ear/hair seams
stay closed while looking around. Blush and mouth details follow the new face.
Run `node tools/render-resident-heads.mjs` for front, three-quarter and side
close-ups of both runtime meshes (`build/resident-heads.png`).

Version 0.7.1 fixes black colours and rectangular faces with Sodium. Mari submits
its own polygon vertices through the active vertex consumer, bypassing Sodium's
cached cuboid geometry. Other entities still use Sodium normally. Palette UVs span
a small area within each solid colour tile for shader tangent calculations.

## Build

```bash
env JAVA_HOME=/path/to/jdk-21 ./gradlew clean build
```

The release jar is generated under `build/libs/`.

`check` runs both resident mesh verifiers, which exercise Minecraft's native
ModelPart vertex submission and check geometry, palette UVs, signature parts and
animation reset headlessly.
Regenerate the mesh with `node tools/generate-mari-resident-model.mjs` and its actual
four-view preview with `node tools/render-mari-preview.mjs`.
Use `generate-seia-resident-model.mjs` and `render-seia-preview.mjs` for Seia.

Run the real Fabric/Mixin compatibility checks (Java 21):

```bash
./gradlew runMariCompatibility -PmariCompatibility=sodium
./gradlew runMariCompatibility -PmariCompatibility=iris
```

These use the launcher's Sodium 0.8.12 and Iris 1.10.7. A fast-path-capable vertex
consumer verifies that Sodium optimizes a vanilla control cube but does not replace
Mari's mesh faces. They exit before window creation; they do not claim a GPU
shader-pack screenshot test. The verification mod is excluded from release JARs.
