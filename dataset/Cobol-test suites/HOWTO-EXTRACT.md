# How to Extract NIST COBOL85 Programs

## Prerequisites

- **GnuCOBOL** (`cobc`) and **Perl** installed
- File **newcob.val** in this directory (from `newcob.val.tar.gz`)

## Steps

1. **Extract all modules** (creates one `.CBL` per program in folders NC, SM, IC, SQ, etc.):

   ```bash
   make all
   ```

2. **Or extract a single module**, e.g. NC:

   ```bash
   make extract-NC
   ```

3. **List what was extracted:**

   ```bash
   make list
   ```

4. **Remove extracted folders and EXEC85:**

   ```bash
   make clean
   ```

That’s it. The Makefile uses the official GnuCOBOL method (EXEC85 + `expand.pl`).
