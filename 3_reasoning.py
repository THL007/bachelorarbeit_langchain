"""
Reasoning layer:
- Agent orchestration
- Inference engine (LangChain + GPT-o4)
"""

from __future__ import annotations

import argparse
import importlib.util
import os
import sys
import re
import subprocess
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

import tiktoken
from dotenv import load_dotenv
from langchain_classic.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.callbacks import BaseCallbackHandler
from langchain_core.outputs import LLMResult
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI

load_dotenv()


def _load_module(file_name: str, module_name: str):
    module_path = Path(__file__).resolve().parent / file_name
    spec = importlib.util.spec_from_file_location(module_name, module_path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Could not load module {file_name}")
    module = importlib.util.module_from_spec(spec)
    # Required before exec_module: dataclasses look up sys.modules[cls.__module__].
    sys.modules[module_name] = module
    spec.loader.exec_module(module)
    return module


ingestion = _load_module("1_ingestion.py", "ingestion_module")
knowledge_mod = _load_module("2_knowledge.py", "knowledge_module")
verification_mod = _load_module("4_verification.py", "verification_module")

KnowledgeConfig = knowledge_mod.KnowledgeConfig
KnowledgeStores = knowledge_mod.KnowledgeStores
JavaValidationEngine = verification_mod.JavaValidationEngine


class TiktokenUsageCallbackHandler(BaseCallbackHandler):
    def __init__(self, model: str = "gpt-o4"):
        super().__init__()
        self.model = model
        try:
            self._encoding = tiktoken.encoding_for_model(model)
        except KeyError:
            self._encoding = tiktoken.get_encoding("cl100k_base")
        self.total_prompt_tokens = 0
        self.total_completion_tokens = 0

    def _count_tokens(self, text: str) -> int:
        return len(self._encoding.encode(text or ""))

    def on_llm_start(self, serialized: Dict[str, Any], prompts: List[str], **kwargs: Any) -> None:
        for prompt in prompts:
            self.total_prompt_tokens += self._count_tokens(prompt)

    def on_llm_end(self, response: LLMResult, **kwargs: Any) -> None:
        for gen_list in response.generations:
            for gen in gen_list:
                text = getattr(gen, "text", None) or (
                    getattr(gen.message, "content", None) if getattr(gen, "message", None) else None
                )
                if text and isinstance(text, str):
                    self.total_completion_tokens += self._count_tokens(text)

    def summary(self) -> Dict[str, int]:
        total = self.total_prompt_tokens + self.total_completion_tokens
        return {
            "prompt_tokens": self.total_prompt_tokens,
            "completion_tokens": self.total_completion_tokens,
            "total_tokens": total,
        }


class PromptManager:
    def __init__(self, directory: Optional[Path] = None):
        self.directory = directory or (Path(__file__).resolve().parent / "prompts")
        self._cache: Dict[str, str] = {}

    def get(self, name: str) -> str:
        if name in self._cache:
            return self._cache[name]
        path = self.directory / f"{name}.txt"
        if not path.exists():
            raise FileNotFoundError(f"Prompt not found: {path}")
        text = path.read_text(encoding="utf-8")
        self._cache[name] = text
        return text

    def get_formatted(self, name: str, **kwargs: Any) -> str:
        return self.get(name).format(**kwargs)


class MigrationReasoningEngine:
    _ALLOWED_COMMANDS = frozenset(
        {
            "ls",
            "tree",
            "find",
            "cat",
            "head",
            "tail",
            "nl",
            "rg",
            "grep",
            "wc",
            "file",
            "diff",
            "sed",
            "echo",
            "printf",
            "touch",
            "mkdir",
            "rm",
            "cp",
            "mv",
            "javac",
            "java",
        }
    )

    def __init__(self) -> None:
        self.workspace_dir = Path("./workspace").resolve()
        self.workspace_dir.mkdir(exist_ok=True)
        self.input_dir = self.workspace_dir / "input"
        self.input_dir.mkdir(exist_ok=True)
        self.output_dir = self.workspace_dir / "output"
        self.output_dir.mkdir(exist_ok=True)

        self.prompt_manager = PromptManager()
        self.parser_engine = ingestion.ParserEngine()
        self.knowledge = KnowledgeStores(KnowledgeConfig.from_env(self.workspace_dir))
        self.validator = JavaValidationEngine(self.output_dir)

        model_name = os.environ.get("REASONING_MODEL", "gpt-o4")
        self.llm = ChatOpenAI(model=model_name)

    def _run_command(self, command: str) -> Dict[str, Any]:
        cmd_stripped = command.strip()
        if not cmd_stripped:
            return {"stdout": "", "stderr": "Error: empty command.", "returncode": -1}
        first = cmd_stripped.split()[0] if cmd_stripped.split() else ""
        if first not in self._ALLOWED_COMMANDS:
            return {
                "stdout": "",
                "stderr": f"Error: command not allowed. Allowed: {', '.join(sorted(self._ALLOWED_COMMANDS))}.",
                "returncode": -1,
            }
        if first == "sed" and " -i " in cmd_stripped:
            cmd_stripped = re.sub(r"\bsed\s+-i\s+(\")", r"sed -i '' \1", cmd_stripped, count=1)
            cmd_stripped = re.sub(r"\bsed\s+-i\s+(')", r"sed -i '' \1", cmd_stripped, count=1)
        try:
            proc = subprocess.run(
                cmd_stripped,
                cwd=str(self.workspace_dir),
                shell=True,
                capture_output=True,
                text=True,
                timeout=60,
            )
            return {"stdout": proc.stdout or "", "stderr": proc.stderr or "", "returncode": proc.returncode}
        except subprocess.TimeoutExpired:
            return {"stdout": "", "stderr": "Command timed out (60s).", "returncode": -1}
        except Exception as exc:
            return {"stdout": "", "stderr": str(exc), "returncode": -1}

    def _list_cobol_files(self) -> List[Tuple[str, str]]:
        cobol_files: List[Tuple[str, str]] = []
        for root, _, files in os.walk(self.input_dir):
            for f in files:
                if f.lower().endswith((".cbl", ".cob", ".cobol", ".cpy")):
                    cobol_files.append((root, f))
        cobol_files.sort(key=lambda x: os.path.join(x[0], x[1]))
        return cobol_files

    def _reindex_output_java(self) -> None:
        for root, _, files in os.walk(self.output_dir):
            for f in files:
                if f.endswith(".java"):
                    rel_out = os.path.relpath(os.path.join(root, f), self.workspace_dir)
                    self.knowledge.reindex_file(rel_out)

    def _compile_and_fix_errors(
        self,
        agent_executor: AgentExecutor,
        run_config: Dict[str, Any],
        max_retries: int,
    ) -> None:
        for _ in range(max_retries):
            ok, errors = self.validator.compile_java()
            if ok:
                break
            self._reindex_output_java()
            errors_str = "\n".join([f"  {e.get('file')}:{e.get('line')}: {e.get('message')}" for e in errors])
            fix_input = self.prompt_manager.get_formatted("fix_errors", errors_str=errors_str)
            agent_executor.invoke({"input": fix_input}, config=run_config)

    def _build_agent(self) -> AgentExecutor:
        @tool
        def tool_chroma_search(query: str, k: int = 6) -> List[Dict[str, Any]]:
            """Search migration patterns and examples relevant to a COBOL snippet or question."""
            return self.knowledge.chroma_search(query, k)

        @tool
        def tool_neo4j_function_usage(func_name: str) -> List[Dict[str, Any]]:
            """Query Neo4j for a COBOL function and nearby usage context."""
            return self.knowledge.neo4j_function_usage(func_name)

        @tool
        def run_command(command: str) -> Dict[str, Any]:
            """Run a shell command in workspace for discovery/edit/compile with allowlist guard."""
            return self._run_command(command)

        tools = [run_command, tool_chroma_search, tool_neo4j_function_usage]

        try:
            system_prompt = self.prompt_manager.get("system")
        except FileNotFoundError:
            system_prompt = (
                "You are a migration assistant. Use tools to inspect/edit files and compile Java. "
                "Always call retrieval tools before edits and iterate until compilation is clean."
            )
        prompt = ChatPromptTemplate.from_messages(
            [
                ("system", system_prompt),
                ("placeholder", "{chat_history}"),
                ("human", "{input}"),
                ("placeholder", "{agent_scratchpad}"),
            ]
        )
        agent = create_tool_calling_agent(self.llm, tools, prompt)
        return AgentExecutor(agent=agent, tools=tools, verbose=True)

    def run_migration_orchestrator(
        self,
        max_compile_retries: int = 5,
        vector_search_k: int = 6,
        callbacks: Optional[List[BaseCallbackHandler]] = None,
        use_direct_mode: bool = False,
    ) -> str:
        agent_executor = self._build_agent()
        run_config: Dict[str, Any] = {"callbacks": callbacks} if callbacks else {}

        self.parser_engine.validate()

        for root, _, files in os.walk(self.input_dir):
            for f in files:
                rel = os.path.relpath(os.path.join(root, f), self.workspace_dir)
                self.knowledge.reindex_file(rel)
        for root, _, files in os.walk(self.output_dir):
            for f in files:
                rel = os.path.relpath(os.path.join(root, f), self.workspace_dir)
                self.knowledge.reindex_file(rel)

        if not use_direct_mode:
            for root, _, files in os.walk(self.input_dir):
                for f in files:
                    if not f.lower().endswith((".cbl", ".cob", ".cobol", ".cpy")):
                        continue
                    full = Path(root) / f
                    rel = os.path.relpath(full, self.workspace_dir)
                    try:
                        tree, code_bytes = ingestion.get_tree(str(full))
                        nodes, rels = ingestion.extract_graph_from_tree(tree, code_bytes, str(full))
                        self.knowledge.sync_graph_to_neo4j(nodes, rels, rel)
                    except Exception:
                        continue

        try:
            hints = self.prompt_manager.get("hints")
        except FileNotFoundError:
            hints = "Use package com.example.migration. DISPLAY -> System.out.println; STOP RUN -> return."

        if use_direct_mode:
            cobol_files = self._list_cobol_files()
            for root, f in cobol_files:
                full = Path(root) / f
                rel_path = os.path.relpath(full, self.workspace_dir)
                content = full.read_text(encoding="utf-8")

                query = f"{rel_path}\n{content[:800]}"
                related_docs = self.knowledge.vectorstore.as_retriever(
                    search_type="mmr", search_kwargs={"k": vector_search_k}
                ).invoke(query)
                related_text = "\n".join([d.page_content for d in related_docs]) if related_docs else "None."

                input_text = self.prompt_manager.get_formatted(
                    "migration_direct",
                    rel_path=rel_path,
                    related_text=related_text,
                    hints=hints,
                )
                agent_executor.invoke({"input": input_text}, config=run_config)

                self._compile_and_fix_errors(agent_executor, run_config, max_compile_retries)
                self._reindex_output_java()
            return "Migration orchestrator finished (direct mode)."

        cobol_files = self._list_cobol_files()
        scope_work_list: List[Tuple[str, Any, Dict[str, List[str]]]] = []
        for root, f in cobol_files:
            full = Path(root) / f
            rel = os.path.relpath(full, self.workspace_dir)
            try:
                tree, code_bytes = ingestion.get_tree(str(full))
                _, rels = ingestion.extract_graph_from_tree(tree, code_bytes, str(full))
                deps = ingestion.get_scope_dependencies_from_relationships(rels)
                scopes = ingestion.get_scopes_migration_order(tree, code_bytes, str(full))
                for scope in scopes:
                    scope_work_list.append((rel, scope, deps))
            except Exception:
                continue

        for rel_path, scope, deps in scope_work_list:
            dep_names = deps.get(scope.name, [])
            deps_text = "Dependencies: " + ", ".join(dep_names) if dep_names else "Dependencies: none"

            query = f"{scope.name} {scope.scope_type}\n{(scope.code_snippet or '')[:500]}"
            related_docs = self.knowledge.vectorstore.as_retriever(
                search_type="mmr", search_kwargs={"k": vector_search_k}
            ).invoke(query)
            related_text = "\n".join([d.page_content for d in related_docs])

            snippet = scope.code_snippet or ""
            input_text = self.prompt_manager.get_formatted(
                "migration_scope",
                scope_name=scope.name,
                scope_type=scope.scope_type,
                rel_path=rel_path,
                deps_text=deps_text,
                scope_code=snippet,
                source_code=snippet,
                related_text=related_text,
                hints=hints,
            )
            agent_executor.invoke({"input": input_text}, config=run_config)
            self._compile_and_fix_errors(agent_executor, run_config, max_compile_retries)
            self._reindex_output_java()

        return "Migration orchestrator finished."


def run_migration_orchestrator(
    max_compile_retries: int = 5,
    vector_search_k: int = 6,
    callbacks: Optional[List[BaseCallbackHandler]] = None,
    use_direct_mode: bool = False,
) -> str:
    engine = MigrationReasoningEngine()
    try:
        return engine.run_migration_orchestrator(
            max_compile_retries=max_compile_retries,
            vector_search_k=vector_search_k,
            callbacks=callbacks,
            use_direct_mode=use_direct_mode,
        )
    finally:
        engine.validator.delete_class_files()


def _cli() -> None:
    parser = argparse.ArgumentParser(description="Run COBOL-to-Java migration.")
    parser.add_argument(
        "--direct",
        action="store_true",
        help="Direct mode: one prompt per file (full input -> full output), no scopes.",
    )
    args = parser.parse_args()

    token_handler = TiktokenUsageCallbackHandler(model=os.environ.get("REASONING_MODEL", "gpt-o4"))
    start_time = time.perf_counter()
    result = run_migration_orchestrator(
        max_compile_retries=5,
        vector_search_k=6,
        callbacks=[token_handler],
        use_direct_mode=args.direct,
    )
    elapsed = time.perf_counter() - start_time
    usage = token_handler.summary()
    print(result)
    print("\n--- Migration stats (tiktoken) ---")
    print(f"  Prompt tokens:     {usage['prompt_tokens']:,}")
    print(f"  Completion tokens: {usage['completion_tokens']:,}")
    print(f"  Total tokens:      {usage['total_tokens']:,}")
    print(f"  Time taken:        {elapsed:.2f} s")


if __name__ == "__main__":
    _cli()
