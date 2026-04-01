package com.example.migration;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Main program that processes customer orders.
 */
public class CustomerMain {
    private final Scanner scanner;
    private String continueFlag;

    /**
     * Constructs the main application.
     */
    public CustomerMain() {
        this.scanner = new Scanner(System.in);
        this.continueFlag = "Y";
    }

    /**
     * Application entry point.
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        new CustomerMain().run();
    }

    /**
     * Runs the main processing loop.
     */
    public void run() {
        displayHeader();
        while (!"N".equalsIgnoreCase(continueFlag)) {
            processCustomer();
        }
        System.out.println();
        System.out.println("Thank you for using the system.");
    }

    private void displayHeader() {
        System.out.println("==============================");
        System.out.println("Customer Discount Calculator");
        System.out.println("==============================");
        System.out.println();
    }

    private void processCustomer() {
        CustomerRecord record = new CustomerRecord();

        System.out.print("Enter Customer ID (5 digits): ");
        String idInput = scanner.nextLine();
        try {
            record.setCustomerId(Integer.parseInt(idInput));
        } catch (NumberFormatException e) {
            record.setCustomerId(0);
        }

        System.out.print("Enter Customer Name: ");
        String nameInput = scanner.nextLine();
        record.setCustomerName(nameInput);

        System.out.print("Enter Purchase Amount: ");
        String amountInput = scanner.nextLine();
        BigDecimal purchaseAmt;
        try {
            purchaseAmt = new BigDecimal(amountInput);
        } catch (NumberFormatException e) {
            purchaseAmt = BigDecimal.ZERO;
        }
        record.setPurchaseAmount(purchaseAmt);

        // Perform discount calculation
        DiscountCalculator.calculate(record);

        // Display results
        System.out.println();
        System.out.println("--- Calculation Results ---");
        System.out.println("Customer: " + record.getCustomerName());
        System.out.println("Purchase Amount: $" + record.getPurchaseAmount());
        System.out.println("Discount Rate: " + record.getDiscountRate() + "%");
        System.out.println("Discount Amount: $" + record.getDiscountAmount());
        System.out.println("Final Amount: $" + record.getFinalAmount());
        System.out.println();

        // Ask to continue
        System.out.print("Process another customer? (Y/N): ");
        continueFlag = scanner.nextLine();
        System.out.println();
    }
}
