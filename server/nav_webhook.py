#!/usr/bin/env python3
"""
MineClaw Messenger Webhook Listener
Runs on navibuntu, listens for Minecraft @nav messages,
and routes them to OpenClaw via local message API.
"""

import json
import socket
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urljoin
import requests
import sys

OPENCLAW_MESSAGE_API = "http://localhost:8000/api/message"
LISTEN_HOST = "localhost"
LISTEN_PORT = 8765


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

            # Route to OpenClaw
            payload = {
                "message": message,
                "source": f"minecraft:{sender}",
                "channel": "discord"  # Adjust as needed
            }

            try:
                response = requests.post(OPENCLAW_MESSAGE_API, json=payload, timeout=5)
                if response.status_code in [200, 202]:
                    self.send_response(200)
                    self.end_headers()
                    self.wfile.write(b'{"status": "ok"}')
                else:
                    self.send_response(502)
                    self.end_headers()
                    self.wfile.write(b'{"error": "OpenClaw API error"}')
            except requests.exceptions.RequestException as e:
                print(f"Error contacting OpenClaw API: {e}", file=sys.stderr)
                self.send_response(502)
                self.end_headers()
                self.wfile.write(b'{"error": "API unavailable"}')

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
    print(f"Forwarding messages to OpenClaw at {OPENCLAW_MESSAGE_API}")
    httpd.serve_forever()


if __name__ == "__main__":
    try:
        run_server()
    except KeyboardInterrupt:
        print("\nShutting down...")
        sys.exit(0)
