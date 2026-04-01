/*
IDENTIFICATION DIVISION.
PROGRAM-ID.    CBL0009
AUTHOR.        Otto B. Mathwiz.

ENVIRONMENT DIVISION.
INPUT-OUTPUT SECTION.
FILE-CONTROL.
    SELECT PRINT-LINE ASSIGN TO PRTLINE.
    SELECT ACCT-REC   ASSIGN TO ACCTREC.

DATA DIVISION.
FILE SECTION.
FD  PRINT-LINE RECORDING MODE F.
01  PRINT-REC.
    05  ACCT-NO-O      PIC X(8).
    05  FILLER         PIC X(02) VALUE SPACES.
    05  LAST-NAME-O    PIC X(20).
    05  FILLER         PIC X(02) VALUE SPACES.
    05  ACCT-LIMIT-O   PIC $$,$$$,$$9.99.
    05  FILLER         PIC X(02) VALUE SPACES.
    05  ACCT-BALANCE-O PIC $$,$$$,$$9.99.
    05  FILLER         PIC X(02) VALUE SPACES.

FD  ACCT-REC RECORDING MODE F.
01  ACCT-FIELDS.
    05  ACCT-NO            PIC X(8).
    05  ACCT-LIMIT         PIC S9(7)V99 COMP-3.
    05  ACCT-BALANCE       PIC S9(7)V99 COMP-3.
    05  LAST-NAME          PIC X(20).
    05  FIRST-NAME         PIC X(15).
    05  CLIENT-ADDR.
        10  STREET-ADDR    PIC X(25).
        10  CITY-COUNTY    PIC X(20).
        10  USA-STATE      PIC X(15).
    05  RESERVED           PIC X(7).
    05  COMMENTS           PIC X(50).

WORKING-STORAGE SECTION.
01  FLAGS.
    05 LASTREC          PIC X VALUE SPACE.

01  TLIMIT-TBALANCE.
    05 TLIMITED            PIC S9(9)V99 COMP-3 VALUE ZERO.
    05 TBALANCE            PIC S9(9)V99 COMP-3 VALUE ZERO.

01  HEADER-1.
    05  FILLER         PIC X(20) VALUE 'Financial Report for'.
    05  FILLER         PIC X(60) VALUE SPACES.

01  HEADER-2.
    05  FILLER         PIC X(05) VALUE 'Year '.
    05  HDR-YR         PIC 9(04).
    05  FILLER         PIC X(02) VALUE SPACES.
    05  FILLER         PIC X(06) VALUE 'Month '.
    05  HDR-MO         PIC X(02).
    05  FILLER         PIC X(02) VALUE SPACES.
    05  FILLER         PIC X(04) VALUE 'Day '.
    05  HDR-DAY        PIC X(02).
    05  FILLER         PIC X(56) VALUE SPACES.

01  HEADER-3.
    05  FILLER         PIC X(08) VALUE 'Account '.
    05  FILLER         PIC X(02) VALUE SPACES.
    05  FILLER         PIC X(10) VALUE 'Last Name '.
    05  FILLER         PIC X(15) VALUE SPACES.
    05  FILLER         PIC X(06) VALUE 'Limit '.
    05  FILLER         PIC X(06) VALUE SPACES.
    05  FILLER         PIC X(08) VALUE 'Balance '.
    05  FILLER         PIC X(40) VALUE SPACES.

01  HEADER-4.
    05  FILLER         PIC X(08) VALUE '--------'.
    05  FILLER         PIC X(02) VALUE SPACES.
    05  FILLER         PIC X(10) VALUE '----------'.
    05  FILLER         PIC X(15) VALUE SPACES.
    05  FILLER         PIC X(10) VALUE '----------'.
    05  FILLER         PIC X(02) VALUE SPACES.
    05  FILLER         PIC X(13) VALUE '-------------'.
    05  FILLER         PIC X(40) VALUE SPACES.

01  TRAILER-1.
    05  FILLER         PIC X(31) VALUE SPACES.
    05  FILLER         PIC X(14) VALUE '--------------'.
    05  FILLER         PIC X(01) VALUE SPACES.
    05  FILLER         PIC X(14) VALUE '--------------'.
    05  FILLER         PIC X(40) VALUE SPACES.

01  TRAILER-2.
    05  FILLER         PIC X(22) VALUE SPACES.
    05  FILLER         PIC X(08) VALUE 'Totals ='.
    05  FILLER         PIC X(01) VALUE SPACES.
    05  TLIMIT-O       PIC $$$,$$$,$$9.99.
    05  FILLER         PIC X(01) VALUE SPACES.
    05  TBALANCE-O     PIC $$$,$$$,$$9.99.
    05  FILLER         PIC X(40) VALUE SPACES.

01 WS-CURRENT-DATE-DATA.
    05  WS-CURRENT-DATE.
        10  WS-CURRENT-YEAR         PIC 9(04).
        10  WS-CURRENT-MONTH        PIC 9(02).
        10  WS-CURRENT-DAY          PIC 9(02).
    05  WS-CURRENT-TIME.
        10  WS-CURRENT-HOUR         PIC 9(02).
        10  WS-CURRENT-MINUTE       PIC 9(02).
        10  WS-CURRENT-SECOND       PIC 9(02).
        10  WS-CURRENT-CENTISECOND  PIC 9(02).

PROCEDURE DIVISION.
OPEN-FILES.
WRITE-HEADERS.
READ-NEXT-RECORD.
WRITE-TLIMIT-TBALANCE.
CLOSE-STOP.
READ-RECORD.
LIMIT-BALANCE-TOTAL.
WRITE-RECORD.
GOBACK.
*/
package com.example.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;

