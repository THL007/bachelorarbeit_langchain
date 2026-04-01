package com.example.migration;

import java.math.BigDecimal;

/**
 * Migrated from COBOL program STRINGIT.
 * Demonstrates text manipulation using MOVE, INSPECT, STRING, and reference modification.
 */
public class StringIt {
    // Working-storage variables (COBOL PIC mappings)
    private String wsFamilyName = padRight("Kirk", 60);
    private String wsGivenName = padRight("James", 60);
    private String wsMiddleName = padRight("Tiberius", 60);

    private String wsGivenNameOut = padRight("", 60);
    private String wsMiddleNameOut = padRight("", 60);
    private String wsFamilyNameOut = padRight("", 60);

    private BigDecimal wsGivenNameLength;
    private BigDecimal wsMiddleNameLength;
    private BigDecimal wsFamilyNameLength;
    private BigDecimal wsOutputLength;

    private String wsOutputArea = padRight("", 180);

    public static void main(String[] args) {
        new StringIt().run();
    }

    public void run() {
        example1();
        example2();
        example3();
        example4();
    }

    private void example1() {
        System.out.println();
        System.out.println("Example 1: Formatting a person's name using MOVE statements");
        // MOVE WS-GIVEN-NAME TO WS-GIVEN-NAME-OUT
        wsGivenNameOut = wsGivenName;
        // MOVE WS-MIDDLE-NAME TO WS-MIDDLE-NAME-OUT
        wsMiddleNameOut = wsMiddleName;
        // MOVE WS-FAMILY-NAME TO WS-FAMILY-NAME-OUT
        wsFamilyNameOut = wsFamilyName;
        System.out.println("Name in first-middle-last-order:");
        String combined = wsGivenNameOut + wsMiddleNameOut + wsFamilyNameOut;
        System.out.println("<" + combined + ">");
    }

    private void example2() {
        System.out.println();
        System.out.println("Example 2: Formatting a person's name using INSPECT and reference modification");
        // INITIALIZE WS-NAME-LENGTHS
        wsGivenNameLength = BigDecimal.ZERO;
        wsMiddleNameLength = BigDecimal.ZERO;
        wsFamilyNameLength = BigDecimal.ZERO;
        // INSPECT WS-GIVEN-NAME TALLYING WS-GIVEN-NAME-LENGTH FOR CHARACTERS BEFORE INITIAL SPACE
        wsGivenNameLength = BigDecimal.valueOf(findLengthBeforeSpace(wsGivenName));
        // INSPECT WS-MIDDLE-NAME TALLYING WS-MIDDLE-NAME-LENGTH FOR CHARACTERS BEFORE INITIAL SPACE
        wsMiddleNameLength = BigDecimal.valueOf(findLengthBeforeSpace(wsMiddleName));
        // INSPECT WS-FAMILY-NAME TALLYING WS-FAMILY-NAME-LENGTH FOR CHARACTERS BEFORE INITIAL SPACE
        wsFamilyNameLength = BigDecimal.valueOf(findLengthBeforeSpace(wsFamilyName));
        System.out.println("Name in first-middle-last order:");
        StringBuilder sb = new StringBuilder();
        sb.append("<");
        sb.append(wsGivenName.substring(0, wsGivenNameLength.intValue()));
        sb.append(' ');
        sb.append(wsMiddleName.substring(0, wsMiddleNameLength.intValue()));
        sb.append(' ');
        sb.append(wsFamilyName.substring(0, wsFamilyNameLength.intValue()));
        sb.append(">");
        System.out.println(sb.toString());
    }

    private void example3() {
        System.out.println();
        System.out.println("Example 3: Formatting a person's name using STRING");
        // STRING WS-GIVEN-NAME DELIMITED BY SPACE, SPACE DELIMITED BY SIZE, WS-MIDDLE-NAME DELIMITED BY SPACE,
        // SPACE DELIMITED BY SIZE, WS-FAMILY-NAME DELIMITED BY SPACE INTO WS-OUTPUT-AREA
        int lenGiven = findLengthBeforeSpace(wsGivenName);
        int lenMiddle = findLengthBeforeSpace(wsMiddleName);
        int lenFamily = findLengthBeforeSpace(wsFamilyName);
        String part1 = wsGivenName.substring(0, lenGiven);
        String part2 = wsMiddleName.substring(0, lenMiddle);
        String part3 = wsFamilyName.substring(0, lenFamily);
        String joined = part1 + " " + part2 + " " + part3;
        wsOutputArea = padRight(joined, 180);
        System.out.println("<" + wsOutputArea + ">");
    }

    private void example4() {
        System.out.println();
        System.out.println("Example 4: Combining INSPECT, STRING, and reference modification");
        // INITIALIZE and INSPECT to get lengths
        int lenGiven = findLengthBeforeSpace(wsGivenName);
        int lenMiddle = findLengthBeforeSpace(wsMiddleName);
        int lenFamily = findLengthBeforeSpace(wsFamilyName);
        wsGivenNameLength = BigDecimal.valueOf(lenGiven);
        wsMiddleNameLength = BigDecimal.valueOf(lenMiddle);
        wsFamilyNameLength = BigDecimal.valueOf(lenFamily);
        // STRING with reference modification into WS-OUTPUT-AREA
        String joined = wsGivenName.substring(0, lenGiven)
                + " " + wsMiddleName.substring(0, lenMiddle)
                + " " + wsFamilyName.substring(0, lenFamily);
        wsOutputArea = padRight(joined, 180);
        // ADD WS-GIVEN-NAME-LENGTH WS-MIDDLE-NAME-LENGTH WS-FAMILY-NAME-LENGTH 2 GIVING WS-OUTPUT-LENGTH
        int total = lenGiven + lenMiddle + lenFamily + 2;
        wsOutputLength = BigDecimal.valueOf(total);
        String truncated = wsOutputArea.substring(0, wsOutputLength.intValue());
        System.out.println("<" + truncated + ">");
    }

    // Helper to find characters before the first space in an alphanumeric field
    private int findLengthBeforeSpace(String s) {
        int idx = s.indexOf(' ');
        return (idx >= 0) ? idx : s.length();
    }

    // Helper to pad or truncate strings to a fixed length, simulating PIC X(n)
    private static String padRight(String s, int length) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= length) {
            return s.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(length);
        sb.append(s);
        for (int i = s.length(); i < length; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
