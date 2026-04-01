package com.example.migration;

import java.util.Scanner;

/**
 * -----------------------
 * Copyright Contributors to the COBOL Programming Course
 * SPDX-License-Identifier: CC-BY-4.0
 * -----------------------
 * This program accepts input and displays output (COBOL ADDAMT)
 */
public class AddAmt {

    // Working-storage variables
    private String custNoIn;           // PIC X(15)
    private int amt1In;                // PIC 9(5)
    private int amt2In;                // PIC 9(5)
    private int amt3In;                // PIC 9(5)

    private String custNoOut;          // PIC X(15)
    private int totalOut;              // PIC 9(6)

    private String moreData = "YES"; // PIC X(3) VALUE 'YES'

    public static void main(String[] args) {
        AddAmt program = new AddAmt();
        program.execute();
    }

    /**
     * ENTRY POINT for procedure division
     */
    public void execute() {
        try (Scanner scanner = new Scanner(System.in)) {
            perform100Main(scanner);
        }
    }

    /**
     * 100-MAIN paragraph logic
     */
    private void perform100Main(Scanner scanner) {
        // PERFORM UNTIL MORE-DATA = 'NO '
        while (!"NO ".equals(moreData)) {
            // DISPLAY 'ENTER NAME       (15 CHARACTERS)'
            System.out.println("ENTER NAME       (15 CHARACTERS)");
            // ACCEPT CUST-NO-IN
            custNoIn = scanner.nextLine();
            if (custNoIn.length() > 15) {
                custNoIn = custNoIn.substring(0, 15);
            }

            // DISPLAY 'Enter amount of first purchase (5 digits)'
            System.out.println("Enter amount of first purchase (5 digits)");
            // ACCEPT AMT1-IN
            amt1In = Integer.parseInt(scanner.nextLine().trim());

            // DISPLAY 'Enter amount of second purchase (5 digits)'
            System.out.println("Enter amount of second purchase (5 digits)");
            // ACCEPT AMT2-IN
            amt2In = Integer.parseInt(scanner.nextLine().trim());

            // DISPLAY 'Enter amount of third purchase (5 digits)'
            System.out.println("Enter amount of third purchase (5 digits)");
            // ACCEPT AMT3-IN
            amt3In = Integer.parseInt(scanner.nextLine().trim());

            // MOVE CUST-NO-IN TO CUST-NO-OUT
            custNoOut = custNoIn;

            // ADD AMT1-IN AMT2-IN AMT3-IN GIVING TOTAL-OUT
            totalOut = amt1In + amt2In + amt3In;

            // DISPLAY CUST-NO-OUT 'Total Amount = ' TOTAL-OUT
            System.out.println(custNoOut + " Total Amount = " + totalOut);

            // DISPLAY 'MORE INPUT DATA (YES/NO)?'
            System.out.println("MORE INPUT DATA (YES/NO)?");
            // ACCEPT MORE-DATA
            String inputMore = scanner.nextLine();
            // INSPECT MORE-DATA CONVERTING 'noyes' to 'NOYES'
            inputMore = inputMore.replace('n', 'N')
                                 .replace('o', 'O')
                                 .replace('y', 'Y')
                                 .replace('e', 'E')
                                 .replace('s', 'S');
            // Ensure length 3 (pad with spaces or truncate)
            if (inputMore.length() < 3) {
                inputMore = (inputMore + "   ").substring(0, 3);
            } else if (inputMore.length() > 3) {
                inputMore = inputMore.substring(0, 3);
            }
            moreData = inputMore;
        }
        // GOBACK.
    }
}
