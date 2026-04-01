"""
Verification layer:
- Java compilation checks
- Error extraction for repair loops
"""

from __future__ import annotations

import re
import subprocess
from pathlib import Path
from typing import Any, Dict, List, Tuple


class JavaValidationEngine:
    def __init__(self, output_dir: Path):
        self.output_dir = output_dir

    def compile_java(self) -> Tuple[bool, List[Dict[str, Any]]]:
        java_files = list(self.output_dir.rglob("*.java"))
        if not java_files:
            return True, []
        try:
            result = subprocess.run(
                ["javac", "-encoding", "UTF-8", "-Xlint:all", *[str(p) for p in java_files]],
                cwd=self.output_dir,
                capture_output=True,
                text=True,
                timeout=60,
            )
        except FileNotFoundError:
            return False, [{"file": "", "line": 0, "message": "javac not found. Install JDK."}]
        except subprocess.TimeoutExpired:
            return False, [{"file": "", "line": 0, "message": "Compilation timed out."}]

        errors: List[Dict[str, Any]] = []
        if result.returncode != 0 and result.stderr:
            for line in result.stderr.splitlines():
                m = re.match(r"([^:]+):(\d+):\s*(.+)", line.strip())
                if m:
                    errors.append({"file": m.group(1), "line": int(m.group(2)), "message": m.group(3)})
                elif line.strip().startswith("error:") or "error:" in line:
                    errors.append({"file": "", "line": 0, "message": line.strip()})
            if not errors:
                errors.append({"file": "", "line": 0, "message": result.stderr[:500]})

        return result.returncode == 0, errors

    def delete_class_files(self) -> None:
        for path in self.output_dir.rglob("*.class"):
            try:
                path.unlink()
            except OSError:
                pass
