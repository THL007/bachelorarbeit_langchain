"""
COBOL Extractor - Parses COBOL files and extracts AST structure
Handles tree parsing and visualization
"""

import os
import time
import ctypes
from dataclasses import dataclass
from typing import List, Dict, Optional

from tree_sitter import Language, Parser


@dataclass
class Scope:
    """A COBOL scope (paragraph or section) with byte range and optional code snippet."""
    file_path: str
    name: str
    scope_type: str  # "paragraph" | "section"
    start_byte: int
    end_byte: int
    code_snippet: Optional[str] = None


# Path to the tree-sitter-cobol grammar directory (same directory as this script)
GRAMMAR_DIR = os.path.join(os.path.dirname(__file__), "tree-sitter-cobol")


def get_parser():
    """
    Initialize and return a COBOL parser
    
    Returns:
        Parser instance configured for COBOL
    """
    if not os.path.exists(GRAMMAR_DIR):
        raise FileNotFoundError(
            f"COBOL grammar not found at {GRAMMAR_DIR}. "
            "Run setup_grammar.py first."
        )
    
    # Find the compiled library (.dylib on macOS, .so on Linux, .dll on Windows)
    lib_extensions = ['.dylib', '.so', '.dll']
    lib_path = None
    
    for ext in lib_extensions:
        potential_path = os.path.join(GRAMMAR_DIR, f"cobol{ext}")
        if os.path.exists(potential_path):
            lib_path = potential_path
            break
    
    if not lib_path:
        raise FileNotFoundError(
            f"COBOL parser library not found in {GRAMMAR_DIR}. "
            "Run 'python setup_grammar.py' to build it."
        )
    
    # Load the library and get the language function
    lib = ctypes.CDLL(lib_path)
    language_func = lib.tree_sitter_COBOL
    language_func.restype = ctypes.c_void_p
    
    # Create Language object from the pointer
    cobol_language = Language(language_func())
    
    # Create and configure parser
    parser = Parser()
    parser.language = cobol_language
    
    return parser


def get_tree(file_path: str):
    """
    Parse a COBOL file and return the tree-sitter tree
    
    Args:
        file_path: Path to COBOL source file
        
    Returns:
        Tuple of (tree, code_bytes)
        - tree: tree-sitter Tree object
        - code_bytes: bytes content of the file (needed for tree operations)
    """
    parser = get_parser()
    
    with open(file_path, "rb") as f:
        code_bytes = f.read()

    tree = parser.parse(code_bytes)
    return tree, code_bytes


def _node_id(node):
    """Generate unique node ID for GraphViz"""
    return f'node_{id(node)}'


def _escape_label(label):
    """Escape special characters in GraphViz labels"""
    return label.replace('"', '\\"').replace('\n', '\\n')


def ast_to_dot(node, code_bytes):
    """
    Convert AST to GraphViz dot format
    
    Args:
        node: tree-sitter root node
        code_bytes: bytes content of the file
        
    Returns:
        String in GraphViz dot format
    """
    dot = []
    
    def walk(n):
        nlabel = _escape_label(n.type)
        if n.child_count == 0:
            snippet = code_bytes[n.start_byte:n.end_byte].decode('utf-8', errors='ignore').strip()
            if snippet:
                nlabel += f': {_escape_label(snippet)}'
        dot.append(f'{_node_id(n)} [label="{nlabel}"];')
        for c in n.children:
            dot.append(f'{_node_id(n)} -> {_node_id(c)};')
            walk(c)
    
    dot.append('digraph AST {')
    walk(node)
    dot.append('}')
    return '\n'.join(dot)


def save_ast_visualization(file_path: str, tree, code_bytes):
    """
    Save AST visualization to a dot file
    
    Args:
        file_path: Path to COBOL source file
        tree: tree-sitter Tree object
        code_bytes: bytes content of the file
    """
    timestamp = time.strftime("%Y%m%d_%H%M%S")
    base_name = os.path.basename(file_path)
    base_no_ext = os.path.splitext(base_name)[0]
    out_dir = os.path.join("output", "ast")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, f"{timestamp}_{base_no_ext}.dot")

    dot_content = ast_to_dot(tree.root_node, code_bytes)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(dot_content)

    print(f"AST Dot file saved to: {out_path}")
    

