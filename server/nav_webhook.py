#!/usr/bin/env python3
"""
MineClaw Messenger Webhook Listener
Runs on navibuntu, listens for Minecraft @nav messages,
and routes them to Discord via OpenClaw session messaging.
"""

import json
from http.server import HTTPServer, BaseHTTPRequestHandler
import sys
import os

# If running on the same machine as OpenClaw, we can import the session API
# Otherwise, we send to Discord directly or a local session

LISTEN_HOST = "localhost"
LISTEN_PORT = 8765

# Discord channel to send messages to (Nav's DM channel)
DISCORD_CHANNEL_ID = "195974388036665344"  # Corban's user ID (DM)


class NavWebhookHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/nav-message":
            self.send_response(404)
            self.end_headers()
            return

        try:
            content_length = int(self.headers.get("Content-Length", 0))
            body = self.rfile.read(content_length).decode("utf-8")
            data = json.loads(body)

            message = data.get("message", "").strip()
            sender = data.get("sender", "minecraft")

            if not message:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b'{"error": "Empty message"}')
                return

            # Format the message for Discord
            formatted_message = f"**[Minecraft]** {message}"

            # Try to use openclaw CLI to send the message
            import subprocess
            try:
                result = subprocess.run(
                    ["openclaw", "message", "send", 
                     "--channel", "discord",
                     "--target", DISCORD_CHANNEL_ID,
                     formatted_message],
                    capture_output=True,
                    text=True,
                    timeout=5
                )
                
                if result.returncode == 0:
                    self.send_response(200)
                    self.end_headers()
                    self.wfile.write(b'{"status": "ok"}')
                    print(f"✓ Message from Minecraft: {message[:50]}...", file=sys.stderr)
                else:
                    self.send_response(502)
                    self.end_headers()
                    self.wfile.write(b'{"error": "OpenClaw CLI error"}')
                    print(f"✗ OpenClaw error: {result.stderr}", file=sys.stderr)
                    
            except subprocess.TimeoutExpired:
                self.send_response(502)
                self.end_headers()
                self.wfile.write(b'{"error": "OpenClaw CLI timeout"}')
                print("✗ OpenClaw CLI timeout", file=sys.stderr)
            except FileNotFoundError:
                self.send_response(502)
                self.end_headers()
                self.wfile.write(b'{"error": "OpenClaw CLI not found"}')
                print("✗ OpenClaw CLI not found in PATH", file=sys.stderr)

        except json.JSONDecodeError:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b'{"error": "Invalid JSON"}')
        except Exception as e:
            print(f"Error processing request: {e}", file=sys.stderr)
            self.send_response(500)
            self.end_headers()
            self.wfile.write(b'{"error": "Internal server error"}')

    def log_message(self, format, *args):
        # Suppress default logging
        pass


def run_server():
    server_address = (LISTEN_HOST, LISTEN_PORT)
    httpd = HTTPServer(server_address, NavWebhookHandler)
    print(f"MineClaw Webhook listening on {LISTEN_HOST}:{LISTEN_PORT}")
    print(f"Forwarding messages to Discord (@{DISCORD_CHANNEL_ID})")
    httpd.serve_forever()


if __name__ == "__main__":
    try:
        run_server()
    except KeyboardInterrupt:
        print("\nShutting down...")
        sys.exit(0)
