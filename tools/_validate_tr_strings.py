#!/usr/bin/env python3
"""Compare EN vs TR string resources: names, dupes, placeholder multisets."""
from __future__ import annotations

import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EN = ROOT / "app" / "src" / "main" / "res" / "values" / "strings.xml"
TR = ROOT / "app" / "src" / "main" / "res" / "values-tr" / "strings.xml"

STRING_RE = re.compile(
    r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>', re.DOTALL
)


def parse(path: Path) -> dict[str, str]:
    text = path.read_text(encoding="utf-8")
    items = STRING_RE.findall(text)
    names = [n for n, _, _ in items]
    dupes = [n for n, c in Counter(names).items() if c > 1]
    if dupes:
        raise SystemExit(f"Duplicate keys in {path}: {dupes}")
    return {n: body for n, _, body in items}


def extract_placeholders(s: str) -> list[str]:
    """Extract conversion specs: %%, %s, %d, %1$d, %2$s, etc."""
    tokens: list[str] = []
    i = 0
    while i < len(s):
        if s[i] != "%":
            i += 1
            continue
        if i + 1 < len(s) and s[i + 1] == "%":
            tokens.append("%%")
            i += 2
            continue
        m = re.match(r"%(?:(\d+)\$)?([sdif])", s[i:])
        if m:
            pos, typ = m.group(1), m.group(2)
            tokens.append(f"%{pos + '$' if pos else ''}{typ}")
            i += m.end()
            continue
        i += 1
    return tokens


def normalize_multiset(tokens: list[str]) -> Counter:
    """Normalize %d and %1$d carefully into typed multiset."""
    c: Counter = Counter()
    for t in tokens:
        if t == "%%":
            c["%%"] += 1
            continue
        m = re.fullmatch(r"%(?:(\d+)\$)?([sdif])", t)
        if not m:
            c[t] += 1
            continue
        pos, typ = m.group(1), m.group(2)
        # Keep positional identity when present; else type-only bucket
        key = f"%{pos}${typ}" if pos else f"%{typ}"
        c[key] += 1
    return c


def main() -> int:
    en = parse(EN)
    tr = parse(TR)
    en_names = set(en)
    tr_names = set(tr)

    missing = sorted(en_names - tr_names)
    extra = sorted(tr_names - en_names)
    ph_mismatches = []

    for name in sorted(en_names & tr_names):
        en_ph = extract_placeholders(en[name])
        tr_ph = extract_placeholders(tr[name])
        # Sequence of conversion types must match (order + types)
        en_types = [
            "%%" if t == "%%" else re.fullmatch(r"%(?:\d+\$)?([sdif])", t).group(1)
            for t in en_ph
        ]
        tr_types = [
            "%%" if t == "%%" else re.fullmatch(r"%(?:\d+\$)?([sdif])", t).group(1)
            for t in tr_ph
        ]
        if en_types != tr_types or normalize_multiset(en_ph) != normalize_multiset(
            tr_ph
        ):
            # Allow positional reordering only when both use explicit positionals
            # and multisets of pos+type match; still require same type multiset
            if normalize_multiset(en_ph) != normalize_multiset(tr_ph) or Counter(
                en_types
            ) != Counter(tr_types):
                ph_mismatches.append(
                    {
                        "name": name,
                        "en": en_ph,
                        "tr": tr_ph,
                        "en_body": en[name],
                        "tr_body": tr[name],
                    }
                )

    header = TR.read_text(encoding="utf-8")[:80]
    header_ok = TR.read_text(encoding="utf-8").startswith(
        '<?xml version="1.0" encoding="utf-8"?>\n<resources>'
    )

    print(f"EN strings: {len(en)}")
    print(f"TR strings: {len(tr)}")
    print(f"Missing in TR: {missing}")
    print(f"Extra in TR: {extra}")
    print(f"Placeholder mismatches: {len(ph_mismatches)}")
    for m in ph_mismatches[:20]:
        print(f"  - {m['name']}: EN={m['en']} TR={m['tr']}")
    print(f"Header OK: {header_ok}")
    print(f"Header preview: {header!r}")

    ok = (
        len(en) == len(tr)
        and not missing
        and not extra
        and not ph_mismatches
        and header_ok
    )
    print("RESULT:", "PASS" if ok else "FAIL")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
