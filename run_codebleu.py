#!/usr/bin/env python3
"""Compare two Java files using CodeBLEU: reference vs. prediction."""

import argparse
from pathlib import Path

from codebleu import calc_codebleu


def main():
    parser = argparse.ArgumentParser(
        description="Compare two Java files with CodeBLEU (reference vs. prediction)."
    )
    parser.add_argument("reference", type=Path, help="Reference Java file (ground truth)")
    parser.add_argument("prediction", type=Path, help="Prediction Java file (to evaluate)")
    args = parser.parse_args()

    reference_path = args.reference
    prediction_path = args.prediction

    if not reference_path.exists():
        raise FileNotFoundError(f"Reference file not found: {reference_path}")

    if not prediction_path.exists():
        raise FileNotFoundError(f"Prediction file not found: {prediction_path}")

    reference = reference_path.read_text(encoding="utf-8", errors="replace")
    prediction = prediction_path.read_text(encoding="utf-8", errors="replace")

    result = calc_codebleu(
        references=[reference],
        predictions=[prediction],
        lang="java",
        weights=(0.25, 0.25, 0.25, 0.25),
        tokenizer=None,
    )

    print(result["codebleu"])
    print(result)


if __name__ == "__main__":
    main()
