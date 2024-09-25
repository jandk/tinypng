package be.twofold.tinypng;

import java.util.*;

/**
 * Specifies what type of pixels are represented
 */
public enum ColorType {
    /**
     * Each pixel is a grayscale sample
     */
    GRAYSCALE(0, 1),

    /**
     * Each pixel is an R,G,B triple
     */
    TRUECOLOR(2, 3),

    /**
     * Each pixel is a palette index; a PLTE chunk shall appear
     */
    INDEXED(3, 1),

    /**
     * Each pixel is a grayscale sample followed by an alpha sample
     */
    GRAYSCALE_ALPHA(4, 2),

    /**
     * Each pixel is an R,G,B triple followed by an alpha sample
     */
    TRUECOLOR_ALPHA(6, 4);

    private final byte value;
    private final int samples;

    ColorType(int value, int samples) {
        this.value = (byte) value;
        this.samples = samples;
    }

    byte value() {
        return value;
    }

    int samples() {
        return samples;
    }

    static Optional<ColorType> fromValue(byte value) {
        switch (value) {
            case 0:
                return Optional.of(GRAYSCALE);
            case 2:
                return Optional.of(TRUECOLOR);
            case 3:
                return Optional.of(INDEXED);
            case 4:
                return Optional.of(GRAYSCALE_ALPHA);
            case 6:
                return Optional.of(TRUECOLOR_ALPHA);
            default:
                return Optional.empty();
        }
    }
}
