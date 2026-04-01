package com.example.migration;

import java.math.BigDecimal;

/**
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID. CBL0014.
 * AUTHOR. Athar Ramzan.
 *
 * DATA DIVISION.
 * WORKING-STORAGE SECTION.
 */
public class CBL0014Processor {
    // DATA DIVISION.
    // WORKING-STORAGE SECTION.

    // 01 JUNK-FIELD      PIC X(05) VALUE "ABCDE".
    private String junkField = "ABCDE";

    // 01 NUM-FIELD-BAD   REDEFINES JUNK-FIELD PIC S9(05) COMP-3.
    private BigDecimal numFieldBad;

    // 01 RESULT          PIC S9(06) COMP-3.
    private BigDecimal result;

    /**
     * PROCEDURE DIVISION.
     */
    public void execute() {
        // DISPLAY "Triggering S0C7...".
        System.out.println("Triggering S0C7...");

        // ADD 100 TO NUM-FIELD-BAD GIVING RESULT.
        try {
            // Convert JUNK-FIELD (PIC X) to numeric for COMP-3; invalid data triggers numeric exception
            numFieldBad = new BigDecimal(junkField);
            result = numFieldBad.add(BigDecimal.valueOf(100));
        } catch (NumberFormatException e) {
            // Numeric data exception (S0C7)
            throw new ArithmeticException("Numeric data exception (S0C7)");
        }

        // DISPLAY "Result: " RESULT.
        System.out.println("Result: " + result);

        // STOP RUN.
        return;
    }

    public static void main(String[] args) {
        CBL0014Processor processor = new CBL0014Processor();
        processor.execute();
    }
}
