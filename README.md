# COBOL → Java Migration (Bachelorarbeit)

Kurze Begleitdokumentation zum Prototypen: **LLM-gestützte Migration von COBOL nach Java** mit **LangChain**, **Neo4j** (Strukturgraph) und **Chroma** (Vektorsuche). Das Modell arbeitet mit Tools (Shell, Graph, Retrieval) und erzeugt Java unter `workspace` (Paket `com.example.migration`).

## Architektur (Pipeline)

| Modul | Rolle |
|--------|--------|
| `1_ingestion.py` | Parser / AST-Extraktion (COBOL) |
| `2_knowledge.py` | Befüllung Neo4j + Chroma |
| `3_reasoning.py` | Orchestrierung, Agent, OpenAI |
| `4_verification.py` | Java-Kompilierung / Validierung |

Einstieg: `main.py` lädt die Reasoning-Pipeline (`3_reasoning.py`).

## Voraussetzungen

- Python 3.12+ (empfohlen), JDK für generierten Code
- **OpenAI API Key** (z. B. `OPENAI_API_KEY` in `.env`)
- **Neo4j** (lokal oder Docker)

## Schnellstart

```bash
python -m venv .venv && source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

Neo4j starten (Beispiel):

```bash
docker compose up -d neo4j
```

`.env` anlegen — Vorlage: `cp .env.example .env`, dann mindestens `OPENAI_API_KEY` setzen; Neo4j-URL/User/Passwort anpassen, falls abweichend.

Migration ausführen:

```bash
python main.py              # scope-basierte Migration
python main.py --direct     # ein Prompt pro Datei (Volltext → Volltext)
```

Optional: Modell über Umgebungsvariable `REASONING_MODEL` (Standard laut Code: `gpt-o4`).

## Weitere Skripte (Auswertung)

- `halstead_java.py` — Halstead-Metriken für Java
- `run_codebleu.py` — CodeBLEU-Vergleich
- `reset_neo4j.py` — Graph zurücksetzen (Vorsicht: Datenverlust)

Ergebnisse und Reports liegen typischerweise unter `results/` und `build/`.

## Docker Compose

Die Datei `docker-compose.yml` definiert zwei Services:

| Service | Standard | Zweck |
|---------|----------|--------|
| **neo4j** | startet mit `docker compose up` | Graphdatenbank (Browser: [http://localhost:7474](http://localhost:7474), Bolt `7687`) |
| **app** | Profil **`run`** | Einmaliger Lauf von `python main.py` im Container |

### Neo4j (empfohlener Weg)

1. `cp .env.example .env` und `OPENAI_API_KEY` (sowie bei Bedarf `NEO4J_USER` / `NEO4J_PASSWORD`) setzen — dieselben Werte nutzt der Neo4j-Container für `NEO4J_AUTH`.
2. Datenbank starten:

   ```bash
   docker compose up -d neo4j
   ```

3. Migration **auf dem Host** (venv wie oben), mit URL zum Container:

   ```bash
   export NEO4J_URL=neo4j://localhost:7687   # steht so auch in .env.example
   python main.py
   ```

So bleiben JDK, Tree-sitter-Grammatik (`cobol.so` / `.dylib`) und der gesamte Python-Code in der gewohnten lokalen Umgebung; Compose liefert nur Neo4j.

### App-Container (Profil `run`)

Der Service **app** ist hinter dem Profil **`run`** verborgen (startet nicht bei einem schlichten `docker compose up`). Er lädt zusätzlich `.env`, setzt u. a. `NEO4J_URL=neo4j://neo4j:7687` und hängt `./workspace` nach `/app/workspace` sowie ein Volume für Chroma unter `/app/chroma_store` ein.

**Einmal ausführen (Build + Lauf):**

```bash
docker compose --profile run run --rm app
```

**Alternativ** (Container im Vordergrund):

```bash
docker compose --profile run up --build app
```

**Hinweis zum mitgelieferten `Dockerfile`:** Es kopiert derzeit nur einen Teil des Repos. Für einen vollständigen Pipeline-Lauf im Container bräuchte das Image u. a. **JDK** (`javac` für `4_verification.py`), alle Pipeline-Dateien (`main.py`, `1_ingestion.py`, `1_extractor.py`, `2_knowledge.py`, `3_reasoning.py`, `4_verification.py`), den Ordner **`prompts/`**, **`tree-sitter-cobol/`** inkl. unter **Linux** gebauter Parser-Bibliothek (`cobol.so`). Solange das Image nicht entsprechend erweitert ist, ist **Neo4j per Compose + Python auf dem Host** die zuverlässige Variante.

## Datensätze

COBOL-Quellen und Testmaterial unter `dataset/` (u. a. Samples, Kursmaterial, Test-Suites).

---

*Hintergrund und Methodik: siehe die eingereichte Bachelorarbeit; dieses README beschreibt nur den Code-Stand des Repositories.*
