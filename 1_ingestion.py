"""
Ingestion layer:
- parser_engine: initialize and validate COBOL parser
- tree: parse COBOL files and expose AST/scope graph extraction APIs

This module is intentionally a facade over the legacy extractor implementation,
so behavior remains stable while the architecture is split into 4 stages.
"""

from __future__ import annotations

import importlib.util
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple


@dataclass
class Scope:
    file_path: str
    name: str
    scope_type: str
    start_byte: int
    end_byte: int
    code_snippet: Optional[str] = None


class ParserEngine:
    """Small parser facade requested by the new architecture."""

    def __init__(self) -> None:
        self._mod = _load_legacy_extractor()

    def get_parser(self):
        return self._mod.get_parser()

    def get_tree(self, file_path: str):
        return self._mod.get_tree(file_path)

    def validate(self) -> None:
        # Validates parser loading and grammar availability.
        self.get_parser()


def _load_legacy_extractor():
    extractor_path = Path(__file__).resolve().parent / "1_extractor.py"
    spec = importlib.util.spec_from_file_location("legacy_extractor", extractor_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Could not load extractor module from {extractor_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def get_parser():
    return _load_legacy_extractor().get_parser()


def get_tree(file_path: str):
    return _load_legacy_extractor().get_tree(file_path)


def save_ast_visualization(file_path: str, tree: Any, code_bytes: bytes) -> None:
    return _load_legacy_extractor().save_ast_visualization(file_path, tree, code_bytes)


def extract_graph_from_tree(tree: Any, code_bytes: bytes, file_path: Optional[str] = None):
    return _load_legacy_extractor().extract_graph_from_tree(tree, code_bytes, file_path)


def get_scope_dependencies_from_relationships(
    relationships: List[Dict[str, Any]],
) -> Dict[str, List[str]]:
    return _load_legacy_extractor().get_scope_dependencies_from_relationships(relationships)


def get_scopes_migration_order(tree: Any, code_bytes: bytes, file_path: str):
    return _load_legacy_extractor().get_scopes_migration_order(tree, code_bytes, file_path)


def extract_cobol_graph(file_path: str):
    return _load_legacy_extractor().extract_cobol_graph(file_path)


def extract_from_path(path: str) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    return _load_legacy_extractor().extract_from_path(path)
