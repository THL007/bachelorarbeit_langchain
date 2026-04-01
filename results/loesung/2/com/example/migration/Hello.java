package com.example.migration;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Migrated from COBOL program HELLO.
 * Prompts the user for a name and displays a personalized greeting.
 */
public class Hello {
    private static final String PROMPT = "Please enter a name:";
    private static final String GREETING = "Hello, ";
    private static final String EXCLAMATION_POINT = "!";

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        new Hello().run();
    }

    /**
     * Runs the greeting logic.
     */
    public void run() {
        // Use try-with-resources for Scanner
        try (Scanner scanner = new Scanner(System.in)) {
            // Display prompt
            System.out.println(PROMPT);
            // Read user input
            String friendInput = scanner.nextLine();

            // Count trailing spaces by reversing the input and tallying leading spaces
            BigDecimal trailingSpaces = BigDecimal.ZERO;
            String reversed = new StringBuilder(friendInput).reverse().toString();
            for (int i = 0; i < reversed.length(); i++) {
                if (reversed.charAt(i) == ' ') {
                    trailingSpaces = trailingSpaces.add(BigDecimal.ONE);
                } else {
                    break;
                }
            }

            // Determine actual name length after trimming trailing spaces
            int nameLength = friendInput.length() - trailingSpaces.intValue();
            if (nameLength < 0) {
                nameLength = 0;
            }
            String trimmedName = friendInput.substring(0, nameLength);

            // Build and display the message
            String message = GREETING + trimmedName + EXCLAMATION_POINT;
            System.out.println(message);
        }
    }
}
