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
- Receives server-authoritative resident appearance snapshots and renders the Mari resident model.

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

The server remains authoritative. This mod provides UI, input transport and rendering.

## Build

```bash
env JAVA_HOME=/path/to/jdk-21 ./gradlew clean build
```

The release jar is generated under `build/libs/`.

`check` runs `verifyMariMesh`, which exercises Minecraft's native ModelPart vertex
submission and checks geometry, palette UVs, normals and animation reset headlessly.
Regenerate the mesh with `node tools/generate-mari-resident-model.mjs` and its actual
four-view preview with `node tools/render-mari-preview.mjs`.
