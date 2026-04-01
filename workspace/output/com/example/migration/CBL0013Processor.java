package com.example.migration;

/**
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0013.
 * AUTHOR.        Athar Ramzan.
 *
 * DATA DIVISION.
 * WORKING-STORAGE SECTION.
 *   01 NUMERATOR     PIC 9(04) VALUE 1000.
 *   01 DENOMINATOR   PIC 9(04) VALUE 0.
 *   01 RESULT        PIC 9(04).
 *
 * PROCEDURE DIVISION.
 *   MAIN-PROCEDURE.
 *       DISPLAY "Starting Division".
 *       DIVIDE NUMERATOR BY DENOMINATOR GIVING RESULT.
 *       DISPLAY "Result is: " RESULT.
 *       STOP RUN.
 */
public class CBL0013Processor {
    // 01 NUMERATOR     PIC 9(04) VALUE 1000.
    private int numerator = 1000;

    // 01 DENOMINATOR   PIC 9(04) VALUE 0.
    private int denominator = 0;

    // 01 RESULT        PIC 9(04).
    private int result;

    public static void main(String[] args) {
        CBL0013Processor processor = new CBL0013Processor();
        processor.mainProcedure();
    }

    /**
     * MAIN-PROCEDURE paragraph logic from CBL0013.
     */
    public void mainProcedure() {
        // DISPLAY "Starting Division".
        System.out.println("Starting Division");
        // DIVIDE NUMERATOR BY DENOMINATOR GIVING RESULT.
        result = numerator / denominator;
        // DISPLAY "Result is: " RESULT.
        System.out.println("Result is: " + result);
        // STOP RUN.
        return;
    }
}
