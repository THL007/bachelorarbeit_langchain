package com.example.migration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0008
 * AUTHOR.        Otto B. Mathwiz.
 */
public class CBL0008Processor {
    // ENVIRONMENT DIVISION: FILE-CONTROL mappings
    private static final String ACCT_REC_FILE = "ACCTREC";
    private static final String PRINT_LINE_FILE = "PRTLINE";
    
    // FD variables
    private RandomAccessFile acctRecFile;
    private BufferedWriter printLineWriter;
    
    // RECORD lengths
    private static final int ACCT_REC_LENGTH = 170;
    private static final int PRINT_REC_LENGTH = 62;

    // WORKING-STORAGE SECTION variables
    // FLAGS
    private char lastRec = ' ';
    
    // TLIMIT-TBALANCE
    private BigDecimal tLimit = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tBalance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    
    // WS-CURRENT-DATE-DATA
    private int wsCurrentYear;
    private int wsCurrentMonth;
    private int wsCurrentDay;
    private int wsCurrentHour;
    private int wsCurrentMinute;
    private int wsCurrentSecond;
    private int wsCurrentCentisecond;

    // HEADER-2 variables
    private int hdrYr;
    private String hdrMo;
    private String hdrDay;

    // Input record container
    private AcctFields currentAccount;

    public static void main(String[] args) {
        new CBL0008Processor().execute();
    }

