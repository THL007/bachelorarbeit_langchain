package com.example.migration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Migration of COBOL program REFORMER.
 * Demonstrates data manipulation using UNSTRING, SEARCH, and reference modification (STRING).
 */
public class Reformer {
    // Input fields
    private String inProductCode;
    private String inProductDesc;
    private String inInvoiceNo;
    private int inQuantity;
    private BigDecimal inUnitPrice;
    private String inInvoiceDate;
    private String inTaxable;

    // Working storage for date components
    private String wsDateMonth;
    private String wsDateDay;
    private String wsDateYear;
    private String wsDateCentury;

    // Output fields
    private String outSku;
    private BigDecimal outQuantity;
    private BigDecimal outUnitPrice;
    private String outInvoiceNo;
    private String outInvDateYYYYMMDD;
    private boolean taxableItem;

    // SKU lookup table
    private final Map<String, String> skuLookup = new HashMap<>();
    private static final String DEFAULT_SKU_PREFIX = "XX00";

    public Reformer() {
        // Initialize SKU lookup entries from WS-SKU-LOOKUP-TABLE
        skuLookup.put("AB", "TC45");
        skuLookup.put("GT", "HH05");
        skuLookup.put("KR", "NB13");
        skuLookup.put("PK", "CC19");
        skuLookup.put("ZW", "YT54");
    }

    public static void main(String[] args) {
        new Reformer().run();
    }

    public void run() {
        // MOVE statements: set input record
        this.inProductCode  = "PK29";
        this.inProductDesc  = "Pastel 29";
        this.inInvoiceNo    = "I00956A5";
        this.inQuantity     = 3;
        this.inUnitPrice    = new BigDecimal("5.49");
        this.inInvoiceDate  = "10/15/22";
        this.inTaxable      = "Y";

        // UNSTRING IN-INVOICE-DATE DELIMITED BY '/'
        String[] dateParts = this.inInvoiceDate.split("/", -1);
        this.wsDateMonth = dateParts[0];
        this.wsDateDay   = dateParts[1];
        this.wsDateYear  = dateParts[2];

        // MOVE FUNCTION CURRENT-DATE TO WS-CURRENT-DATE-DATA, then set century
        LocalDate current = LocalDate.now();
        this.wsDateCentury = String.format("%02d", current.getYear() / 100);
        this.outInvDateYYYYMMDD = wsDateCentury + wsDateYear + wsDateMonth + wsDateDay;

        System.out.println();
        System.out.println("Using CURRENT-DATE function call, MOVE statement:");
        System.out.println("IN-INVOICE-DATE <" + inInvoiceDate + "> converted to " +
                           "OUT-INV-DATE-YYYY-MM-DD <" + outInvDateYYYYMMDD + ">");

        // SEARCH WS-SKU-LOOKUP-ENTRY
        String prefix = inProductCode.substring(0, 2);
        String skuPrefix = skuLookup.getOrDefault(prefix, DEFAULT_SKU_PREFIX);
        // STRING WS-SKU-PREFIX DELIMITED BY SIZE, IN-PRODUCT-CODE(3:2) INTO OUT-SKU
        this.outSku = skuPrefix + inProductCode.substring(2);

        System.out.println();
        System.out.println("Using Table search, STRING statement:");
        System.out.println("IN-PRODUCT-CODE <" + inProductCode + "> converted to OUT-SKU <" + outSku + ">");

        // IF IN-TAXABLE IS EQUAL TO 'Y' SET TAXABLE-ITEM TO TRUE/ FALSE
        this.taxableItem = "Y".equals(inTaxable);

        System.out.println();
        System.out.println("Using IF/ELSE, SET statement:");
        System.out.println("IN-TAXABLE <" + inTaxable + "> converted to ");
        if (taxableItem) {
            System.out.println("TAXABLE-ITEM condition name TRUE");
        } else {
            System.out.println("TAXABLE-ITEM condition name FALSE");
        }

        // MOVE IN-QUANTITY TO OUT-QUANTITY, IN-UNIT-PRICE TO OUT-UNIT-PRICE
        this.outQuantity  = BigDecimal.valueOf(inQuantity);
        this.outUnitPrice = inUnitPrice;

        System.out.println();
        System.out.println("Using MOVE statements:");
        System.out.println("IN-QUANTITY <" + inQuantity + "> converted to packed OUT-QUANTITY <" + outQuantity + ">");
        System.out.println("IN-UNIT-PRICE <" + inUnitPrice + "> converted to packed OUT-UNIT-PRICE <" + outUnitPrice + ">");

        // MOVE IN-INVOICE-NO TO OUT-INVOICE-NO
        this.outInvoiceNo = inInvoiceNo;

        // DISPLAY Converted record with first 41 and next 24 characters
        System.out.println();
        System.out.println("Converted record:");
        String part1 = padRight(outSku, 10)
                       + (taxableItem ? "T" : "N")
                       + padRight(inProductDesc, 30);
        // Build full record (fields concatenated)
        String fullRecord = part1
                            + outQuantity.toPlainString()
                            + outUnitPrice.toPlainString()
                            + outInvoiceNo
                            + outInvDateYYYYMMDD;
        String displayPart1 = fullRecord.length() >= 41
                              ? fullRecord.substring(0, 41)
                              : padRight(fullRecord, 41);
        String displayPart2;
        int start = 49;
        if (fullRecord.length() > start) {
            displayPart2 = fullRecord.substring(start);
        } else {
            displayPart2 = "";
        }
        System.out.println("<" + displayPart1 + "........" + displayPart2 + ">");

        // Convert record to hex and split into high-order and low-order strings
        byte[] bytes = fullRecord.getBytes(StandardCharsets.UTF_8);
        String hex = toHex(bytes);
        StringBuilder high = new StringBuilder();
        StringBuilder low  = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (i % 2 == 0) {
                high.append(c);
            } else {
                low.append(c);
            }
        }
        System.out.println("<" + high.toString() + ">");
        System.out.println("<" + low.toString() + ">");
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) {
            return s.substring(0, n);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
