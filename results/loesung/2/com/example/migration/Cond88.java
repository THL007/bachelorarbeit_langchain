package com.example.migration;

/**
 * Migration of COND88 COBOL program.
 * Demonstrates use of 88-level condition names.
 */
public class Cond88 {
    private String theAnswer;
    private boolean simple88;
    private boolean simple88WithFalse;
    private char categoryCode;
    private int personAge;

    public Cond88() {
        // Initialize default values
        this.theAnswer = "";
        this.simple88 = false;
        this.simple88WithFalse = false;
        this.categoryCode = ' ';
        this.personAge = 0;
    }

    public boolean isSimple88() {
        return simple88;
    }

    public void setSimple88(boolean simple88) {
        this.simple88 = simple88;
    }

    public boolean isSimple88WithFalse() {
        return simple88WithFalse;
    }

    public void setSimple88WithFalse(boolean simple88WithFalse) {
        this.simple88WithFalse = simple88WithFalse;
    }

    public char getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(char categoryCode) {
        this.categoryCode = categoryCode;
    }

    public boolean isCategoryA() {
        return categoryCode == 'A' || categoryCode == '3' || categoryCode == '7';
    }

    public boolean isCategoryB() {
        return categoryCode == 'B' || categoryCode == '9' || categoryCode == 'X';
    }

    public int getPersonAge() {
        return personAge;
    }

    public void setPersonAge(int personAge) {
        this.personAge = personAge;
    }

    public boolean isChild() {
        return personAge >= 0 && personAge <= 12;
    }

    public boolean isTeen() {
        return personAge >= 13 && personAge <= 19;
    }

    public boolean isYoungAdult() {
        return personAge >= 20 && personAge <= 35;
    }

    public boolean isAdult() {
        return personAge >= 36 && personAge <= 49;
    }

    public boolean isMiddleAged() {
        return personAge >= 50 && personAge <= 59;
    }

    public boolean isSenior() {
        return personAge >= 60 && personAge <= 74;
    }

    public boolean isElderly() {
        return personAge >= 75 && personAge <= 200;
    }

    /**
     * Executes the logic translated from the COND88 COBOL program.
     */
    public void execute() {
        // Example 1: Simple 88-level
        setSimple88(true);
        if (isSimple88()) {
            theAnswer = "true";
        }
        if (!isSimple88()) {
            theAnswer = "false";
        }

        // Example 2: 88-level with FALSE clause
        setSimple88WithFalse(true);
        if (isSimple88WithFalse()) {
            theAnswer = "true";
        }
        setSimple88WithFalse(false);
        if (!isSimple88WithFalse()) {
            theAnswer = "false";
        }

        // Example 3: 88-level with multiple values
        setCategoryCode('A');
        if (isCategoryA()) {
            theAnswer = "true";
        }
        setCategoryCode('E');
        if (isCategoryA()) {
            theAnswer = "A";
        } else if (isCategoryB()) {
            theAnswer = "B";
        } else {
            theAnswer = "?";
        }

        // Example 4: 88-level with a range of values
        setPersonAge(37);
        if (isChild()) {
            theAnswer = "child";
        } else if (isTeen()) {
            theAnswer = "teen";
        } else if (isYoungAdult()) {
            theAnswer = "young";
        } else if (isAdult()) {
            theAnswer = "adult";
        } else if (isMiddleAged()) {
            theAnswer = "middle";
        } else if (isSenior()) {
            theAnswer = "senior";
        } else if (isElderly()) {
            theAnswer = "elderly";
        } else {
            theAnswer = "ageless";
        }
    }

    /**
     * Entry point corresponding to COBOL STOP RUN.
     */
    public static void main(String[] args) {
        Cond88 program = new Cond88();
        program.execute();
        return;
    }
}