    public void execute() {
        try {
            openFiles();
            writeHeaders();
            readNextRecord();
            writeTlimitTbalance();
            closeStop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // OPEN-FILES
    private void openFiles() throws IOException {
        acctRecFile = new RandomAccessFile(ACCT_REC_FILE, "r");
        Files.deleteIfExists(Paths.get(PRINT_LINE_FILE));
        printLineWriter = Files.newBufferedWriter(Paths.get(PRINT_LINE_FILE));
    }

    // WRITE-HEADERS (HEADER-1 to HEADER-4)
    private void writeHeaders() throws IOException {
        // MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA
        LocalDate currentDate = LocalDate.now();
        wsCurrentYear = currentDate.getYear();
        wsCurrentMonth = currentDate.getMonthValue();
        wsCurrentDay = currentDate.getDayOfMonth();
        // time fields not moved in COBOL but declared
        wsCurrentHour = 0;
        wsCurrentMinute = 0;
        wsCurrentSecond = 0;
        wsCurrentCentisecond = 0;
        
        hdrYr = wsCurrentYear;
        hdrMo = String.format("%02d", wsCurrentMonth);
        hdrDay = String.format("%02d", wsCurrentDay);

        // HEADER-1
        Header1Rec h1 = new Header1Rec();
        h1.filler1 = "Financial Report for";
        h1.filler2 = repeat(' ', 60);
        printLineWriter.write(h1.toRecordString());
        printLineWriter.newLine();

        // HEADER-2
        Header2Rec h2 = new Header2Rec();
        h2.filler1 = "Year ";
        h2.hdrYr = String.format("%04d", hdrYr);
        h2.filler2 = "  ";
        h2.filler3 = "Month ";
        h2.hdrMo = hdrMo;
        h2.filler4 = "  ";
        h2.filler5 = "Day ";
        h2.hdrDay = hdrDay;
        h2.filler6 = repeat(' ', 56);
        printLineWriter.write(h2.toRecordString());
        printLineWriter.newLine();

        // blank line after ADVANCING 1 LINES
        printLineWriter.write(repeat(' ', PRINT_REC_LENGTH));
        printLineWriter.newLine();

        // HEADER-3
        Header3Rec h3 = new Header3Rec();
        h3.filler1 = "Account ";
        h3.filler2 = "  ";
        h3.filler3 = "Last Name ";
        h3.filler4 = repeat(' ', 15);
        h3.filler5 = "Limit ";
        h3.filler6 = repeat(' ', 6);
        h3.filler7 = "Balance ";
        h3.filler8 = repeat(' ', 40);
        printLineWriter.write(h3.toRecordString());
        printLineWriter.newLine();

        // HEADER-4
        Header4Rec h4 = new Header4Rec();
        h4.filler1 = "--------";
        h4.filler2 = "  ";
        h4.filler3 = "----------";
        h4.filler4 = repeat(' ', 15);
        h4.filler5 = "----------";
        h4.filler6 = "  ";
        h4.filler7 = "-------------";
        h4.filler8 = repeat(' ', 40);
        printLineWriter.write(h4.toRecordString());
        printLineWriter.newLine();
    }

    // READ-NEXT-RECORD
    private void readNextRecord() throws IOException {
        performReadRecord();
        while (lastRec != 'Y') {
            limitBalanceTotal();
            writeRecord();
            performReadRecord();
        }
    }

    // WRITE-TLIMIT-TBALANCE
    private void writeTlimitTbalance() throws IOException {
        // TRAILER-1
        Trailer1Rec t1 = new Trailer1Rec();
        t1.filler1 = repeat(' ', 31);
        t1.filler2 = "--------------";
        t1.filler3 = " ";
        t1.filler4 = "--------------";
        t1.filler5 = repeat(' ', 40);
        printLineWriter.write(t1.toRecordString());
        printLineWriter.newLine();

        // TRAILER-2
        Trailer2Rec t2 = new Trailer2Rec();
        t2.filler1 = repeat(' ', 22);
        t2.filler2 = "Totals =";
        t2.filler3 = " ";
        t2.tlimitO = formatMoney(tLimit);
        t2.filler4 = " ";
        t2.tbalanceO = formatMoney(tBalance);
        t2.filler5 = repeat(' ', 40);
        printLineWriter.write(t2.toRecordString());
        printLineWriter.newLine();
    }

    // CLOSE-STOP
    private void closeStop() throws IOException {
        acctRecFile.close();
        printLineWriter.close();
    }

    // READ-RECORD (READ ACCT-REC)
    private void performReadRecord() throws IOException {
        byte[] record = new byte[ACCT_REC_LENGTH];
        int bytesRead = acctRecFile.read(record);
        if (bytesRead < ACCT_REC_LENGTH) {
            lastRec = 'Y';
        } else {
            AcctFields fields = AcctFields.fromBytes(record);
            currentAccount = fields;
        }
    }

    // LIMIT-BALANCE-TOTAL
    private void limitBalanceTotal() {
        tLimit = tLimit.add(currentAccount.acctLimit).setScale(2, RoundingMode.UNNECESSARY);
        tBalance = tBalance.add(currentAccount.acctBalance).setScale(2, RoundingMode.UNNECESSARY);
    }

    // WRITE-RECORD
    private void writeRecord() throws IOException {
        DetailRec rec = new DetailRec();
        rec.acctNoO = currentAccount.acctNo;
        rec.lastNameO = currentAccount.lastName;
        rec.acctLimitO = formatMoney(currentAccount.acctLimit);
        rec.acctBalanceO = formatMoney(currentAccount.acctBalance);
        printLineWriter.write(rec.toRecordString());
        printLineWriter.newLine();
    }

    // Utility methods
    private static String repeat(char c, int count) {
        char[] arr = new char[count];
        Arrays.fill(arr, c);
        return new String(arr);
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    private static String formatMoney(BigDecimal value) {
        String formatted = String.format("$%,.2f", value);
        if (formatted.length() > 13) {
            formatted = formatted.substring(formatted.length() - 13);
        }
        return String.format("%13s", formatted);
    }

    // Nested classes for COBOL structures

    // 01 ACCT-FIELDS
    private static class AcctFields {
        String acctNo;              // PIC X(8)
        BigDecimal acctLimit;       // PIC S9(7)V99 COMP-3
        BigDecimal acctBalance;     // PIC S9(7)V99 COMP-3
        String lastName;            // PIC X(20)
        String firstName;           // PIC X(15)
        String streetAddr;          // PIC X(25)
        String cityCounty;          // PIC X(20)
        String usaState;            // PIC X(15)
        String reserved;            // PIC X(7)
        String comments;            // PIC X(50)

        static AcctFields fromBytes(byte[] record) {
            AcctFields af = new AcctFields();
            af.acctNo = new String(record, 0, 8, StandardCharsets.UTF_8).trim();
            byte[] limitBytes = Arrays.copyOfRange(record, 8, 13);
            af.acctLimit = PackedDecimalUtil.unpack(limitBytes, 2);
            byte[] balanceBytes = Arrays.copyOfRange(record, 13, 18);
            af.acctBalance = PackedDecimalUtil.unpack(balanceBytes, 2);
            af.lastName = new String(record, 18, 20, StandardCharsets.UTF_8).trim();
            af.firstName = new String(record, 38, 15, StandardCharsets.UTF_8).trim();
            af.streetAddr = new String(record, 53, 25, StandardCharsets.UTF_8).trim();
            af.cityCounty = new String(record, 78, 20, StandardCharsets.UTF_8).trim();
            af.usaState = new String(record, 98, 15, StandardCharsets.UTF_8).trim();
            af.reserved = new String(record, 113, 7, StandardCharsets.UTF_8).trim();
            af.comments = new String(record, 120, 50, StandardCharsets.UTF_8).trim();
            return af;
        }
    }

    // 01 DetailRec for WRITE-RECORD
    private static class DetailRec {
        String acctNoO = repeat(' ', 8);         // PIC X(8)
        String filler1 = repeat(' ', 2);         // PIC X(02)
        String lastNameO = repeat(' ', 20);      // PIC X(20)
        String filler2 = repeat(' ', 2);         // PIC X(02)
        String acctLimitO = repeat(' ', 13);     // PIC $$,$$$,$$9.99 (13)
        String filler3 = repeat(' ', 2);         // PIC X(02)
        String acctBalanceO = repeat(' ', 13);   // PIC $$,$$$,$$9.99 (13)
        String filler4 = repeat(' ', 2);         // PIC X(02)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(acctNoO, 8));
            sb.append(padRight(filler1, 2));
            sb.append(padRight(lastNameO, 20));
            sb.append(padRight(filler2, 2));
            sb.append(padRight(acctLimitO, 13));
            sb.append(padRight(filler3, 2));
            sb.append(padRight(acctBalanceO, 13));
            sb.append(padRight(filler4, 2));
            return sb.toString();
        }
    }

    // HEADER-1
    private static class Header1Rec {
        String filler1 = repeat(' ', 20);        // PIC X(20)
        String filler2 = repeat(' ', 60);        // PIC X(60)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(filler1, 20));
            sb.append(padRight(filler2, 42)); // 20+42=62 to fill record
            return sb.toString();
        }
    }

    // HEADER-2
    private static class Header2Rec {
        String filler1 = repeat(' ', 5);         // PIC X(05)
        String hdrYr;                            // PIC 9(04)
        String filler2 = repeat(' ', 2);         // PIC X(02)
        String filler3 = repeat(' ', 6);         // PIC X(06)
        String hdrMo;                            // PIC X(02)
        String filler4 = repeat(' ', 2);         // PIC X(02)
        String filler5 = repeat(' ', 4);         // PIC X(04)
        String hdrDay;                           // PIC X(02)
        String filler6 = repeat(' ', 56);        // PIC X(56)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(filler1, 5));
            sb.append(padRight(hdrYr, 4));
            sb.append(padRight(filler2, 2));
            sb.append(padRight(filler3, 6));
            sb.append(padRight(hdrMo, 2));
            sb.append(padRight(filler4, 2));
            sb.append(padRight(filler5, 4));
            sb.append(padRight(hdrDay, 2));
            sb.append(padRight(filler6, 41)); // 5+4+2+6+2+2+4+2+41=68? adjust to 62
            sb.setLength(PRINT_REC_LENGTH);
            return sb.toString();
        }
    }