def extract_cobol_graph(file_path: str):
    """
    Extract graph structure from a COBOL file

    Args:
        file_path: Path to COBOL source file

    Returns:
        Tuple of (nodes, relationships)
        - nodes: List of dicts with 'type' and 'name'
        - relationships: List of dicts with 'from', 'to', 'type'
    """
    tree, code_bytes = get_tree(file_path)
    save_ast_visualization(file_path, tree, code_bytes)
    nodes, relationships = extract_graph_from_tree(tree, code_bytes, file_path)
    return nodes, relationships


def _get_text(node, code_bytes: bytes) -> str:
    """Get text content from a node."""
    return code_bytes[node.start_byte:node.end_byte].decode("utf-8", errors="ignore").strip()


def _collect_scope_headers(root, code_bytes: bytes) -> List[tuple]:
    """Walk tree and collect paragraph_header and section_header nodes as (type, name, start_byte, end_byte)."""
    headers = []

    def walk(node):
        if node.type == "paragraph_header":
            name = _get_text(node, code_bytes).rstrip(".").strip()
            if name:
                headers.append(("paragraph", name, node.start_byte, node.end_byte))
        elif node.type == "section_header":
            name = _get_text(node, code_bytes).split()[0] if _get_text(node, code_bytes) else node.type
            headers.append(("section", name, node.start_byte, node.end_byte))
        for child in node.children:
            walk(child)

    walk(root)
    return headers


def get_all_scopes(tree, code_bytes: bytes, file_path: str) -> List[Scope]:
    """
    Collect all scopes (paragraph/section) with byte ranges from the tree.
    Scope body extends from header start to the next header's start (or end of file).
    """
    root = tree.root_node
    headers = _collect_scope_headers(root, code_bytes)
    if not headers:
        return []

    # Sort by start_byte and assign end_byte = next header start or end of root
    headers_sorted = sorted(headers, key=lambda h: h[2])
    scopes = []
    for i, (scope_type, name, start_byte, _) in enumerate(headers_sorted):
        end_byte = headers_sorted[i + 1][2] if i + 1 < len(headers_sorted) else root.end_byte
        snippet = code_bytes[start_byte:end_byte].decode("utf-8", errors="ignore")
        scopes.append(Scope(
            file_path=file_path,
            name=name,
            scope_type=scope_type,
            start_byte=start_byte,
            end_byte=end_byte,
            code_snippet=snippet,
        ))
    return scopes


def get_scope_dependencies_from_relationships(relationships: List[Dict]) -> Dict[str, List[str]]:
    """
    Build scope name -> list of called scope names from extract_graph_from_tree relationships.
    CALLS go from Statement (id like 'Paragraph:FOO:stmt:N') to Paragraph (id like 'Paragraph:BAR').
    """
    deps = {}
    for rel in relationships:
        if rel.get("type") != "CALLS":
            continue
        from_id = rel.get("from", "")
        to_id = rel.get("to", "")
        if ":stmt:" in from_id:
            caller = from_id.split(":stmt:")[0]  # e.g. "Paragraph:MAIN-PROCESS"
        else:
            continue
        caller_name = caller.split(":", 1)[1] if ":" in caller else caller
        callee_name = to_id.split(":", 1)[1] if ":" in to_id else to_id
        deps.setdefault(caller_name, []).append(callee_name)
    # Deduplicate
    for k in deps:
        deps[k] = list(dict.fromkeys(deps[k]))
    return deps


