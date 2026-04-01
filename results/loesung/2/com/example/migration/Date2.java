package com.example.migration;

import java.time.LocalDateTime;

/**
 * Migration of COBOL DATE2 program to Java.
 * Demonstrates obtaining and formatting the current date and time.
 */
public class Date2 {
    private static final String[] DAY_NAMES = {
        "",
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private static final String[] MONTH_NAMES = {
        "",
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    private static final String[] DAY_ORDINAL_SUFFIXES = {
        "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"
    };

    /**
     * Application entry point.
     */
    public static void main(String[] args) {
        new Date2().run();
    }

    /**
     * Runs the date/time formatting logic.
     */
    public void run() {
        LocalDateTime now = LocalDateTime.now();

        int year = now.getYear();
        int month = now.getMonthValue();
        int dayOfMonth = now.getDayOfMonth();
        int dayOfWeekValue = now.getDayOfWeek().getValue(); // 1=Monday, ..., 7=Sunday

        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();
        int hundredth = now.getNano() / 10_000_000; // hundredths of a second

        // Format date verbosely: "Today is Monday, the 3rd of January, 2023"
        int dayLastDigit = dayOfMonth % 10;
        String suffix = DAY_ORDINAL_SUFFIXES[dayLastDigit];
        String fullDate = new StringBuilder()
            .append("Today is ")
            .append(DAY_NAMES[dayOfWeekValue])
            .append(", the ")
            .append(String.format("%02d", dayOfMonth))
            .append(suffix)
            .append(" of ")
            .append(MONTH_NAMES[month])
            .append(", ")
            .append(year)
            .toString();

        System.out.println();
        System.out.println("Example 1: Current date formatted verbosely: " + fullDate);

        // Shorthand date, US style MM/DD/YY
        String shorthandUs = String.format("%02d/%02d/%02d", month, dayOfMonth, year % 100);
        System.out.println();
        System.out.println("Example 2: Shorthand date, US style MM/DD/YY: " + shorthandUs);

        // Shorthand date, European style DD.MM.YY
        String shorthandEuro = String.format("%02d.%02d.%02d", dayOfMonth, month, year % 100);
        System.out.println();
        System.out.println("Example 3: Shorthand date, European style DD.MM.YY: " + shorthandEuro);

        // Time with precision of hundredths of a second
        System.out.println();
        System.out.println("Example 4: Time with precision of hundredths of a second:");
        System.out.println(String.format("%02d:%02d:%02d.%02d", hour, minute, second, hundredth));
    }
}
