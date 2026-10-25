"""
临时本地模拟 LLM 服务 (仅供 D 组员自检用, 验收前可删除)
- 模拟 DeepSeek /v1/chat/completions 的返回格式
- 故意返回 3 种真实世界常见的"脏"返回, 用来检验 AIUtil 的解析健壮性:
    1) content 里是纯 JSON
    2) content 被 ```json ... ``` 包裹
    3) need_human=true 的转人工返回
"""
import json
from http.server import BaseHTTPRequestHandler, HTTPServer


def wrap(content):
    return json.dumps({
        "id": "mock-1",
        "object": "chat.completion",
        "choices": [{"index": 0, "message": {"role": "assistant", "content": content},
                     "finish_reason": "stop"}]
    }, ensure_ascii=False)


def pick(body):
    if "情感分析" in body:
        return wrap('{"emotion": "negative"}')
    if "奖学金" in body:
        return wrap('{"answer": "这个问题我转交人工客服", "need_human": true, "confidence": 0.30}')
    if "成绩" in body:
        return wrap('```json\n{"answer": "登录教务系统在成绩查询栏目可查看本学期成绩。", '
                    '"need_human": false, "confidence": 0.88}\n```')
    return wrap('{"answer": "建议先到操场失物招领处登记，并联系辅导员发布寻物启事。", '
                '"need_human": false, "confidence": 0.82}')


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        n = int(self.headers.get('Content-Length', 0))
        body = self.rfile.read(n).decode('utf-8')
        auth = self.headers.get('Authorization', '')
        print("[mock-llm] POST %s | Authorization=%s | body %d bytes"
              % (self.path, auth[:30], len(body)), flush=True)
        resp = pick(body).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.send_header('Content-Length', str(len(resp)))
        self.end_headers()
        self.wfile.write(resp)

    def log_message(self, *args):
        pass


if __name__ == '__main__':
    print("[mock-llm] listening on http://127.0.0.1:8787", flush=True)
    HTTPServer(('127.0.0.1', 8787), Handler).serve_forever()
