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
import java.util.Locale;

public class CBL0033Processor {
    private BufferedReader acctRecReader;
    private BufferedWriter printLineWriter;
    private String lastRec = " ";
    private int counter = 0;
    private AcctFields currentFields;

    private static final DecimalFormat MONEY_FORMAT;
    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        MONEY_FORMAT = new DecimalFormat("##,###,###,##0.00", symbols);
        MONEY_FORMAT.setRoundingMode(RoundingMode.UNNECESSARY);
    }

    public static void main(String[] args) {
        try {
            new CBL0033Processor().execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void execute() throws IOException {
        openFiles();
        readFirstRecord();
        readAnotherRecord();
        readNextRecords();
        callingSubprogram();
        closeStop();
    }

    private void openFiles() throws IOException {
        acctRecReader = new BufferedReader(new FileReader("ACCTREC"));
        printLineWriter = new BufferedWriter(new FileWriter("PRTLINE"));
    }

    private void readFirstRecord() throws IOException {
        readRecord();
        writeRecord();
        readTenRecords();
        // NOTE: DISPLAY ' THIS IS THE FIRST RECORD ' (unreachable in COBOL)
        if (false) {
            System.out.println(" THIS IS THE FIRST RECORD ");
        }
    }

    private void readTenRecords() throws IOException {
        for (int i = 0; i < 10; i++) {
            readRecord();
            writeRecord();
        }
    }

    private void readAnotherRecord() throws IOException {
        readRecord();
        writeRecord();
    }

    private void readNextRecords() throws IOException {
        for (counter = 1; counter <= 34; counter++) {
            readRecord();
            writeRecord();
        }
    }

    private void callingSubprogram() {
        new Hello().execute();
    }

    private void closeStop() throws IOException {
        if (acctRecReader != null) acctRecReader.close();
        if (printLineWriter != null) printLineWriter.close();
        // GOBACK => return
    }

    private void readRecord() throws IOException {
        String line = acctRecReader.readLine();
        if (line == null) {
            lastRec = "Y";
            return;
        }
        AcctFields f = new AcctFields();
        f.acctNo = line.substring(0, 8);
        String limitStr = line.substring(8, 20).trim();
        f.acctLimit = parseMoney(limitStr);
        String balanceStr = line.substring(20, 32).trim();
        f.acctBalance = parseMoney(balanceStr);
        f.lastName = line.substring(32, 52).trim();
        f.firstName = line.substring(52, 67).trim();
        f.streetAddr = line.substring(67, 92).trim();
        f.cityCounty = line.substring(92, 112).trim();
        f.usaState = line.substring(112, 127).trim();
        f.reserved = line.substring(127, 134).trim();
        f.comments = line.substring(134, 184).trim();
        currentFields = f;
    }

    private void writeRecord() throws IOException {
        PrintRec out = new PrintRec();
        out.acctNo = currentFields.acctNo;
        out.acctLimit = formatMoney(currentFields.acctLimit);
        out.acctBalance = formatMoney(currentFields.acctBalance);
        out.lastName = currentFields.lastName;
        out.firstName = currentFields.firstName;
        out.comments = currentFields.comments;
        StringBuilder sb = new StringBuilder();
        sb.append(padRight(out.acctNo, 8));
        sb.append(padLeft(out.acctLimit, 12));
        sb.append(padLeft(out.acctBalance, 12));
        sb.append(padRight(out.lastName, 20));
        sb.append(padRight(out.firstName, 15));
        sb.append(padRight(out.comments, 50));
        printLineWriter.write(sb.toString());
        printLineWriter.newLine();
    }

    private BigDecimal parseMoney(String s) {
        String cleaned = s.replace(",", "");
        return new BigDecimal(cleaned).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String formatMoney(BigDecimal money) {
        return MONEY_FORMAT.format(money);
    }

    private static String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }

    private static String padLeft(String s, int n) {
        if (s == null) s = "";
        return String.format("%" + n + "s", s);
    }

    private static class AcctFields {
        String acctNo;
        BigDecimal acctLimit;
        BigDecimal acctBalance;
        String lastName;
        String firstName;
        String streetAddr;
        String cityCounty;
        String usaState;
        String reserved;
        String comments;
    }

    private static class PrintRec {
        String acctNo;
        String acctLimit;
        String acctBalance;
        String lastName;
        String firstName;
        String comments;
    }
}
