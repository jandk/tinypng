package be.twofold.tinypng;

/**
 * Specifies what type of pixels are represented
 */
public enum ColorType {
    /**
     * Each pixel is a grayscale sample
     */
    GRAY(0, 1),

    /**
     * Each pixel is an R,G,B triple
     */
    RGB(2, 3),

    /**
     * Each pixel is a palette index; a PLTE chunk shall appear
     */
    INDEXED(3, 1),

    /**
     * Each pixel is a grayscale sample followed by an alpha sample
     */
    GRAY_ALPHA(4, 2),

    /**
     * Each pixel is an R,G,B triple followed by an alpha sample
     */
    RGB_ALPHA(6, 4);

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
}
