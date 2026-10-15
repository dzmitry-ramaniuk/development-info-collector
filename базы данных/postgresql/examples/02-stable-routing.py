"""Python 3: проверка routing в независимых процессах без внешних библиотек."""
import hashlib
import json
import os
import subprocess
import sys


def shard_for(user_id: int, shard_count: int) -> int:
    if shard_count <= 0:
        raise ValueError("shard_count должен быть положительным")
    digest = hashlib.sha256(str(user_id).encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big") % shard_count


def routes():
    return [shard_for(user_id, 3) for user_id in (1, 2, 42, 12345)]


if __name__ == "__main__":
    if "--child" in sys.argv:
        print(json.dumps(routes()))
    else:
        expected = routes()
        for seed in ("1", "2", "random"):
            env = dict(os.environ, PYTHONHASHSEED=seed)
            result = subprocess.check_output(
                [sys.executable, __file__, "--child"], env=env, text=True
            )
            if json.loads(result) != expected:
                raise RuntimeError(f"Routing зависит от процесса: seed={seed}")
        print(f"Стабильный routing для трёх шардов: {expected}")
