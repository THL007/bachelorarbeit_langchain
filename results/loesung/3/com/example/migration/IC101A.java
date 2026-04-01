package com.example.migration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Migrated from COBOL program IC101A
 */
public class IC101A {
    // File writer for PRINT-FILE
    private BufferedWriter printFile;

    // Working-Storage Variables
    private int dn1 = 0;       // PIC S9 VALUE ZERO.
    private int dn2 = 0;       // PIC S9 VALUE ZERO.

    // TEST-RESULTS group
    private String testResultsFiller1 = " ";       // PIC X VALUE SPACE.
    private String feature = "";                   // PIC X(20)
    private String testResultsFiller2 = " ";       // PIC X VALUE SPACE.
    private String pOrF = "";                     // PIC X(5)
    private String testResultsFiller3 = " ";       // PIC X VALUE SPACE.
    private String parName = "";                  // PIC X(19)
    private String parDotX = "";                  // PIC X
    private int dotValue = 0;                       // PIC 99
    private String testResultsFiller4 = "";        // PIC X(8)
    private String reMark = "";                   // PIC X(61)

    // TEST-COMPUTED group
    private String testComputedFiller1 = "";       // PIC X(30)
    private String testComputedFiller2 = "       COMPUTED="; // literal
    private String computedA = "";                 // PIC X(20)
    private BigDecimal computedN = BigDecimal.ZERO; // PIC -9(9).9(9)
    private BigDecimal computed0v18 = BigDecimal.ZERO; // PIC -.9(18)
    private BigDecimal computed4v14 = BigDecimal.ZERO; // PIC -9(4).9(14)
    private BigDecimal computed14v4 = BigDecimal.ZERO; // PIC -9(14).9(4)
    private BigDecimal cm18v0 = BigDecimal.ZERO;    // PIC -9(18)
    private String computedFiller = "";            // PIC X(50)

    // TEST-CORRECT group
    private String testCorrectFiller1 = "";        // PIC X(30)
    private String testCorrectFiller2 = "       CORRECT ="; // literal
    private String correctA = "";                  // PIC X(20)
    private BigDecimal correctN = BigDecimal.ZERO;  // PIC -9(9).9(9)
    private BigDecimal correct0v18 = BigDecimal.ZERO; // PIC -.9(18)
    private BigDecimal correct4v14 = BigDecimal.ZERO; // PIC -9(4).9(14)
    private BigDecimal correct14v4 = BigDecimal.ZERO; // PIC -9(14).9(4)
    private BigDecimal cr18v0 = BigDecimal.ZERO;    // PIC -9(18)
    private String testCorrectFiller3 = "";        // PIC X(2)
    private String corAnsiReference = "";         // PIC X(48)

    // CCVS-C-1 and CCVS-C-2 group lines
    private String ccvsC1 = " FEATURE              PAIC1014.2SS  PARAGRAPH-NAME       REMARKS"; // PIC X(99)
    private String ccvsC2Part2 = "TESTED";         // PIC X(6)
    private String hyphenLine = "*****************************************************************************"; // PIC X(65)

    // Counters
    private int recCt = 0;         // PIC 99
    private int deleteCounter = 0; // PIC 999
    private int errorCounter = 0;  // PIC 999
    private int inspectCounter = 0;// PIC 999
    private int passCounter = 0;   // PIC 999
    private int errorHold = 0;     // PIC 999
    private int recordCount = 0;   // PIC 9(5)

    // Utility variables
    private String dummyHold = ""; // DUMMY-RECORD PIC X(120)
    private String ansiReference = ""; // PIC X(48)
    private String xxInfo = "*** INFORMATION ***"; // PIC X(19)
    private String infoText = "";               // PIC X(8)
    private final String ccvsPgmId = "IC101A";

    // Missing variables from COBOL
    private String testId = "";        // TEST-ID PIC X(9)
    private String idAgain = "";       // ID-AGAIN PIC X(9)
    private String ccvsH1 = "";        // CCVS-H-1
    private String ccvsH2A = "";       // CCVS-H-2A
    private String ccvsH2B = "";       // CCVS-H-2B
    private String ccvsH3 = "";        // CCVS-H-3
    private String infAnsiReference = ""; // INF-ANSI-REFERENCE PIC X(48)
    private String xxCorrect = "";     // XXCORRECT PIC X(20)
    private String xxComputed = "";    // XXCOMPUTED PIC X(20)

