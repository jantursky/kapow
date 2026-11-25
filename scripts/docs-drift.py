#!/usr/bin/env python3
"""Checks that the hand-written docs match the public Kotlin API.

Two checks, both against the commonMain source set of every kapow* module:

1. Effect pages: the `## Parameters` table of every effect page must list exactly the
   non-hidden parameters of the factory it documents (names as a set; order does not matter).
   Pages map to factories by file name, with OVERRIDES for the names that differ.
   A page with a table and no known factory fails, so a new page cannot skip the check.
2. Reference listings: every ```kotlin block in reference/*.mdx that has no kapow import and
   declares `public fun` must match a public function of the same name, with the same parameter
   names in the same order.

Every mismatch is printed before the script exits with status 1. `--list` prints the factory index.
Run `python3 scripts/docs-drift.py` (or `--list` to debug the parser).
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KOTLIN_SOURCES = sorted(ROOT.glob("kapow*/src/commonMain"))
EFFECTS = ROOT / "apps/docs/docs/effects"
REFERENCE = [ROOT / "apps/docs/docs/reference/types.mdx", ROOT / "apps/docs/docs/reference/composables.mdx"]

# Effect page file name (without .mdx) -> factory keys. A key is "Owner.name" or a bare top-level name.
# A page matches when its table equals any non-hidden overload of any listed key.
OVERRIDES = {
    "more-tilt": ["kapowTilt"],
    "more-skeleton": ["kapowSkeleton"],
    "more-trigger": ["changeEffect"],
    "more-interaction": ["changeEffect", "conditionalEffect"],
    "text-counter": ["KapowCounter"],
    "text-typewriter": ["KapowTypewriter"],
    "delay": ["ChangeEffect.delay", "ConditionalEffect.delay", "ProgressEffect.delay", "KapowEnter.delay", "KapowExit.delay"],
    "transition-stagger": ["KapowStagger.delay"],
    "transition-rotate3d": ["KapowEnter.rotate3D", "KapowExit.rotate3D"],
}

PREFIX_OWNERS = {
    "change": ["ChangeEffect"],
    "conditional": ["ConditionalEffect"],
    "progress": ["ProgressEffect"],
    "transition": ["KapowEnter", "KapowExit"],
}

DECL = re.compile(r"^(\s*)(?:(?:public|internal|private|open|abstract|sealed|data|value|override|@\w+)\s+)*"
                  r"(class|object|interface|enum\s+class|companion\s+object)\b\s*(\w*)")
FUN = re.compile(r"^(\s*)public\s+fun\s+(?:[\w.<>]+\.)?(\w+)\s*\(")
BLOCK_LISTING = re.compile(r"^```kotlin[^\n]*\n(.*?)^```", re.S | re.M)
TABLE_ROW = re.compile(r"^\|([^|]*)\|")
BACKTICKED = re.compile(r"`([^`]+)`")


def display(path: Path) -> str:
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def split_params(text: str) -> list[str]:
    """Parameter names from the text between the parentheses of a declaration."""
    names, depth, current = [], 0, ""
    pieces = []
    for ch in text:
        if ch in "([{<":
            depth += 1
        elif ch in ")]}>" and depth > 0:
            depth -= 1
        if ch == "," and depth == 0:
            pieces.append(current)
            current = ""
        else:
            current += ch
    pieces.append(current)
    for piece in pieces:
        piece = re.sub(r"//.*", "", piece).strip()
        if not piece:
            continue
        piece = re.sub(r"^@\w+(\([^)]*\))?\s*", "", piece)
        piece = re.sub(r"^(vararg|noinline|crossinline)\s+", "", piece)
        piece = re.sub(r"^(val|var)\s+", "", piece)
        match = re.match(r"(\w+)\s*:", piece)
        if match:
            names.append(match.group(1))
    return names


def declaration_args(lines: list[str], start: int) -> str:
    """Text inside the parentheses that open on lines[start], across lines, balanced."""
    text = "\n".join(lines[start:])
    open_at = text.index("(")
    depth = 0
    for index in range(open_at, len(text)):
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
            if depth == 0:
                return text[open_at + 1:index]
    raise ValueError(f"unbalanced parentheses in declaration: {lines[start].strip()}")


def is_hidden(lines: list[str], index: int) -> bool:
    for back in range(index - 1, max(index - 5, -1), -1):
        line = lines[back].strip()
        if line.startswith("public ") or line.startswith("fun ") or line.startswith("}"):
            return False
        if "DeprecationLevel.HIDDEN" in line:
            return True
    return False


def build_index() -> dict[str, list[list[str]]]:
    """Map 'Owner.name' (or bare 'name' for top-level) to the non-hidden overloads' parameter names."""
    index: dict[str, list[list[str]]] = {}
    for source in (f for root in KOTLIN_SOURCES for f in sorted(root.rglob("*.kt"))):
        lines = source.read_text().splitlines()
        owners: list[tuple[int, str]] = []  # (indent, name)
        for number, line in enumerate(lines):
            stripped = line.strip()
            if not stripped or stripped.startswith(("*", "//", "/*")):
                continue
            indent = len(line) - len(line.lstrip())
            # A closing brace at or left of a scope's indent ends that scope.
            if stripped.startswith("}"):
                while owners and owners[-1][0] >= indent:
                    owners.pop()
            decl = DECL.match(line)
            if decl:
                while owners and owners[-1][0] >= indent:
                    owners.pop()
                kind, name = decl.group(2), decl.group(3)
                if kind.startswith("companion"):
                    owners.append((indent, "\0companion"))
                else:
                    owners.append((indent, name))
                continue
            fun = FUN.match(line)
            if fun:
                # A function at or left of a scope's indent is outside that scope.
                while owners and owners[-1][0] >= indent:
                    owners.pop()
            if not fun or is_hidden(lines, number):
                continue
            # A companion object is transparent: its functions belong to the enclosing class.
            named = [name for _, name in owners if name != "\0companion"]
            owner = named[-1] if named else ""
            key = f"{owner}.{fun.group(2)}" if owner else fun.group(2)
            params = split_params(declaration_args(lines, number))
            index.setdefault(key, []).append(params)
    return index


