#!/usr/bin/env python3
"""
MineClaw Messenger Webhook Listener
Routes Minecraft messages to OpenClaw gateway
"""

import json
from http.server import HTTPServer, BaseHTTPRequestHandler
import sys
import urllib.request
import urllib.error

LISTEN_HOST = "0.0.0.0"
LISTEN_PORT = 8765
OPENCLAW_GATEWAY = "http://100.69.177.99:9898/v1/responses"
DISCORD_CHANNEL_ID = "195974388036665344"


class NavWebhookHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        pass

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

            formatted = f"**[Minecraft {sender}]** {message}"

            try:
                payload = json.dumps({
                    "action": "send",
                    "channel": "discord",
                    "target": DISCORD_CHANNEL_ID,
                    "message": formatted
                }).encode()

                req = urllib.request.Request(
                    OPENCLAW_GATEWAY,
                    data=payload,
                    headers={"Content-Type": "application/json"}
                )

                with urllib.request.urlopen(req, timeout=5) as resp:
                    self.send_response(200)
                    self.end_headers()
                    self.wfile.write(b'{"status": "ok"}')
                    print(f"[OK] {message[:50]}", file=sys.stderr)

            except urllib.error.URLError as e:
                self.send_response(502)
                self.end_headers()
                self.wfile.write(json.dumps({"error": f"Gateway: {str(e)}"}).encode())
                print(f"[ERR] Gateway: {e}", file=sys.stderr)

        except Exception as e:
            self.send_response(500)
            self.end_headers()
            self.wfile.write(json.dumps({"error": str(e)}).encode())
            print(f"[ERR] {e}", file=sys.stderr)


if __name__ == "__main__":
    server = HTTPServer((LISTEN_HOST, LISTEN_PORT), NavWebhookHandler)
    print(f"MineClaw Webhook listening on port {LISTEN_PORT}", file=sys.stderr)
    sys.stderr.flush()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Shutdown", file=sys.stderr)
        server.server_close()
