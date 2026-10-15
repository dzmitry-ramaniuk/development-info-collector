#!/usr/bin/env python3
"""Собирает и запускает полные Java-блоки, отмеченные runnable в учебных страницах."""
from pathlib import Path
import os
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parent.parent
BLOCK = re.compile(r"<!-- runnable: ([A-Za-z][A-Za-z0-9_]*\.java) -->\s*```java\n(.*?)\n```", re.S)


def main():
    java_home = os.environ.get("JAVA_HOME")
    java = str(Path(java_home) / "bin/java") if java_home else shutil.which("java")
    javac = str(Path(java_home) / "bin/javac") if java_home else shutil.which("javac")
    if not java or not javac:
        raise SystemExit("Нужен полный JDK 17+ с java и javac; при необходимости задайте JAVA_HOME.")
    count = 0
    with tempfile.TemporaryDirectory(prefix="learning-java-") as temporary:
        for page in sorted(ROOT.rglob("*.md")):
            if any(part in {".git", "work", "target", "node_modules"} for part in page.relative_to(ROOT).parts):
                continue
            text = page.read_text(encoding="utf-8")
            matches = list(BLOCK.finditer(text))
            if len(matches) != text.count("<!-- runnable:"):
                raise SystemExit(f"Неверно оформлен runnable-блок: {page.relative_to(ROOT)}")
            for match in matches:
                count += 1
                directory = Path(temporary) / str(count)
                directory.mkdir()
                source = directory / match[1]
                source.write_text(match[2] + "\n", encoding="utf-8")
                subprocess.run([javac, "--release", "17", "-encoding", "UTF-8", "-Xlint:all", "-d", str(directory), str(source)], check=True, timeout=30)
                subprocess.run([java, "-ea", "-cp", str(directory), source.stem], check=True, timeout=15)
                print(f"OK: {page.relative_to(ROOT)} → {source.name}", flush=True)
    if count == 0:
        raise SystemExit("Не найдено ни одного runnable Java-примера.")
    print(f"Проверено полных Java-примеров из Markdown: {count}")


if __name__ == "__main__":
    main()
