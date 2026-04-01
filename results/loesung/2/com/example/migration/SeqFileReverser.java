package com.example.migration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Migrated from COBOL program CPSEQFR.
 * Copies a fixed-length sequential input file to an output file,
 * reversing the two fields in each record and counting records processed.
 */
public class SeqFileReverser {
    private static final int RECORD_LENGTH = 40;
    private final Path inputPath;
    private final Path outputPath;
    private BigDecimal recordCount = BigDecimal.ZERO;

    public SeqFileReverser(String inputFile, String outputFile) {
        this.inputPath = Paths.get(inputFile);
        this.outputPath = Paths.get(outputFile);
    }

    /**
     * Processes the input file, writing reversed-field records to the output file.
     * @throws IOException if an I/O error occurs
     */
    public void process() throws IOException {
        try (InputStream in = Files.newInputStream(inputPath);
             OutputStream out = Files.newOutputStream(outputPath)) {
            byte[] buffer = new byte[RECORD_LENGTH];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                if (bytesRead < RECORD_LENGTH) {
                    for (int i = bytesRead; i < RECORD_LENGTH; i++) {
                        buffer[i] = ' ';
                    }
                }
                String inputFirst = new String(buffer, 0, 10, StandardCharsets.UTF_8);
                String inputLast = new String(buffer, 10, 30, StandardCharsets.UTF_8);
                String outputFirst = padRight(inputLast, 30);
                String outputLast = padRight(inputFirst, 10);
                String outputRecord = outputFirst + outputLast;
                out.write(outputRecord.getBytes(StandardCharsets.UTF_8));
                recordCount = recordCount.add(BigDecimal.ONE);
            }
        }
    }

    public BigDecimal getRecordCount() {
        return recordCount;
    }

    private static String padRight(String s, int length) {
        if (s.length() >= length) {
            return s.substring(0, length);
        }
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < length) {
            sb.append(' ');
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java com.example.migration.SeqFileReverser <inputFile> <outputFile>");
            return;
        }
        SeqFileReverser reverser = new SeqFileReverser(args[0], args[1]);
        try {
            reverser.process();
        } catch (IOException e) {
            System.out.println("Error processing files: " + e.getMessage());
        }
        System.out.println("NUMBER OF RECORDS PROCESSED: " + reverser.getRecordCount());
    }
}
