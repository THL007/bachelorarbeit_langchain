#!/usr/bin/env python3
"""
Aggregate Halstead metrics over Java sources using javalang's lexer.

Operator / operand split (documented, consistent across corpora):
- Operators: Operator tokens, Separator tokens, Keyword tokens except this/super.
- Operands: Identifier, all Literal subclasses, keywords this and super.

Difficulty D = (eta1 / 2) * (N2 / eta2)
Volume   V = (N1 + N2) * log2(eta1 + eta2)
Effort   E = D * V

(eta1 = distinct operators, eta2 = distinct operands, N1/N2 = total occurrences.)
"""

from __future__ import annotations

import argparse
import math
import sys
from collections import Counter
from pathlib import Path

from javalang.tokenizer import (
    Annotation,
    EndOfInput,
    Identifier,
    JavaToken,
    Keyword,
    Literal,
    Operator,
    Separator,
    tokenize,
)

OPERAND_KEYWORDS = frozenset({"this", "super"})


def classify_token(tok: JavaToken) -> tuple[str, str] | None:
    if isinstance(tok, EndOfInput):
        return None
    if isinstance(tok, Literal):
        return "opnd", f"lit:{type(tok).__name__}:{tok.value!r}"
    if isinstance(tok, Identifier):
        return "opnd", f"id:{tok.value}"
    if isinstance(tok, Keyword):
        if tok.value in OPERAND_KEYWORDS:
            return "opnd", f"kw:{tok.value}"
        return "op", f"kw:{tok.value}"
    if isinstance(tok, Operator):
        return "op", f"sym:{tok.value}"
    if isinstance(tok, Separator):
        return "op", f"sep:{tok.value}"
    if isinstance(tok, Annotation):
        return "op", "@"
    return None


def analyze_source(code: str) -> tuple[Counter, Counter]:
    op = Counter()
    opnd = Counter()
    try:
        tokens = list(tokenize(code, ignore_errors=True))
    except Exception:
        return op, opnd
    for tok in tokens:
        c = classify_token(tok)
        if c is None:
            continue
        kind, key = c
        if kind == "op":
            op[key] += 1
        else:
            opnd[key] += 1
    return op, opnd


def aggregate(root: Path) -> tuple[Counter, Counter, list[str]]:
    total_op = Counter()
    total_opnd = Counter()
    errors: list[str] = []
    for path in sorted(root.rglob("*.java")):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError as e:
            errors.append(f"{path}: {e}")
            continue
        o, od = analyze_source(text)
        total_op += o
        total_opnd += od
    return total_op, total_opnd, errors


def halstead_numbers(op: Counter, opnd: Counter) -> dict[str, float]:
    n1 = len(op)
    n2 = len(opnd)
    N1 = sum(op.values())
    N2 = sum(opnd.values())
    eta = n1 + n2
    N = N1 + N2
    if eta <= 0:
        return {
            "eta1": 0,
            "eta2": 0,
            "N1": 0,
            "N2": 0,
            "N": 0,
            "volume": 0.0,
            "difficulty": 0.0,
            "effort": 0.0,
        }
    volume = N * math.log2(eta) if eta > 0 else 0.0
    difficulty = (n1 / 2.0) * (N2 / n2) if n2 > 0 else 0.0
    effort = difficulty * volume
    return {
        "eta1": n1,
        "eta2": n2,
        "N1": N1,
        "N2": N2,
        "N": N,
        "volume": volume,
        "difficulty": difficulty,
        "effort": effort,
    }


def format_report(name: str, root: Path, m: dict[str, float], errors: list[str]) -> str:
    lines = [
        "=" * 72,
        f"Halstead metrics (tokenizer-based) — {name}",
        "=" * 72,
        f"Root: {root}",
        "",
        "Distinct operators (eta1):     %d" % m["eta1"],
        "Distinct operands (eta2):      %d" % m["eta2"],
        "Total operator occurrences N1: %d" % m["N1"],
        "Total operand occurrences N2:  %d" % m["N2"],
        "Program length N = N1 + N2:    %d" % m["N"],
        "",
        "Volume V = N * log2(eta1 + eta2):     %.2f" % m["volume"],
        "Difficulty D = (eta1/2) * (N2/eta2):  %.2f" % m["difficulty"],
        "Effort E = D * V:                     %.2f" % m["effort"],
        "",
        "Notes:",
        "- Operators: javalang Operator + Separator + keywords (except this/super).",
        "- Operands: identifiers, literals, this, super.",
        "- Comparable across folders only under this same definition.",
    ]
    if errors:
        lines.extend(["", "Read/token issues (%d):" % len(errors)] + errors[:20])
        if len(errors) > 20:
            lines.append("...")
    lines.append("")
    return "\n".join(lines)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("roots", nargs="+", type=Path, help="Directories of Java sources")
    ap.add_argument(
        "-o",
        "--output-dir",
        type=Path,
        default=Path("results"),
        help="Where to write halstead_<foldername>.txt",
    )
    args = ap.parse_args()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    summaries = []
    for root in args.roots:
        if not root.is_dir():
            print(f"Skip (not a directory): {root}", file=sys.stderr)
            continue
        op, opnd, err = aggregate(root.resolve())
        m = halstead_numbers(op, opnd)
        label = root.name
        text = format_report(label, root.resolve(), m, err)
        out = args.output_dir / f"halstead_{label}.txt"
        out.write_text(text, encoding="utf-8")
        print(text)
        summaries.append((label, m))
    if len(summaries) == 2:
        (a, ma), (b, mb) = summaries
        cmp_path = args.output_dir / "halstead_comparison.txt"
        cmp_path.write_text(
            "\n".join(
                [
                    "Halstead comparison (same methodology)",
                    "",
                    f"{'Metric':<20} {a:<22} {b}",
                    f"{'Difficulty D':<20} {ma['difficulty']:<22.2f} {mb['difficulty']:.2f}",
                    f"{'Volume V':<20} {ma['volume']:<22.2f} {mb['volume']:.2f}",
                    f"{'Effort E':<20} {ma['effort']:<22.2f} {mb['effort']:.2f}",
                    f"{'eta1':<20} {int(ma['eta1']):<22d} {int(mb['eta1'])}",
                    f"{'eta2':<20} {int(ma['eta2']):<22d} {int(mb['eta2'])}",
                    f"{'N (N1+N2)':<20} {int(ma['N']):<22d} {int(mb['N'])}",
                    "",
                ]
            ),
            encoding="utf-8",
        )
        print(cmp_path.read_text(encoding="utf-8"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
