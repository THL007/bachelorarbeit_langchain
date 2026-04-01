/*
 *-----------------------
 * Copyright Contributors to the COBOL Programming Course
 * SPDX-License-Identifier: CC-BY-4.0
 *-----------------------
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0002
 * AUTHOR.        Otto B. Fun.
 *
 * ENVIRONMENT DIVISION.
 * INPUT-OUTPUT SECTION.
 * FILE-CONTROL.
 *     SELECT PRINT-LINE ASSIGN TO PRTLINE.
 *     SELECT ACCT-REC   ASSIGN TO ACCTREC.
 *
 * DATA DIVISION.
 * FILE SECTION.
 * FD PRINT-LINE RECORDING MODE F.
 * 01 PRINT-REC.
 *     05 ACCT-NO-O      PIC X(8).
 *     05 ACCT-LIMIT-O   PIC $$,$$$,$$9.99.
 *     05 ACCT-BALANCE-O PIC $$,$$$,$$9.99.
 *     05 LAST-NAME-O    PIC X(20).
 *     05 FIRST-NAME-O   PIC X(15).
 *     05 COMMENTS-O     PIC X(50).
 *
 * FD ACCT-REC RECORDING MODE F.
 * 01 ACCT-FIELDS.
 *     05 ACCT-NO        PIC X(8).
 *     05 ACCT-LIMIT     PIC S9(7)V99 COMP-3.
 *     05 ACCT-BALANCE   PIC S9(7)V99 COMP-3.
 *     05 LAST-NAME      PIC X(20).
 *     05 FIRST-NAME     PIC X(15).
 *     05 CLIENT-ADDR.
 *         10 STREET-ADDR    PIC X(25).
 *         10 CITY-COUNTY    PIC X(20).
 *         10 USA-STATE      PIC X(15).
 *     05 RESERVED       PIC X(7).
 *     05 COMMENTS       PIC X(50).
 *
 * WORKING-STORAGE SECTION.
 * 01 FLAGS.
 *     05 LASTREC       PIC X VALUE SPACE.
 *
 * PROCEDURE DIVISION.
 * OPEN-FILES.
 * READ-NEXT-RECORD.
 * CLOSE-STOP.
 * READ-RECORD.
 * WRITE-RECORD.
 */

package com.example.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CBL0002Processor {
    // SELECT PRINT-LINE ASSIGN TO PRTLINE -> print-line output
    private BufferedWriter printLineWriter;
    // SELECT ACCT-REC ASSIGN TO ACCTREC -> acct-rec input
    private BufferedReader acctRecReader;

    // WORKING-STORAGE: LASTREC PIC X VALUE SPACE (' ' -> false)
    private boolean lastRec;

    // ACCT-FIELDS input record fields
    private String acctNo;              // PIC X(8)
    private BigDecimal acctLimit;       // PIC S9(7)V99 COMP-3
    private BigDecimal acctBalance;     // PIC S9(7)V99 COMP-3
    private String lastName;            // PIC X(20)
    private String firstName;           // PIC X(15)
    private String streetAddr;          // PIC X(25)
    private String cityCounty;          // PIC X(20)
    private String usaState;            // PIC X(15)
    private String reserved;            // PIC X(7)
    private String comments;            // PIC X(50)

    // PRINT-REC output record fields
    private String acctNoO;             // PIC X(8)
    private BigDecimal acctLimitO;      // PIC $$,$$$,$$9.99
    private BigDecimal acctBalanceO;    // PIC $$,$$$,$$9.99
    private String lastNameO;           // PIC X(20)
    private String firstNameO;          // PIC X(15)
    private String commentsO;           // PIC X(50)

    // Decimal format for PIC $$,$$$,$$9.99
    private static final DecimalFormat CURRENCY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        CURRENCY_FORMAT = new DecimalFormat("'$$'###,###,##0.00", symbols);
    }

    public static void main(String[] args) {
        CBL0002Processor program = new CBL0002Processor();
        program.execute();
    }

    /**
     * ENTRY POINT for PROCEDURE DIVISION: OPEN-FILES, READ-NEXT-RECORD, CLOSE-STOP
     */
    public void execute() {
        try {
            openFiles();
            readNextRecord();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                closeStop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * OPEN-FILES.
     */
    private void openFiles() throws IOException {
        acctRecReader = Files.newBufferedReader(Paths.get("ACCTREC"));
        printLineWriter = Files.newBufferedWriter(Paths.get("PRTLINE"));
        lastRec = false;
    }

    /**
     * READ-NEXT-RECORD.
     */
    private void readNextRecord() throws IOException {
        // PERFORM READ-RECORD
        readRecord();
        // PERFORM UNTIL LASTREC = 'Y'
        while (!lastRec) {
            // PERFORM WRITE-RECORD
            writeRecord();
            // PERFORM READ-RECORD
            readRecord();
        }
    }

    /**
     * READ-RECORD.
     */
    private void readRecord() throws IOException {
        String line = acctRecReader.readLine();
        if (line == null) {
            // AT END MOVE 'Y' TO LASTREC
            lastRec = true;
            return;
        }
        String[] parts = line.split(",", -1);
        acctNo = parts[0];
        acctLimit = new BigDecimal(parts[1]);
        acctBalance = new BigDecimal(parts[2]);
        lastName = parts[3];
        firstName = parts[4];
        streetAddr = parts[5];
        cityCounty = parts[6];
        usaState = parts[7];
        reserved = parts[8];
        comments = parts[9];
    }

    /**
     * WRITE-RECORD.
     */
    private void writeRecord() throws IOException {
        // MOVE ACCT-NO TO ACCT-NO-O
        acctNoO = acctNo;
        // MOVE ACCT-LIMIT TO ACCT-LIMIT-O
        acctLimitO = acctLimit;
        // MOVE ACCT-BALANCE TO ACCT-BALANCE-O
        acctBalanceO = acctBalance;
        // MOVE LAST-NAME TO LAST-NAME-O
        lastNameO = lastName;
        // MOVE FIRST-NAME TO FIRST-NAME-O
        firstNameO = firstName;
        // MOVE COMMENTS TO COMMENTS-O
        commentsO = comments;
        // WRITE PRINT-REX
        StringBuilder sb = new StringBuilder();
        sb.append(ensureLength(acctNoO, 8));
        sb.append(ensureLength(formatCurrency(acctLimitO), 11));
        sb.append(ensureLength(formatCurrency(acctBalanceO), 11));
        sb.append(ensureLength(lastNameO, 20));
        sb.append(ensureLength(firstNameO, 15));
        sb.append(ensureLength(commentsO, 50));
        printLineWriter.write(sb.toString());
        printLineWriter.newLine();
    }

    /**
     * CLOSE-STOP.
     */
    private void closeStop() throws IOException {
        if (acctRecReader != null) acctRecReader.close();
        if (printLineWriter != null) printLineWriter.close();
    }

    /**
     * Ensure string is fixed length, padded or truncated.
     */
    private static String ensureLength(String value, int length) {
        if (value == null) value = "";
        if (value.length() > length) return value.substring(0, length);
        return String.format("%-" + length + "s", value);
    }

    /**
     * Format BigDecimal as currency according to PIC $$,$$$,$$9.99.
     */
    private static String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }
}
