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

Version 0.6.0 adds the `resident_appearance_v1` handshake feature, keeps ordinary
villagers on the vanilla renderer, and renders residents assigned the `mari`
appearance with the bundled fox-ear veil model, palette texture, and animation.

The server remains authoritative. This mod only provides the client UI and packet transport.

## Build

```bash
env JAVA_HOME=/path/to/jdk-21 ./gradlew clean build
```

The release jar is generated under `build/libs/`.
