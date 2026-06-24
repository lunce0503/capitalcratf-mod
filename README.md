# CapitalCraft Mod

Fabric 1.21.11 client mod for CapitalCraft.

## Features

- Connects to the Paper server through the `capitalcraft:main` custom payload channel.
- Sends a required client handshake with the `finance_ui` feature.
- Opens the finance screen with the `V` key.
- Requests the player's balance from the server.
- Sends transfer requests to the server-side VillageFinance plugin.

The server remains authoritative. This mod only provides the client UI and packet transport.

## Build

```bash
./gradlew build
```

The release jar is generated under `build/libs/`.