public class CBL0009Processor {
    private InputStream acctRecInput;
    private BufferedWriter printLineWriter;

    // File record buffers
    private String acctNoO;
    private BigDecimal acctLimitO;
    private BigDecimal acctBalanceO;
    private String lastNameO;

    // Input record fields
    private static class ClientAddr {
        String streetAddr;
        String cityCounty;
        String usaState;
    }

    private static class AcctFields {
        String acctNo;
        BigDecimal acctLimit;
        BigDecimal acctBalance;
        String lastName;
        String firstName;
        ClientAddr clientAddr = new ClientAddr();
        String reserved;
        String comments;
    }

    private AcctFields currentAccount = new AcctFields();

    // Working-storage
    private boolean lastRec = false;
    private BigDecimal tLimited = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tBalance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    // WS-CURRENT-DATE-DATA fields
    private int wsCurrentYear;
    private int wsCurrentMonth;
    private int wsCurrentDay;
    private int wsCurrentHour;
    private int wsCurrentMinute;
    private int wsCurrentSecond;
    private int wsCurrentCentisecond;

    // Header fields
    private int hdrYr;
    private String hdrMo;
    private String hdrDay;

    // Trailer variables
    private BigDecimal tLimitO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tBalanceO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    // Decimal format for output fields
    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("###,###,###,##0.00", symbols);
        MONEY_FORMAT.setRoundingMode(RoundingMode.UNNECESSARY);
    }

    public static void main(String[] args) {
        try {
            new CBL0009Processor().execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void execute() throws IOException {
        openFiles();
        writeHeaders();
        readNextRecord();
        writeTlimitTbalance();
        closeStop();
        // GOBACK equivalent: return from program
    }

    // OPEN-FILES
    private void openFiles() throws IOException {
        // OPEN INPUT  ACCT-REC
        acctRecInput = Files.newInputStream(Paths.get("ACCTREC"));
        // OPEN OUTPUT PRINT-LINE
        printLineWriter = Files.newBufferedWriter(Paths.get("PRINTLINE"));
    }

    // WRITE-HEADERS
    private void writeHeaders() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        // MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA
        wsCurrentYear = now.getYear();
        wsCurrentMonth = now.getMonthValue();
        wsCurrentDay = now.getDayOfMonth();
        wsCurrentHour = now.getHour();
        wsCurrentMinute = now.getMinute();
        wsCurrentSecond = now.getSecond();
        wsCurrentCentisecond = now.getNano() / 10000000;

        // MOVE WS-CURRENT-YEAR  TO HDR-YR and similar
        hdrYr = wsCurrentYear;
        hdrMo = String.format("%02d", wsCurrentMonth);
        hdrDay = String.format("%02d", wsCurrentDay);

        // WRITE PRINT-REC FROM HEADER-1
        String header1 = String.format("%-20s", "Financial Report for") + repeat(' ', 60);
        printLineWriter.write(header1);
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM HEADER-2
        StringBuilder h2 = new StringBuilder();
        h2.append("Year ");
        h2.append(String.format("%04d", hdrYr));
        h2.append("  ");
        h2.append("Month ");
        h2.append(hdrMo);
        h2.append("  ");
        h2.append("Day ");
        h2.append(hdrDay);
        h2.append(repeat(' ', 56));
        printLineWriter.write(h2.toString());
        printLineWriter.newLine();

        // MOVE SPACES TO PRINT-REC and WRITE AFTER ADVANCING 1 LINES
        printLineWriter.write(repeat(' ', header1.length()));
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM HEADER-3
        StringBuilder h3 = new StringBuilder();
        h3.append(String.format("%-8s", "Account "));
        h3.append("  ");
        h3.append(String.format("%-10s", "Last Name "));
        h3.append(repeat(' ', 15));
        h3.append(String.format("%-6s", "Limit "));
        h3.append(repeat(' ', 6));
        h3.append(String.format("%-8s", "Balance "));
        h3.append(repeat(' ', 40));
        printLineWriter.write(h3.toString());
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM HEADER-4
        StringBuilder h4 = new StringBuilder();
        h4.append(String.format("%-8s", "--------"));
        h4.append("  ");
        h4.append(String.format("%-10s", "----------"));
        h4.append(repeat(' ', 15));
        h4.append(String.format("%-10s", "----------"));
        h4.append("  ");
        h4.append(String.format("%-13s", "-------------"));
        h4.append(repeat(' ', 40));
        printLineWriter.write(h4.toString());
        printLineWriter.newLine();

        // MOVE SPACES TO PRINT-REC
        printLineWriter.write(repeat(' ', header1.length()));
        printLineWriter.newLine();
    }

    // READ-NEXT-RECORD
    private void readNextRecord() throws IOException {
        // PERFORM READ-RECORD
        readRecord();
        // PERFORM UNTIL LASTREC = 'Y'
        while (!lastRec) {
            // PERFORM LIMIT-BALANCE-TOTAL
            limitBalanceTotal();
            // PERFORM WRITE-RECORD
            writeRecord();
            // PERFORM READ-RECORD
            readRecord();
        }
    }

    // WRITE-TLIMIT-TBALANCE
    private void writeTlimitTbalance() throws IOException {
        // MOVE TLIMIT   TO TLIMIT-O
        tLimitO = tLimited;
        // MOVE TBALANCE TO TBALANCE-O
        tBalanceO = tBalance;

        // WRITE PRINT-REC FROM TRAILER-1
        String trailer1 = repeat(' ', 31) + "--------------" + " " + "--------------" + repeat(' ', 40);
        printLineWriter.write(trailer1);
        printLineWriter.newLine();

        // WRITE PRINT-REC FROM TRAILER-2
        String limitStr = MONEY_FORMAT.format(tLimitO);
        String balanceStr = MONEY_FORMAT.format(tBalanceO);
        String trailer2 = repeat(' ', 22) + "Totals =" + " "
                            + padLeft(limitStr, 12) + " "
                            + padLeft(balanceStr, 12)
                            + repeat(' ', 40);
        printLineWriter.write(trailer2);
        printLineWriter.newLine();
    }

    // CLOSE-STOP
    private void closeStop() throws IOException {
        // CLOSE ACCT-REC
        if (acctRecInput != null) acctRecInput.close();
        // CLOSE PRINT-LINE
        if (printLineWriter != null) printLineWriter.close();
    }

    // READ-RECORD
    private void readRecord() throws IOException {
        // READ ACCT-REC
        byte[] buffer = new byte[170];
        int bytesRead = acctRecInput.read(buffer);
        // AT END MOVE 'Y' TO LASTREC
        if (bytesRead < 0) {
            lastRec = true;
            return;
        }
        // Unpack fields
        currentAccount.acctNo = new String(buffer, 0, 8).trim();
        currentAccount.acctLimit = decodeComp3(buffer, 8, 5, 2);
        currentAccount.acctBalance = decodeComp3(buffer, 13, 5, 2);
        currentAccount.lastName = new String(buffer, 18, 20).trim();
        currentAccount.firstName = new String(buffer, 38, 15).trim();
        currentAccount.clientAddr.streetAddr = new String(buffer, 53, 25).trim();
        currentAccount.clientAddr.cityCounty = new String(buffer, 78, 20).trim();
        currentAccount.clientAddr.usaState = new String(buffer, 98, 15).trim();
        currentAccount.reserved = new String(buffer, 113, 7).trim();
        currentAccount.comments = new String(buffer, 120, 50).trim();
    }

    // LIMIT-BALANCE-TOTAL
    private void limitBalanceTotal() {
        // COMPUTE TLIMIT   = TLIMIT   + ACCT-LIMIT   END-COMPUTE
        tLimited = tLimited.add(currentAccount.acctLimit);
        // COMPUTE TBALANCE = TBALANCE + ACCT-BALANCE END-COMPUTE
        tBalance = tBalance.add(currentAccount.acctBalance);
    }

    // WRITE-RECORD
    private void writeRecord() throws IOException {
        // MOVE ACCT-NO      TO  ACCT-NO-O
        acctNoO = padRight(currentAccount.acctNo, 8);
        // MOVE ACCT-LIMIT   TO  ACCT-LIMIT-O
        acctLimitO = currentAccount.acctLimit;
        // MOVE ACCT-BALANCE TO  ACCT-BALANCE-O
        acctBalanceO = currentAccount.acctBalance;
        // MOVE LAST-NAME    TO  LAST-NAME-O
        lastNameO = padRight(currentAccount.lastName, 20);

        // WRITE PRINT-REC
        StringBuilder rec = new StringBuilder();
        rec.append(padRight(acctNoO, 8));
        rec.append(repeat(' ', 2));
        rec.append(padRight(lastNameO, 20));
        rec.append(repeat(' ', 2));
        rec.append(padLeft(MONEY_FORMAT.format(acctLimitO), 11));
        rec.append(repeat(' ', 2));
        rec.append(padLeft(MONEY_FORMAT.format(acctBalanceO), 11));
        rec.append(repeat(' ', 2));

        printLineWriter.write(rec.toString());
        printLineWriter.newLine();
    }

    // Utility methods
    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }

    private static String padLeft(String s, int length) {
        if (s.length() >= length) return s;
        return repeat(' ', length - s.length()) + s;
    }

    private static String padRight(String s, int length) {
        if (s.length() >= length) return s;
        return s + repeat(' ', length - s.length());
    }

    // Unpack COMP-3 packed decimals
    private static BigDecimal decodeComp3(byte[] data, int offset, int length, int scale) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int b = data[offset + i] & 0xFF;
            int high = (b & 0xF0) >>> 4;
            int low = b & 0x0F;
            if (i < length - 1) {
                digits.append(high).append(low);
            } else {
                digits.append(high);
                char signNibble = Integer.toHexString(low).toUpperCase().charAt(0);
                boolean negative = (signNibble == 'D' || signNibble == 'B');
                BigDecimal val = new BigDecimal(digits.toString());
                val = val.movePointLeft(scale);
                if (negative) val = val.negate();
                return val.setScale(scale, RoundingMode.UNNECESSARY);
            }
        }
        return BigDecimal.ZERO.setScale(scale, RoundingMode.UNNECESSARY);
    }
}
