      * Copybook: Customer Data Structure
      * Shared data definitions for customer processing
       01 CUSTOMER-RECORD.
          05 CUSTOMER-ID          PIC 9(5).
          05 CUSTOMER-NAME        PIC X(30).
          05 PURCHASE-AMOUNT      PIC 9(7)V99.
          05 DISCOUNT-RATE        PIC 9V99.
          05 DISCOUNT-AMOUNT      PIC 9(7)V99.
          05 FINAL-AMOUNT         PIC 9(7)V99.

