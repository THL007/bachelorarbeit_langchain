package com.example.migration;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Migrated CUSTOMER-MAIN program, including MAIN-PROCESS and PROCESS-CUSTOMER logic.
 */
public class CustomerMain {
    public static void main(String[] args) {
        // Initialize scanner for user input
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("==============================");
            System.out.println("Customer Discount Calculator");
            System.out.println("==============================");
            System.out.println();

            String continueFlag = "Y";
            // Loop until user chooses to stop
            while (!"N".equalsIgnoreCase(continueFlag)) {
                processCustomer(scanner);
                // Ask to continue
                System.out.print("Process another customer? (Y/N): ");
                continueFlag = scanner.nextLine().trim().toUpperCase();
                System.out.println();
            }

            System.out.println();
            System.out.println("Thank you for using the system.");
        }
    }

    /**
     * PROCESS-CUSTOMER paragraph logic migrated to Java.
     *
     * @param scanner the Scanner for user input
     */
    private static void processCustomer(Scanner scanner) {
        // Accept Customer ID
        System.out.print("Enter Customer ID (5 digits): ");
        String idInput = scanner.nextLine().trim();
        if (idInput.length() > 5) {
            idInput = idInput.substring(0, 5);
        }
        int customerId = Integer.parseInt(idInput);

        // Accept Customer Name
        System.out.print("Enter Customer Name: ");
        String customerName = scanner.nextLine();
        if (customerName.length() > 30) {
            customerName = customerName.substring(0, 30);
        }

        // Accept Purchase Amount
        System.out.print("Enter Purchase Amount: ");
        BigDecimal purchaseAmount = new BigDecimal(scanner.nextLine().trim());

        CustomerRecord record = new CustomerRecord(customerId, customerName, purchaseAmount);

        // Call utility program to calculate discount
        DiscountCalculator.DiscountResult result = DiscountCalculator.calculateDiscount(purchaseAmount);
        record.setDiscountRate(result.getDiscountRate());
        record.setDiscountAmount(result.getDiscountAmount());
        record.setFinalAmount(result.getFinalAmount());

        // Display results
        System.out.println();
        System.out.println("--- Calculation Results ---");
        System.out.println("Customer: " + record.getCustomerName());
        System.out.println("Purchase Amount: $" + record.getPurchaseAmount());
        System.out.println("Discount Rate: " + record.getDiscountRate() + "%");
        System.out.println("Discount Amount: $" + record.getDiscountAmount());
        System.out.println("Final Amount: $" + record.getFinalAmount());
        System.out.println();
    }

    // NOTE: Data structure mapped from COBOL COPY 'customer-data.cpy'
    private static class CustomerRecord {
        private final int customerId;
        private final String customerName;
        private final BigDecimal purchaseAmount;
        private BigDecimal discountRate;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;

        CustomerRecord(int customerId, String customerName, BigDecimal purchaseAmount) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.purchaseAmount = purchaseAmount;
        }

        public int getCustomerId() {
            return customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public BigDecimal getPurchaseAmount() {
            return purchaseAmount;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public void setDiscountRate(BigDecimal discountRate) {
            this.discountRate = discountRate;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public void setDiscountAmount(BigDecimal discountAmount) {
            this.discountAmount = discountAmount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }

        public void setFinalAmount(BigDecimal finalAmount) {
            this.finalAmount = finalAmount;
        }
    }
}
