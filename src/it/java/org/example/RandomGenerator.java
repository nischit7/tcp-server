package org.example;

import java.util.Random;

/**
 * Generates some 9 digit random numbers. Used for testing purpose.
 */
public final class RandomGenerator {

    private RandomGenerator() {
        // Nothing to do
    }

    /**
     * Generates a random number.
     *
     * @return int
     */
    public static int randomInt() {
        return 100000000 + new Random().nextInt(900000000);
    }
}
