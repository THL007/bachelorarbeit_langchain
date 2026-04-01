package com.example.migration;

import java.math.BigDecimal;

/**
 * Migration of COBOL program IC105A.
 *
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID. IC105A.
 *
 * VALIDATION FOR: "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH."
 * "COBOL 85 VERSION 4.2, Apr  1993 SSVG"
 *
 * X-CARDS USED BY THIS PROGRAM:
 *   X-55  - SYSTEM PRINTER NAME.
 *   X-82  - SOURCE COMPUTER NAME.
 *   X-83  - OBJECT COMPUTER NAME.
 *
 * ENVIRONMENT DIVISION.
 * CONFIGURATION SECTION.
 * SOURCE-COMPUTER. GNU-Linux.
 * OBJECT-COMPUTER. GNU-Linux.
 *
 * DATA DIVISION.
 * LINKAGE SECTION.
 *   77  DN1 PICTURE 999.
 *   77  DN2 PICTURE S99 COMPUTATIONAL.
 *
 * PROCEDURE DIVISION USING DN1 DN2.
 */
public class IC105A {
    /**
     * LINKAGE SECTION variables.
     */
    private int dn1;           // PIC 999
    private BigDecimal dn2;    // PIC S99 COMPUTATIONAL

    /**
     * Executes the IC105A logic.
     * @param dn1 parameter DN1 (integer)
     * @return updated DN2 value (BigDecimal)
     */
    public BigDecimal execute(int dn1) {
        this.dn1 = dn1;
        this.dn2 = BigDecimal.ZERO;
        sectIc1050001();
        return this.dn2;
    }

    /**
     * SECT-IC105-0001 SECTION.
     */
    private void sectIc1050001() {
        exitTest001();
    }

    /**
     * EXIT-TEST-001.
     * IF DN1 IS NOT EQUAL TO 1
     *    GO TO EXIT-TEST-002.
     * MOVE 1 TO DN2.
     */
    private void exitTest001() {
        if (dn1 != 1) {
            exitTest002();
            return;
        }
        dn2 = BigDecimal.valueOf(1);
        exitStatement001();
        return;
    }

    /**
     * EXIT-STATEMENT-001.
     * EXIT PROGRAM.
     */
    private void exitStatement001() {
        // Return to caller (Exit Program)
    }

    /**
     * EXIT-TEST-002.
     * IF DN1 IS NOT EQUAL TO 2
     *    GO TO EXIT-TEST-003.
     * MOVE 2 TO DN2.
     */
    private void exitTest002() {
        if (dn1 != 2) {
            exitTest003();
            return;
        }
        dn2 = BigDecimal.valueOf(2);
        exitStatement002();
        return;
    }

    /**
     * EXIT-STATEMENT-002.
     * EXIT PROGRAM.
     */
    private void exitStatement002() {
        // Return to caller (Exit Program)
    }

    /**
     * EXIT-TEST-003.
     * IF DN1 NOT EQUAL TO 3
     *    GO TO EXIT-TEST-004.
     * MOVE 3 TO DN2.
     */
    private void exitTest003() {
        if (dn1 != 3) {
            exitTest004();
            return;
        }
        dn2 = BigDecimal.valueOf(3);
        exitStatement003();
        return;
    }

    /**
     * EXIT-STATEMENT-003.
     * EXIT PROGRAM.
     */
    private void exitStatement003() {
        // Return to caller (Exit Program)
    }

    /**
     * EXIT-TEST-004.
     * MOVE 4 TO DN2.
     * GO TO EXIT-STATEMENT-004.
     */
    private void exitTest004() {
        dn2 = BigDecimal.valueOf(4);
        exitStatement004();
        return;
    }

    /**
     * EXTRANEOUS-PARAGRAPH.
     * THIS PARAGRAPH IS NEVER EXECUTED.
     * MOVE 5 TO DN2.
     */
    private void extraneousParagraph() {
        dn2 = BigDecimal.valueOf(5);
    }

    /**
     * EXIT-STATEMENT-004.
     * EXIT PROGRAM.
     */
    private void exitStatement004() {
        // Return to caller (Exit Program)
    }
}