def get_scopes_migration_order(tree, code_bytes: bytes, file_path: str) -> List[Scope]:
    """
    Return scopes in migration order: callees before callers (topological order).
    Uses get_all_scopes and extract_graph_from_tree to get dependencies.
    """
    nodes, relationships = extract_graph_from_tree(tree, code_bytes, file_path)
    scopes = get_all_scopes(tree, code_bytes, file_path)
    deps = get_scope_dependencies_from_relationships(relationships)

    # Topological sort: callees before callers. deps[caller] = [callees]. So in_degree[caller] = number of callees in this file.
    from collections import deque

    scope_names = {s.name for s in scopes}
    in_degree = {
        name: len([c for c in deps.get(name, []) if c in scope_names])
        for name in scope_names
    }
    queue = deque([n for n in scope_names if in_degree[n] == 0])
    order = []
    while queue:
        n = queue.popleft()
        order.append(n)
        # Scopes that call n can now be considered (one less dependency)
        for caller in scope_names:
            if caller not in order and n in deps.get(caller, []):
                in_degree[caller] -= 1
                if in_degree[caller] == 0:
                    queue.append(caller)
    for name in scope_names:
        if name not in order:
            order.append(name)
    scope_by_name = {s.name: s for s in scopes}
    return [scope_by_name[name] for name in order if name in scope_by_name]



