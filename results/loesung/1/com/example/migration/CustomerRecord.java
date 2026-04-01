package com.example.migration;

import java.math.BigDecimal;

/**
 * Data structure representing a customer record.
 */
public class CustomerRecord {
    private int customerId;
    private String customerName;
    private BigDecimal purchaseAmount;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    /**
     * Gets the customer ID.
     * @return the customer ID
     */
    public int getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer ID.
     * @param customerId the customer ID
     */
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the customer name.
     * @return the customer name
     */
    public String getCustomerName() {
        return customerName;
    }

    /**
     * Sets the customer name.
     * @param customerName the customer name
     */
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    /**
     * Gets the purchase amount.
     * @return the purchase amount
     */
    public BigDecimal getPurchaseAmount() {
        return purchaseAmount;
    }

    /**
     * Sets the purchase amount.
     * @param purchaseAmount the purchase amount
     */
    public void setPurchaseAmount(BigDecimal purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    /**
     * Gets the discount rate (e.g., 0.15 for 15%).
     * @return the discount rate
     */
    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    /**
     * Sets the discount rate (e.g., 0.15 for 15%).
     * @param discountRate the discount rate
     */
    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    /**
     * Gets the discount amount.
     * @return the discount amount
     */
    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    /**
     * Sets the discount amount.
     * @param discountAmount the discount amount
     */
    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    /**
     * Gets the final amount after discount.
     * @return the final amount
     */
    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    /**
     * Sets the final amount after discount.
     * @param finalAmount the final amount
     */
    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }
}
