package com.example.migration;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Migrated from COBOL program CBL0012.
 */
public class CBL0012Processor {

    // Record layout for PRINT-REC
    private static class PrintRec {
        String acctNoO;         // PIC X(8)
        String filler1;         // PIC X(02)
        String lastNameO;       // PIC X(20)
        String filler2;         // PIC X(02)
        BigDecimal acctLimitO;  // PIC $$,$$$,$$9.99
        String filler3;         // PIC X(02)
        BigDecimal acctBalanceO;// PIC $$,$$$,$$9.99
        String filler4;         // PIC X(02)
    }

    // Record layout for ACCT-FIELDS
    private static class AcctFields {
        String acctNo;          // PIC X(8)
        BigDecimal acctLimit;   // PIC S9(7)V99 COMP-3
        BigDecimal acctBalance; // PIC S9(7)V99 COMP-3
        String lastName;        // PIC X(20)
        String firstName;       // PIC X(15)
        String streetAddr;      // PIC X(25)
        String cityCounty;      // PIC X(20)
        String usaState;        // PIC X(15)
        String reserved;        // PIC X(7)
        String comments;        // PIC X(50)
    }

    // Working-storage group for current date
    private static class WSCurrentDateData {
        int wsCurrentYear;      // PIC 9(04)
        int wsCurrentMonth;     // PIC 9(02)
        int wsCurrentDay;       // PIC 9(02)
    }

    // File streams
    private DataInputStream acctRecStream;
    private BufferedWriter printLineWriter;

    // Record instances
    private PrintRec printRec = new PrintRec();
    private AcctFields acctFields = new AcctFields();