    // HEADER-3
    private static class Header3Rec {
        String filler1 = repeat(' ', 8);         // PIC X(08)
        String filler2 = repeat(' ', 2);         // PIC X(02)
        String filler3 = repeat(' ', 10);        // PIC X(10)
        String filler4 = repeat(' ', 15);        // PIC X(15)
        String filler5 = repeat(' ', 6);         // PIC X(06)
        String filler6 = repeat(' ', 6);         // PIC X(06)
        String filler7 = repeat(' ', 8);         // PIC X(08)
        String filler8 = repeat(' ', 40);        // PIC X(40)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(filler1, 8));
            sb.append(padRight(filler2, 2));
            sb.append(padRight(filler3, 10));
            sb.append(padRight(filler4, 15));
            sb.append(padRight(filler5, 6));
            sb.append(padRight(filler6, 6));
            sb.append(padRight(filler7, 8));
            sb.append(padRight(filler8, 17)); // adjust to 62
            sb.setLength(PRINT_REC_LENGTH);
            return sb.toString();
        }
    }

    // HEADER-4
    private static class Header4Rec {
        String filler1 = repeat(' ', 8);         // PIC X(08)
        String filler2 = repeat(' ', 2);         // PIC X(02)
        String filler3 = repeat(' ', 10);        // PIC X(10)
        String filler4 = repeat(' ', 15);        // PIC X(15)
        String filler5 = repeat(' ', 10);        // PIC X(10)
        String filler6 = repeat(' ', 2);         // PIC X(02)
        String filler7 = repeat(' ', 13);        // PIC X(13)
        String filler8 = repeat(' ', 40);        // PIC X(40)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(filler1, 8));
            sb.append(padRight(filler2, 2));
            sb.append(padRight(filler3, 10));
            sb.append(padRight(filler4, 15));
            sb.append(padRight(filler5, 10));
            sb.append(padRight(filler6, 2));
            sb.append(padRight(filler7, 13));
            sb.append(padRight(filler8, 12));
            sb.setLength(PRINT_REC_LENGTH);
            return sb.toString();
        }
    }

    // TRAILER-1
    private static class Trailer1Rec {
        String filler1 = repeat(' ', 31);        // PIC X(31)
        String filler2 = repeat(' ', 14);        // PIC X(14)
        String filler3 = repeat(' ', 1);         // PIC X(01)
        String filler4 = repeat(' ', 14);        // PIC X(14)
        String filler5 = repeat(' ', 40);        // PIC X(40)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(filler1, 31));
            sb.append(padRight(filler2, 14));
            sb.append(padRight(filler3, 1));
            sb.append(padRight(filler4, 14));
            sb.append(padRight(filler5, 2)); // to reach 62
            sb.setLength(PRINT_REC_LENGTH);
            return sb.toString();
        }
    }

    // TRAILER-2
    private static class Trailer2Rec {
        String filler1 = repeat(' ', 22);        // PIC X(22)
        String filler2 = repeat(' ', 8);         // PIC X(08)
        String filler3 = repeat(' ', 1);         // PIC X(01)
        String tlimitO;                          // PIC $$$,$$$,$$9.99 (13)
        String filler4 = repeat(' ', 1);         // PIC X(01)
        String tbalanceO;                        // PIC $$$,$$$,$$9.99 (13)
        String filler5 = repeat(' ', 40);        // PIC X(40)

        String toRecordString() {
            StringBuilder sb = new StringBuilder(PRINT_REC_LENGTH);
            sb.append(padRight(filler1, 22));
            sb.append(padRight(filler2, 8));
            sb.append(padRight(filler3, 1));
            sb.append(padRight(tlimitO, 13));
            sb.append(padRight(filler4, 1));
            sb.append(padRight(tbalanceO, 13));
            sb.append(padRight(filler5, 3)); // to reach 62
            sb.setLength(PRINT_REC_LENGTH);
            return sb.toString();
        }
    }

    // Utility for unpacking COMP-3 packed decimals
    private static class PackedDecimalUtil {
        static BigDecimal unpack(byte[] data, int scale) {
            StringBuilder digits = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                int b = data[i] & 0xFF;
                int hi = (b >> 4) & 0xF;
                int lo = b & 0xF;
                if (i < data.length - 1) {
                    digits.append(hi);
                    digits.append(lo);
                } else {
                    digits.append(hi);
                    if (lo == 0xD) {
                        digits.insert(0, '-');
                    }
                }
            }
            BigDecimal val = new BigDecimal(digits.toString());
            return val.movePointLeft(scale);
        }
    }
}
