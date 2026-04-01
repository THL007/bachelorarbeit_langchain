package com.example.migration;

import java.math.BigDecimal;

/**
 * Auto-generated migration of COBOL program IC104A.
 */
public class IC104A {
    // File description fields for FD PRINT-FILE
    private static final String REPORT_FILE = "REPORT";
    private String printRec = String.format("%-120s", "");
    private String dummyRecord = String.format("%-120s", "");

    // Working-storage constants
    public static class Constants {
        public static final String AN_CONSTANT = "IC104";
        public static final BigDecimal NUM_CONSTANT = new BigDecimal("0.7654");
    }

    // Linkage section group GRP-01
    public static class Grp01 {
        private String anField; // PIC X(5)
        private int numDisplay; // PIC 99
        private GrpLevel grpLevel = new GrpLevel();

        public String getAnField() { return anField; }
        public void setAnField(String anField) { this.anField = padRight(anField, 5); }

        public int getNumDisplay() { return numDisplay; }
        public void setNumDisplay(int numDisplay) { this.numDisplay = numDisplay; }

        public GrpLevel getGrpLevel() { return grpLevel; }

        public static class GrpLevel {
            private String aField; // PIC A(3)

            public String getAField() { return aField; }
            public void setAField(String aField) { this.aField = padRight(aField, 3); }
        }
    }

    // Linkage section elementary item ELEM-01
    public static class Elem01 {
        private BigDecimal value; // PIC V9(4) COMPUTATIONAL

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }

    // Linkage section group GRP-02
    public static class Grp02 {
        private Grp03 grp03 = new Grp03();

        public Grp03 getGrp03() { return grp03; }

        public static class Grp03 {
            private int numItem;      // PIC S99
            private String editedField; // PIC XXBX0X

            public int getNumItem() { return numItem; }
            public void setNumItem(int numItem) { this.numItem = numItem; }

            public String getEditedField() { return editedField; }
            public void setEditedField(String editedField) { this.editedField = padRight(editedField, 6); }
        }
    }

    /**
     * Entry point for the migrated program logic.
     * @param grp01 linkage parameter GRP-01
     * @param elem01 linkage parameter ELEM-01
     * @param grp02 linkage parameter GRP-02
     */
    public void run(Grp01 grp01, Elem01 elem01, Grp02 grp02) {
        // SECTION SECT-IC104-0001
        callTest06(grp01, elem01, grp02);
        callExit06();
    }

    /**
     * Paragraph CALL-TEST-06
     */
    private void callTest06(Grp01 grp01, Elem01 elem01, Grp02 grp02) {
        // MOVE AN-CONSTANT TO AN-FIELD.
        grp01.setAnField(Constants.AN_CONSTANT);
        // ADD 25 TO NUM-DISPLAY.
        grp01.setNumDisplay(grp01.getNumDisplay() + 25);
        // MOVE "YES" TO A-FIELD.
        grp01.getGrpLevel().setAField("YES");
        // MOVE NUM-CONSTANT TO ELEM-01.
        elem01.setValue(Constants.NUM_CONSTANT);
        // MOVE NUM-DISPLAY TO NUM-ITEM.
        grp02.getGrp03().setNumItem(grp01.getNumDisplay());
        // MOVE "ABCD" TO EDITED-FIELD.
        grp02.getGrp03().setEditedField("ABCD");
    }

    /**
     * Paragraph CALL-EXIT-06
     */
    private void callExit06() {
        // EXIT PROGRAM.
        return;
    }

    // Utility padding method for alphanumeric fields
    private static String padRight(String input, int length) {
        if (input == null) {
            input = "";
        }
        if (input.length() >= length) {
            return input.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(input);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
