package com.example.migration;

/**
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID. IC102A.
 *
 * VALIDATION FOR:-
 * "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH."
 * "COBOL 85 VERSION 4.2, Apr  1993 SSVG"
 *
 * X-CARDS USED BY THIS PROGRAM ARE :-
 *   X-55  - SYSTEM PRINTER NAME.
 *   X-82  - SOURCE COMPUTER NAME.
 *   X-83  - OBJECT COMPUTER NAME.
 *
 * THIS PROGRAM TESTS THE USE OF THE LINKAGE SECTION
 * AND USING PHRASE IN THE PROCEDURE DIVISION HEADER.
 */
public class IC102A {

    // FILE-CONTROL: SELECT PRINT-FILE ASSIGN TO "REPORT".
    private static final String PRINT_FILE_PATH = "REPORT";

    // FD  PRINT-FILE.
    // 01 PRINT-REC PIC X(120).
    private String printRec;

    // 01 DUMMY-RECORD PIC X(120).
    private String dummyRecord;

    // WORKING-STORAGE SECTION.
    // 77 DN2 PIC S9 VALUE ZERO.
    private int dn2 = 0;

    // LINKAGE SECTION.
    // 77 DN1 PIC S9.
    public static class IntHolder {
        public int value;
        public IntHolder(int value) {
            this.value = value;
        }
    }

    /**
     * PROCEDURE DIVISION USING DN1.
     * Entry point for IC102A program logic.
     * @param dn1Holder linkage variable DN1
     */
    public void execute(IntHolder dn1Holder) {
        // Initialize working-storage variable DN2
        this.dn2 = 0;
        // Call section SECT-IC102-0001
        sectIC1020001(dn1Holder);
    }

    /**
     * SECT-IC102-0001 SECTION.
     */
    private void sectIC1020001(IntHolder dn1Holder) {
        // CALL-TEST-001
        callTest001(dn1Holder);
        // CALL-EXIT-001
        callExit001();
    }

    /**
     * CALL-TEST-001.
     * ADD 1 TO DN2.
     * MOVE DN2 TO DN1.
     */
    private void callTest001(IntHolder dn1Holder) {
        // ADD 1 TO DN2
        dn2 = dn2 + 1;
        // MOVE DN2 TO DN1
        dn1Holder.value = dn2;
    }

    /**
     * CALL-EXIT-001.
     * EXIT PROGRAM.
     */
    private void callExit001() {
        // EXIT PROGRAM: return to caller
        return;
    }
}
