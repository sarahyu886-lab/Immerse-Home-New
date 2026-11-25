"""Local-only semantic planner. Provider keys stay in .private and never enter the APK."""
import argparse
import json
import os
from pathlib import Path
import re
import secrets
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

CONFIG = Path(__file__).resolve().parents[1] / ".private/semantic_llm.json"
DEFAULT_BASE_URL = "https://api.openai.com/v1"
DEFAULT_MODEL = "gpt-4.1-mini"
SYSTEM_PROMPT = (
    "You are a room-edit semantic planner for a 4x5x2.5 meter standard room. "
    "Return only JSON with keys asset_prompt, anchor, wall_id, room_edit, summary, notes. "
    "anchor must be one of wall, ceiling, floor. wall_id must be one of north, south, east, west or empty. "
    "room_edit can be add_window or empty. If the request is about a window, force anchor=wall and room_edit=add_window. "
    "If the request is about a lamp/light/chandelier, force anchor=ceiling. Keep notes short."
)


def initialize(path):
    path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    with os.fdopen(fd, "w") as stream:
        json.dump(
            {
                "api_key": "",
                "base_url": DEFAULT_BASE_URL,
                "model": DEFAULT_MODEL,
                "local_access_token": secrets.token_urlsafe(32),
            },
            stream,
            indent=2,
        )
        stream.write("\n")


def load_config(path):
    if path.stat().st_mode & 0o077:
        raise ValueError("Private config must have mode 600: chmod 600 <config>")
    config = json.loads(path.read_text())
    if not isinstance(config.get("api_key"), str) or not config["api_key"].strip():
        raise ValueError("Fill api_key in the private config first.")
    if not isinstance(config.get("base_url"), str) or not config["base_url"].startswith("http"):
        raise ValueError("base_url must be a valid http(s) URL.")
    if not isinstance(config.get("model"), str) or not config["model"].strip():
        raise ValueError("model must be a non-empty string.")
    return config


def fallback_plan(prompt):
    lower = prompt.lower()
    wall_id = "north"
    if any(token in lower for token in ("东", "east", "右")):
        wall_id = "east"
    elif any(token in lower for token in ("西", "west", "左")):
        wall_id = "west"
    elif any(token in lower for token in ("南", "south", "前")):
        wall_id = "south"
    if "窗" in lower or "window" in lower:
        return {
            "asset_prompt": prompt or "simple wall window frame",
            "anchor": "wall",
            "wall_id": wall_id,
            "room_edit": "add_window",
            "summary": f"窗户请求已约束到{wall_id}墙。",
            "notes": ["使用本地关键词兜底"]
        }
    if any(token in lower for token in ("吊灯", "吸顶灯", "ceiling light", "chandelier")) or ("灯" in lower and "落地灯" not in lower):
        return {
            "asset_prompt": prompt or "ceiling mounted lamp",
            "anchor": "ceiling",
            "wall_id": "",
            "room_edit": "",
            "summary": "灯具请求已约束到天花板。",
            "notes": ["使用本地关键词兜底"]
        }
    return {
        "asset_prompt": prompt,
        "anchor": "floor",
        "wall_id": "",
        "room_edit": "",
        "summary": "将按文本请求生成模型。",
        "notes": ["更复杂的语义建议使用大模型"]
    }


def normalize_plan(plan, prompt):
    normalized = fallback_plan(prompt)
    normalized.update({
        "asset_prompt": plan.get("asset_prompt") or normalized["asset_prompt"],
        "anchor": plan.get("anchor") or normalized["anchor"],
        "wall_id": plan.get("wall_id") or normalized["wall_id"],
        "room_edit": plan.get("room_edit") or normalized["room_edit"],
        "summary": plan.get("summary") or normalized["summary"],
        "notes": [note for note in plan.get("notes", []) if isinstance(note, str) and note.strip()] or normalized["notes"],
    })
    lower = prompt.lower()
    if "窗" in lower or "window" in lower:
        normalized["anchor"] = "wall"
        normalized["room_edit"] = "add_window"
        if normalized["wall_id"] not in ("north", "south", "east", "west"):
            normalized["wall_id"] = fallback_plan(prompt)["wall_id"]
    if any(token in lower for token in ("吊灯", "吸顶灯", "ceiling light", "chandelier")) or ("灯" in lower and "落地灯" not in lower):
        normalized["anchor"] = "ceiling"
        normalized["room_edit"] = ""
        normalized["wall_id"] = ""
    if normalized["anchor"] not in ("wall", "ceiling", "floor"):
        normalized["anchor"] = fallback_plan(prompt)["anchor"]
    return normalized


def call_model(config, payload, opener=urlopen):
    request = Request(
        config["base_url"].rstrip("/") + "/chat/completions",
        data=json.dumps(
            {
                "model": config["model"],
                "temperature": 0.1,
                "messages": [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": json.dumps(payload, ensure_ascii=False)},
                ],
            }
        ).encode(),
        headers={
            "Authorization": "Bearer " + config["api_key"],
            "Content-Type": "application/json",
        },
    )
    with opener(request, timeout=60) as response:
        body = json.loads(response.read())
    content = body["choices"][0]["message"]["content"]
    if isinstance(content, list):
        content = "".join(part.get("text", "") for part in content if isinstance(part, dict))
    text = str(content).strip()
    text = re.sub(r"^```json\s*|```$", "", text, flags=re.IGNORECASE | re.MULTILINE).strip()
    return json.loads(text)


def handler_for(config, opener=urlopen):
    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass

        def send_json(self, status, value):
            body = json.dumps(value).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def authorized(self):
            if self.headers.get("Origin"):
                self.send_json(403, {"error": "browser_origin_blocked"})
                return False
            return True

        def do_GET(self):
            if not self.authorized():
                return
            if self.path == "/health":
                self.send_json(200, {"configured": True, "model": config["model"]})
            else:
                self.send_json(404, {"error": "not_found"})

        def do_POST(self):
            if not self.authorized():
                return
            if self.path != "/plan":
                self.send_json(404, {"error": "not_found"})
                return
            try:
                length = int(self.headers.get("Content-Length", "0"))
                if not 0 < length <= 8192:
                    raise ValueError()
                payload = json.loads(self.rfile.read(length))
                prompt = payload.get("prompt", "")
                if not isinstance(prompt, str):
                    raise ValueError()
                result = normalize_plan(call_model(config, payload, opener=opener), prompt)
                self.send_json(200, result)
            except (ValueError, TypeError, KeyError, IndexError, json.JSONDecodeError):
                self.send_json(400, {"error": "invalid_request"})
            except (HTTPError, URLError, TimeoutError):
                prompt = payload.get("prompt", "") if 'payload' in locals() and isinstance(payload, dict) else ""
                self.send_json(200, normalize_plan(fallback_plan(prompt), prompt))
    return Handler


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", type=Path, default=CONFIG)
    parser.add_argument("--init", action="store_true")
    parser.add_argument("--check-config", action="store_true")
    parser.add_argument("--port", type=int, default=8798)
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
        server = HTTPServer(("127.0.0.1", args.port), handler_for(config))
        print(f"Semantic gateway listening on 127.0.0.1:{args.port}; keys never printed.")
        server.serve_forever()
    except (OSError, ValueError) as error:
        print(
            f"Configuration error: {type(error).__name__}. "
            "Check private file path, permissions and required fields."
        )
        raise SystemExit(1)


if __name__ == "__main__":
    main()
