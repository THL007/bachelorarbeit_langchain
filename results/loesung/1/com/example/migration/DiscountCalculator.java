package com.example.migration;

import java.math.BigDecimal;

/**
 * Performs discount calculations on a CustomerRecord.
 */
public class DiscountCalculator {

    /**
     * Calculates discount rate, discount amount, and final amount for the given record.
     * @param record the customer record to update
     */
    public static void calculate(CustomerRecord record) {
        BigDecimal purchaseAmount = record.getPurchaseAmount();
        BigDecimal discountRate;
        if (purchaseAmount.compareTo(new BigDecimal("10000")) >= 0) {
            discountRate = new BigDecimal("0.15");
        } else if (purchaseAmount.compareTo(new BigDecimal("5000")) >= 0) {
            discountRate = new BigDecimal("0.10");
        } else if (purchaseAmount.compareTo(new BigDecimal("1000")) >= 0) {
            discountRate = new BigDecimal("0.05");
        } else {
            discountRate = BigDecimal.ZERO;
        }
        record.setDiscountRate(discountRate);
        BigDecimal discountAmount = purchaseAmount.multiply(discountRate);
        record.setDiscountAmount(discountAmount);
        BigDecimal finalAmount = purchaseAmount.subtract(discountAmount);
        record.setFinalAmount(finalAmount);
    }
}
