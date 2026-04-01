/*
 *-----------------------
 * Copyright Contributors to the COBOL Programming Course
 * SPDX-License-Identifier: CC-BY-4.0
 *-----------------------
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0004
 * AUTHOR.        Otto B. Formatted
 *--------------------
 */
package com.example.migration;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * CBL0004Processor - migrated from COBOL CBL0004.cobol
 */
public class CBL0004Processor {
    // Group definitions for FILE SECTION record layouts and WORKING-STORAGE
    private static class PrintRec {
        String acctNoO;        // PIC X(8)
        String lastNameO;      // PIC X(20)
        BigDecimal acctLimitO; // PIC $$,$$$,$$9.99
        BigDecimal acctBalanceO; // PIC $$,$$$,$$9.99
    }

    private static class AcctFields {
        String acctNo;            // PIC X(8)
        BigDecimal acctLimit;     // PIC S9(7)V99 COMP-3
        BigDecimal acctBalance;   // PIC S9(7)V99 COMP-3
        String lastName;          // PIC X(20)
        String firstName;         // PIC X(15)
        String streetAddr;        // PIC X(25)
        String cityCounty;        // PIC X(20)
        String usaState;          // PIC X(15)
        String reserved;          // PIC X(7)
        String comments;          // PIC X(50)
    }

    private static class WSCurrentDate {
        int wsCurrentYear;      // PIC 9(04)
        int wsCurrentMonth;     // PIC 9(02)
        int wsCurrentDay;       // PIC 9(02)
    }

    private static class WSCurrentTime {
        int wsCurrentHour;       // PIC 9(02)
        int wsCurrentMinute;     // PIC 9(02)
        int wsCurrentSecond;     // PIC 9(02)
        int wsCurrentCentisecond; // PIC 9(02)
    }

    private static class WSCurrentDateData {
        WSCurrentDate WS_CURRENT_DATE = new WSCurrentDate();
        WSCurrentTime WS_CURRENT_TIME = new WSCurrentTime();
    }

    // File readers and writers
    private DataInputStream acctRecStream;
    private BufferedWriter printLineWriter;

    // Record group instances
    private PrintRec printRec;
    private AcctFields acctFields;

    // Working-storage group instance
    private WSCurrentDateData wsCurrentDateData;

    // Working-storage variables (other)
    private char lastRec;       // PIC X
    private String header1;     // HEADER-1 group
    private String header2;     // HEADER-2 group
    private String header3;     // HEADER-3 group
    private String header4;     // HEADER-4 group
    private int hdrYr;          // PIC 9(04)
    private int hdrMo;          // PIC X(02)
    private int hdrDay;         // PIC X(02)

    public CBL0004Processor() {
        this.printRec = new PrintRec();
        this.acctFields = new AcctFields();
        this.wsCurrentDateData = new WSCurrentDateData();
    }