def page_key_candidates(page: str) -> list[str] | None:
    if page in OVERRIDES:
        return OVERRIDES[page]
    prefix, _, rest = page.partition("-")
    if prefix not in PREFIX_OWNERS or not rest:
        return None
    name = re.sub(r"-(\w)", lambda m: m.group(1).upper(), rest)
    return [f"{owner}.{name}" for owner in PREFIX_OWNERS[prefix]]


def table_params(text: str) -> list[str] | None:
    """Names from the Parameters table, or None when the page has no such table."""
    match = re.search(r"^## Parameters\s*$(.*?)(?=^## |\Z)", text, re.S | re.M)
    if not match:
        return None
    names = []
    for line in match.group(1).splitlines():
        row = TABLE_ROW.match(line)
        if row:  # a first cell may name several parameters, as in "`isEnabled`, `feedback`"
            names.extend(BACKTICKED.findall(row.group(1)))
    return names


def check_effect_pages(index, problems: list[str]) -> None:
    for page in sorted(EFFECTS.glob("*.mdx")):
        names = table_params(page.read_text())
        if names is None:
            continue
        candidates = page_key_candidates(page.stem)
        rel = display(page)
        if not candidates:
            problems.append(f"{rel}: has a Parameters table but no factory mapping; add it to OVERRIDES in scripts/docs-drift.py")
            continue
        known = [key for key in candidates if key in index]
        if not known:
            problems.append(f"{rel}: no public factory found for {', '.join(candidates)}")
            continue
        wanted = set(names)
        options = [(key, params) for key in known for params in index[key]]
        if any(set(params) == wanted for _, params in options):
            continue
        key, params = options[0]
        missing = sorted(set(params) - wanted)
        extra = sorted(wanted - set(params))
        problems.append(
            f"{rel}: Parameters table differs from {key}: "
            f"not in table {missing or '[]'}, not in factory {extra or '[]'}"
        )


def check_listings(index, problems: list[str]) -> None:
    for page in REFERENCE:
        text = page.read_text()
        rel = display(page)
        for block in BLOCK_LISTING.findall(text):
            if "import com.jantursky.kapow" in block or "public fun" not in block:
                continue
            lines = block.splitlines()
            for number, line in enumerate(lines):
                fun = FUN.match(line)
                if not fun:
                    continue
                name = fun.group(2)
                listed = split_params(declaration_args(lines, number))
                overloads = [params for key, group in index.items()
                             if key == name or key.endswith(f".{name}") for params in group]
                if not overloads:
                    problems.append(f"{rel}: listing `{name}` has no public function in the Kotlin sources")
                elif listed not in overloads:
                    problems.append(
                        f"{rel}: listing `{name}({', '.join(listed)})` matches no declaration; "
                        f"source has {'; '.join('(' + ', '.join(p) + ')' for p in overloads)}"
                    )


def main() -> int:
    index = build_index()
    if "--list" in sys.argv:
        for key in sorted(index):
            for params in index[key]:
                print(f"{key}({', '.join(params)})")
        return 0
    problems: list[str] = []
    check_effect_pages(index, problems)
    check_listings(index, problems)
    for problem in problems:
        print(problem)
    if problems:
        print(f"docs-drift: {len(problems)} mismatch(es)")
        return 1
    print(f"docs-drift: {len(index)} factories checked, docs match")
    return 0


if __name__ == "__main__":
    sys.exit(main())
