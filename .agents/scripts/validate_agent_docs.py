#!/usr/bin/env python3
"""Validate project Agent rules, Skills, links, and routing-case coverage."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Optional
from urllib.parse import unquote


SCRIPT_DIR = Path(__file__).resolve().parent
AGENTS_ROOT = SCRIPT_DIR.parent
REPO_ROOT = AGENTS_ROOT.parent
SKILLS_ROOT = AGENTS_ROOT / "skills"
README_PATH = AGENTS_ROOT / "README.md"
ROUTING_CASES_PATH = AGENTS_ROOT / "evals" / "skill-routing-cases.json"

ERRORS: list[str] = []
WARNINGS: list[str] = []


def repo_relative(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return str(path.resolve())


def add_error(path: Path, message: str) -> None:
    ERRORS.append(f"{repo_relative(path)}: {message}")


def add_warning(path: Path, message: str) -> None:
    WARNINGS.append(f"{repo_relative(path)}: {message}")


def read_utf8(path: Path, *, reject_bom: bool = False) -> str:
    try:
        data = path.read_bytes()
    except OSError as exc:
        add_error(path, f"cannot read file: {exc}")
        return ""

    if reject_bom and data.startswith(b"\xef\xbb\xbf"):
        add_error(path, "UTF-8 BOM found; frontmatter must start at the first byte")

    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        add_error(path, "invalid UTF-8 text")
        return data.decode("utf-8", errors="replace")


def frontmatter_value(lines: list[str], key: str) -> Optional[str]:
    pattern = re.compile(rf"^{re.escape(key)}:\s*(.+)$")
    for line in lines:
        match = pattern.match(line)
        if not match:
            continue
        value = match.group(1).strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
            value = value[1:-1]
        return value
    return None


def validate_skills() -> tuple[list[Path], set[str]]:
    if not SKILLS_ROOT.is_dir():
        add_error(SKILLS_ROOT, "skills root not found")
        return [], set()

    skill_dirs = sorted(path for path in SKILLS_ROOT.iterdir() if path.is_dir())
    skill_names: set[str] = set()

    for skill_dir in skill_dirs:
        skill_path = skill_dir / "SKILL.md"
        if not skill_path.is_file():
            add_error(skill_dir, "missing SKILL.md")
            continue

        text = read_utf8(skill_path, reject_bom=True)
        lines = text.splitlines()
        if len(lines) < 4 or lines[0] != "---":
            add_error(skill_path, "frontmatter must start on the first line")
            continue

        try:
            frontmatter_end = lines.index("---", 1)
        except ValueError:
            add_error(skill_path, "frontmatter closing delimiter is missing")
            continue

        frontmatter_lines = lines[1:frontmatter_end]
        keys = [
            match.group(1)
            for line in frontmatter_lines
            if (match := re.match(r"^([a-zA-Z0-9_-]+):", line))
        ]
        unknown_keys = sorted(set(keys) - {"name", "description"})
        if unknown_keys:
            add_error(
                skill_path,
                f"frontmatter allows only name/description; found: {', '.join(unknown_keys)}",
            )

        name = frontmatter_value(frontmatter_lines, "name")
        description = frontmatter_value(frontmatter_lines, "description")
        if not name:
            add_error(skill_path, "name is missing")
        elif not re.fullmatch(r"[a-z0-9-]{1,64}", name):
            add_error(
                skill_path,
                "name must use lowercase letters, digits, or hyphens and be at most 64 characters",
            )
        elif name != skill_dir.name:
            add_error(skill_path, f"name '{name}' does not match directory '{skill_dir.name}'")
        else:
            skill_names.add(name)

        if not description:
            add_error(skill_path, "description is missing")
        elif "<" in description or ">" in description:
            add_error(skill_path, "description must not contain angle brackets")
        elif len(description) > 300:
            add_warning(skill_path, f"description has {len(description)} characters; verify trigger words stay concise")

        body = "\n".join(lines[frontmatter_end + 1 :])
        if re.search(r"(?m)^#{1,6}\s*(触发条件|何时使用|使用时机)\s*$", body):
            add_error(skill_path, "trigger conditions belong in description, not a body heading")
        if len(lines) > 500:
            add_warning(skill_path, f"{len(lines)} lines; consider moving details to references")

        references_root = skill_dir / "references"
        if references_root.is_dir():
            for reference in sorted(path for path in references_root.rglob("*") if path.is_file()):
                relative_reference = reference.relative_to(references_root)
                if len(relative_reference.parts) > 1:
                    add_error(reference, "references supports one directory level only")
                reference_link = f"references/{relative_reference.as_posix()}"
                if reference_link not in text:
                    add_error(reference, "not linked directly from the sibling SKILL.md")

        metadata_path = skill_dir / "agents" / "openai.yaml"
        if not metadata_path.is_file():
            add_error(skill_dir, "missing agents/openai.yaml")
        else:
            metadata = read_utf8(metadata_path)
            for field in ("display_name", "short_description", "default_prompt"):
                if not re.search(rf'(?m)^\s*{field}\s*:\s*"[^"]+"\s*$', metadata):
                    add_error(metadata_path, f"{field} must exist and use double quotes")
            if name and f"${name}" not in metadata:
                add_error(metadata_path, f"default_prompt must reference '${name}'")

    return skill_dirs, skill_names


def validate_skill_registry(skill_dirs: list[Path]) -> None:
    if not README_PATH.is_file():
        add_error(AGENTS_ROOT, "missing .agents/README.md")
        return

    readme = read_utf8(README_PATH)
    registered = set(
        re.findall(r"(?m)^\|\s*`([a-z0-9-]+)`\s*\|", readme)
    )
    directory_skills = {path.name for path in skill_dirs}
    for missing in sorted(directory_skills - registered):
        add_error(README_PATH, f"skill registry is missing '{missing}'")
    for stale in sorted(registered - directory_skills):
        add_error(README_PATH, f"skill registry contains missing directory '{stale}'")


def markdown_files() -> list[Path]:
    files: set[Path] = set()
    for root_name in ("AGENTS.md", "CLAUDE.md"):
        candidate = REPO_ROOT / root_name
        if candidate.is_file():
            files.add(candidate)
    for directory in (AGENTS_ROOT, REPO_ROOT / "docs"):
        if directory.is_dir():
            files.update(path for path in directory.rglob("*.md") if path.is_file())
    return sorted(files)


def markdown_target(raw_target: str) -> str:
    target = raw_target.strip()
    if target.startswith("<") and ">" in target:
        return target[1 : target.index(">")]
    title_match = re.match(r"^(.*?)(?:\s+[\"'].*[\"'])$", target)
    return title_match.group(1).strip() if title_match else target


def validate_markdown_links() -> None:
    link_pattern = re.compile(r"\[[^\]]*\]\((?P<target>[^)]+)\)")
    windows_absolute = re.compile(r"^[A-Za-z]:[\\/]")
    uri_scheme = re.compile(r"^[A-Za-z][A-Za-z0-9+.-]*:")

    for markdown_path in markdown_files():
        markdown = read_utf8(markdown_path)
        for match in link_pattern.finditer(markdown):
            target = markdown_target(match.group("target"))
            if not target or target.startswith("#"):
                continue
            if windows_absolute.match(target) or target.startswith(("/", "~/", "~\\", "file://")):
                add_error(markdown_path, f"Markdown link must not use a machine-absolute path: {target}")
                continue
            if uri_scheme.match(target):
                continue

            local_target = unquote(target.split("#", 1)[0])
            if not local_target:
                continue
            resolved_target = (markdown_path.parent / local_target).resolve()
            if not resolved_target.exists():
                add_error(markdown_path, f"local Markdown link does not exist: {local_target}")


def string_list(case: dict[str, object], field: str, case_id: str) -> list[str]:
    value = case.get(field)
    if not isinstance(value, list) or any(not isinstance(item, str) or not item for item in value):
        add_error(ROUTING_CASES_PATH, f"case '{case_id}' field '{field}' must be a string list")
        return []
    if len(value) != len(set(value)):
        add_error(ROUTING_CASES_PATH, f"case '{case_id}' field '{field}' contains duplicates")
    return value


def validate_routing_cases(skill_names: set[str]) -> int:
    if not ROUTING_CASES_PATH.is_file():
        add_error(ROUTING_CASES_PATH, "routing cases file is missing")
        return 0

    text = read_utf8(ROUTING_CASES_PATH)
    try:
        document = json.loads(text)
    except json.JSONDecodeError as exc:
        add_error(ROUTING_CASES_PATH, f"invalid JSON at line {exc.lineno}, column {exc.colno}: {exc.msg}")
        return 0

    if not isinstance(document, dict) or document.get("version") != 1:
        add_error(ROUTING_CASES_PATH, "root object must use version 1")
        return 0
    cases = document.get("cases")
    if not isinstance(cases, list) or not cases:
        add_error(ROUTING_CASES_PATH, "cases must be a non-empty list")
        return 0

    valid_categories = {"positive", "negative", "combination", "ambiguous"}
    valid_tiers = {"quick", "standard", "figma", "review", "docs"}
    ids: set[str] = set()
    prompts: set[str] = set()
    categories: set[str] = set()
    expected_coverage: set[str] = set()
    forbidden_coverage: set[str] = set()

    for index, raw_case in enumerate(cases, start=1):
        if not isinstance(raw_case, dict):
            add_error(ROUTING_CASES_PATH, f"case #{index} must be an object")
            continue

        case_id = raw_case.get("id")
        if not isinstance(case_id, str) or not re.fullmatch(r"[a-z0-9-]+", case_id):
            add_error(ROUTING_CASES_PATH, f"case #{index} has an invalid id")
            case_id = f"#{index}"
        elif case_id in ids:
            add_error(ROUTING_CASES_PATH, f"duplicate case id '{case_id}'")
        ids.add(case_id)

        category = raw_case.get("category")
        if category not in valid_categories:
            add_error(ROUTING_CASES_PATH, f"case '{case_id}' has invalid category '{category}'")
        elif isinstance(category, str):
            categories.add(category)

        prompt = raw_case.get("prompt")
        if not isinstance(prompt, str) or not prompt.strip():
            add_error(ROUTING_CASES_PATH, f"case '{case_id}' prompt is missing")
        elif prompt in prompts:
            add_error(ROUTING_CASES_PATH, f"case '{case_id}' repeats another prompt")
        else:
            prompts.add(prompt)

        tier = raw_case.get("expected_tier")
        if tier not in valid_tiers:
            add_error(ROUTING_CASES_PATH, f"case '{case_id}' has invalid expected_tier '{tier}'")

        expected = string_list(raw_case, "expected_skills", case_id)
        forbidden = string_list(raw_case, "forbidden_skills", case_id)
        overlap = sorted(set(expected) & set(forbidden))
        if overlap:
            add_error(ROUTING_CASES_PATH, f"case '{case_id}' both expects and forbids: {', '.join(overlap)}")
        for skill_name in expected + forbidden:
            if skill_name not in skill_names:
                add_error(ROUTING_CASES_PATH, f"case '{case_id}' references unknown skill '{skill_name}'")
        expected_coverage.update(expected)
        forbidden_coverage.update(forbidden)

        if category == "positive" and not expected:
            add_error(ROUTING_CASES_PATH, f"positive case '{case_id}' must expect at least one skill")
        if category == "negative" and not forbidden:
            add_error(ROUTING_CASES_PATH, f"negative case '{case_id}' must forbid at least one skill")
        if category == "combination" and len(expected) < 2:
            add_error(ROUTING_CASES_PATH, f"combination case '{case_id}' must expect at least two skills")
        if category == "ambiguous" and not forbidden:
            add_error(ROUTING_CASES_PATH, f"ambiguous case '{case_id}' must state forbidden skills")

        reason = raw_case.get("reason")
        if not isinstance(reason, str) or not reason.strip():
            add_error(ROUTING_CASES_PATH, f"case '{case_id}' reason is missing")

    for category in sorted(valid_categories - categories):
        add_error(ROUTING_CASES_PATH, f"routing cases are missing category '{category}'")
    for skill_name in sorted(skill_names - expected_coverage):
        add_error(ROUTING_CASES_PATH, f"skill '{skill_name}' has no expected-trigger case")
    for skill_name in sorted(skill_names - forbidden_coverage):
        add_error(ROUTING_CASES_PATH, f"skill '{skill_name}' has no forbidden-trigger case")

    return len(cases)


def main() -> int:
    skill_dirs, skill_names = validate_skills()
    validate_skill_registry(skill_dirs)
    validate_markdown_links()
    routing_case_count = validate_routing_cases(skill_names)

    print(f"Project skills: {len(skill_dirs)}")
    print(f"Routing cases: {routing_case_count}")
    for warning in WARNINGS:
        print(f"WARNING: {warning}", file=sys.stderr)
    for error in ERRORS:
        print(f"ERROR: {error}", file=sys.stderr)

    if ERRORS:
        print(f"Validation failed: {len(ERRORS)} error(s), {len(WARNINGS)} warning(s).")
        return 1
    print(f"Validation passed: 0 errors, {len(WARNINGS)} warning(s).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
