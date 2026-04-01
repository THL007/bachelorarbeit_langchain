package com.example.migration;

import java.util.Scanner;

/**
 * Migrated from COBOL program ATTRACT.
 * Calculates the gravitational attraction between two bodies.
 */
public class Attract {
    private static final double G = 6.67428e-11;
    private static final String VELOCITY_TEXT = "velocity";
    private static final String POSITION_TEXT = "position";
    private static final String IDENTIFY_BODY_PROMPT = "Enter attributes of body # ";
    private static final String PROMPT_FOR_BODY_MASS = "Please enter the mass of the body in KG:";
    private static final String PROMPT_FOR_VELOCITY_OR_POSITION_START = "Please enter the body's ";
    private static final String PROMPT_AXIS_ON = " on the ";
    private static final String PROMPT_AXIS_END = " axis:";
    private static final String BODY_ATTRIBUTES_SUFFIX = " attributes:";
    private static final String BODY_MASS_DISPLAY = "    mass: ";
    private static final String BODY_VELOCITY_DISPLAY = "    vx, vy: ";
    private static final String BODY_POSITION_DISPLAY = "    px, py: ";
    private static final String PROMPT_TO_CONTINUE = "Do you want to proceed? (Y/n)";
    private static final String GOODBYE_DISPLAY = "Maybe next time. Bye!";
    private static final String DISTANCE_DISPLAY = "The distance between the bodies is: ";
    private static final String DISTANCE_IS_ZERO_DISPLAY = "The bodies are in the same position!";
    private static final String FORCE_DISPLAY = "The force of attaction is: ";
    private static final String FORCE_ALONG_AXIS_PREFIX = "The force along the ";
    private static final String FORCE_ALONG_AXIS_SUFFIX = " axis: ";

    private final Body[] bodies = new Body[2];
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        new Attract().run();
    }

    public void run() {
        for (int i = 0; i < bodies.length; i++) {
            bodies[i] = new Body();
            solicitAttributesOfBody(i);
        }
        for (int i = 0; i < bodies.length; i++) {
            verifyAttributesOfBody(i);
        }
        System.out.println();
        System.out.println(PROMPT_TO_CONTINUE);
        String input = scanner.nextLine();
        if (!input.isEmpty()) {
            char reply = input.charAt(0);
            if (reply == 'n' || reply == 'N') {
                System.out.println(GOODBYE_DISPLAY);
                return;
            }
        }
        computeAttraction();
    }

    private void solicitAttributesOfBody(int index) {
        System.out.println();
        System.out.println(IDENTIFY_BODY_PROMPT + (index + 1));
        System.out.println(PROMPT_FOR_BODY_MASS);
        bodies[index].mass = Double.parseDouble(scanner.nextLine());

        System.out.println(PROMPT_FOR_VELOCITY_OR_POSITION_START + VELOCITY_TEXT + PROMPT_AXIS_ON + 'X' + PROMPT_AXIS_END);
        bodies[index].vx = Double.parseDouble(scanner.nextLine());
        System.out.println(PROMPT_FOR_VELOCITY_OR_POSITION_START + VELOCITY_TEXT + PROMPT_AXIS_ON + 'Y' + PROMPT_AXIS_END);
        bodies[index].vy = Double.parseDouble(scanner.nextLine());

        System.out.println(PROMPT_FOR_VELOCITY_OR_POSITION_START + POSITION_TEXT + PROMPT_AXIS_ON + 'X' + PROMPT_AXIS_END);
        bodies[index].px = Double.parseDouble(scanner.nextLine());
        System.out.println(PROMPT_FOR_VELOCITY_OR_POSITION_START + POSITION_TEXT + PROMPT_AXIS_ON + 'Y' + PROMPT_AXIS_END);
        bodies[index].py = Double.parseDouble(scanner.nextLine());
    }

    private void verifyAttributesOfBody(int index) {
        System.out.println();
        System.out.println("Body #" + (index + 1) + BODY_ATTRIBUTES_SUFFIX);
        System.out.println(BODY_MASS_DISPLAY + bodies[index].mass);
        System.out.println(BODY_VELOCITY_DISPLAY + bodies[index].vx + ", " + bodies[index].vy);
        System.out.println(BODY_POSITION_DISPLAY + bodies[index].px + ", " + bodies[index].py);
    }

    private void computeAttraction() {
        double dx = bodies[0].px - bodies[1].px;
        double dy = bodies[0].py - bodies[1].py;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d == 0) {
            System.out.println(DISTANCE_IS_ZERO_DISPLAY);
            return;
        }
        System.out.println(DISTANCE_DISPLAY + d);

        double f = (G * bodies[0].mass * bodies[1].mass) / (d * d);
        System.out.println(FORCE_DISPLAY + f);

        double theta = Math.atan(dx);
        double fx = Math.cos(theta * f);
        double fy = Math.sin(theta * f);

        System.out.println(FORCE_ALONG_AXIS_PREFIX + 'X' + FORCE_ALONG_AXIS_SUFFIX + fx);
        System.out.println(FORCE_ALONG_AXIS_PREFIX + 'Y' + FORCE_ALONG_AXIS_SUFFIX + fy);
    }

    /**
     * Represents a body with mass, velocity, and position.
     */
    private static class Body {
        double mass;
        double vx;
        double vy;
        double px;
        double py;
    }
}
