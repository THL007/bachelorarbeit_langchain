package com.example.migration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Migrated from COBOL program BRAKES.
 * Calculates the temperature change (delta T) of truck brakes after a downhill run.
 */
public class Brakes {

    // Given values from COBOL WORKING-STORAGE
    private final BigDecimal brakeMass; // mass of brake material (m) in kg
    private final BigDecimal specificHeat; // specific heat (sh) in J/(kg*C)
    private final BigDecimal truckWeight; // weight of truck (w) in kg
    private final BigDecimal verticalDistance; // vertical displacement (d) in m
    private final BigDecimal gravityAcceleration; // acceleration due to gravity (a) in m/s^2

    // Calculated values
    private BigDecimal potentialEnergyLoss; // Mgh
    private BigDecimal brakeHeatCapacity; // m * sh
    private BigDecimal deltaTCelsius; // temperature change in Celsius

    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public Brakes() {
        this.brakeMass = new BigDecimal("100");
        this.specificHeat = new BigDecimal("800");
        this.truckWeight = new BigDecimal("10000");
        this.verticalDistance = new BigDecimal("75.0");
        this.gravityAcceleration = new BigDecimal("9.8");
    }

    /**
     * Performs the engineering calculations:
     * potentialEnergyLoss = truckWeight * gravityAcceleration * verticalDistance
     * brakeHeatCapacity = brakeMass * specificHeat
     * deltaTCelsius = potentialEnergyLoss / brakeHeatCapacity
     */
    public void compute() {
        potentialEnergyLoss = truckWeight
                .multiply(gravityAcceleration)
                .multiply(verticalDistance);
        brakeHeatCapacity = brakeMass.multiply(specificHeat);
        // divide with precision, set scale later for display
        deltaTCelsius = potentialEnergyLoss.divide(brakeHeatCapacity, 10, RoundingMode.HALF_UP);
    }

    public void displayResult() {
        String formattedDelta = decimalFormat.format(deltaTCelsius);
        System.out.println("deltaT-Celsius: " + formattedDelta);
    }

    public static void main(String[] args) {
        Brakes program = new Brakes();
        program.compute();
        program.displayResult();
    }
}
