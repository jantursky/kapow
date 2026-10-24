#!/usr/bin/env python3
"""Copies the Kotlin samples from the docs and READMEs into :samples:docs-check, which compiles them.

A sample counts when its fenced block imports com.jantursky.kapow, so Gradle snippets and the
signature listings in the reference pages are skipped. Each sample is a whole file: imports first,
then top-level declarations. Each gets its own package to keep top-level names apart.
Run `python3 scripts/doc-samples.py && ./gradlew :samples:docs-check:compileTestKotlinJvm`.
"""

import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = [
    ROOT / "README.md",
    *sorted(ROOT.glob("kapow*/README.md")),
    *sorted((ROOT / "apps/docs/docs").rglob("*.mdx")),
]
OUT = ROOT / "samples/docs-check/src/jvmTest/kotlin/generated"
FENCE = re.compile(r"^```kotlin[^\n]*\n(.*?)^```", re.S | re.M)


def package_name(source: Path, index: int) -> str:
    stem = re.sub(r"[^a-z0-9]+", "_", str(source.relative_to(ROOT).with_suffix("")).lower()).strip("_")
    return f"samples.{stem}.s{index}"


def main() -> None:
    shutil.rmtree(OUT, ignore_errors=True)
    OUT.mkdir(parents=True)
    count = 0
    for source in SOURCES:
        if not source.exists():
            continue
        blocks = [b for b in FENCE.findall(source.read_text()) if "import com.jantursky.kapow" in b]
        for index, block in enumerate(blocks, start=1):
            package = package_name(source, index)
            target = OUT / f"{package.replace('.', '_')}.kt"
            target.write_text(f"// From {source.relative_to(ROOT)}, sample {index}.\npackage {package}\n\n{block}")
            count += 1
    print(f"{count} samples written to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
