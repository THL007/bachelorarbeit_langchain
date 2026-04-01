package com.example.migration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * DOC: Migrated from COBOL program MOVEME.
 * Demonstrates MOVE operations and hexadecimal display of data.
 */
public class Moveme {
    private String wsOriginalValue;
    private int wsOriginalLength;
    private int wsBinaryItem4;
    private long wsBinaryItem8;
    private BigDecimal wsPackedDecimalItem;
    private float wsSinglePrecision;
    private double wsDoublePrecision;
    private BigDecimal wsDisplayNumericSigned;
    private BigDecimal wsDisplayNumericUnsigned;
    private String wsDisplayNumericFormatted;
    private String wsDisplayCurrencyValue;
    private String wsAlphaField1;
    private BigDecimal wsPackedField2;
    private boolean itIsSo;
    private BigDecimal wsTableSize;
    private List<Character> wsTableEntry;
    private String wsResult;

    public static void main(String[] args) {
        new Moveme().run();
    }

    // NOTE: Entry point for COBOL PROCEDURE DIVISION logic
    public void run() {
        System.out.println();
        System.out.println("Results of MOVE statements");
        System.out.println("Text values shown in hexadecimal will be in ");
        System.out.println("  ASCII when the sample program is run on an ");
        System.out.println("  ASCII-based system, and in EBCDIC when run ");
        System.out.println("  on an EBCDIC-based system.");

        example1();
        example2();
        example3();
        example4();
        example5();
        example6();
        example7();
        example8();
        example9();
        example10();
        example11();
        example13();
        example14();
    }

    private void example1() {
        // Alphanumeric MOVE
        wsOriginalValue = "Repent, Harlequin!";
        wsOriginalLength = 18;
        System.out.println();
        System.out.println("Example 1");
        System.out.println("Result of MOVE 'Repent, Harlequin!' to item defined as PIC X(...)");
        System.out.println("Text value: \"" + wsOriginalValue.substring(0, wsOriginalLength) + "\"");
    }

