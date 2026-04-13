       IDENTIFICATION DIVISION.
       PROGRAM-ID. CUSTOMER-MAIN.
       AUTHOR. COBOL-EXAMPLE.
       DATE-WRITTEN. 2024-01-01.
      *
      * Main program that processes customer orders
      * Demonstrates COPY and CALL interactions
      *
       ENVIRONMENT DIVISION.
       
       DATA DIVISION.
       WORKING-STORAGE SECTION.
      * Use COPY to include shared data structure
       COPY 'customer-data.cpy'.
       
       01 WS-CONTINUE           PIC X VALUE 'Y'.
       
       PROCEDURE DIVISION.
       MAIN-PROCESS.
           DISPLAY "==============================".
           DISPLAY "Customer Discount Calculator".
           DISPLAY "==============================".
           DISPLAY " ".
           
           PERFORM PROCESS-CUSTOMER 
               UNTIL WS-CONTINUE = 'N'.
           
           DISPLAY " ".
           DISPLAY "Thank you for using the system.".
           STOP RUN.
       
       PROCESS-CUSTOMER.
      *    Get customer information
           DISPLAY "Enter Customer ID (5 digits): "
               WITH NO ADVANCING.
           ACCEPT CUSTOMER-ID.
           
           DISPLAY "Enter Customer Name: "
               WITH NO ADVANCING.
           ACCEPT CUSTOMER-NAME.
           
           DISPLAY "Enter Purchase Amount: "
               WITH NO ADVANCING.
           ACCEPT PURCHASE-AMOUNT.
           
      *    Call utility program to calculate discount
           CALL 'DISCOUNT-CALC' USING CUSTOMER-RECORD.
           
      *    Display results
           DISPLAY " ".
           DISPLAY "--- Calculation Results ---".
           DISPLAY "Customer: " CUSTOMER-NAME.
           DISPLAY "Purchase Amount: $" PURCHASE-AMOUNT.
           DISPLAY "Discount Rate: " DISCOUNT-RATE "%".
           DISPLAY "Discount Amount: $" DISCOUNT-AMOUNT.
           DISPLAY "Final Amount: $" FINAL-AMOUNT.
           DISPLAY " ".
           
      *    Ask to continue
           DISPLAY "Process another customer? (Y/N): "
               WITH NO ADVANCING.
           ACCEPT WS-CONTINUE.
           DISPLAY " ".