    // Working-storage variables
    private char lastRec;                           // PIC X
    private BigDecimal tlLimit = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);  // TLIMIT
    private BigDecimal tBalance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);// TBALANCE
    private WSCurrentDateData wsDateData = new WSCurrentDateData();  // WS-CURRENT-DATE-DATA
    private int hdrYr;       // PIC 9(04)
    private String hdrMo;    // PIC X(02)
    private String hdrDay;   // PIC X(02)

    // Decimal format for monetary values
    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("###,###,###,##0.00", symbols);
        MONEY_FORMAT.setRoundingMode(RoundingMode.UNNECESSARY);
    }

    public static void main(String[] args) {
        CBL0012Processor processor = new CBL0012Processor();
        try {
            processor.execute();
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
    }

    // OPEN-FILES
    public void openFiles() throws IOException {
        acctRecStream = new DataInputStream(new FileInputStream("ACCTREC"));
        printLineWriter = Files.newBufferedWriter(Paths.get("PRTLINE"));
        lastRec = ' ';
    }

    // WRITE-HEADERS
    public void writeHeaders() throws IOException {
        // MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA
        LocalDate now = LocalDate.now();
        wsDateData.wsCurrentYear = now.getYear();
        wsDateData.wsCurrentMonth = now.getMonthValue();
        wsDateData.wsCurrentDay = now.getDayOfMonth();
        // MOVE WS-CURRENT-YEAR TO HDR-YR
        hdrYr = wsDateData.wsCurrentYear;
        // MOVE WS-CURRENT-MONTH TO HDR-MO
        hdrMo = String.format("%02d", wsDateData.wsCurrentMonth);
        // MOVE WS-CURRENT-DAY TO HDR-DAY
        hdrDay = String.format("%02d", wsDateData.wsCurrentDay);

        // WRITE PRINT-REC FROM HEADER-1
        String header1 = String.format("%-20s%-60s", "Financial Report for", "");
        printLineWriter.write(header1);
        printLineWriter.newLine();
        // WRITE PRINT-REC FROM HEADER-2
        String header2 = String.format("%-5s%04d%-2s%-6s%02s%-2s%-4s%02s%-56s",
                "Year ", hdrYr, "", "Month ", hdrMo, "", "Day ", hdrDay, "");
        printLineWriter.write(header2);
        printLineWriter.newLine();
        // MOVE SPACES TO PRINT-REC and WRITE AFTER ADVANCING 1 LINES
        printLineWriter.write("");
        printLineWriter.newLine();
        // WRITE PRINT-REC FROM HEADER-3
        String header3 = String.format("%-8s%-2s%-10s%-15s%-6s%-6s%-8s%-40s",
                "Account ", "", "Last Name ", "", "Limit ", "", "Balance ", "");
        printLineWriter.write(header3);
        printLineWriter.newLine();
        // WRITE PRINT-REC FROM HEADER-4
        String header4 = String.format("%-8s%-2s%-10s%-15s%-10s%-2s%-13s%-40s",
                "--------", "", "----------", "", "----------", "", "-------------", "");
        printLineWriter.write(header4);
        printLineWriter.newLine();
        // MOVE SPACES TO PRINT-REC
        printLineWriter.write("");
        printLineWriter.newLine();
    }

    // READ-NEXT-RECORD
    public void readNextRecord() throws IOException {
        // PERFORM READ-RECORD
        readRecord();
        // PERFORM UNTIL LASTREC = 'Y'
        while (lastRec != 'Y') {
            performLimitBalanceTotal();
            writeRecord();
            readRecord();
        }
    }

    // WRITE-TLIMIT-TBALANCE
    public void writeTlimitTbalance() throws IOException {
        // MOVE TLIMIT TO TLIMIT-O
        printRec.acctLimitO = tlLimit;
        // MOVE TBALANCE TO TBALANCE-O
        printRec.acctBalanceO = tBalance;
        // WRITE PRINT-REC FROM TRAILER-1
        String trailer1 = String.format("%-31s%-14s%-1s%-14s%-40s", "", "--------------", "", "--------------", "");
        printLineWriter.write(trailer1);
        printLineWriter.newLine();
        // WRITE PRINT-REC FROM TRAILER-2
        String formattedTlimit = MONEY_FORMAT.format(printRec.acctLimitO);
        String formattedTbalance = MONEY_FORMAT.format(printRec.acctBalanceO);
        String trailer2 = String.format("%-22s%-8s%-1s%12s%-1s%12s%-40s",
                "", "Totals =", "", formattedTlimit, "", formattedTbalance, "");
        printLineWriter.write(trailer2);
        printLineWriter.newLine();
    }

    // CLOSE-STOP
    public void closeStop() throws IOException {
        acctRecStream.close();
        printLineWriter.close();
    }

    // READ-RECORD
    public void readRecord() throws IOException {
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
            acctFields.streetAddr = new String(streetBytes, StandardCharsets.US_ASCII).trim();
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
            // Normal record, LASTREC remains not 'Y'
        } catch (EOFException e) {
            // AT END MOVE 'Y' TO LASTREC
            lastRec = 'Y';
        }
    }

    // LIMIT-BALANCE-TOTAL
    public void performLimitBalanceTotal() {
        tlLimit = tlLimit.add(acctFields.acctLimit);
        tBalance = tBalance.add(acctFields.acctBalance);
    }

    // WRITE-RECORD
    public void writeRecord() throws IOException {
        // MOVE ACCT-NO TO ACCT-NO-O
        printRec.acctNoO = acctFields.acctNo;
        // MOVE FILLER
        printRec.filler1 = "";
        // MOVE LAST-NAME(1:1) TO LAST-NAME-O(1:1)
        String ln = acctFields.lastName;
        String firstChar = ln.length() >= 1 ? ln.substring(0, 1) : "";
        // MOVE FUNCTION LOWER-CASE(LAST-NAME(2:19)) TO LAST-NAME-O(2:19)
        String rest = ln.length() > 1 ? ln.substring(1) : "";
        rest = rest.toLowerCase(Locale.US);
        printRec.lastNameO = (firstChar + rest);
        // Move fillers
        printRec.filler2 = "";
        // MOVE ACCT-LIMIT TO ACCT-LIMIT-O
        printRec.acctLimitO = acctFields.acctLimit;
        printRec.filler3 = "";
        // MOVE ACCT-BALANCE TO ACCT-BALANCE-O
        printRec.acctBalanceO = acctFields.acctBalance;
        printRec.filler4 = "";

        // Format output line
        String formattedLimit = MONEY_FORMAT.format(printRec.acctLimitO);
        String formattedBalance = MONEY_FORMAT.format(printRec.acctBalanceO);
        String line = String.format("%-8s%-2s%-20s%-2s%12s%-2s%12s%-2s", 
                printRec.acctNoO, printRec.filler1, printRec.lastNameO, printRec.filler2,
                formattedLimit, printRec.filler3, formattedBalance, printRec.filler4);
        printLineWriter.write(line);
        printLineWriter.newLine();
    }

    // Helper to read COMP-3 packed decimal
    private BigDecimal readComp3Decimal(DataInputStream dis, int integerDigits, int decimalDigits) throws IOException {
        int totalDigits = integerDigits + decimalDigits;
        int byteLen = (totalDigits + 2) / 2;
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
}
