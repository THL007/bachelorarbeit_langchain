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

/**
 *-----------------------
 * Copyright Contributors to the COBOL Programming Course
 * SPDX-License-Identifier: CC-BY-4.0
 *-----------------------
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0001
 * AUTHOR.        Otto B. Fun.
 *--------------------
 * ENVIRONMENT DIVISION.
 *--------------------
 * INPUT-OUTPUT SECTION.
 * FILE-CONTROL.
 *    SELECT PRINT-LINE ASSIGN TO PRTLINE.
 *    SELECT ACCT-REC   ASSIGN TO ACCTREC.
 *
 * DATA DIVISION.
 *--------------------
 * FILE SECTION.
 * FD PRINT-LINE RECORDING MODE F.
 * 01 PRINT-REC.
 *    05 ACCT-NO-O      PIC X(8).
 *    05 ACCT-LIMIT-O   PIC $$,$$$,$$9.99.
 *    05 ACCT-BALANCE-O PIC $$,$$$,$$9.99.
 *    05 LAST-NAME-O    PIC X(20).
 *    05 FIRST-NAME-O   PIC X(15).
 *    05 COMMENTS-O     PIC X(50).
 *
 * FD ACCT-REC RECORDING MODE F.
 * 01 ACCT-FIELDS.
 *    05 ACCT-NO            PIC X(8).
 *    05 ACCT-LIMIT         PIC S9(7)V99 COMP-3.
 *    05 ACCT-BALANCE       PIC S9(7)V99 COMP-3.
 *    05 LAST-NAME          PIC X(20).
 *    05 FIRST-NAME         PIC X(15).
 *    10 CLIENT-ADDR.
 *       10 STREET-ADDR    PIC X(25).
 *       10 CITY-COUNTY    PIC X(20).
 *       10 USA-STATE      PIC X(15).
 *    05 RESERVED           PIC X(7).
 *    05 COMMENTS           PIC X(50).
 *
 * WORKING-STORAGE SECTION.
 * 01 FLAGS.
 *    05 LASTREC           PIC X VALUE SPACE.
 *
 * PROCEDURE DIVISION.
 * OPEN-FILES.
 * READ-NEXT-RECORD.
 * CLOSE-STOP.
 * READ-RECORD.
 * WRITE-RECORD.
 */
public class CBL0001Processor {
    // File readers and writers
    // SELECT PRINT-LINE ASSIGN TO PRTLINE.
    // SELECT ACCT-REC   ASSIGN TO ACCTREC.
    private BufferedReader acctRecReader; // FD ACCT-REC
    private BufferedWriter printLineWriter; // FD PRINT-LINE

    // Input record fields (ACCT-FIELDS)
    // 05 ACCT-NO            PIC X(8).
    private String acctNo;
    // 05 ACCT-LIMIT         PIC S9(7)V99 COMP-3.
    private BigDecimal acctLimit;
    // 05 ACCT-BALANCE       PIC S9(7)V99 COMP-3.
    private BigDecimal acctBalance;
    // 05 LAST-NAME          PIC X(20).
    private String lastName;
    // 05 FIRST-NAME         PIC X(15).
    private String firstName;
    // 10 CLIENT-ADDR.
    //    10 STREET-ADDR    PIC X(25).
    private String streetAddr;
    //    10 CITY-COUNTY    PIC X(20).
    private String cityCounty;
    //    10 USA-STATE      PIC X(15).
    private String usaState;
    // 05 RESERVED           PIC X(7).
    private String reserved;
    // 05 COMMENTS           PIC X(50).
    private String commentsIn;

    // Output record fields (PRINT-REC)
    // 05 ACCT-NO-O      PIC X(8).
    private String acctNoOut;
    // 05 ACCT-LIMIT-O   PIC $$,$$$,$$9.99.
    private String acctLimitOut;
    // 05 ACCT-BALANCE-O PIC $$,$$$,$$9.99.
    private String acctBalanceOut;
    // 05 LAST-NAME-O    PIC X(20).
    private String lastNameOut;
    // 05 FIRST-NAME-O   PIC X(15).
    private String firstNameOut;
    // 05 COMMENTS-O     PIC X(50).
    private String commentsOut;

    // Working-storage variable
    // 05 LASTREC           PIC X VALUE SPACE.
    private boolean lastRec;

    private DecimalFormat decimalFormat;

    public static void main(String[] args) {
        CBL0001Processor program = new CBL0001Processor();
        program.execute();
    }

    /**
     * OPEN-FILES.
     */
    private void openFiles() throws IOException {
        // OPEN INPUT ACCT-REC.
        acctRecReader = Files.newBufferedReader(Paths.get("ACCTREC"));
        // OPEN OUTPUT PRINT-LINE.
        printLineWriter = Files.newBufferedWriter(Paths.get("PRTLINE"));
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        decimalFormat = new DecimalFormat("$##,###,##0.00", symbols);
        // INITIALIZE LASTREC = SPACE (' ' -> false)
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
     * CLOSE-STOP.
     */
    private void closeStop() throws IOException {
        // CLOSE ACCT-REC.
        if (acctRecReader != null) {
            acctRecReader.close();
        }
        // CLOSE PRINT-LINE.
        if (printLineWriter != null) {
            printLineWriter.close();
        }
        // GOBACK
    }

    /**
     * READ-RECORD.
     */
    private void readRecord() throws IOException {
        // READ ACCT-REC
        String line = acctRecReader.readLine();
        if (line == null) {
            // AT END MOVE 'Y' TO LASTREC
            lastRec = true;
            return;
        }
        String[] fields = line.split(",", -1);
        acctNo = ensureLength(fields[0], 8);
        acctLimit = new BigDecimal(fields[1]);
        acctBalance = new BigDecimal(fields[2]);
        lastName = ensureLength(fields[3], 20);
        firstName = ensureLength(fields[4], 15);
        streetAddr = fields[5];
        cityCounty = fields[6];
        usaState = fields[7];
        reserved = fields[8];
        commentsIn = ensureLength(fields[9], 50);
        lastRec = false;
    }

    /**
     * WRITE-RECORD.
     */
    private void writeRecord() throws IOException {
        // MOVE ACCT-NO TO ACCT-NO-O
        acctNoOut = acctNo;
        // MOVE ACCT-LIMIT TO ACCT-LIMIT-O
        acctLimitOut = decimalFormat.format(acctLimit);
        // MOVE ACCT-BALANCE TO ACCT-BALANCE-O
        acctBalanceOut = decimalFormat.format(acctBalance);
        // MOVE LAST-NAME TO LAST-NAME-O
        lastNameOut = lastName;
        // MOVE FIRST-NAME TO FIRST-NAME-O
        firstNameOut = firstName;
        // MOVE COMMENTS TO COMMENTS-O
        commentsOut = commentsIn;
        // WRITE PRINT-REC
        String record = String.join(",",
                acctNoOut,
                acctLimitOut,
                acctBalanceOut,
                lastNameOut,
                firstNameOut,
                commentsOut);
        printLineWriter.write(record);
        printLineWriter.newLine();
    }

    /**
     * Ensure string is of fixed length, padded or truncated.
     */
    private static String ensureLength(String value, int length) {
        if (value.length() > length) {
            return value.substring(0, length);
        }
        return String.format("%-" + length + "s", value);
    }

    /**
     * ENTRY POINT for PROCEDURE DIVISION
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
        // STOP RUN (end of program)
    }
}
