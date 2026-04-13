       IDENTIFICATION DIVISION.
       PROGRAM-ID. DISCOUNT-CALC.
       AUTHOR. COBOL-EXAMPLE.
      *
      * Utility program to calculate customer discount
      * Called by main program with customer data
      *
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 WS-TEMP-DISCOUNT      PIC 9(7)V99.
       
       LINKAGE SECTION.
       COPY 'customer-data.cpy'.
       
       PROCEDURE DIVISION USING CUSTOMER-RECORD.
       
       CALCULATE-DISCOUNT.
      *    Calculate discount based on purchase amount tiers
           IF PURCHASE-AMOUNT >= 10000
               MOVE 0.15 TO DISCOUNT-RATE
           ELSE IF PURCHASE-AMOUNT >= 5000
               MOVE 0.10 TO DISCOUNT-RATE
           ELSE IF PURCHASE-AMOUNT >= 1000
               MOVE 0.05 TO DISCOUNT-RATE
           ELSE
               MOVE 0.00 TO DISCOUNT-RATE
           END-IF.
           
      *    Calculate actual discount amount
           COMPUTE DISCOUNT-AMOUNT = 
               PURCHASE-AMOUNT * DISCOUNT-RATE.
           
      *    Calculate final amount after discount
           COMPUTE FINAL-AMOUNT = 
               PURCHASE-AMOUNT - DISCOUNT-AMOUNT.
           
           GOBACK.

