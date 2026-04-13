package com.example.migration;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Utility class to calculate discounts based on purchase amount tiers.
 */
public class DiscountCalculator {

    /**
     * Calculates the discount rate, discount amount, and final amount after discount
     * for a given purchase amount.
     *
     * @param purchaseAmount the purchase amount
     * @return a DiscountResult containing rate, amount, and final total
     */
    public static DiscountResult calculateDiscount(BigDecimal purchaseAmount) {
        Objects.requireNonNull(purchaseAmount, "purchaseAmount must not be null");

        BigDecimal discountRate;
        if (purchaseAmount.compareTo(new BigDecimal("10000.00")) >= 0) {
            discountRate = new BigDecimal("0.15");
        } else if (purchaseAmount.compareTo(new BigDecimal("5000.00")) >= 0) {
            discountRate = new BigDecimal("0.10");
        } else if (purchaseAmount.compareTo(new BigDecimal("1000.00")) >= 0) {
            discountRate = new BigDecimal("0.05");
        } else {
            discountRate = BigDecimal.ZERO;
        }

        BigDecimal discountAmount = purchaseAmount.multiply(discountRate);
        BigDecimal finalAmount = purchaseAmount.subtract(discountAmount);

        return new DiscountResult(discountRate, discountAmount, finalAmount);
    }

    /**
     * Container for discount calculation results.
     */
    public static class DiscountResult {
        private final BigDecimal discountRate;
        private final BigDecimal discountAmount;
        private final BigDecimal finalAmount;

        /**
         * Constructs a DiscountResult.
         *
         * @param discountRate   the discount rate applied
         * @param discountAmount the amount discounted
         * @param finalAmount    the final amount after discount
         */
        public DiscountResult(BigDecimal discountRate, BigDecimal discountAmount, BigDecimal finalAmount) {
            this.discountRate = discountRate;
            this.discountAmount = discountAmount;
            this.finalAmount = finalAmount;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public BigDecimal getDiscountAmount() {
            return discountAmount;
        }

        public BigDecimal getFinalAmount() {
            return finalAmount;
        }
    }
}
