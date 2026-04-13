"""
Thin entrypoint.

Architecture:
1_ingestion.py   -> parser engine and AST tree extraction
2_knowledge.py   -> Neo4j graph + Chroma vector store
3_reasoning.py   -> LangChain orchestration + GPT-o4
4_verification.py-> Java validation engine
"""

from __future__ import annotations

import importlib.util
import sys
from pathlib import Path


def _load_reasoning_module():
    module_path = Path(__file__).resolve().parent / "3_reasoning.py"
    spec = importlib.util.spec_from_file_location("reasoning_module", module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Could not load reasoning module from {module_path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules["reasoning_module"] = module
    spec.loader.exec_module(module)
    return module


def main() -> None:
    _load_reasoning_module()._cli()


if __name__ == "__main__":
    main()
