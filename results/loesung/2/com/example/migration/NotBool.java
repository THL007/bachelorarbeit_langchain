package com.example.migration;

import java.math.BigDecimal;

/**
 * Migrated from COBOL program NOTBOOL.
 * Demonstrates various pseudo-boolean implementations.
 */
public class NotBool {
    private String theAnswer = "";
    private String ex2Flag;
    private static final String EX2_TRUE_VALUE = "T";

    private String ex3Flag;
    private static final String EX3_YES_VALUE = "Y";

    private String ex4Flag;
    private static final String EX4_TRUE_VALUE = "1";
    private static final String EX4_FALSE_VALUE = "0";

    private String ex5Field;
    /** 88-level EX5-FLAG when ex5Field equals "T" */
    private static final String EX5_FLAG_TRUE = "T";

    private String ex6Flag;
    /** 88-level EX6-FLAG when ex6Flag equals "T" or false when "F" */
    private static final String EX6_FLAG_TRUE = "T";
    private static final String EX6_FLAG_FALSE = "F";

    private BigDecimal ex7Flag;
    private static final BigDecimal EX7_TRUE = BigDecimal.valueOf(1);
    private static final BigDecimal EX7_FALSE = BigDecimal.valueOf(-1);

    public static void main(String[] args) {
        NotBool demo = new NotBool();
        demo.runExamples();
    }

    private void runExamples() {
        example2();
        example3();
        example4();
        example5();
        example6();
        example7();
    }

    private void example2() {
        // Example 2: PIC X with 'T' = true, SPACE = false
        ex2Flag = EX2_TRUE_VALUE;
        if (ex2Flag.equals(EX2_TRUE_VALUE)) {
            theAnswer = "true";
        } else {
            theAnswer = "false";
        }
        System.out.println("Example2 Answer: " + theAnswer);

        // Toggle the flag
        if (ex2Flag.equals(EX2_TRUE_VALUE)) {
            ex2Flag = " ";
        } else {
            ex2Flag = EX2_TRUE_VALUE;
        }
        System.out.println("Example2 Toggle: ex2Flag='" + ex2Flag + "'");
    }

    private void example3() {
        // Example 3: PIC X with 'Y' = yes, 'N' = no
        ex3Flag = EX3_YES_VALUE;
        if (ex3Flag.equals(EX3_YES_VALUE)) {
            theAnswer = "yes";
        } else {
            theAnswer = "no";
        }
        System.out.println("Example3 Answer: " + theAnswer);

        // Another coding style
        if (!ex3Flag.equals(EX3_YES_VALUE)) {
            theAnswer = "no";
        }
        System.out.println("Example3 Second Style Answer: " + theAnswer);

        // Toggle the flag
        if (ex3Flag.equals(EX3_YES_VALUE)) {
            ex3Flag = "N";
        } else {
            ex3Flag = EX3_YES_VALUE;
        }
        System.out.println("Example3 Toggle: ex3Flag='" + ex3Flag + "'");
    }

    private void example4() {
        // Example 4: PIC X with '1' = true, '0' = false
        ex4Flag = EX4_TRUE_VALUE;
        if (ex4Flag.equals(EX4_TRUE_VALUE)) {
            theAnswer = "true";
        } else if (ex4Flag.equals(EX4_FALSE_VALUE)) {
            theAnswer = "false";
        } else {
            theAnswer = "not set";
        }
        System.out.println("Example4 Answer: " + theAnswer);

        // Toggle the flag
        if (ex4Flag.equals(EX4_TRUE_VALUE)) {
            ex4Flag = EX4_FALSE_VALUE;
        } else {
            ex4Flag = EX4_TRUE_VALUE;
        }
        System.out.println("Example4 Toggle: ex4Flag='" + ex4Flag + "'");
    }

    private void example5() {
        // Example 5: 88-level item without FALSE
        ex5Field = EX5_FLAG_TRUE; // SET EX5-FLAG TO TRUE
        if (isEx5Flag()) {
            theAnswer = "true";
            System.out.println("Example5 Answer: " + theAnswer);
        }

        // Toggle the flag
        if (isEx5Flag()) {
            ex5Field = " ";
        } else {
            ex5Field = EX5_FLAG_TRUE;
        }
        System.out.println("Example5 Toggle: ex5Field='" + ex5Field + "'");
    }

    private void example6() {
        // Example 6: 88-level item with FALSE
        ex6Flag = EX6_FLAG_TRUE; // SET EX6-FLAG TO TRUE
        if (isEx6Flag()) {
            theAnswer = "true";
            System.out.println("Example6 Answer: " + theAnswer);
        }

        // Toggle the flag
        if (isEx6Flag()) {
            ex6Flag = EX6_FLAG_FALSE;
        } else {
            // COBOL toggles EX5-FLAG on ELSE
            ex5Field = EX5_FLAG_TRUE;
        }
        System.out.println("Example6 Toggle: ex6Flag='" + ex6Flag + "', ex5Field='" + ex5Field + "'");
    }

    private void example7() {
        // Example 7: numeric pseudo-boolean using COMP-3
        ex7Flag = EX7_TRUE;
        if (ex7Flag.equals(EX7_TRUE)) {
            theAnswer = "true";
            System.out.println("Example7 Answer: " + theAnswer);
        }

        // Toggle the flag: COMPUTE EX7-FLAG = EX7-FLAG * -EX7-FLAG
        ex7Flag = ex7Flag.multiply(ex7Flag.negate());
        System.out.println("Example7 Toggle: ex7Flag=" + ex7Flag);
    }

    private boolean isEx5Flag() {
        return EX5_FLAG_TRUE.equals(ex5Field);
    }

    private boolean isEx6Flag() {
        return EX6_FLAG_TRUE.equals(ex6Flag);
    }
}
