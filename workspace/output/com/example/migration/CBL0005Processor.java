package com.example.migration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;

/**
 * Program ID: CBL0005
 * Author: Otto B. Formatted
 * Migrated from COBOL to Java
 */
public class CBL0005Processor {
    private static final String PRINT_LINE_FILE = "PRTLINE";
    private static final String ACCT_REC_FILE = "ACCTREC";

    // FILE handles
    private RandomAccessFile acctRecFile;
    private BufferedWriter printLineWriter;

    // FILE SECTION: PRINT-REC output record fields
    private String acctNoO;
    private String lastNameO;
    private String acctLimitO;
    private String acctBalanceO;

    // FILE SECTION: ACCT-FIELDS input record fields
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

    // WORKING-STORAGE SECTION
    private char lastRec = ' ';

    // WS-CURRENT-DATE-DATA
    private int wsCurrentYear;
    private int wsCurrentMonth;
    private int wsCurrentDay;
    private int wsCurrentHour;
    private int wsCurrentMinute;
    private int wsCurrentSecond;
    private int wsCurrentCentisecond;

    private int hdrYr;
    private String hdrMo;
    private String hdrDay;

    public static void main(String[] args) {
        new CBL0005Processor().execute();
    }

    public void execute() {
        try {
            openFiles();
            openFilesEnd();
            writeHeaders();
            readNextRecord();
            closeStop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // OPEN-FILES
    private void openFiles() throws IOException {
        acctRecFile = new RandomAccessFile(ACCT_REC_FILE, "r");
        printLineWriter = new BufferedWriter(new FileWriter(PRINT_LINE_FILE));
    }

    // OPEN-FILES-END
    private void openFilesEnd() {
        // End of open-files paragraph for visual delimiter
    }

    // WRITE-HEADERS
    private void writeHeaders() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        wsCurrentYear = now.getYear();
        wsCurrentMonth = now.getMonthValue();
        wsCurrentDay = now.getDayOfMonth();
        wsCurrentHour = now.getHour();
        wsCurrentMinute = now.getMinute();
        wsCurrentSecond = now.getSecond();
        wsCurrentCentisecond = now.getNano() / 10_000_000;

        hdrYr = wsCurrentYear;
        hdrMo = String.format("%02d", wsCurrentMonth);
        hdrDay = String.format("%02d", wsCurrentDay);

        // HEADER-1 (Financial Report for)
        String header1 = String.format("%-20s", "Financial Report for")
                + String.format("%-60s", "");
        printLineWriter.write(header1);
        printLineWriter.newLine();

        // HEADER-2 (Year, Month, Day)
        String header2 = String.format("%-5s", "Year ")
                + String.format("%04d", hdrYr)
                + String.format("%-2s", "")
                + String.format("%-6s", "Month ")
                + hdrMo
                + String.format("%-2s", "")
                + String.format("%-4s", "Day ")
                + hdrDay
                + String.format("%-56s", "");
        printLineWriter.write(header2);
        printLineWriter.newLine();

        // MOVE SPACES TO PRINT-REC (blank line)
        printLineWriter.write(String.format("%-62s", ""));
        printLineWriter.newLine();

        // HEADER-3 (Account, Last Name, Limit, Balance)
        String header3 = String.format("%-8s", "Account ")
                + String.format("%-2s", "")
                + String.format("%-10s", "Last Name ")
                + String.format("%-15s", "")
                + String.format("%-6s", "Limit ")
                + String.format("%-6s", "")
                + String.format("%-8s", "Balance ")
                + String.format("%-40s", "");
        printLineWriter.write(header3);
        printLineWriter.newLine();

        // HEADER-4 (--------, ----------, ----------, -------------)
        String header4 = String.format("%-8s", "--------")
                + String.format("%-2s", "")
                + String.format("%-10s", "----------")
                + String.format("%-15s", "")
                + String.format("%-10s", "----------")
                + String.format("%-2s", "")
                + String.format("%-13s", "-------------")
                + String.format("%-40s", "");
        printLineWriter.write(header4);
        printLineWriter.newLine();

        // MOVE SPACES TO PRINT-REC (blank line)
        printLineWriter.write(String.format("%-62s", ""));
        printLineWriter.newLine();
    }

    // READ-NEXT-RECORD
    private void readNextRecord() throws IOException {
        // PERFORM READ-RECORD
        readRecord();
        // PERFORM UNTIL LASTREC = 'Y'
        while (lastRec != 'Y') {
            // PERFORM WRITE-RECORD
            writeRecord();
            // PERFORM READ-RECORD
            readRecord();
        }
    }

    // CLOSE-STOP
    private void closeStop() throws IOException {
        // CLOSE ACCT-REC
        if (acctRecFile != null) {
            acctRecFile.close();
        }
        // CLOSE PRINT-LINE
        if (printLineWriter != null) {
            printLineWriter.close();
        }
        // GOBACK
        System.exit(0);
    }

    // READ-RECORD
    private void readRecord() throws IOException {
        try {
            byte[] acctNoBuf = new byte[8];
            acctRecFile.readFully(acctNoBuf);
            acctNo = new String(acctNoBuf, StandardCharsets.US_ASCII);

            byte[] acctLimitBuf = new byte[5];
            acctRecFile.readFully(acctLimitBuf);
            acctLimit = decodeComp3(acctLimitBuf, 2);

            byte[] acctBalanceBuf = new byte[5];
            acctRecFile.readFully(acctBalanceBuf);
            acctBalance = decodeComp3(acctBalanceBuf, 2);

            byte[] lastNameBuf = new byte[20];
            acctRecFile.readFully(lastNameBuf);
            lastName = new String(lastNameBuf, StandardCharsets.US_ASCII);

            byte[] firstNameBuf = new byte[15];
            acctRecFile.readFully(firstNameBuf);
            firstName = new String(firstNameBuf, StandardCharsets.US_ASCII);

            byte[] streetAddrBuf = new byte[25];
            acctRecFile.readFully(streetAddrBuf);
            streetAddr = new String(streetAddrBuf, StandardCharsets.US_ASCII);

            byte[] cityCountyBuf = new byte[20];
            acctRecFile.readFully(cityCountyBuf);
            cityCounty = new String(cityCountyBuf, StandardCharsets.US_ASCII);

            byte[] usaStateBuf = new byte[15];
            acctRecFile.readFully(usaStateBuf);
            usaState = new String(usaStateBuf, StandardCharsets.US_ASCII);

            byte[] reservedBuf = new byte[7];
            acctRecFile.readFully(reservedBuf);
            reserved = new String(reservedBuf, StandardCharsets.US_ASCII);

            byte[] commentsBuf = new byte[50];
            acctRecFile.readFully(commentsBuf);
            comments = new String(commentsBuf, StandardCharsets.US_ASCII);

            lastRec = ' ';
        } catch (IOException e) {
            // AT END MOVE 'Y' TO LASTREC
            lastRec = 'Y';
        }
    }

    // WRITE-RECORD
    private void writeRecord() throws IOException {
        // MOVE ACCT-NO TO ACCT-NO-O
        acctNoO = acctNo;
        // MOVE LAST-NAME TO LAST-NAME-O
        lastNameO = lastName;
        // MOVE ACCT-LIMIT TO ACCT-LIMIT-O
        DecimalFormat df = new DecimalFormat("##,###,##0.00");
        df.setGroupingUsed(true);
        acctLimitO = df.format(acctLimit);
        // MOVE ACCT-BALANCE TO ACCT-BALANCE-O
        acctBalanceO = df.format(acctBalance);

        // WRITE PRINT-REC
        String record = String.format("%-8s", acctNoO)
                + "  " + String.format("%-20s", lastNameO)
                + "  " + String.format("%13s", acctLimitO)
                + "  " + String.format("%13s", acctBalanceO)
                + "  ";
        printLineWriter.write(record);
        printLineWriter.newLine();
    }

    // Helper for COMP-3 decoding
    private BigDecimal decodeComp3(byte[] packed, int scale) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packed.length; i++) {
            int b = packed[i] & 0xFF;
            int hi = (b >> 4) & 0xF;
            int lo = b & 0xF;
            if (i < packed.length - 1) {
                sb.append(hi);
                sb.append(lo);
            } else {
                sb.append(hi);
                char signChar = Integer.toHexString(lo).toUpperCase().charAt(0);
                BigDecimal value = new BigDecimal(sb.toString());
                if (signChar == 'D') {
                    value = value.negate();
                }
                return value.movePointLeft(scale);
            }
        }
        return BigDecimal.ZERO;
    }
}