def extract_graph_from_tree(tree, code_bytes, file_path=None):
    """
    Extract graph structure (nodes and relationships) from a tree-sitter tree
    Following the enhanced schema with Program, Division, Section, Paragraph, Statement, etc.
    
    Args:
        tree: tree-sitter Tree object
        code_bytes: bytes content of the file
        file_path: Optional file path for Program node
        
    Returns:
        Tuple of (nodes, relationships)
        - nodes: List of dicts with node type and properties
        - relationships: List of dicts with 'from', 'to', 'type', and optional properties
    """
    import uuid
    
    nodes = []
    relationships = []
    
    # Context tracking
    program_id = None
    current_division = None
    current_section = None
    current_paragraph = None
    statement_order = {}  # paragraph_id -> counter
    var_level_stack = []  # [(level_int, var_id), ...] for COBOL data hierarchy (CONTAINS)
    
    # Node ID generators
    def get_node_id(node_type, identifier):
        """Generate unique node ID"""
        return f"{node_type}:{identifier}"
    
    def get_text(node):
        """Get text content from node"""
        return code_bytes[node.start_byte:node.end_byte].decode('utf-8', errors='ignore').strip()
    
    # Extract program name from file path or default
    if file_path:
        from pathlib import Path
        program_name = Path(file_path).stem
        program_file = str(Path(file_path).name)
    else:
        program_name = "UNNAMED"
        program_file = "unknown.cbl"
    
    program_id = get_node_id("Program", program_name)
    nodes.append({
        'type': 'Program',
        'id': program_id,
        'name': program_name,
        'file': program_file
    })
    is_copybook = file_path and file_path.lower().endswith('.cpy')
    copybook_id = get_node_id("Copybook", program_name) if is_copybook else None
    if copybook_id:
        nodes.append({
            'type': 'Copybook',
            'id': copybook_id,
            'name': program_name,
            'file': program_file
        })

    def traverse(node, parent_context=None):
        nonlocal current_division, current_section, current_paragraph
        
        node_text = get_text(node)
        
        # 1. Detect Divisions
        if node.type.endswith('_division'):
            div_name = node.type.replace('_division', '').upper() + ' DIVISION'
            div_id = get_node_id("Division", div_name)
            
            # Check if already added
            if not any(n.get('id') == div_id for n in nodes):
                nodes.append({
                    'type': 'Division',
                    'id': div_id,
                    'name': div_name
                })
                relationships.append({
                    'from': program_id,
                    'to': div_id,
                    'type': 'HAS_DIVISION'
                })
            
            current_division = div_id
            current_section = None  # Reset section when entering division
        
        # 2. Detect Sections
        elif node.type.endswith('_section'):
            section_name = node_text.split()[0] if node_text else node.type.replace('_section', '').upper()
            section_id = get_node_id("Section", section_name)
            
            if not any(n.get('id') == section_id for n in nodes):
                nodes.append({
                    'type': 'Section',
                    'id': section_id,
                    'name': section_name
                })
                
                if current_division:
                    relationships.append({
                        'from': current_division,
                        'to': section_id,
                        'type': 'HAS_SECTION'
                    })
            
            current_section = section_id
        
        # 3. Detect Paragraph Headers
        elif node.type == 'paragraph_header':
            para_name = node_text.rstrip('.').strip()
            if para_name:
                para_id = get_node_id("Paragraph", para_name)
                
                if not any(n.get('id') == para_id for n in nodes):
                    nodes.append({
                        'type': 'Paragraph',
                        'id': para_id,
                        'name': para_name
                    })
                    
                    if current_section:
                        relationships.append({
                            'from': current_section,
                            'to': para_id,
                            'type': 'HAS_PARAGRAPH'
                        })
                
                current_paragraph = para_id
                statement_order[para_id] = 0
        
        # 4. Detect Statements
        elif node.type.endswith('_statement') or node.type in ('stop_statement', 'perform_statement_call_proc'):
            # Statements before first paragraph (e.g. under procedure_division) need an implicit paragraph
            if not current_paragraph and current_division:
                initial_id = get_node_id("Paragraph", "__INITIAL__")
                if not any(n.get("id") == initial_id for n in nodes):
                    nodes.append({
                        "type": "Paragraph",
                        "id": initial_id,
                        "name": "__INITIAL__",
                    })
                    if current_section:
                        relationships.append({
                            "from": current_section,
                            "to": initial_id,
                            "type": "HAS_PARAGRAPH",
                        })
                current_paragraph = initial_id
                statement_order[initial_id] = 0
            if current_paragraph:
                statement_order[current_paragraph] = statement_order.get(current_paragraph, 0) + 1
                stmt_order = statement_order[current_paragraph]
                
                stmt_id = f"{current_paragraph}:stmt:{stmt_order}"
                stmt_type = node.type.replace('_statement', '').upper()
                
                nodes.append({
                    'type': 'Statement',
                    'id': stmt_id,
                    'statement_type': stmt_type,
                    'text': node_text[:200],  # Limit text length
                    'order': stmt_order
                })
                
                relationships.append({
                    'from': current_paragraph,
                    'to': stmt_id,
                    'type': 'HAS_STATEMENT'
                })
                
                # Process statement-specific relationships
                process_statement(node, stmt_id, code_bytes)
        
        # 4b. COPY statement: this file (Program) COPIES a copybook
        elif node.type == 'copy_statement':
            # copy_statement: _COPY, field('book', optional(choice(WORD, string))), ...
            if node.child_count >= 2:
                book_node = node.child(1)
                if book_node and book_node.type in ('WORD', 'string'):
                    book_name = get_text(book_node).strip().strip("'\"")
                    if book_name:
                        from pathlib import Path
                        book_stem = Path(book_name).stem
                        target_copybook_id = get_node_id("Copybook", book_stem)
                        relationships.append({
                            'from': program_id,
                            'to': target_copybook_id,
                            'type': 'COPIES'
                        })

        # 5. Detect Variables from data descriptions (with CONTAINS hierarchy and REDEFINES)
        elif node.type == 'data_description' or node.type == 'data_description_entry':
            var_info = extract_variable_info(node, code_bytes)
            if var_info:
                var_id = get_node_id("Variable", var_info['name'])
                level_str = var_info.get('level') or '01'
                try:
                    level_int = int(level_str.strip())
                except (ValueError, AttributeError):
                    level_int = 1
                # COBOL: higher level number = deeper (05 under 01, 10 under 05)
                while var_level_stack and var_level_stack[-1][0] >= level_int:
                    var_level_stack.pop()
                if not any(n.get('id') == var_id for n in nodes):
                    nodes.append({
                        'type': 'Variable',
                        'id': var_id,
                        'name': var_info['name'],
                        'level': var_info.get('level'),
                        'picture': var_info.get('picture'),
                        'default': var_info.get('default')
                    })
                if copybook_id:
                    relationships.append({'from': copybook_id, 'to': var_id, 'type': 'DEFINES'})
                if var_level_stack:
                    relationships.append({
                        'from': var_level_stack[-1][1],
                        'to': var_id,
                        'type': 'CONTAINS'
                    })
                var_level_stack.append((level_int, var_id))
                if var_info.get('redefines'):
                    relationships.append({
                        'from': var_id,
                        'to': get_node_id("Variable", var_info['redefines']),
                        'type': 'REDEFINES'
                    })
        
        # Continue recursion
        for child in node.children:
            traverse(child, node)
    
    def extract_variable_info(node, code_bytes):
        """Extract variable information from data description (including REDEFINES target)."""
        var_info = {}
        for child in node.children:
            if child.type == 'entry_name':
                var_info['name'] = get_text(child)
            elif child.type == 'level_number':
                var_info['level'] = get_text(child)
            elif child.type == 'picture_clause':
                pic_text = get_text(child)
                var_info['picture'] = pic_text.replace('PIC', '').replace('PICTURE', '').strip()
            elif child.type == 'value_clause':
                var_info['default'] = get_text(child).replace('VALUE', '').strip()
            elif child.type == 'redefines_clause' and child.child_count >= 2:
                # redefines_clause = _REDEFINES, _identifier
                var_info['redefines'] = get_text(child.child(1)).strip()
        return var_info if 'name' in var_info else None
    
    # COBOL reserved/figurative words to skip when detecting variable references
    _COBOL_SKIP_WORDS = frozenset({
        'FILLER', 'SPACE', 'SPACES', 'ZERO', 'ZEROS', 'ZEROES',
        'HIGH-VALUE', 'HIGH-VALUES', 'LOW-VALUE', 'LOW-VALUES',
        'QUOTE', 'QUOTES', 'NULL', 'NULLS', 'ALL',
        'SECTION', 'DIVISION', 'THRU', 'THROUGH',
        'TRUE', 'FALSE', 'CORRESPONDING', 'CORR',
        'ROUNDED', 'REMAINDER', 'SIZE', 'ERROR',
        'NOT', 'END', 'ELSE', 'THEN', 'WHEN',
        'ON', 'OFF', 'IS', 'ARE', 'THAN', 'EQUAL', 'GREATER', 'LESS',
        'ALSO', 'OTHER', 'ANY', 'OF', 'IN', 'BY', 'FROM', 'TO', 'INTO',
        'GIVING', 'WITH', 'NO', 'AT', 'UP', 'DOWN',
        'DELIMITED', 'DELIMITER', 'COUNT', 'POINTER',
        'TALLYING', 'REPLACING', 'LEADING', 'TRAILING',
        'FIRST', 'INITIAL', 'REFERENCE', 'CONTENT',
        'LINE', 'LINES', 'PAGE', 'COL', 'COLUMN',
        'INPUT', 'OUTPUT', 'EXTEND', 'I-O',
        'OPTIONAL', 'GLOBAL', 'EXTERNAL', 'UPON',
        'ADVANCING', 'BEFORE', 'AFTER',
    })

    # Parent node types that indicate the WORD is a label/procedure reference, not a variable
    _LABEL_PARENT_TYPES = frozenset({
        'label', 'perform_procedure', '_call_header',
        'paragraph_header', 'section_header',
    })

    def _find_labels_in_subtree(node):
        """Collect all label text from a subtree."""
        labels = []
        def _walk(n):
            if n.type == 'label':
                t = get_text(n).strip()
                if t:
                    labels.append(t)
            for c in n.children:
                _walk(c)
        _walk(node)
        return labels

    def _find_words_in_subtree(node):
        """Collect all WORD text from a subtree (for file/variable name detection)."""
        words = []
        def _walk(n):
            if n.type == 'WORD':
                t = get_text(n).strip()
                if t and t.upper() not in _COBOL_SKIP_WORDS:
                    words.append(t)
            for c in n.children:
                _walk(c)
        _walk(node)
        return words

    def _has_ancestor_type(node, target_types, up_to_node):
        """Check if any ancestor of node (up to up_to_node) has type in target_types."""
        cur = node.parent
        while cur and cur != up_to_node:
            if cur.type in target_types:
                return True
            cur = cur.parent
        return False

    def _preceding_keyword(node):
        """Return uppercase text of the nearest preceding sibling (keyword) of node."""
        sib = node.prev_sibling
        while sib:
            t = get_text(sib).strip().upper()
            if t in ('TO', 'INTO', 'GIVING'):
                return t
            if t and t not in ('', ','):
                return None
            sib = sib.prev_sibling
        return None

    def process_statement(stmt_node, stmt_id, code_bytes):
        """Process statement to extract variables, operators, literals, calls, goto, file ops, etc."""
        stmt_type = stmt_node.type

        # Extract variables, operators, and literals from expressions
        read_vars, modify_vars = extract_expression_elements(stmt_node, stmt_id, code_bytes)
        for r in read_vars:
            for m in modify_vars:
                if r != m:
                    relationships.append({'from': r, 'to': m, 'type': 'FEEDS'})

        # --- PERFORM (with THRU detection) ---
        if stmt_type == 'perform_statement_call_proc':
            labels = []
            for child in stmt_node.children:
                if child.type == 'perform_procedure':
                    for subchild in child.children:
                        if subchild.type == 'label':
                            target_para = get_text(subchild).strip()
                            if target_para:
                                labels.append(target_para)
            for i, target_para in enumerate(labels):
                target_id = get_node_id("Paragraph", target_para)
                rel_type = 'CALLS' if i == 0 else 'CALLS_THRU'
                relationships.append({'from': stmt_id, 'to': target_id, 'type': rel_type})

        # --- CALL ---
        elif stmt_type == 'call_statement':
            for child in stmt_node.children:
                if child.type == 'string':
                    target_name = get_text(child).strip().strip("'\"")
                    if target_name:
                        target_id = get_node_id("Paragraph", target_name)
                        relationships.append({'from': stmt_id, 'to': target_id, 'type': 'CALLS'})
                    break
                elif child.type == 'WORD':
                    target_name = get_text(child).strip()
                    if target_name and target_name.upper() not in ('CALL', 'USING', 'BY', 'REFERENCE', 'CONTENT', 'VALUE'):
                        target_id = get_node_id("Paragraph", target_name)
                        relationships.append({'from': stmt_id, 'to': target_id, 'type': 'CALLS'})
                    break
                elif child.type == '_call_header' and child.child_count >= 2:
                    name_node = child.child(1)
                    if name_node:
                        target_name = get_text(name_node).strip().strip("'\"")
                        if target_name:
                            target_id = get_node_id("Paragraph", target_name)
                            relationships.append({'from': stmt_id, 'to': target_id, 'type': 'CALLS'})
                    break

        # --- GO TO ---
        elif stmt_type in ('go_to_statement', 'goto_statement', 'go_statement',
                           'go_to_depending_statement', 'alter_statement'):
            targets = _find_labels_in_subtree(stmt_node)
            if not targets:
                targets = [w for w in _find_words_in_subtree(stmt_node)
                           if w.upper() not in ('GO', 'TO', 'DEPENDING', 'ON', 'ALTER', 'PROCEED')]
            for target_para in targets:
                target_id = get_node_id("Paragraph", target_para)
                relationships.append({'from': stmt_id, 'to': target_id, 'type': 'GOTO_CALLS'})

        # --- File operations: OPEN, CLOSE, READ, WRITE, REWRITE, DELETE, START ---
        elif stmt_type in ('open_statement', 'close_statement', 'read_statement',
                           'write_statement', 'rewrite_statement', 'delete_statement',
                           'start_statement'):
            op = stmt_type.replace('_statement', '').upper()
            _FILE_OP_SKIP = frozenset({
                'OPEN', 'CLOSE', 'READ', 'WRITE', 'REWRITE', 'DELETE', 'START',
                'INPUT', 'OUTPUT', 'EXTEND', 'I-O',
                'INVALID', 'KEY', 'RECORD', 'STATUS', 'LOCK', 'UNLOCK',
                'NO', 'NEXT', 'PREVIOUS', 'ADVANCING', 'PAGE', 'BEFORE', 'AFTER',
                'END-READ', 'END-WRITE', 'END-REWRITE', 'END-DELETE', 'END-START',
            })
            file_words = _find_words_in_subtree(stmt_node)
            for fw in file_words:
                if fw.upper() not in _FILE_OP_SKIP:
                    file_var_id = get_node_id("Variable", fw)
                    relationships.append({'from': stmt_id, 'to': file_var_id, 'type': f'FILE_{op}'})

        # --- ADD / SUBTRACT / MULTIPLY / DIVIDE ---
        elif stmt_type in ('add_statement', 'subtract_statement',
                           'multiply_statement', 'divide_statement'):
            pass  # handled by expression extraction with improved MODIFIES detection

        # --- DISPLAY (all referenced vars are READS) ---
        elif stmt_type == 'display_statement':
            pass  # handled by expression extraction (default is READS)

        # --- ACCEPT (target var is MODIFIES) ---
        elif stmt_type == 'accept_statement':
            pass  # handled by expression extraction with accept_statement check

        # --- STRING / UNSTRING / INSPECT ---
        elif stmt_type in ('string_statement', 'unstring_statement', 'inspect_statement'):
            pass  # handled by expression extraction

        # --- EVALUATE / IF / SEARCH / SET / INITIALIZE ---
        elif stmt_type in ('evaluate_statement', 'if_statement', 'search_statement',
                           'set_statement', 'initialize_statement'):
            pass  # handled by expression extraction

        # --- EXIT / STOP / CONTINUE ---
        elif stmt_type in ('exit_statement', 'stop_statement', 'stop_run_statement',
                           'continue_statement'):
            pass  # no variable relationships

        # --- COMPUTE / MOVE ---
        elif stmt_type in ('compute_statement', 'move_statement'):
            pass  # handled by expression extraction

    def extract_expression_elements(node, stmt_id, code_bytes):
        """Extract operators, literals, and variables from expressions. Returns (read_var_ids, modify_var_ids)."""
        read_vars = set()
        modify_vars = set()
        operator_types = ['*', '/', '+', '-', '**']
        node_text = get_text(node).upper()

        # Check for arithmetic operators
        for op in operator_types:
            if op in node_text and node.type in ['arithmetic_expression', 'compute_statement', 'arithmetic_x']:
                op_id = f"{stmt_id}:op:{uuid.uuid4().hex[:8]}"
                nodes.append({'type': 'Operator', 'id': op_id, 'op': op})
                relationships.append({'from': stmt_id, 'to': op_id, 'type': 'HAS_EXPRESSION'})
                break

        # Detect literals (numbers and strings)
        if node.type in ['numeric_literal', 'string_literal', 'literal']:
            literal_value = get_text(node)
            literal_kind = 'integer' if literal_value.replace('.', '').replace('-', '').isdigit() else 'string'
            literal_id = f"{stmt_id}:lit:{uuid.uuid4().hex[:8]}"
            nodes.append({'type': 'Literal', 'id': literal_id, 'value': literal_value, 'kind': literal_kind})
            relationships.append({'from': stmt_id, 'to': literal_id, 'type': 'HAS_EXPRESSION'})

        # Detect variable usage (WORD nodes)
        if node.type == 'WORD':
            var_name = get_text(node)
            var_upper = var_name.upper()
            parent_type = node.parent.type if node.parent else ""

            # Skip: label/procedure references, COBOL keywords, single-char names
            if (parent_type not in _LABEL_PARENT_TYPES
                    and var_upper not in _COBOL_SKIP_WORDS
                    and len(var_name) >= 2):
                var_id = get_node_id("Variable", var_name)
                parent = node.parent
                grandparent = parent.parent if parent else None

                # Determine READS vs MODIFIES:
                # 1. ACCEPT / INITIALIZE → target is MODIFIES
                # 2. Preceded by TO / INTO / GIVING keyword → MODIFIES
                # 3. First identifier in COMPUTE target → MODIFIES
                # 4. Otherwise → READS
                rel_type = "READS"

                # Find the enclosing statement type
                stmt_ancestor = node.parent
                while stmt_ancestor and not (stmt_ancestor.type.endswith('_statement') or stmt_ancestor.type == 'stop_statement'):
                    stmt_ancestor = stmt_ancestor.parent
                stmt_ancestor_type = stmt_ancestor.type if stmt_ancestor else ""

                if stmt_ancestor_type in ('accept_statement', 'initialize_statement'):
                    rel_type = "MODIFIES"
                elif _preceding_keyword(node) in ('TO', 'INTO', 'GIVING'):
                    rel_type = "MODIFIES"
                elif grandparent and grandparent.type in ('compute_statement', 'move_statement',
                                                          'add_statement', 'subtract_statement',
                                                          'multiply_statement', 'divide_statement'):
                    if parent == grandparent.children[0]:
                        rel_type = "MODIFIES"

                relationships.append({'from': stmt_id, 'to': var_id, 'type': rel_type})
                if rel_type == "READS":
                    read_vars.add(var_id)
                else:
                    modify_vars.add(var_id)

        for child in node.children:
            r, m = extract_expression_elements(child, stmt_id, code_bytes)
            read_vars |= r
            modify_vars |= m
        return read_vars, modify_vars
    
    # Start traversal
    traverse(tree.root_node)
    
    return nodes, relationships


