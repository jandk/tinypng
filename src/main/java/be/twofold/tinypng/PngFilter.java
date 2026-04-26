package be.twofold.tinypng;

/**
 * Represents the type of filtering to apply to a scanline.
 */
public enum PngFilter {
    /**
     * No filtering is applied.
     */
    NONE,

    /**
     * Filters using the difference between the current pixel and the pixel immediately to its left.
     */
    SUB,

    /**
     * Filters using the difference between the current pixel and the pixel immediately above.
     */
    UP,

    /**
     * Filters using the average of the three pixels immediately to the left and above.
     */
    AVERAGE,

    /**
     * Filters using the paeth filter, which predicts the pixel value based on the neighboring pixels.
     */
    PAETH,

    /**
     * Heuristic filtering based on the minimum sum of absolute differences (SAD) for each scanline.
     */
    ADAPTIVE,

    /**
     * Heuristic filtering based on the minimum entropy for each scanline.
     */
    MIN_ENTROPY,
}
