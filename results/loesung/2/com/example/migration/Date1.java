package com.example.migration;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

/**
 * Migration of COBOL DATE1 program to Java.
 */
public class Date1 {
    private static final String[] MONTH_NAMES = {
        "",
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    private static final String[] MONTH_ABBREVS = {
        "",
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    private static final String[] ORDINAL_SUFFIXES = {
        "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"
    };

    public static void main(String[] args) {
        new Date1().run();
    }

    public void run() {
        ZonedDateTime now = ZonedDateTime.now();

        int fullYear = now.getYear();
        int month = now.getMonthValue();
        int dayOfMonth = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();

        int offsetTotalSeconds = now.getOffset().getTotalSeconds();
        char timezoneDirection = offsetTotalSeconds >= 0 ? '+' : '-';
        int offsetHours = Math.abs(offsetTotalSeconds) / 3600;
        int offsetMinutes = (Math.abs(offsetTotalSeconds) % 3600) / 60;

        // Example 1: NCSA common log format timestamp
        String ncsaTimestamp = String.format(
            "[%02d/%s/%04d:%02d:%02d:%02d %c%02d%02d]",
            dayOfMonth,
            MONTH_ABBREVS[month],
            fullYear,
            hour,
            minute,
            second,
            timezoneDirection,
            offsetHours,
            offsetMinutes
        );

        System.out.println();
        System.out.println("Example 1: Timestamp in NCSA common log format: " + ncsaTimestamp);

        // Example 2: verbose date formatting
        int dayLastDigit = dayOfMonth % 10;
        String fullDate = MONTH_NAMES[month]
            + " "
            + dayOfMonth
            + ORDINAL_SUFFIXES[dayLastDigit]
            + ", "
            + fullYear;

        System.out.println();
        System.out.println("Example 2: Current date formatted verbosely: " + fullDate);

        // Example 3: shorthand US style MM/DD/YY
        int year2Digit = fullYear % 100;
        String shorthandUs = String.format(
            "%02d/%02d/%02d",
            month,
            dayOfMonth,
            year2Digit
        );

        System.out.println();
        System.out.println("Example 3: Shorthand date, US style MM/DD/YY: " + shorthandUs);

        // Example 4: shorthand European style DD.MM.YY
        String shorthandEuro = String.format(
            "%02d.%02d.%02d",
            dayOfMonth,
            month,
            year2Digit
        );

        System.out.println();
        System.out.println("Example 4: Shorthand date, European style DD.MM.YY: " + shorthandEuro);
    }
}
