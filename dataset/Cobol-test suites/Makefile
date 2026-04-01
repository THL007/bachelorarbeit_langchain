# NIST COBOL85 Test Suite - Extract individual COBOL programs
# Uses the official GnuCOBOL method: EXEC85 + expand.pl
# See: https://gnucobol.sourceforge.io/ (tests/cobol85)
# Run from this directory: make all  (or make extract-NC for one module)

ROOT := $(shell pwd)
RM = rm -rf
SED = sed
PERL = perl
COBC = cobc
COBC_FLAGS = -std=cobol85 -debug -x

# All modules from the NIST suite (same as GnuCOBOL Makefile.am)
MODULES_ALL = NC SM IC SQ RL ST SG OB IF RW DB IX CM

.PHONY: all modules clean EXEC85 extract-%

# Default: build EXEC85 and extract all modules
all: EXEC85 extract-all

# Build the EXEC85 extractor from newcob.val (official sed from GnuCOBOL)
EXEC85.cob: newcob.val
	@echo "Extracting EXEC85 program from newcob.val (official GnuCOBOL method)..."
	$(SED) -e '/^\*END/,$d' \
		-e '1,/^\*HEADER/d' \
		-e 's/^002500.*/002500     SELECT  OPTIONAL POPULATION-FILE                             EXEC84.2/' \
		-e 's/^002700.*/002700     "NEWCOB_VAL" ORGANIZATION LINE SEQUENTIAL.                   EXEC84.2/' \
		-e 's/^003000.*/003000     "newcob.tmp" ORGANIZATION LINE SEQUENTIAL.                   EXEC84.2/' \
		-e 's/^003100.*/003100                                                                  EXEC84.2/' \
		-e 's/^003400.*/003400     "unused".                                                    EXEC84.2/' \
		-e 's/^003700.*/003700     "newcob.log".                                                EXEC84.2/' \
		-e 's/^004000.*/004000     "EXEC85.conf" ORGANIZATION LINE SEQUENTIAL.                  EXEC84.2/' \
		newcob.val > $@
	@echo "EXEC85.cob generated."

# Compile EXEC85 (requires GnuCOBOL: cobc)
EXEC85: EXEC85.cob
	@echo "Compiling EXEC85..."
	$(COBC) $(COBC_FLAGS) EXEC85.cob
	@echo "EXEC85 built."

# Extract all modules
extract-all: EXEC85
	@for m in $(MODULES_ALL); do $(MAKE) extract-$$m; done
	@echo "All modules extracted."

# Extract a single module (e.g. make extract-NC)
extract-%: EXEC85 newcob.val EXEC85.conf.in expand.pl
	@m=$*; \
	echo "Building module directory $$m..."; \
	mkdir -p ./$$m; \
	echo "*SELECT-MODULE $$m" > ./$$m/EXEC85.conf; \
	cat EXEC85.conf.in >> ./$$m/EXEC85.conf; \
	(cd $$m && NEWCOB_VAL="$(ROOT)/newcob.val" COB_UNIX_LF=Y ../EXEC85) || { $(RM) ./$$m; exit 1; }; \
	$(PERL) expand.pl $$m/newcob.tmp $$m || { $(RM) ./$$m; exit 1; }; \
	echo "Finished module $$m."

# List extracted programs
list:
	@echo "Extracted .CBL programs by module:"; \
	for m in $(MODULES_ALL); do \
	  if [ -d "$$m" ]; then \
	    count=$$(ls $$m/*.CBL 2>/dev/null | wc -l); \
	    echo "  $$m: $$count programs"; \
	  fi; \
	done

clean:
	$(RM) $(MODULES_ALL) EXEC85 EXEC85.exe EXEC85.cob copy copyalt
	@echo "Cleaned."
