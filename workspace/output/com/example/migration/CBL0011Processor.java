/*
 * IDENTIFICATION DIVISION.
 * PROGRAM-ID.    CBL0011
 * AUTHOR.        Otto B. Intrisic.
 *
 * ENVIRONMENT DIVISION.
 * INPUT-OUTPUT SECTION.
 * FILE-CONTROL.
 *     SELECT PRINT-LINE ASSIGN TO PRTLINE.
 *     SELECT ACCT-REC   ASSIGN TO ACCTREC.
 */
package com.example.migration;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.EOFException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;

public class CBL0011Processor {
    private DataInputStream acctRecStream;
    private BufferedWriter printLineWriter;

    private boolean lastRec;
    private BigDecimal tLimit = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tBalance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    // WS-CURRENT-DATE-DATA fields
    private int wsCurrentYear;
    private int wsCurrentMonth;
    private int wsCurrentDay;
    private int wsCurrentHour;
    private int wsCurrentMinute;
    private int wsCurrentSecond;
    private int wsCurrentCentisecond;

    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("###,###,###,##0.00", symbols);
        MONEY_FORMAT.setRoundingMode(RoundingMode.UNNECESSARY);
    }

    public static void main(String[] args) {
        try {
            new CBL0011Processor().execute();
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
        return; // GOBACK
    }

    /* OPEN-FILES */
    private void openFiles() throws IOException {
        acctRecStream = new DataInputStream(new FileInputStream("ACCTREC"));
        printLineWriter = new BufferedWriter(new FileWriter("PRTLINE"));
    }

    /* WRITE-HEADERS */
    private void writeHeaders() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        this.wsCurrentYear = now.getYear();
        this.wsCurrentMonth = now.getMonthValue();
        this.wsCurrentDay = now.getDayOfMonth();
        this.wsCurrentHour = now.getHour();
        this.wsCurrentMinute = now.getMinute();
        this.wsCurrentSecond = now.getSecond();
        this.wsCurrentCentisecond = now.getNano() / 10_000_000;

        String hdrYr = String.format("%04d", wsCurrentYear);
        String hdrMo = String.format("%02d", wsCurrentMonth);
        String hdrDay = String.format("%02d", wsCurrentDay);

        String header1 = padRight("Financial Report for", 20) + padRight("", 60);
        String header2 = "Year " + hdrYr + padRight("", 2)
                + "Month " + hdrMo + padRight("", 2)
                + "Day " + hdrDay + padRight("", 56);
        String header3 = "Account " + padRight("", 2)
                + "Last Name " + padRight("", 15)
                + "Limit " + padRight("", 6)
                + "Balance " + padRight("", 40);
        String header4 = "--------" + padRight("", 2)
                + "----------" + padRight("", 15)
                + "----------" + padRight("", 2)
                + "-------------" + padRight("", 40);

        printLineWriter.write(header1);
        printLineWriter.newLine();
        printLineWriter.write(header2);
        printLineWriter.newLine();
        printLineWriter.write(padRight("", getPrintRecLength())); // MOVE SPACES TO PRINT-REC
        printLineWriter.newLine(); // WRITE AFTER ADVANCING 1 LINES
        printLineWriter.write(header3);
        printLineWriter.newLine();
        printLineWriter.write(header4);
        printLineWriter.newLine();

        // MOVE SPACES TO PRINT-REC
        PrintRec blankRec = new PrintRec();
        blankRec.acctNoO = padRight("", 8);
        blankRec.lastNameO = padRight("", 20);
        blankRec.acctLimitO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        blankRec.acctBalanceO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    }

    /* READ-NEXT-RECORD */
    private void readNextRecord() throws IOException {
        readRecord();
        while (!lastRec) {
            limitBalanceTotal();
            writeRecord();
            readRecord();
        }
    }

    /* WRITE-TLIMIT-TBALANCE */
    private void writeTlimitTbalance() throws IOException {
        String trailer1 = padRight("", 31)
                + "--------------" + padRight("", 1)
                + "--------------" + padRight("", 40);
        String trailer2 = padRight("", 22)
                + "Totals =" + padRight("", 1)
                + padLeft(MONEY_FORMAT.format(tLimit), 13)
                + padRight("", 1)
                + padLeft(MONEY_FORMAT.format(tBalance), 13)
                + padRight("", 40);
        printLineWriter.write(trailer1);
        printLineWriter.newLine();
        printLineWriter.write(trailer2);
        printLineWriter.newLine();
    }

    /* CLOSE-STOP */
    private void closeStop() throws IOException {
        if (acctRecStream != null) acctRecStream.close();
        if (printLineWriter != null) printLineWriter.close();
    }

    /* READ-RECORD */
    private AcctFields currentRecord;

    private void readRecord() throws IOException {
        try {
            byte[] buffer = new byte[170]; acctRecStream.readFully(buffer);
            DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(buffer));
            byte[] bytes;
            bytes = new byte[8]; in.readFully(bytes);
            String acctNo = new String(bytes, "UTF-8");
            bytes = new byte[5]; in.readFully(bytes);
            BigDecimal acctLimit = PackedDecimalUtil.unpackToBigDecimal(bytes, 2);
            bytes = new byte[5]; in.readFully(bytes);
            BigDecimal acctBalance = PackedDecimalUtil.unpackToBigDecimal(bytes, 2);
            bytes = new byte[20]; in.readFully(bytes);
            String lastName = new String(bytes, "UTF-8");
            bytes = new byte[15]; in.readFully(bytes);
            String firstName = new String(bytes, "UTF-8");
            bytes = new byte[25]; in.readFully(bytes);
            String streetAddr = new String(bytes, "UTF-8");
            bytes = new byte[20]; in.readFully(bytes);
            String cityCounty = new String(bytes, "UTF-8");
            bytes = new byte[15]; in.readFully(bytes);
            String usaState = new String(bytes, "UTF-8");
            bytes = new byte[7]; in.readFully(bytes);
            String reserved = new String(bytes, "UTF-8");
            bytes = new byte[50]; in.readFully(bytes);
            String comments = new String(bytes, "UTF-8");
            currentRecord = new AcctFields(
                    acctNo, acctLimit, acctBalance,
                    lastName, firstName,
                    streetAddr, cityCounty, usaState,
                    reserved, comments);
            lastRec = false;
        } catch (EOFException e) {
            lastRec = true;
        }
    }

    /* LIMIT-BALANCE-TOTAL */
    private void limitBalanceTotal() {
        tLimit = tLimit.add(currentRecord.getAcctLimit());
        tBalance = tBalance.add(currentRecord.getAcctBalance());
    }

    /* WRITE-RECORD */
    private void writeRecord() throws IOException {
        PrintRec rec = new PrintRec();
        rec.acctNoO = currentRecord.getAcctNo();
        String last = currentRecord.getLastName();
        String lastNameO = "";
        if (last != null && last.length() >= 1) {
            String firstChar = last.substring(0, 1);
            String rest = last.length() > 1 ? last.substring(1).toLowerCase() : "";
            lastNameO = firstChar + rest;
        }
        rec.lastNameO = lastNameO;
        rec.acctLimitO = currentRecord.getAcctLimit();
        rec.acctBalanceO = currentRecord.getAcctBalance();
        printLineWriter.write(rec.toRecordString());
        printLineWriter.newLine();
    }

    private static String padRight(String s, int length) {
        if (s.length() >= length) return s.substring(0, length);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < length) sb.append(' ');
        return sb.toString();
    }

    private static String padLeft(String s, int length) {
        if (s.length() >= length) return s.substring(s.length() - length);
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - s.length()) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    private int getPrintRecLength() {
        return 62; // 8+2+20+2+13+2+13+2
    }

    private static class AcctFields {
        private final String acctNo;
        private final BigDecimal acctLimit;
        private final BigDecimal acctBalance;
        private final String lastName;
        private final String firstName;
        private final String streetAddr;
        private final String cityCounty;
        private final String usaState;
        private final String reserved;
        private final String comments;

        public AcctFields(String acctNo, BigDecimal acctLimit, BigDecimal acctBalance,
                          String lastName, String firstName,
                          String streetAddr, String cityCounty, String usaState,
                          String reserved, String comments) {
            this.acctNo = acctNo;
            this.acctLimit = acctLimit;
            this.acctBalance = acctBalance;
            this.lastName = lastName;
            this.firstName = firstName;
            this.streetAddr = streetAddr;
            this.cityCounty = cityCounty;
            this.usaState = usaState;
            this.reserved = reserved;
            this.comments = comments;
        }

        public String getAcctNo() { return acctNo; }
        public BigDecimal getAcctLimit() { return acctLimit; }
        public BigDecimal getAcctBalance() { return acctBalance; }
        public String getLastName() { return lastName; }
        public String getFirstName() { return firstName; }
        public String getStreetAddr() { return streetAddr; }
        public String getCityCounty() { return cityCounty; }
        public String getUsaState() { return usaState; }
        public String getReserved() { return reserved; }
        public String getComments() { return comments; }
    }

    private static class PrintRec {
        String acctNoO;
        String lastNameO;
        BigDecimal acctLimitO;
        BigDecimal acctBalanceO;

        public String toRecordString() {
            StringBuilder sb = new StringBuilder();
            sb.append(padRight(acctNoO, 8));
            sb.append(padRight("", 2));
            sb.append(padRight(lastNameO, 20));
            sb.append(padRight("", 2));
            sb.append(padLeft(MONEY_FORMAT.format(acctLimitO), 13));
            sb.append(padRight("", 2));
            sb.append(padLeft(MONEY_FORMAT.format(acctBalanceO), 13));
            sb.append(padRight("", 2));
            return sb.toString();
        }
    }

    private static class PackedDecimalUtil {
        public static BigDecimal unpackToBigDecimal(byte[] data, int scale) {
            StringBuilder digits = new StringBuilder();
            boolean negative = false;
            for (int i = 0; i < data.length; i++) {
                int b = data[i] & 0xFF;
                int high = (b & 0xF0) >> 4;
                int low = b & 0x0F;
                if (i == data.length - 1) {
                    digits.append(high);
                    if (low == 0x0D) negative = true;
                } else {
                    digits.append(high).append(low);
                }
            }
            BigDecimal value = new BigDecimal(new java.math.BigInteger(digits.toString()), scale);
            return negative ? value.negate() : value;
        }
    }
}