def extract_from_path(path: str):
    """
    Extract graph structure from a file or directory
    
    Args:
        path: Path to COBOL file or directory containing COBOL files
        
    Returns:
        Tuple of (nodes, relationships) aggregated from all files
    """
    from pathlib import Path

    path_obj = Path(path)
    all_nodes = []
    all_relationships = []

    if path_obj.is_file():
        # Single file
        print(f"Processing file: {path}")
        tree, code_bytes = get_tree(str(path_obj))
        save_ast_visualization(str(path_obj), tree, code_bytes)
        nodes, rels = extract_graph_from_tree(tree, code_bytes, str(path_obj))
        all_nodes.extend(nodes)
        all_relationships.extend(rels)
        print(f"  → Extracted {len(nodes)} nodes, {len(rels)} relationships")

    elif path_obj.is_dir():
        # Directory - find all COBOL files
        cobol_extensions = ['.cbl', '.cob', '.cobol', '.cpy', '.CBL', '.COB', '.COBOL', '.CPY']
        cobol_files = []

        for ext in cobol_extensions:
            cobol_files.extend(path_obj.glob(f"**/*{ext}"))

        if not cobol_files:
            print(f"No COBOL files found in {path}")
            return all_nodes, all_relationships

        print(f"Found {len(cobol_files)} COBOL file(s) in {path}")

        for file_path in cobol_files:
            try:
                print(f"Processing: {file_path.name}")
                tree, code_bytes = get_tree(str(file_path))
                save_ast_visualization(str(file_path), tree, code_bytes)
                nodes, rels = extract_graph_from_tree(tree, code_bytes, str(file_path))
                all_nodes.extend(nodes)
                all_relationships.extend(rels)
                print(f"  → Extracted {len(nodes)} nodes, {len(rels)} relationships")
            except Exception as e:
                print(f"  ✗ Error processing {file_path.name}: {e}")
    
    else:
        raise ValueError(f"Path does not exist: {path}")
    
    # Remove duplicates based on node ID or type+name combination
    unique_nodes = []
    seen_nodes = set()
    for node in all_nodes:
        # Use 'id' if available, otherwise use type+name
        if 'id' in node:
            node_key = node['id']
        elif 'name' in node:
            node_key = (node['type'], node['name'])
        else:
            # For nodes without id or name, use type + all properties
            node_key = (node['type'], str(sorted(node.items())))
        
        if node_key not in seen_nodes:
            seen_nodes.add(node_key)
            unique_nodes.append(node)
    
    print(f"\nTotal: {len(unique_nodes)} unique nodes, {len(all_relationships)} relationships")
    return unique_nodes, all_relationships
