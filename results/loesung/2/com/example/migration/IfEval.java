package com.example.migration;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class IfEval {

    private String resultOfCompare;
    private String alpha1;
    private String alpha2;
    private BigDecimal numeric1;
    private BigDecimal numeric2;
    private String numeric2Str;

    public static void main(String[] args) {
        new IfEval().run();
    }

    public void run() {
        // Example 1: IF statement, 2 alphanumeric items
        alpha1 = "cucumber";
        alpha2 = "radish";

        // Compare two alphanumeric items, conventional style
        if (alpha1.equals(alpha2)) {
            resultOfCompare = "equal";
        }
        if (!alpha1.equals(alpha2)) {
            resultOfCompare = "different";
        }
        if (alpha1.equals(alpha2)) {
            resultOfCompare = "equal";
        } else {
            resultOfCompare = "different";
        }

        // Compare two alphanumeric items, traditional style
        if (alpha1.equals(alpha2)) {
            resultOfCompare = "equal";
        }
        if (alpha1.equals(alpha2)) {
            resultOfCompare = "equal";
        } else {
            resultOfCompare = "different";
        }

        // Compare two alphanumeric items, modern style
        if (alpha1.equals(alpha2)) {
            resultOfCompare = "equal";
        }

        // Example 2: IF statement, alphanumeric field vs literal
        if (alpha1.equals("foobar")) {
            resultOfCompare = "equal";
        } else {
            resultOfCompare = "different";
        }

        // Example 3: Verify a numeric item contains numeric data
        numeric2Str = "garbage";
        if (isNumeric(numeric2Str)) {
            numeric2 = new BigDecimal(numeric2Str).add(BigDecimal.ONE);
        } else {
            numeric2 = BigDecimal.ONE;
        }

        // Example 4: Verify a numeric item is greater than zero
        numeric1 = BigDecimal.ZERO;
        numeric2 = new BigDecimal("100");
        if (numeric1.compareTo(BigDecimal.ZERO) > 0) {
            numeric2 = numeric2.divide(numeric1, 0, RoundingMode.HALF_UP);
        } else {
            numeric2 = numeric2.subtract(BigDecimal.ONE);
        }

        // Example 5: IF statement, two numeric fields
        numeric1 = new BigDecimal("7");
        numeric2 = new BigDecimal("36");
        if (numeric1.compareTo(numeric2) > 0) {
            resultOfCompare = "numeric-1";
        } else {
            resultOfCompare = "numeric-2";
        }

        // Example 6: EVALUATE statement
        numeric1 = new BigDecimal("8");
        numeric2 = new BigDecimal("13");
        if (numeric1.compareTo(numeric2) > 0) {
            resultOfCompare = "numeric-1";
        } else if (numeric1.compareTo(numeric2) < 0) {
            resultOfCompare = "numeric-2";
        } else {
            resultOfCompare = "equal";
        }

        // Example 6: EVALUATE statement, two conditions
        numeric1 = new BigDecimal("8");
        numeric2 = new BigDecimal("13");
        alpha1 = "THX-1138";
        alpha2 = "Terminator";
        if (numeric1.compareTo(numeric2) > 0 && alpha1.startsWith("THX")) {
            resultOfCompare = "THX and numeric-1";
        } else if (numeric1.compareTo(numeric2) < 0 && alpha1.startsWith("THX")) {
            resultOfCompare = "THX and numeric-2";
        } else if (numeric1.compareTo(numeric1) == 0 && alpha2.equals("Terminator")) {
            resultOfCompare = "Terminator and equal numbers";
        } else {
            resultOfCompare = "undefined";
        }

        // End of COBOL GOBACK equivalent
        return;
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
