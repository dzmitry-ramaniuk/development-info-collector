#!/usr/bin/env python3
"""Проверяет локальные Markdown-ссылки, оглавления и полноту навигации."""
from pathlib import Path
from urllib.parse import unquote
import re
import sys

ROOT = Path(__file__).resolve().parent.parent
EXCLUDED = {"AGENTS.md", ".github/copilot-instructions.md", ".github/ISSUE_TEMPLATE/revision.md"}
FILES = sorted(p for p in ROOT.rglob("*.md") if ".git" not in p.parts and p.relative_to(ROOT).as_posix() not in EXCLUDED)
LINK_RE = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
HEADING_RE = re.compile(r"^(#{1,3})\s+(.+?)\s*#*$")


def source_lines(path):
    """Возвращает строки вне fenced code blocks."""
    result, fenced = [], False
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if re.match(r"^\s*```", line):
            fenced = not fenced
        elif not fenced:
            result.append((number, line))
    return result


def slug(title, used):
    title = re.sub(r"<[^>]+>", "", re.sub(r"[`*_~]", "", title)).lower().strip()
    value = re.sub(r"[^\w\- ]", "", title, flags=re.UNICODE).replace(" ", "-")
    index = used.get(value, 0)
    used[value] = index + 1
    return value if index == 0 else f"{value}-{index}"


def headings(path):
    result, used = [], {}
    for number, line in source_lines(path):
        match = HEADING_RE.match(line)
        if match:
            title = match.group(2).strip()
            result.append((number, len(match.group(1)), title, slug(title, used)))
    return result


def source_for_link(path):
    """Сопоставляет опубликованный Jekyll URL *.html с исходным файлом *.md."""
    return path.with_suffix(".md") if path.suffix.lower() == ".html" else path


errors = []
anchors = {path.resolve(): {item[3] for item in headings(path)} for path in FILES}
for path in FILES:
    for number, line in source_lines(path):
        for match in LINK_RE.finditer(line):
            raw_destination = match.group(1).strip()
            destination = raw_destination.strip("<>")
            if re.match(r"^(?:https?:|mailto:|tel:)", destination):
                continue
            raw_path, _, fragment = destination.partition("#")
            if raw_path and re.search(r"\s", raw_path) and not (
                raw_destination.startswith("<") and raw_destination.endswith(">")
            ):
                errors.append(
                    f"{path.relative_to(ROOT)}:{number}: пробел в пути должен быть закодирован как %20: {destination}"
                )
            target = (path.parent / unquote(raw_path)).resolve() if raw_path else path.resolve()
            target = source_for_link(target)
            if raw_path and not target.exists():
                errors.append(f"{path.relative_to(ROOT)}:{number}: нет файла {destination}")
            elif fragment and target.suffix.lower() == ".md" and unquote(fragment) not in anchors.get(target, set()):
                errors.append(f"{path.relative_to(ROOT)}:{number}: нет якоря {destination}")

# Оглавление должно идти сразу после H1 и повторять порядок H2/H3.
for path in FILES:
    if not path.read_text(encoding="utf-8").startswith("---\n"):
        errors.append(f"{path.relative_to(ROOT)}: отсутствует YAML front matter для сборки Jekyll")
    if path.name == "README.md":
        continue
    items = headings(path)
    sections = [item for item in items if item[1] == 2 and item[2] != "Содержание"]
    if len(sections) < 2:
        continue
    toc = next((item for item in items if item[1] == 2 and item[2] == "Содержание"), None)
    h1 = next((item for item in items if item[1] == 1), None)
    if not toc:
        errors.append(f"{path.relative_to(ROOT)}: отсутствует оглавление")
        continue
    first_after_h1 = next((item for item in items if item[0] > h1[0]), None)
    if first_after_h1 != toc:
        errors.append(f"{path.relative_to(ROOT)}:{toc[0]}: оглавление расположено не сразу после H1")
    toc_links = []
    for number, line in source_lines(path):
        if number <= toc[0]:
            continue
        if line.startswith("## "):
            break
        toc_links.extend(re.findall(r"\]\(#([^)]+)\)", line))
    expected = [item[3] for item in items if item[1] in (2, 3) and item[2] != "Содержание"]
    if toc_links != expected:
        errors.append(f"{path.relative_to(ROOT)}:{toc[0]}: порядок или состав оглавления не совпадает с H2/H3")

# Каждый тематический файл должен быть перечислен в README своего каталога.
for path in FILES:
    if path.name == "README.md":
        continue
    local_readme = path.parent / "README.md"
    if not local_readme.exists():
        errors.append(f"{path.relative_to(ROOT)}: в каталоге нет README.md")
    elif path.with_suffix(".html").name not in local_readme.read_text(encoding="utf-8"):
        errors.append(f"{local_readme.relative_to(ROOT)}: отсутствует ссылка на {path.name}")

# Главный каталог обязан содержать каждую учебную и обзорную страницу.
root_readme = (ROOT / "README.md").read_text(encoding="utf-8")
root_targets = {
    str(Path(unquote(link.partition("#")[0])).with_suffix(".md"))
    if link.partition("#")[0].lower().endswith(".html")
    else unquote(link.partition("#")[0])
    for link in LINK_RE.findall(root_readme)
}
for path in FILES:
    relative = path.relative_to(ROOT).as_posix()
    if relative != "README.md" and relative not in root_targets:
        errors.append(f"README.md: в полном каталоге отсутствует {relative}")

if errors:
    print("Ошибки Markdown-навигации:")
    print("\n".join(f"- {error}" for error in errors))
    sys.exit(1)
print(f"Проверено Markdown-файлов: {len(FILES)}; битых ссылок и ошибок оглавлений нет.")
