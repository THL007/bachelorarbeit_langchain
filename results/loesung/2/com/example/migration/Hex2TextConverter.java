package com.example.migration;

/**
 * Converter for transforming binary data into displayable hexadecimal characters.
 */
public class Hex2TextConverter {

    /**
     * Hexadecimal character lookup string.
     */
    private static final String HEX_CHARS = "0123456789ABCDEF";

    /**
     * Converts the given original value (as a text string representing raw bytes) into its hexadecimal representation.
     *
     * @param originalValue string containing raw byte values; PIC X(n) in COBOL
     * @param originalLength number of bytes to convert; PIC S9(n) COMP in COBOL
     * @return hexadecimal string of length originalLength * 2
     * @throws IllegalArgumentException if input parameters are invalid
     */
    public String convert(String originalValue, int originalLength) {
        if (originalValue == null) {
            throw new IllegalArgumentException("originalValue must not be null");
        }
        if (originalLength < 0 || originalLength > originalValue.length()) {
            throw new IllegalArgumentException(
                    "originalLength out of range: " + originalLength);
        }

        StringBuilder result = new StringBuilder(originalLength * 2);
        for (int i = 0; i < originalLength; i++) {
            char ch = originalValue.charAt(i);
            int byteValue = ch & 0xFF;
            int quotient = byteValue / 16;
            int remainder = byteValue % 16;

            // Append high-order hex character and low-order hex character
            result.append(HEX_CHARS.charAt(quotient));
            result.append(HEX_CHARS.charAt(remainder));
        }
        return result.toString();
    }
}
