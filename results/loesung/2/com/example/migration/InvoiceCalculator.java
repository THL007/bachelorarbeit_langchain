package com.example.migration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Migration of COBOL program INVCALC to Java.
 * Calculates invoice totals for sample invoice data.
 */
public class InvoiceCalculator {

    private BigDecimal salesTaxRate = new BigDecimal("0.065");

    private int workingIndex;
    private BigDecimal cumulativePriceBeforeTax = BigDecimal.ZERO;
    private BigDecimal cumulativePriceWithTax = BigDecimal.ZERO;
    private BigDecimal cumulativeSalesTax = BigDecimal.ZERO;
    private BigDecimal lineWorkingTotal = BigDecimal.ZERO;
    private BigDecimal lineWorkingTax = BigDecimal.ZERO;

    private String invDate;
    private String invNumber;
    private BigDecimal invTotalAmount = BigDecimal.ZERO;
    private BigDecimal invTotalBeforeTax = BigDecimal.ZERO;
    private BigDecimal invTotalSalesTax = BigDecimal.ZERO;
    private boolean isReturn;
    private int invLineItemCount;
    private List<LineItem> invLineItems = new ArrayList<>();

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat TAX_FORMAT = new DecimalFormat("$#,##0.000");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("$#,##0.00");
    private static final DecimalFormat QUANTITY_FORMAT = new DecimalFormat("#,##0");
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat(".00000");

    public static void main(String[] args) {
        new InvoiceCalculator().run();
    }

    public void run() {
        invLineItems.clear();

        invDate = "20230914";
        invNumber = "Sample 1";
        invLineItemCount = 3;

        invLineItems.add(new LineItem("PROD004411", new BigDecimal("18.55"), 2, true));
        invLineItems.add(new LineItem("PROD004412", new BigDecimal("6.32"), 4, false));
        invLineItems.add(new LineItem("PROD004413", new BigDecimal("2.28"), 8, true));

        cumulativePriceBeforeTax = BigDecimal.ZERO;
        cumulativePriceWithTax = BigDecimal.ZERO;
        cumulativeSalesTax = BigDecimal.ZERO;

        for (workingIndex = 0; workingIndex < invLineItemCount; workingIndex++) {
            LineItem item = invLineItems.get(workingIndex);

            lineWorkingTotal = item.unitPrice.multiply(new BigDecimal(item.quantity));
            cumulativePriceBeforeTax = cumulativePriceBeforeTax.add(lineWorkingTotal);

            if (item.taxable) {
                lineWorkingTax = lineWorkingTotal.multiply(salesTaxRate)
                        .setScale(3, RoundingMode.HALF_UP);
                lineWorkingTotal = lineWorkingTotal.add(lineWorkingTax);
            } else {
                lineWorkingTax = BigDecimal.ZERO;
            }
            cumulativePriceWithTax = cumulativePriceWithTax.add(lineWorkingTotal);
            cumulativeSalesTax = cumulativeSalesTax.add(lineWorkingTax);
        }

        invTotalSalesTax = cumulativeSalesTax;
        invTotalBeforeTax = cumulativePriceBeforeTax;
        invTotalAmount = cumulativePriceWithTax;

        printInvoiceDetails();
    }

    private void invalidInvoiceData() {
        System.out.println("Invalid invoice data");
    }

    private void printInvoiceDetails() {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println("Invoice Number:   " + invNumber);

        String year = invDate.substring(0, 4);
        String month = invDate.substring(4, 6);
        String day = invDate.substring(6, 8);
        String formattedDate = year + "/" + month + "/" + day;
        System.out.println("Invoice Date:     " + formattedDate);

        System.out.println("Total Amount:     " + AMOUNT_FORMAT.format(invTotalAmount));
        System.out.println("Total Before Tax: " + AMOUNT_FORMAT.format(invTotalBeforeTax));
        System.out.println("Total Sales Tax:    " + TAX_FORMAT.format(invTotalSalesTax));
        System.out.println("Sales Tax Rate:     " + RATE_FORMAT.format(salesTaxRate));

        if (isReturn) {
            System.out.println("This is a return");
        }

        for (int i = 0; i < invLineItemCount; i++) {
            System.out.println();
            System.out.println("Line " + String.format("%3d", i + 1));
            LineItem item = invLineItems.get(i);
            System.out.println("SKU  " + item.sku);
            System.out.println("Quantity " + QUANTITY_FORMAT.format(item.quantity));
            System.out.println("Unit Price:  " + PRICE_FORMAT.format(item.unitPrice));
            if (item.taxable) {
                System.out.println("Taxable Item");
            } else {
                System.out.println("Nontaxable Item");
            }
        }
    }

    private static class LineItem {
        String sku;
        BigDecimal unitPrice;
        int quantity;
        boolean taxable;

        LineItem(String sku, BigDecimal unitPrice, int quantity, boolean taxable) {
            this.sku = sku;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.taxable = taxable;
        }
    }
}
