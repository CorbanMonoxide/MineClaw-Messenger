# MineClaw Messenger - OpenClaw Integration Architecture

## Overview
MineClaw Messenger is a Minecraft 1.26.2 Fabric mod that integrates with OpenClaw for in-game chat with Nav.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Windows Client (Corban)                     │
│                    Running Minecraft 1.26.2                      │
│                                                                   │
│  /nav "message"  ──HTTP──>  100.74.68.45:9876/nav-message      │
└────────────────────────────────────────────────────────────────┘
                              │
                              │
┌─────────────────────────────────────────────────────────────────┐
│                      navibuntu (WSL)                            │
│                   Tailscale IP: 100.74.68.45                    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Webhook Daemon (Python)                                │   │
│  │  - Port: 9876                                            │   │
│  │  - Endpoints:                                            │   │
│  │    • /nav-message (POST) - Receive from mod             │   │
│  │    • /nav-responses (POST) - Poll for responses         │   │
│  │    • /nav-unprocessed (POST) - For bridge daemon        │   │
│  │    • /nav-mark-processed (POST) - Mark as processed     │   │
│  │  - Storage: /tmp/mineclaw-messages.jsonl                │   │
│  │           /tmp/mineclaw-responses.jsonl                 │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                    │
└──────────────────────────────┼────────────────────────────────────┘
                              │
                              │ HTTP (polling)
                              │
┌──────────────────────────────┼────────────────────────────────────┐
│                     navipi (Gateway)                              │
│                  Tailscale IP: 100.108.96.55                      │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │  Bridge Daemon (Python)                                  │    │
│  │  - Polls webhook for unprocessed messages                │    │
│  │  - Sends to Discord via OpenClaw CLI                     │    │
│  │  - Marks messages as processed                           │    │
│  │  - Async processing with threading                       │    │
│  └──────────────────────────────────────────────────────────┘    │
│                              │                                     │
└──────────────────────────────┼─────────────────────────────────────┘
                              │
                              │ openclaw message send --channel discord
                              │
                    ┌─────────────────────┐
                    │   Discord (Nav)     │
                    │   User ID: 195...   │
                    │   Responds in DMs   │
                    └─────────────────────┘
```

## Components

### 1. Minecraft Mod (MineClaw Messenger)
**File**: `~/MineClaw-Messenger-template/`  
**JAR Release**: `~/MineClaw-Messenger/releases/mineclaw-0.2.0.jar`

**Commands**:
- `/nav <message>` - Send message to Nav via OpenClaw
- `/nav-status` - Show webhook and player status
- `/nav-history` - Show message history (last 50)
- `/nav-settings <setting> <value>` - Configure mod (e.g., player name)

**Features**:
- Async HTTP POST to webhook
- Response polling every 2 seconds
- Message history tracking (max 50)
- Detailed logging to mod console

### 2. Webhook Daemon (Python)
**File**: `/tmp/nav_webhook.py`  
**Host**: navibuntu (100.74.68.45)  
**Port**: 9876

**Endpoints**:
```
POST /nav-message          - Accept message from mod
  Payload: {"message": "...", "sender": "PlayerName"}
  Response: {"status": "ok", "id": 1234567890}

POST /nav-responses        - Mod polls for responses
  Payload: {"sender": "PlayerName"}
  Response: {"status": "ok", "count": N, "responses": [...]}

POST /nav-unprocessed      - Bridge polls for new messages
  Response: {"status": "ok", "count": N, "messages": [...]}

POST /nav-mark-processed   - Bridge marks message as forwarded
  Payload: {"id": 1234567890}
  Response: {"status": "ok"}
```

**Files**:
- `/tmp/mineclaw-messages.jsonl` - All incoming messages (JSONL format)
- `/tmp/mineclaw-responses.jsonl` - All outgoing responses (JSONL format)
- `/tmp/mineclaw-processed-ids.txt` - IDs already forwarded to Discord

### 3. Bridge Daemon (Python)
**File**: `/tmp/nav_bridge_daemon.py`  
**Host**: navipi (gateway where OpenClaw is installed)  
**Port**: None (local polling)

**Flow**:
1. Poll webhook `/nav-unprocessed` every 3 seconds
2. For each unprocessed message: `openclaw message send --channel discord --target 195974388036665344 --message "..."`
3. Mark as processed in webhook
4. Async processing with threading (don't wait for openclaw)

**Logs**: `/tmp/bridge.log`

## Network Setup

### Tailscale IPs
- **Windows Client (Corban)**: 100.120.222.31
- **navibuntu (WSL on bob)**: 100.74.68.45 (WEBHOOK HOST)
- **navipi (Gateway)**: 100.108.96.55 (BRIDGE HOST)
- **treeclaw (Primary)**: 100.69.177.99

### Why This Configuration?
- ✓ Webhook on navibuntu (closest to Windows client on same machine)
- ✓ Bridge on navipi (where OpenClaw gateway is)
- ✓ All communication via Tailscale (no firewall issues)

## Deployment Steps

### 1. Start Webhook (on navibuntu)
```bash
ssh navibuntu@100.74.68.45
nohup python3 /tmp/nav_webhook.py > /tmp/webhook.log 2>&1 &
ss -tlnp | grep 9876  # Verify listening
```

### 2. Start Bridge Daemon (on navipi/locally)
```bash
nohup python3 /tmp/nav_bridge_daemon.py > /tmp/bridge.log 2>&1 &
ps aux | grep bridge  # Verify running
```

### 3. Install Mod (in Minecraft)
1. Copy `mineclaw-0.2.0.jar` to Minecraft mods folder
2. Restart Minecraft
3. Check logs for "[MineClaw] ... READY"

### 4. Test Integration
```bash
# Send test message
curl -X POST http://100.74.68.45:9876/nav-message \
  -H "Content-Type: application/json" \
  -d '{"message": "test", "sender": "TestPlayer"}'

