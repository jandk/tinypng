package be.twofold.tinypng;

import java.util.*;

/**
 * The number of bits per sample or per palette index
 */
public enum BitDepth {
    /**
     * One bit
     */
    ONE(1),

    /**
     * Two bits
     */
    TWO(2),

    /**
     * Four bits
     */
    FOUR(4),

    /**
     * Eight bits
     */
    EIGHT(8),

    /**
     * Sixteen bits
     */
    SIXTEEN(16);

    private final byte value;

    BitDepth(int value) {
        this.value = (byte) value;
    }

    byte value() {
        return value;
    }

    static Optional<BitDepth> fromValue(int value) {
        switch (value) {
            case 1:
                return Optional.of(ONE);
            case 2:
                return Optional.of(TWO);
            case 4:
                return Optional.of(FOUR);
            case 8:
                return Optional.of(EIGHT);
            case 16:
                return Optional.of(SIXTEEN);
            default:
                return Optional.empty();
        }
    }
}
