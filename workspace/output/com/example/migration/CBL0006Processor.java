package com.example.migration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

/**
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0006
 * AUTHOR.        Otto B. Boolean.
 *
 * ENVIRONMENT DIVISION.
 * INPUT-OUTPUT SECTION.
 * FILE-CONTROL.
 *     SELECT PRINT-LINE ASSIGN TO PRTLINE.
 *     SELECT ACCT-REC   ASSIGN TO ACCTREC.
 *
 * DATA DIVISION.
 * FILE SECTION.
 * FD  PRINT-LINE RECORDING MODE F.
 * FD  ACCT-REC  RECORDING MODE F.
 *
 * WORKING-STORAGE SECTION.
 * 01 FLAGS.
 *     05 LASTREC          PIC X VALUE SPACE.
 * 01 CLIENTS-PER-STATE.
 *     05 FILLER           PIC X(19) VALUE 'Virginia Clients = '.
 *     05 VIRGINIA-CLIENTS PIC 9(3) VALUE ZERO.
 *
 * PROCEDURE DIVISION.
 */
public class CBL0006Processor {
    private static final String PRINT_LINE_FILE = "PRTLINE";
    private static final String ACCT_REC_FILE = "ACCTREC";

    private RandomAccessFile acctRecFile;
    private BufferedWriter printLineWriter;

    // FILE SECTION: PRINT-REC output fields
    private String acctNoO;
    private String lastNameO;
    private BigDecimal acctLimitO;
    private BigDecimal acctBalanceO;

    // FILE SECTION: ACCT-FIELDS input fields
    private String acctNo;
    private BigDecimal acctLimit;
    private BigDecimal acctBalance;
    private String lastName;
    private String firstName;
    private String streetAddr;
    private String cityCounty;
    private String usaState;
    private String reserved;
    private String comments;

    // WORKING-STORAGE SECTION: FLAGS and counters
    private char lastRec = ' ';
    private int virginiaClients = 0;

    // WORKING-STORAGE SECTION: WS-CURRENT-DATE-DATA
    private int wsCurrentYear;
    private int wsCurrentMonth;
    private int wsCurrentDay;
    private int wsCurrentHour;
    private int wsCurrentMinute;
    private int wsCurrentSecond;
    private int wsCurrentCentisecond;

    // HEADER literals from 01 HEADER-1 to 01 HEADER-4
    private final String header1 = "Financial Report for" + String.format("%60s", "");
    private final String header3 = "Account  Last Name       Limit  Balance                                   ";
    private final String header4 = "--------  ----------      ----------  -------------                    ";
    private int hdrYr;
    private String hdrMo;
    private String hdrDay;

