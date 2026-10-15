#!/usr/bin/env python3
"""Учебный MCP 2025-11-25: stdio, один immutable ресурс и read-only tool.

Запуск: python3 examples/ai-agents/demo_mcp.py
Offline-проверка: python3 examples/ai-agents/demo_mcp.py --self-test
Только стандартная библиотека Python 3.12+. Файлы, сеть и shell tools не доступны.
Это ограниченный учебный сервер, а не универсальная реализация MCP.
"""

import json
import subprocess
import sys

VERSION = "2025-11-25"
URI = "demo://catalog/sku-001"
ITEM = {"sku": "sku-001", "name": "Учебная книга", "priceMinor": 99000, "currency": "RUB"}
TOOL = {
    "name": "get_demo_item",
    "description": "Читает единственный синтетический товар sku-001; не работает с файлами или сетью.",
    "inputSchema": {
        "type": "object",
        "properties": {"sku": {"type": "string"}},
        "required": ["sku"],
        "additionalProperties": False,
    },
    "annotations": {"readOnlyHint": True, "destructiveHint": False, "openWorldHint": False},
}


def error(request_id, code, message):
    return {"jsonrpc": "2.0", "id": request_id, "error": {"code": code, "message": message}}


class Server:
    def __init__(self):
        self.initialized = False
        self.ready = False

    def handle(self, request):
        if not isinstance(request, dict):
            return error(None, -32600, "Expected one JSON-RPC object; batches are not supported")
        request_id = request.get("id")
        if (request.get("jsonrpc") != "2.0" or not isinstance(request.get("method"), str)
                or ("id" in request and (isinstance(request_id, bool)
                    or not isinstance(request_id, (str, int))))):
            return error(None, -32600, "Invalid JSON-RPC request")
        method = request["method"]
        params = request.get("params", {})
        # Notifications never receive a response, including unknown notifications.
        if "id" not in request:
            if method == "notifications/initialized" and self.initialized:
                self.ready = True
            return None
        if not isinstance(params, dict):
            return error(request_id, -32602, "params must be an object")
        if method == "initialize":
            if self.initialized:
                return error(request_id, -32600, "Already initialized")
            if (not isinstance(params.get("protocolVersion"), str)
                    or not isinstance(params.get("capabilities"), dict)
                    or not isinstance(params.get("clientInfo"), dict)):
                return error(request_id, -32602, "Missing initialization fields")
            # A client that cannot use our version must disconnect after negotiation.
            self.initialized = True
            result = {
                "protocolVersion": VERSION,
                "capabilities": {"tools": {}, "resources": {}},
                "serverInfo": {"name": "demo-catalog", "version": "1.0.0"},
            }
        elif method == "ping":
            result = {}
        elif not self.ready:
            return error(request_id, -32600, "Complete initialize + notifications/initialized first")
        elif method == "tools/list":
            result = {"tools": [TOOL]}
        elif method == "resources/list":
            result = {"resources": [{"uri": URI, "name": "demo-item", "mimeType": "application/json"}]}
        elif method == "resources/templates/list":
            result = {"resourceTemplates": []}
        elif method == "resources/read":
            if params.get("uri") != URI:
                return error(request_id, -32002, "Resource not found")
            result = {"contents": [{"uri": URI, "mimeType": "application/json",
                                    "text": json.dumps(ITEM, ensure_ascii=False)}]}
        elif method == "tools/call":
            if params.get("name") != TOOL["name"]:
                return error(request_id, -32602, "Unknown tool")
            arguments = params.get("arguments", {})
            if (not isinstance(arguments, dict) or set(arguments) != {"sku"}
                    or not isinstance(arguments["sku"], str)):
                return error(request_id, -32602, "Expected exactly one string argument: sku")
            found = arguments["sku"] == ITEM["sku"]
            content = json.dumps(ITEM, ensure_ascii=False) if found else "Unknown demo SKU"
            result = {"content": [{"type": "text", "text": content}], "isError": not found}
        else:
            return error(request_id, -32601, "Method not found")
        return {"jsonrpc": "2.0", "id": request_id, "result": result}


def serve():
    sys.stdin.reconfigure(encoding="utf-8")
    sys.stdout.reconfigure(encoding="utf-8")
    server = Server()
    # MCP stdio: one UTF-8 JSON message per line; stdout contains only protocol.
    for line in sys.stdin:
        try:
            request = json.loads(line)
        except json.JSONDecodeError:
            response = error(None, -32700, "Parse error")
        else:
            response = server.handle(request)
        if response is not None:
            print(json.dumps(response, ensure_ascii=False), flush=True)


def self_test():
    def req(request_id, method, params=None):
        return {"jsonrpc": "2.0", "id": request_id, "method": method, "params": params or {}}

    initialization = {"protocolVersion": VERSION, "capabilities": {},
                      "clientInfo": {"name": "offline-self-test", "version": "1.0.0"}}
    messages = [
        req(0, "tools/list"),
        req(1, "initialize", initialization),
        {"jsonrpc": "2.0", "method": "notifications/initialized"},
        req(2, "tools/list"),
        req(3, "resources/list"),
        req(4, "resources/read", {"uri": URI}),
        req(5, "tools/call", {"name": "get_demo_item", "arguments": {"sku": "sku-001"}}),
        req(6, "tools/call", {"name": "get_demo_item", "arguments": {"sku": "other"}}),
        req(7, "tools/call", {"name": "get_demo_item", "arguments": {"sku": "sku-001", "path": "/etc/passwd"}}),
        req(8, "resources/read", {"uri": "file:///etc/passwd"}),
        req(9, "delete_everything"),
        {"jsonrpc": "2.0", "method": "notifications/unknown"},
        req(10, "ping"),
        req(11, "initialize", initialization),
        [req(12, "ping")],
    ]
    wire = "\n".join(json.dumps(message) for message in messages) + "\nnot-json\n"
    process = subprocess.run([sys.executable, __file__], input=wire, text=True,
                             capture_output=True, timeout=5, check=True)
    replies = [json.loads(line) for line in process.stdout.splitlines()]
    assert len(replies) == 14, "A notification received a response, or a request was lost"
    by_id = {reply["id"]: reply for reply in replies if reply["id"] is not None}
    assert by_id[0]["error"]["code"] == -32600
    assert by_id[1]["result"]["protocolVersion"] == VERSION
    assert by_id[2]["result"]["tools"] == [TOOL]
    assert by_id[3]["result"]["resources"][0]["uri"] == URI
    assert json.loads(by_id[4]["result"]["contents"][0]["text"]) == ITEM
    assert json.loads(by_id[5]["result"]["content"][0]["text"]) == ITEM
    assert by_id[5]["result"]["isError"] is False
    assert by_id[6]["result"]["isError"] is True
    assert by_id[7]["error"]["code"] == -32602
    assert by_id[8]["error"]["code"] == -32002
    assert by_id[9]["error"]["code"] == -32601
    assert by_id[10]["result"] == {}
    assert by_id[11]["error"]["code"] == -32600
    assert [reply["error"]["code"] for reply in replies if reply["id"] is None] == [-32600, -32700]
    assert process.stderr == "", "Server wrote unexpected diagnostics"
    print("PASS: initialization, tools/resources, immutable fixture, invalid input, notification silence, stdio framing")


if __name__ == "__main__":
    if sys.argv[1:] == ["--self-test"]:
        self_test()
    elif sys.argv[1:]:
        raise SystemExit("Usage: demo_mcp.py [--self-test]")
    else:
        serve()