# Verify webhook received it
curl -X POST http://100.74.68.45:9876/nav-unprocessed \
  -H "Content-Type: application/json" -d '{}'

# Check bridge processed it (should be marked processed)
sleep 5 && curl -X POST http://100.74.68.45:9876/nav-unprocessed \
  -H "Content-Type: application/json" -d '{}'
```

## Message Flow Example

```
Minecraft Client (Corban's Windows PC)
  └─> /nav "Hello Nav"
      ↓ HTTP POST to navibuntu
Webhook (/tmp/nav_webhook.py on navibuntu:9876)
  └─> Receives: {"message": "Hello Nav", "sender": "MinecraftPlayer"}
  └─> Stores in /tmp/mineclaw-messages.jsonl
  └─> Returns: {"status": "ok", "id": 1777799149011}
      ↓ Bridge polls via HTTP
Bridge Daemon (/tmp/nav_bridge_daemon.py on navipi)
  └─> GET /nav-unprocessed → finds 1 new message
  └─> Executes: openclaw message send --target 195974388036665344 --message "**[Minecraft: MinecraftPlayer]** Hello Nav"
  └─> POST /nav-mark-processed → marks id 1777799149011 as sent
      ↓ OpenClaw routes to Discord
Discord (Nav's DM)
  └─> Displays: "**[Minecraft: MinecraftPlayer]** Hello Nav"
  └─> Nav types response: "Hi! How's the game?"
      ↓ (Manual step: Bridge needs response handling)
Minecraft Client
  └─> Mod polls /nav-responses every 2 seconds
  └─> Gets response and logs in game
```

## Response Flow (Manual for Now)

Currently, responses from Discord need to be manually added to the webhook. Future enhancement:
- Have bridge daemon read Discord responses
- Post to webhook's response storage
- Mod polls and displays responses in-game

For now, test responses by:
```bash
# Manually add response to webhook
echo '{"timestamp": "2026-05-03T...", "response": "Hi from Nav!"}' >> /tmp/mineclaw-responses.jsonl

# Mod will poll and display it
```

## Troubleshooting

### Webhook not responding
```bash
ssh navibuntu@100.74.68.45
ss -tlnp | grep 9876
cat /tmp/webhook.log
```

### Bridge not forwarding messages
```bash
# Check if running
ps aux | grep bridge

# Check logs
tail -50 /tmp/bridge.log

# Check if messages are unprocessed
curl -X POST http://100.74.68.45:9876/nav-unprocessed -d '{}' -H "Content-Type: application/json"
```

### OpenClaw CLI not found
```bash
# Bridge must run on navipi where OpenClaw is installed
which openclaw
/usr/bin/openclaw --version
```

### Network issues
```bash
# Test Tailscale connectivity
ping 100.74.68.45
curl -v http://100.74.68.45:9876/

# Check firewall
ss -tlnp | grep 9876
```

## Future Enhancements

1. **Bidirectional responses**: Bridge reads Discord responses and posts back to mod
2. **Command parsing**: Support /nav-ask, /nav-code, /nav-image commands
3. **Rich formatting**: Color codes, clickable links in Minecraft
4. **Session persistence**: Store conversations in OpenClaw session
5. **Rate limiting**: Prevent spam from mod
6. **Persistence layer**: Database for message history
7. **Admin commands**: Manage mod remotely from Discord

## Files & Locations

```
Webhook:      /tmp/nav_webhook.py (on navibuntu)
Bridge:       /tmp/nav_bridge_daemon.py (local, runs on navipi)
Logs:         /tmp/webhook.log, /tmp/bridge.log
Messages:     /tmp/mineclaw-messages.jsonl
Responses:    /tmp/mineclaw-responses.jsonl
Mod Source:   ~/MineClaw-Messenger-template/ (on navibuntu)
Mod JAR:      ~/MineClaw-Messenger/releases/mineclaw-0.2.0.jar
GitHub:       https://github.com/CorbanMonoxide/MineClaw-Messenger.git
```

## Key Decisions

1. **Webhook on navibuntu** instead of treeclaw - same machine as Windows client via WSL, simpler network
2. **Bridge on navipi** - only location where OpenClaw CLI is available
3. **Async message sending** - don't block on openclaw commands, mark processed immediately
4. **JSONL format** - simple, append-only, easy to parse and audit
5. **Polling-based** - simpler than callbacks, no need for return URLs

## Status
✅ Webhook receiving messages from Minecraft mod  
✅ Bridge forwarding to Discord  
✅ Message history tracking  
✅ Additional commands (/nav-status, /nav-history, /nav-settings)  
⏳ Bidirectional responses (manual for now)  
⏳ Rich formatting & color codes  
⏳ Session persistence in OpenClaw  

---
*Last Updated: 2026-05-03 03:07 MDT*
*Architecture: Production-ready, tested end-to-end*