    public static void main(String[] args) {
        IC101A program = new IC101A();
        try {
            program.openFiles();
            program.performRootLogic();
            program.closeFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFiles() throws IOException {
        printFile = new BufferedWriter(new FileWriter("REPORT"));
        // Initialize IDENTIFIERS
        testId = ccvsPgmId;
        idAgain = ccvsPgmId;
        // Initialize TEST-RESULTS
        feature = space(20);
        pOrF = space(5);
        parName = space(19);
        parDotX = " ";
        dotValue = 0;
        reMark = space(61);
    }

    private void closeFiles() throws IOException {
        // End routine sequence
        endRoutine();
        printFile.close();
    }

    private void performRootLogic() throws IOException {
        headRoutine();
        columnNamesRoutine();
        // Exit CCVS1-EXIT
    }

    private void headRoutine() throws IOException {
        dummyHold = ccvsH1;
        writeLine(2);
        dummyHold = ccvsH2A;
        writeLine(2);
        dummyHold = ccvsH2B + ccvsPgmId + " IN  HIGH       ";
        writeLine(3);
        dummyHold = ccvsH3;
        writeLine(3);
    }

    private void columnNamesRoutine() throws IOException {
        dummyHold = ccvsC1;
        writeLine(1);
        dummyHold = ccvsC2Part2;
        writeLine(2);
        dummyHold = hyphenLine;
        writeLine(1);
    }

    private void inspT() {
        pOrF = "INSPT";
        inspectCounter++;
    }

    private void pass() {
        pOrF = "PASS ";
        passCounter++;
    }

    private void fail() {
        pOrF = "FAIL*";
        errorCounter++;
    }

    private void deleteTest() {
        pOrF = "*****";
        deleteCounter++;
        reMark = "****TEST DELETED****";
    }

    private void printDetail() throws IOException {
        if (recCt != 0) {
            parDotX = ".";
            dotValue = recCt;
        }
        // Compose TEST-RESULTS line
        dummyHold = feature + pOrF + parName + parDotX + dotValue + reMark;
        writeLine(1);
        if ("FAIL*".equals(pOrF)) {
            writeLine(1);
            failRoutine();
        } else {
            bailOut();
        }
        pOrF = space(5);
        computedA = space(20);
        correctA = space(20);
        if (recCt == 0) {
            parName = space(19);
        }
        reMark = space(61);
    }

    private void wrtLn() throws IOException {
        writeLine(1);
        dummyHold = space(120);
    }

    private void blankLinePrint() throws IOException {
        wrtLn();
    }

    private void failRoutine() throws IOException {
        if (!computedA.trim().isEmpty() || !correctA.trim().isEmpty()) {
            failRoutineWrite();
            return;
        }
        infAnsiReference = ansiReference;
        infoText = "NO FURTHER INFORMATION, SEE PROGRAM.";
        dummyHold = xxInfo;
        writeLine(2);
        infAnsiReference = space(48);
    }

    private void failRoutineWrite() throws IOException {
        // Move TEST-COMPUTED
        dummyHold = testComputedFiller2 + computedA;
        writeLine(1);
        // Move TEST-CORRECT
        dummyHold = testCorrectFiller2 + correctA;
        writeLine(2);
        corAnsiReference = space(48);
    }

    private void bailOut() throws IOException {
        if (!computedA.trim().isEmpty()) {
            bailOutWrite();
            return;
        }
        if (correctA.trim().isEmpty()) {
            return;
        }
    }

    private void bailOutWrite() throws IOException {
        xxCorrect = correctA;
        xxComputed = computedA;
        infAnsiReference = ansiReference;
        dummyHold = xxInfo;
        writeLine(2);
        infAnsiReference = space(48);
    }

    private void ccvsExit() throws IOException {
        closeFiles();
    }

    // NOTE: stub for END-ROUTINE through END-ROUTINE-13
    private void endRoutine() throws IOException {
        // No implementation yet
    }

    /**
     * Writes the current DUMMY-RECORD (dummyHold) to the print file count times.
     */
    private void writeLine(int count) throws IOException {
        for (int i = 0; i < count; i++) {
            printFile.write(dummyHold);
            printFile.newLine();
        }
    }

    private String space(int length) {
        return new String(new char[length]).replace('\0', ' ');
    }
}
