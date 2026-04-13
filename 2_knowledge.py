"""
Knowledge layer:
- Graph database: Neo4j
- Vector store: ChromaDB
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, List, Optional

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_neo4j import Neo4jGraph
from langchain_openai import OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter


def _node_id_to_name(node_id: str) -> str:
    return node_id.split(":", 1)[-1] if ":" in node_id else node_id


def _read_workspace_text(path: Path) -> str:
    """Read workspace file text; COBOL sources may be UTF-8, Latin-1, or Windows-1252."""
    data = path.read_bytes()
    if not data:
        return ""
    for encoding in (
        "utf-8",
        "utf-8-sig",
        "utf-16",
        "utf-16-le",
        "utf-16-be",
        "cp1252",
    ):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    # Any byte sequence is valid in Latin-1 (legacy mainframe/PC exports).
    return data.decode("iso-8859-1")


@dataclass
class KnowledgeConfig:
    workspace_dir: Path
    chroma_dir: Path
    chroma_collection: str
    embedding_model: str
    neo4j_url: str
    neo4j_user: str
    neo4j_password: str

    @classmethod
    def from_env(cls, workspace_dir: Path) -> "KnowledgeConfig":
        return cls(
            workspace_dir=workspace_dir,
            chroma_dir=Path(os.environ.get("CHROMA_DIR", "./chroma_store")),
            chroma_collection=os.environ.get("CHROMA_COLLECTION", "migration_store"),
            embedding_model=os.environ.get("EMBEDDING_MODEL", "text-embedding-3-large"),
            neo4j_url=os.environ.get("NEO4J_URL", "neo4j://localhost:7687"),
            neo4j_user=os.environ.get("NEO4J_USER", "neo4j"),
            neo4j_password=os.environ.get("NEO4J_PASSWORD", "0123456789"),
        )


class KnowledgeStores:
    def __init__(self, config: KnowledgeConfig):
        self.config = config
        self.embeddings = OpenAIEmbeddings(model=config.embedding_model)
        self.vectorstore = Chroma(
            embedding_function=self.embeddings,
            persist_directory=str(config.chroma_dir),
            collection_name=config.chroma_collection,
        )
        self.graph = Neo4jGraph(
            url=config.neo4j_url,
            username=config.neo4j_user,
            password=config.neo4j_password,
            refresh_schema=False,
        )

    def index_file(self, filepath: str, source_type: str = "file") -> None:
        safe_path = self.config.workspace_dir / filepath
        if not safe_path.exists() or not safe_path.is_file():
            return
        content = _read_workspace_text(safe_path)

        doc = Document(page_content=content, metadata={"source": filepath, "type": source_type})
        splitter = RecursiveCharacterTextSplitter(chunk_size=1200, chunk_overlap=120)
        chunks = splitter.split_documents([doc])
        if chunks:
            self.vectorstore.add_documents(chunks)

        self.graph.query("MATCH (n {source: $source}) DETACH DELETE n", params={"source": filepath})
        self.graph.query(
            "CREATE (n:File {source: $source, type: $type, content: $content})",
            params={"source": filepath, "type": source_type, "content": content[:1000]},
        )

    def delete_index_for_file(self, filepath: str) -> None:
        try:
            self.vectorstore.delete(ids=None, where={"source": filepath})
        except Exception:
            pass
        self.graph.query("MATCH (n {source: $source}) DETACH DELETE n", params={"source": filepath})

    def reindex_file(self, filepath: str) -> None:
        self.delete_index_for_file(filepath)
        if filepath.startswith("input/"):
            source_type = "input_source"
        elif filepath.startswith("output/"):
            source_type = "output_source"
        else:
            source_type = "workspace_file"
        self.index_file(filepath, source_type)

    def ingest_texts(self, texts: List[str], source: str = "seed") -> None:
        docs = [Document(page_content=t, metadata={"source": source}) for t in texts]
        splitter = RecursiveCharacterTextSplitter(chunk_size=1200, chunk_overlap=120)
        chunks = splitter.split_documents(docs)
        if chunks:
            self.vectorstore.add_documents(chunks)

    def chroma_search(self, query: str, k: int = 6) -> List[Dict[str, Any]]:
        retriever = self.vectorstore.as_retriever(search_type="mmr", search_kwargs={"k": k})
        results = retriever.invoke(query)
        return [{"text": d.page_content, "meta": d.metadata} for d in results]

    def neo4j_function_usage(self, func_name: str) -> List[Dict[str, Any]]:
        cypher = """
        MATCH (f:Function {name: $func})
        OPTIONAL MATCH (parent:Function)-[:HAS_DIVISION|HAS_SECTION|HAS_PARAGRAPH]->(f)
        OPTIONAL MATCH (f)-[:HAS_DIVISION|HAS_SECTION|HAS_PARAGRAPH]->(child:Function)
        OPTIONAL MATCH (f)-[:READS]->(v:Variable)
        RETURN f.name AS function, f.file AS file, f.type AS type,
               [x IN collect(DISTINCT parent.name) WHERE x IS NOT NULL] AS containers,
               [x IN collect(DISTINCT child.name) WHERE x IS NOT NULL] AS contained,
               [x IN collect(DISTINCT v.name) WHERE x IS NOT NULL] AS reads
        """
        return self.graph.query(cypher, params={"func": func_name})

    def sync_graph_to_neo4j(
        self,
        nodes: List[Dict[str, Any]],
        relationships: List[Dict[str, Any]],
        file_path: str,
    ) -> None:
        for n in nodes:
            if n.get("type") not in ("Program", "Division", "Section", "Paragraph"):
                continue
            name = n.get("name") or _node_id_to_name(n.get("id", ""))
            t = n.get("type")
            if t == "Program" and (n.get("file") or "").lower().endswith(".cpy"):
                graph_type = "copybook"
            else:
                graph_type = (
                    "program"
                    if t == "Program"
                    else "division"
                    if t == "Division"
                    else "paragraph"
                    if t == "Paragraph"
                    else "section"
                )
            self.graph.query(
                """
                MERGE (f:Function {name: $name, file: $file})
                SET f.type = $type
                """,
                params={"name": name, "file": file_path, "type": graph_type},
            )

        for n in nodes:
            if n.get("type") != "Copybook":
                continue
            name = n.get("name") or _node_id_to_name(n.get("id", ""))
            self.graph.query(
                """
                MERGE (c:Copybook {name: $name})
                SET c.file = $file
                """,
                params={"name": name, "file": file_path},
            )

        for n in nodes:
            if n.get("type") != "Variable":
                continue
            name = n.get("name") or _node_id_to_name(n.get("id", ""))
            self.graph.query(
                """
                MERGE (v:Variable {name: $name, file: $file})
                SET v.level = $level, v.picture = $picture
                """,
                params={
                    "name": name,
                    "file": file_path,
                    "level": n.get("level") or "",
                    "picture": n.get("picture") or "",
                },
            )

        rel_table = {
            "HAS_PARAGRAPH": ("Function", "Function"),
            "HAS_SECTION": ("Function", "Function"),
            "HAS_DIVISION": ("Function", "Function"),
            "READS": ("Function", "Variable"),
            "MODIFIES": ("Function", "Variable"),
            "CONTAINS": ("Variable", "Variable"),
            "REDEFINES": ("Variable", "Variable"),
            "FEEDS": ("Variable", "Variable"),
            "COPIES": ("Function", "Copybook"),
            "DEFINES": ("Copybook", "Variable"),
        }
        standard_rel_query = """
            MERGE (a:{from_label} {{name: $from_name, file: $file}})
            WITH a
            MERGE (b:{to_label} {{name: $to_name, file: $file}})
            WITH a, b
            MERGE (a)-[:{rel_type}]->(b)
        """

        for rel in relationships:
            rel_type = rel.get("type")
            from_id = rel.get("from", "")
            to_id = rel.get("to", "")
            from_name = _node_id_to_name(from_id.split(":stmt:")[0] if ":stmt:" in from_id else from_id)
            to_name = _node_id_to_name(to_id)
            if not from_name or not to_name:
                continue
            if rel_type == "CALLS":
                if ":stmt:" not in from_id:
                    continue
                self.graph.query(
                    """
                    MERGE (a:Function {name: $caller, file: $file})
                    WITH a
                    MERGE (b:Function {name: $callee, file: $file})
                    WITH a, b
                    MERGE (a)-[:CALLS]->(b)
                    """,
                    params={"caller": from_name, "callee": to_name, "file": file_path},
                )
                continue
            if rel_type == "COPIES":
                default_cpy = to_name + ".cpy" if not (to_name or "").lower().endswith(".cpy") else to_name
                self.graph.query(
                    """
                    MERGE (a:Function {name: $from_name, file: $file})
                    WITH a
                    MERGE (b:Copybook {name: $to_name})
                    ON CREATE SET b.file = $default_cpy
                    WITH a, b
                    MERGE (a)-[:COPIES]->(b)
                    """,
                    params={
                        "from_name": from_name,
                        "to_name": to_name,
                        "file": file_path,
                        "default_cpy": default_cpy,
                    },
                )
                continue
            if rel_type not in rel_table:
                continue
            from_label, to_label = rel_table[rel_type]
            q = standard_rel_query.format(
                from_label=from_label, to_label=to_label, rel_type=rel_type
            ).strip()
            self.graph.query(q, params={"from_name": from_name, "to_name": to_name, "file": file_path})
