# MineClaw Messenger

Send messages to Nav directly from Minecraft chat.

## Features

- Client-side Fabric mod for Minecraft 1.26.2
- Type `@nav <message>` in chat to send a message
- Local webhook listener bridges to OpenClaw
- Zero server modification needed

## Installation

### 1. Install Fabric Loader

Download Fabric installer from [fabricmc.net](https://fabricmc.net/) for 1.26.2.

### 2. Build the Mod

```bash
./gradlew build
```

The compiled JAR will be in `build/libs/mineclaw-messenger-1.0.0.jar`

### 3. Install to Mods Folder

Copy the JAR to your Minecraft mods folder:
- Windows: `%APPDATA%\.minecraft\mods\`
- Linux: `~/.minecraft/mods/`
- macOS: `~/Library/Application Support/minecraft/mods/`

### 4. Run the Webhook Listener

On navibuntu (or wherever OpenClaw is running):

```bash
python3 server/nav_webhook.py
```

This listens on `localhost:8765` and forwards messages to your OpenClaw instance.

## Usage

1. Launch Minecraft with the mod installed
2. Type `@nav <your message>` in any chat
3. You'll see `✓ Message sent to Nav` if it succeeds
4. Nav will receive the message on Discord (or wherever OpenClaw routes it)

## Architecture

- **Client (Minecraft)**: Fabric mod intercepts chat, sends HTTP POST to webhook
- **Webhook (navibuntu)**: Python server listens, forwards to OpenClaw message API
- **OpenClaw**: Receives message, routes to Discord/active channel

## Configuration

Edit `server/nav_webhook.py` to change:
- `LISTEN_PORT`: webhook listener port (default: 8765)
- `OPENCLAW_MESSAGE_API`: OpenClaw message endpoint

## Troubleshooting

- **Messages not sending?** Check that `nav_webhook.py` is running: `ps aux | grep nav_webhook`
- **Connection refused?** Ensure Minecraft is targeting localhost:8765
- **OpenClaw not receiving?** Verify OpenClaw API endpoint in `nav_webhook.py`

---

**Status**: Alpha. Functional but untested across network boundaries.