    private void example2() {
        // 32-bit binary MOVE
        wsBinaryItem4 = 375502;
        byte[] asBytes = ByteBuffer.allocate(4).putInt(wsBinaryItem4).array();
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 2");
        System.out.println("Result of MOVE 375502 to item defined as \"PIC S9(09) COMP\"");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example3() {
        // 64-bit binary MOVE
        wsBinaryItem8 = -281064762375502L;
        byte[] asBytes = ByteBuffer.allocate(8).putLong(wsBinaryItem8).array();
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 3");
        System.out.println("Result of MOVE -281064762375502 to item defined as \"PIC S9(16) COMP\"");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example4() {
        // 32-bit binary overwrite with spaces
        byte[] asBytes = new byte[4];
        for (int i = 0; i < asBytes.length; i++) {
            asBytes[i] = ' ';
        }
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 4");
        System.out.println("Result of MOVE SPACES that overwrites an item defined as \"PIC S9(09) COMP\"");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example5() {
        // Single-precision floating-point MOVE
        wsSinglePrecision = 6.23e-24f;
        byte[] asBytes = ByteBuffer.allocate(4).putFloat(wsSinglePrecision).array();
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 5");
        System.out.println("Result of MOVE numeric value to COMP-1 item");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example6() {
        // Double-precision floating-point MOVE
        wsDoublePrecision = 3246.16e-32;
        byte[] asBytes = ByteBuffer.allocate(8).putDouble(wsDoublePrecision).array();
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 6");
        System.out.println("Result of MOVE numeric value to COMP-2 item");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example7() {
        // Packed decimal MOVE
        wsPackedDecimalItem = new BigDecimal("-256.095");
        byte[] asBytes = toPackedDecimal(wsPackedDecimalItem, 4, 3);
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 7");
        System.out.println("Result of MOVE -256.095 to item defined as \"PIC S9(04)V9(03) COMP-3\"");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example8() {
        // Packed decimal overwrite with spaces
        byte[] asBytes = new byte[4];
        for (int i = 0; i < asBytes.length; i++) {
            asBytes[i] = ' ';
        }
        wsOriginalLength = asBytes.length;
        wsResult = hexString(asBytes);
        System.out.println();
        System.out.println("Example 8");
        System.out.println("Result of MOVE SPACES that overwrites an item defined as \"PIC S9(04)V9(03) COMP-3\"");
        System.out.println("Hex value: " + wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example9() {
        // Display Numeric Signed
        wsDisplayNumericSigned = new BigDecimal("-4832.61");
        System.out.println();
        System.out.println("Example 9");
        System.out.println("Result of MOVE -4832.61 to item defined as \"PIC S9(05)V9(02)\"");
        System.out.println("Result: " + wsDisplayNumericSigned.toPlainString());
    }

    private void example10() {
        // Display Numeric with formatting
        wsDisplayNumericFormatted = formatNumber(new BigDecimal("-4832.61"), "#,##0.00");
        System.out.println();
        System.out.println("Example 10");
        System.out.println("Result of MOVE -4832.61 to item defined as \"PIC -ZZ,ZZ9.99\"");
        System.out.println("Result: " + wsDisplayNumericFormatted);
    }

    private void example11() {
        // Display Numeric with formatting for currency
        wsDisplayCurrencyValue = formatCurrency(new BigDecimal("-4832.61"));
        System.out.println();
        System.out.println("Example 11");
        System.out.println("Result of MOVE -4832.61 to item defined as \"PIC -$$,$$9.99\"");
        System.out.println("Result: " + wsDisplayCurrencyValue);
    }

    private void example13() {
        // Initialize group item
        byte[] alpha = new byte[5];
        for (int i = 0; i < alpha.length; i++) alpha[i] = ' ';
        byte[] packedF2 = toPackedDecimal(BigDecimal.ZERO, 5, 0);
        byte boolByte = ' ';
        byte[] sizeBytes = toPackedDecimal(BigDecimal.ZERO, 3, 0);
        byte[] combined = concat(alpha, packedF2, new byte[]{boolByte}, sizeBytes);
        wsOriginalLength = combined.length;
        wsResult = hexString(combined);
        System.out.println();
        System.out.println("Example 13");
        System.out.println("WS-GROUP-ITEM after INITIALIZE statement");
        System.out.println(wsResult.substring(0, wsOriginalLength * 2));
    }

    private void example14() {
        // MOVE SPACES to group item
        byte[] alpha = new byte[5];
        byte[] packedF2 = new byte[3];
        byte boolByte = ' ';
        byte[] sizeBytes = new byte[2];
        byte[] combined = concat(alpha, packedF2, new byte[]{boolByte}, sizeBytes);
        wsOriginalLength = combined.length;
        wsResult = hexString(combined);
        System.out.println();
        System.out.println("Example 14");
        System.out.println("WS-GROUP-ITEM after MOVE SPACES statement");
        System.out.println(wsResult.substring(0, wsOriginalLength * 2));
    }

    private String hexString(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02X", b));
        return sb.toString();
    }

    private byte[] toPackedDecimal(BigDecimal value, int intDigits, int fracDigits) {
        // COMP-3 packed decimal: 1 nibble per digit, last nibble is sign.
        BigDecimal scaled = value.setScale(fracDigits);
        BigInteger unscaled = scaled.movePointRight(fracDigits).abs().toBigInteger();
        String digits = unscaled.toString();
        int totalDigits = intDigits + fracDigits;
        digits = String.format("%" + totalDigits + "s", digits).replace(' ', '0');
        int byteLen = (totalDigits + 1) / 2 + 1;
        byte[] result = new byte[byteLen];
        int pos = 0;
        for (int i = 0; i < totalDigits; i += 2) {
            int hi = Character.digit(digits.charAt(i), 10);
            int lo = 0;
            if (i + 1 < digits.length()) lo = Character.digit(digits.charAt(i + 1), 10);
            result[pos++] = (byte) ((hi << 4) | lo);
        }
        int signNibble = value.signum() < 0 ? 0x0D : 0x0C;
        result[pos - 1] = (byte) ((result[pos - 1] & 0xF0) | signNibble);
        return result;
    }

    private String formatNumber(BigDecimal number, String pattern) {
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }

    private String formatCurrency(BigDecimal number) {
        DecimalFormat df = new DecimalFormat("$#,##0.00");
        return df.format(number);
    }

    private byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] arr : arrays) total += arr.length;
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] arr : arrays) {
            System.arraycopy(arr, 0, result, pos, arr.length);
            pos += arr.length;
        }
        return result;
    }
}