    public static void main(String[] args) {
        CBL0004Processor processor = new CBL0004Processor();
        try {
            processor.openFiles();
            processor.writeHeaders();
            processor.readNextRecord();
            processor.closeStop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openFiles() throws IOException {
        acctRecStream = new DataInputStream(new FileInputStream("ACCTREC"));
        printLineWriter = new BufferedWriter(new FileWriter("PRTLINE"));
        lastRec = 'N';
        openFilesEnd();
    }

    public void openFilesEnd() {
        // OPEN-FILES-END paragraph: no operations
    }

    public void writeHeaders() throws IOException {
        // WRITE-HEADERS paragraph
        // Initialize current date and time
        LocalDateTime now = LocalDateTime.now();
        // Populate group fields
        wsCurrentDateData.WS_CURRENT_DATE.wsCurrentYear = now.getYear();
        wsCurrentDateData.WS_CURRENT_DATE.wsCurrentMonth = now.getMonthValue();
        wsCurrentDateData.WS_CURRENT_DATE.wsCurrentDay = now.getDayOfMonth();
        wsCurrentDateData.WS_CURRENT_TIME.wsCurrentHour = now.getHour();
        wsCurrentDateData.WS_CURRENT_TIME.wsCurrentMinute = now.getMinute();
        wsCurrentDateData.WS_CURRENT_TIME.wsCurrentSecond = now.getSecond();
        wsCurrentDateData.WS_CURRENT_TIME.wsCurrentCentisecond = wsCurrentDateData.WS_CURRENT_TIME.wsCurrentSecond * 100;

        // Move values to header fields
        hdrYr = wsCurrentDateData.WS_CURRENT_DATE.wsCurrentYear;
        hdrMo = wsCurrentDateData.WS_CURRENT_DATE.wsCurrentMonth;
        hdrDay = wsCurrentDateData.WS_CURRENT_DATE.wsCurrentDay;

        // Build headers according to PIC clauses
        header1 = String.format("%-20s%-60s", "Financial Report for", "");
        header2 = String.format("%-5s%04d%-2s%-6s%02d%-2s%-4s%02d%-56s",
                "Year ", hdrYr, "", "Month ", hdrMo, "", "Day ", hdrDay, "");
        header3 = String.format("%-8s%-2s%-10s%-15s%-6s%-6s%-8s%-40s",
                "Account ", "", "Last Name ", "", "Limit ", "", "Balance ", "");
        header4 = String.format("%-8s%-2s%-10s%-15s%-10s%-2s%-13s%-40s",
                "--------", "", "----------", "", "----------", "", "-------------", "");

        // Write headers to output file
        printLineWriter.write(header1);
        printLineWriter.newLine();
        printLineWriter.write(header2);
        printLineWriter.newLine();
        printLineWriter.write("");
        printLineWriter.newLine();
        printLineWriter.write(header3);
        printLineWriter.newLine();
        printLineWriter.write(header4);
        printLineWriter.newLine();
        printLineWriter.write("");
        printLineWriter.newLine();
    }

    public void readNextRecord() throws IOException {
        // READ-NEXT-RECORD paragraph
        // Perform initial read
        readRecord();
        // Loop until LASTREC = 'Y'
        while (lastRec != 'Y') {
            writeRecord();
            readRecord();
        }
    }

    public void closeStop() throws IOException {
        // CLOSE-STOP paragraph
        acctRecStream.close();
        printLineWriter.close();
        // GOBACK
    }

    public void readRecord() throws IOException {
        // READ-RECORD paragraph
        try {
            byte[] acctNoBytes = new byte[8];
            acctRecStream.readFully(acctNoBytes);
            acctFields.acctNo = new String(acctNoBytes, StandardCharsets.US_ASCII).trim();

            acctFields.acctLimit = readComp3Decimal(acctRecStream, 7, 2);
            acctFields.acctBalance = readComp3Decimal(acctRecStream, 7, 2);

            byte[] lastNameBytes = new byte[20];
            acctRecStream.readFully(lastNameBytes);
            acctFields.lastName = new String(lastNameBytes, StandardCharsets.US_ASCII).trim();

            byte[] firstNameBytes = new byte[15];
            acctRecStream.readFully(firstNameBytes);
            acctFields.firstName = new String(firstNameBytes, StandardCharsets.US_ASCII).trim();

            byte[] streetBytes = new byte[25];
            acctRecStream.readFully(streetBytes);
            acctFields.streetAddr = new String( streetBytes, StandardCharsets.US_ASCII ).trim();

            byte[] cityBytes = new byte[20];
            acctRecStream.readFully(cityBytes);
            acctFields.cityCounty = new String(cityBytes, StandardCharsets.US_ASCII).trim();

            byte[] stateBytes = new byte[15];
            acctRecStream.readFully(stateBytes);
            acctFields.usaState = new String(stateBytes, StandardCharsets.US_ASCII).trim();

            byte[] reservedBytes = new byte[7];
            acctRecStream.readFully(reservedBytes);
            acctFields.reserved = new String(reservedBytes, StandardCharsets.US_ASCII).trim();

            byte[] commentsBytes = new byte[50];
            acctRecStream.readFully(commentsBytes);
            acctFields.comments = new String(commentsBytes, StandardCharsets.US_ASCII).trim();
        } catch (EOFException e) {
            // At end of file, set LASTREC flag
            lastRec = 'Y';
        }
    }

    public void writeRecord() throws IOException {
        // WRITE-RECORD paragraph
        printRec.acctNoO = acctFields.acctNo;
        printRec.acctLimitO = acctFields.acctLimit;
        printRec.acctBalanceO = acctFields.acctBalance;
        printRec.lastNameO = acctFields.lastName;

        String formattedLimit = formatCurrency(printRec.acctLimitO);
        String formattedBalance = formatCurrency(printRec.acctBalanceO);

        String line = String.format("%-8s%2s%-20s%2s%12s%2s%12s%2s",
                printRec.acctNoO, "  ", printRec.lastNameO, "  ", formattedLimit, "  ", formattedBalance, "  ");
        printLineWriter.write(line);
        printLineWriter.newLine();
    }

    private BigDecimal readComp3Decimal(DataInputStream dis, int integerDigits, int decimalDigits) throws IOException {
        int totalDigits = integerDigits + decimalDigits;
        int byteLen = (totalDigits + 1 + 1) / 2;
        byte[] data = new byte[byteLen];
        dis.readFully(data);
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int hi = (data[i] & 0xF0) >>> 4;
            int lo = data[i] & 0x0F;
            if (i < data.length - 1) {
                digits.append(hi).append(lo);
            } else {
                digits.append(hi);
                if (lo == 0x0D) {
                    digits.insert(0, '-');
                }
            }
        }
        BigDecimal value = new BigDecimal(digits.toString());
        if (decimalDigits > 0) {
            value = value.movePointLeft(decimalDigits);
        }
        return value;
    }

    private String formatCurrency(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(amount);
    }
}
