package com.example.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Demonstrates how to read and write sequential datasets (QSAM) with variable-length records.
 * This program reads VARFILE1, appends data to each record, and writes the records to VARFILE2.
 * It also counts the number of records processed.
 */
public class Cpseqvr {
    private static final String INPUT_FILE = "VARFILE1";
    private static final String OUTPUT_FILE = "VARFILE2";
    private static final int RECORD_FIELD_LENGTH = 5;
    private static final String RECORD_FIELD_FILLER = "XXXXX";

    private BigDecimal recordCount = BigDecimal.ZERO;

    public static void main(String[] args) {
        new Cpseqvr().run();
    }

    public void run() {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(INPUT_FILE));
        } catch (IOException e) {
            System.out.println("INFILE STATUS ON OPEN: " + e.getMessage());
            return;
        }

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(OUTPUT_FILE));
        } catch (IOException e) {
            System.out.println("OUTFILE STATUS ON OPEN: " + e.getMessage());
            try {
                reader.close();
            } catch (IOException ex) {
                // ignore
            }
            return;
        }

        try (BufferedReader r = reader; BufferedWriter w = writer) {
            String line;
            while ((line = r.readLine()) != null) {
                int inRecLen = line.length();
                int fieldCount = inRecLen / RECORD_FIELD_LENGTH;
                fieldCount += 1;
                String outputRecord = line + RECORD_FIELD_FILLER;
                w.write(outputRecord);
                w.newLine();
                recordCount = recordCount.add(BigDecimal.ONE);
            }
        } catch (IOException e) {
            System.out.println("Error processing files: " + e.getMessage());
        }

        System.out.println("NUMBER OF RECORDS PROCESSED: " + recordCount);
    }
}
