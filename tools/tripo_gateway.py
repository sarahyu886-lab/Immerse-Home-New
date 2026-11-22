"""Local-only Tripo adapter. Credentials never enter Gradle or the APK."""
import argparse
import hmac
import json
import os
from pathlib import Path
import re
import secrets
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

CONFIG = Path(__file__).resolve().parents[1] / ".private/tripo.json"
UPSTREAM = "https://api.tripo3d.ai/v2/openapi"


def initialize(path):
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    # Exclusive creation prevents overwriting a key the user already entered.
    fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(fd, "w") as stream:
        json.dump({"tripo_api_key": "", "local_access_token": secrets.token_urlsafe(32)}, stream, indent=2)
        stream.write("\n")


def load_config(path):
    if path.stat().st_mode & 0o077:
        raise ValueError("Private config must have mode 600: chmod 600 <config>")
    config = json.loads(path.read_text())
    if not isinstance(config.get("tripo_api_key"), str) or not config["tripo_api_key"].strip():
        raise ValueError("Fill tripo_api_key in the private config first.")
    if not isinstance(config.get("local_access_token"), str) or len(config["local_access_token"]) < 24:
        raise ValueError("local_access_token must contain at least 24 characters.")
    return config


def handler_for(config, allow_generation=False, opener=urlopen):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass  # Never log payloads, headers, image URLs, or credentials.

        def setup(self):
            super().setup()
            self.connection.settimeout(15)

        def send_json(self, status, value):
            body = json.dumps(value).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def authorized(self):
            token = self.headers.get("Authorization", "").removeprefix("Bearer ")
            if self.headers.get("Origin") or not hmac.compare_digest(token, config["local_access_token"]):
                self.send_json(403, {"error": "local_auth_required"})
                return False
            return True

        def relay(self, path, payload=None, content_type="application/json"):
            request = Request(UPSTREAM + path, data=payload, headers={
                "Authorization": "Bearer " + config["tripo_api_key"],
                "Content-Type": content_type
            })
            try:
                with opener(request, timeout=60) as response:
                    data = response.read(2 * 1024 * 1024 + 1)
                    if len(data) > 2 * 1024 * 1024:
                        raise ValueError("Response too large")
                    self.send_json(200, json.loads(data))
            except HTTPError as error:
                # Do not expose upstream response bodies which can echo private input.
                self.send_json(502, {"error": "tripo_http_error", "upstream_status": error.code})
            except (URLError, ValueError, TimeoutError):
                self.send_json(502, {"error": "tripo_unavailable"})

        def do_GET(self):
            if not self.authorized():
                return
            if self.path == "/health":
                self.send_json(200, {"configured": True, "generation_enabled": allow_generation})
            elif re.fullmatch(r"/tasks/[A-Za-z0-9_-]{1,128}", self.path):
                self.relay("/task/" + self.path.split("/")[-1])
            else:
                self.send_json(404, {"error": "not_found"})

        def do_POST(self):
            if not self.authorized():
                return
            if self.path not in ("/upload", "/tasks"):
                self.send_json(404, {"error": "not_found"})
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                limit = 20 * 1024 * 1024 if self.path == "/upload" else 8192
                if not 0 < length <= limit:
                    raise ValueError()
                payload = self.rfile.read(length)
                if len(payload) != length:
                    raise ValueError()
                if self.path == "/upload":
                    content_type = self.headers.get("Content-Type", "")
                    if not content_type.startswith("multipart/form-data; boundary="):
                        raise ValueError()
                    self.relay("/upload", payload, content_type)
                    return
                if not allow_generation or self.headers.get("X-Confirm-Generation") != "yes":
                    self.send_json(403, {"error": "generation_requires_explicit_cost_confirmation"})
                    return
                task = json.loads(payload)
                file = task.get("file", {})
                if task.get("type") != "image_to_model" or file.get("type") not in ("jpg", "jpeg", "png"):
                    raise ValueError()
                if not isinstance(file.get("file_token"), str) or not file["file_token"]:
                    raise ValueError()
                # Only forward the small supported surface, never arbitrary task parameters.
                task = {"type": "image_to_model",
                        "file": {"type": file["type"], "file_token": file["file_token"]}}
                self.relay("/task", json.dumps(task).encode())
            except (ValueError, TypeError, AttributeError):
                self.send_json(400, {"error": "invalid_request"})
    return Handler


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=CONFIG)
    parser.add_argument("--init", action="store_true")
    parser.add_argument("--check-config", action="store_true")
    parser.add_argument("--allow-generation", action="store_true")
    parser.add_argument("--port", type=int, default=8787)
    args = parser.parse_args()
    try:
        if args.init:
            initialize(args.config)
            print(f"Created private config: {args.config}")
            return
        config = load_config(args.config)
        if args.check_config:
            print("Local configuration valid; no network call made.")
            return
        server = HTTPServer(("127.0.0.1", args.port), handler_for(config, args.allow_generation))
        print(f"Local gateway listening on 127.0.0.1:{args.port}; keys never printed.")
        server.serve_forever()
    except (OSError, ValueError) as error:
        # Config errors only; do not print config contents or upstream exception URLs.
        print(f"Configuration error: {type(error).__name__}. Check private file path, permissions and required fields.")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
