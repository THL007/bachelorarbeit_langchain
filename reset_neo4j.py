#!/usr/bin/env python3
"""Reset Neo4j database: delete all nodes and relationships."""

import os
import sys

from dotenv import load_dotenv
from neo4j import GraphDatabase

load_dotenv()

URI = os.getenv("NEO4J_URI", "neo4j://localhost:7687")
USER = os.getenv("NEO4J_USER", "neo4j")
PASSWORD = os.getenv("NEO4J_PASSWORD", "")


def reset():
    driver = GraphDatabase.driver(URI, auth=(USER, PASSWORD))
    try:
        with driver.session() as session:
            result = session.run("MATCH (n) DETACH DELETE n")
            summary = result.consume()
            deleted = (summary.counters.nodes_deleted if summary.counters else 0) or 0
        print(f"Neo4j reset: deleted {deleted} node(s).")
    finally:
        driver.close()


if __name__ == "__main__":
    try:
        reset()
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)
