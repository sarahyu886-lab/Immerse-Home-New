"""Offline tests only. No real API key or Tripo request is used."""
import json
from pathlib import Path
import tempfile
import threading
import unittest
from http.server import HTTPServer
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from tripo_gateway import initialize, load_config, handler_for


class GatewayTest(unittest.TestCase):
    def test_private_initialization_never_overwrites(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "tripo.json"
            initialize(path)
            self.assertEqual(0, path.stat().st_mode & 0o077)
            with self.assertRaises(FileExistsError):
                initialize(path)
            with self.assertRaises(ValueError):
                load_config(path)
            data = json.loads(path.read_text())
            data["tripo_api_key"] = "offline-test-secret"
            path.write_text(json.dumps(data))
            self.assertEqual("offline-test-secret", load_config(path)["tripo_api_key"])

    def test_unauthorized_and_paid_requests_do_not_reach_upstream(self):
        calls = []
        config = {"tripo_api_key": "never-return-this", "local_access_token": "local-test-token"}
        server = HTTPServer(("127.0.0.1", 0), handler_for(config, opener=lambda *a, **kw: calls.append(a)))
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        url = f"http://127.0.0.1:{server.server_port}"
        try:
            with self.assertRaises(HTTPError) as denied:
                urlopen(url + "/health")
            self.assertEqual(403, denied.exception.code)
            request = Request(url + "/health", headers={"Authorization": "Bearer local-test-token"})
            with urlopen(request) as response:
                body = response.read()
                self.assertNotIn(b"never-return-this", body)
                self.assertFalse(json.loads(body)["generation_enabled"])
            request = Request(url + "/tasks", data=b"{}", headers={
                "Authorization": "Bearer local-test-token", "Content-Type": "application/json"})
            with self.assertRaises(HTTPError) as denied:
                urlopen(request)
            self.assertEqual(403, denied.exception.code)
            self.assertEqual([], calls)
        finally:
            server.shutdown()
            server.server_close()
            thread.join()


if __name__ == "__main__":
    unittest.main()
