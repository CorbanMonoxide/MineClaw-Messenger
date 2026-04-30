# MineClaw Messenger

Send messages to Nav directly from Minecraft chat.

## Features

- Client-side Fabric mod for Minecraft 1.26.2
- Type `@nav <message>` in chat to send a message
- Local webhook listener bridges to OpenClaw Discord integration
- Zero server modification needed
- Messages appear in Discord as `[Minecraft] Your message`

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
- Linux: `~/.minecraft/mods\`
- macOS: `~/Library/Application Support/minecraft/mods/`

### 4. Run the Webhook Listener

On navibuntu (or any machine with OpenClaw + `openclaw` CLI available):

```bash
python3 server/nav_webhook.py
```

This listens on `localhost:8765` and routes messages to Discord via OpenClaw's CLI.

## Usage

1. Launch Minecraft with the mod installed
2. Type `@nav <your message>` in any chat
3. You'll see `✓ Message sent to Nav` if it succeeds
4. Corban receives the message in Discord from the Minecraft bot

## Architecture

- **Client (Minecraft)**: Fabric mod intercepts `@nav` messages, sends HTTP POST to webhook
- **Webhook (navibuntu)**: Python server listens, calls `openclaw message send` to route to Discord
- **OpenClaw**: Sends message through Discord bot integration

## Configuration

Edit `server/nav_webhook.py` to change:
- `LISTEN_PORT`: webhook listener port (default: 8765)
- `DISCORD_CHANNEL_ID`: target Discord user/channel (default: 195974388036665344 = Corban)

## Troubleshooting

- **Messages not sending?** 
  - Check webhook is running: `ps aux | grep nav_webhook`
  - Verify Minecraft mod is installed: check mods folder
  - Look for errors: `python3 server/nav_webhook.py` (run foreground to see logs)

- **OpenClaw CLI not found?** 
  - Ensure `openclaw` is in PATH
  - Try: `which openclaw`
  - If missing, reinstall: `npm install -g openclaw`

- **Connection refused in Minecraft?** 
  - Webhook must be running: start it first
  - Check port 8765 is listening: `netstat -tlnp | grep 8765` or `lsof -i :8765`

---

**Status**: Alpha. Tested on Minecraft 1.26.2 with Fabric + OpenClaw.