    public static void main(String[] args) {
        CBL0006Processor processor = new CBL0006Processor();
        try {
            processor.openFiles();
            processor.writeHeaders();
            processor.readNextRecord();
            processor.closeStop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // OPEN-FILES: OPEN INPUT ACCT-REC and OPEN OUTPUT PRINT-LINE
    private void openFiles() throws IOException {
        acctRecFile = new RandomAccessFile(ACCT_REC_FILE, "r");
        printLineWriter = new BufferedWriter(new FileWriter(PRINT_LINE_FILE));
    }

    // WRITE-HEADERS: MOVE CURRENT-DATE and write HEADER-1 through HEADER-4
    private void writeHeaders() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        // MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA
        wsCurrentYear = now.getYear();
        wsCurrentMonth = now.getMonthValue();
        wsCurrentDay = now.getDayOfMonth();
        wsCurrentHour = now.getHour();
        wsCurrentMinute = now.getMinute();
        wsCurrentSecond = now.getSecond();
        wsCurrentCentisecond = now.getNano() / 10_000_000;

        // MOVE WS-CURRENT-YEAR TO HDR-YR, WS-CURRENT-MONTH TO HDR-MO, WS-CURRENT-DAY TO HDR-DAY
        hdrYr = wsCurrentYear;
        hdrMo = String.format("%02d", wsCurrentMonth);
        hdrDay = String.format("%02d", wsCurrentDay);

        // WRITE PRINT-REC FROM HEADER-1
        printLineWriter.write(header1);
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM HEADER-2 (Year, Month, Day)
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Year ").append(String.format("%04d", hdrYr));
        sb2.append("  Month ").append(hdrMo);
        sb2.append("  Day ").append(hdrDay);
        while (sb2.length() < 80) {
            sb2.append(' ');
        }
        printLineWriter.write(sb2.toString());
        printLineWriter.newLine();

        // MOVE SPACES TO PRINT-REC and WRITE PRINT-REC AFTER ADVANCING 1 LINES
        printLineWriter.write(String.format("%80s", ""));
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM HEADER-3
        printLineWriter.write(header3);
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM HEADER-4
        printLineWriter.write(header4);
        printLineWriter.newLine();
    }

    // READ-NEXT-RECORD: PERFORM READ-RECORD then loop until LASTREC = 'Y'
    private void readNextRecord() throws IOException {
        // PERFORM READ-RECORD
        readRecord();
        // PERFORM UNTIL LASTREC = 'Y'
        while (lastRec != 'Y') {
            // PERFORM IS-STATE-VIRGINIA
            isStateVirginia();
            // PERFORM WRITE-RECORD
            writeRecord();
            // PERFORM READ-RECORD
            readRecord();
        }
    }

    // READ-RECORD: READ ACCT-REC and set LASTREC
    private void readRecord() throws IOException {
        String line = acctRecFile.readLine();
        if (line == null) {
            // AT END MOVE 'Y' TO LASTREC
            lastRec = 'Y';
        } else {
            lastRec = 'N';
            // Parse ACCT-FIELDS
            acctNo = line.substring(0, 8).trim();
            String limitStr = line.substring(8, 17).trim();
            acctLimit = new BigDecimal(limitStr).movePointLeft(2);
            String balanceStr = line.substring(17, 26).trim();
            acctBalance = new BigDecimal(balanceStr).movePointLeft(2);
            lastName = line.substring(26, 46).trim();
            firstName = line.substring(46, 61).trim();
            streetAddr = line.substring(61, 86).trim();
            cityCounty = line.substring(86, 106).trim();
            usaState = line.substring(106, 121).trim();
            reserved = line.substring(121, 128).trim();
            comments = line.substring(128, 178).trim();
        }
    }

    // IS-STATE-VIRGINIA: IF USA-STATE = 'Virginia' THEN ADD 1 TO VIRGINIA-CLIENTS
    private void isStateVirginia() {
        if ("Virginia".equals(usaState)) {
            virginiaClients += 1;
        }
    }

    // WRITE-RECORD: MOVE fields to PRINT-REC and WRITE PRINT-REC
    private void writeRecord() throws IOException {
        acctNoO = acctNo;
        acctLimitO = acctLimit;
        acctBalanceO = acctBalance;
        lastNameO = lastName;
        String record = formatPrintRec();
        printLineWriter.write(record);
        printLineWriter.newLine();
    }

    // Helper to format PRINT-REC record line
    private String formatPrintRec() {
        DecimalFormat moneyFormat = new DecimalFormat("$#,##0.00");
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-8s", acctNoO));
        sb.append("  ");
        sb.append(String.format("%-20s", lastNameO));
        sb.append("  ");
        sb.append(String.format("%9s", moneyFormat.format(acctLimitO)));
        sb.append("  ");
        sb.append(String.format("%9s", moneyFormat.format(acctBalanceO)));
        sb.append("  ");
        while (sb.length() < 80) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // CLOSE-STOP: WRITE summary, CLOSE ACCT-REC and PRINT-LINE, GOBACK.
    private void closeStop() throws IOException {
        String summary = String.format("Virginia Clients = %3d", virginiaClients);
        printLineWriter.write(summary);
        printLineWriter.newLine();
        acctRecFile.close();
        printLineWriter.close();
    }
}
