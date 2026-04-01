package com.example.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;

public class CBL0010Processor {
    private BufferedReader acctRecReader;
    private BufferedWriter printLineWriter;

    // Print record buffer and length (62 characters)
    private static final int PRINT_REC_LENGTH = 62;

    // File record fields (ACCT-REC)
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

    // Working storage flags
    private boolean lastRec = false;

    // Accumulators
    private BigDecimal tLimit = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tBalance = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tLimitO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
    private BigDecimal tBalanceO = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);

    // Header date/time fields
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

    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("##,###,##0.00", symbols);
        MONEY_FORMAT.setRoundingMode(RoundingMode.UNNECESSARY);
    }

    public static void main(String[] args) {
        try {
            new CBL0010Processor().execute();
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

    private void openFiles() throws IOException {
        acctRecReader = new BufferedReader(new FileReader("ACCTREC"));
        printLineWriter = new BufferedWriter(new FileWriter("PRTLINE"));
    }

    private void writeHeaders() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        wsCurrentYear = now.getYear();
        wsCurrentMonth = now.getMonthValue();
        wsCurrentDay = now.getDayOfMonth();
        wsCurrentHour = now.getHour();
        wsCurrentMinute = now.getMinute();
        wsCurrentSecond = now.getSecond();
        wsCurrentCentisecond = now.get(ChronoField.MILLI_OF_SECOND) / 10;
        hdrYr = wsCurrentYear;
        hdrMo = String.format("%02d", wsCurrentMonth);
        hdrDay = String.format("%02d", wsCurrentDay);

        // HEADER-1
        String header1 = padRight("Financial Report for", 20) + repeat(' ', 60);
        writeRecordLine(header1);

        // HEADER-2
        String header2 = "Year " + hdrYr + "  Month " + hdrMo + "  Day " + hdrDay + repeat(' ', 56);
        writeRecordLine(header2);

        // blank line (MOVE SPACES TO PRINT-REC and WRITE AFTER ADVANCING)
        String blank = repeat(' ', PRINT_REC_LENGTH);
        writeRecordLine(blank);

        // HEADER-3
        String header3 = padRight("Account ", 8) + repeat(' ',2)
            + padRight("Last Name ", 10) + repeat(' ',15)
            + padRight("Limit ",6) + repeat(' ',6)
            + padRight("Balance ",8) + repeat(' ',40);
        writeRecordLine(header3);

        // HEADER-4
        String header4 = "--------" + repeat(' ',2) + "----------" + repeat(' ',15)
            + "----------" + repeat(' ',2) + "-------------" + repeat(' ',40);
        writeRecordLine(header4);
    }

    private void readNextRecord() throws IOException {
        readRecord();
        while (!lastRec) {
            limitBalanceTotal();
            writeDataRecord();
            readRecord();
        }
    }

    private void writeTlimitTbalance() throws IOException {
        tLimitO = tLimit;
        tBalanceO = tBalance;
        // TRAILER-1
        String trailer1 = repeat(' ',31) + "--------------" + ' ' + "--------------" + repeat(' ',40);
        writeRecordLine(trailer1);
        // TRAILER-2
        String trailer2 = repeat(' ',22) + "Totals =" + ' '
            + padLeft(MONEY_FORMAT.format(tLimitO),13) + ' '
            + padLeft(MONEY_FORMAT.format(tBalanceO),13) + repeat(' ',40);
        writeRecordLine(trailer2);
    }

    private void closeStop() throws IOException {
        acctRecReader.close();
        printLineWriter.close();
    }

    private void readRecord() throws IOException {
        String record = acctRecReader.readLine();
        if (record == null) {
            lastRec = true;
            return;
        }
        acctNo = record.substring(0,8).trim();
        acctLimit = new BigDecimal(record.substring(8,17).trim()).movePointLeft(2);
        acctBalance = new BigDecimal(record.substring(17,26).trim()).movePointLeft(2);
        lastName = record.substring(26,46).trim();
        firstName = record.substring(46,61).trim();
        streetAddr = record.substring(61,86).trim();
        cityCounty = record.substring(86,106).trim();
        usaState = record.substring(106,121).trim();
        reserved = record.substring(121,128).trim();
        comments = record.substring(128, Math.min(record.length(), 178)).trim();
    }

    private void limitBalanceTotal() {
        tLimit = tLimit.add(acctLimit);
        tBalance = tBalance.add(acctBalance);
    }

    private void writeDataRecord() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(padRight(acctNo,8));
        sb.append(repeat(' ',2));
        sb.append(padRight(lastName,20));
        sb.append(repeat(' ',2));
        sb.append(padLeft(MONEY_FORMAT.format(acctLimit),13));
        sb.append(repeat(' ',2));
        sb.append(padLeft(MONEY_FORMAT.format(acctBalance),13));
        sb.append(repeat(' ',2));
        writeRecordLine(sb.toString());
    }

    private void writeRecordLine(String data) throws IOException {
        String out = data.length() > PRINT_REC_LENGTH
            ? data.substring(0, PRINT_REC_LENGTH)
            : padRight(data, PRINT_REC_LENGTH);
        printLineWriter.write(out);
        printLineWriter.newLine();
    }

    private static String repeat(char ch, int count) {
        return new String(new char[count]).replace('\0', ch);
    }

    private static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    private static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }
}
