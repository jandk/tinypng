package be.twofold.tinypng;

import java.util.*;

/**
 * Describes the dimensions, color model, and bit depth of a PNG image.
 *
 * <p>Use {@link #of} for all color types except indexed, and {@link #indexed} for palette-based images.
 */
public final class PngFormat {
    private final int width;
    private final int height;
    private final PngColorType colorType;
    private final int bitDepth;

    // Indexed color properties
    private final byte[] palette;
    private final byte[] transparency;

    // Animation properties
    private final int frameCount;
    private final int playCount;

    private PngFormat(
        int width,
        int height,
        PngColorType colorType,
        int bitDepth,
        byte[] palette,
        byte[] transparency,
        int frameCount,
        int playCount
    ) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than 0");
        }
        validateColorAndDepth(colorType, bitDepth);
        validatePalette(colorType, bitDepth, palette, transparency);
        validateAnimation(frameCount, playCount);

        this.width = width;
        this.height = height;
        this.colorType = colorType;
        this.bitDepth = bitDepth;
        this.palette = palette != null ? palette.clone() : null;
        this.transparency = transparency != null ? transparency.clone() : null;
        this.frameCount = frameCount;
        this.playCount = playCount;
    }

    private static void validateColorAndDepth(PngColorType colorType, int bitDepth) {
        Objects.requireNonNull(colorType, "colorType must not be null");
        if (bitDepth == 8) {
            return;
        }
        if (colorType != PngColorType.INDEXED && bitDepth == 16) {
            return;
        }
        if ((colorType == PngColorType.GRAY || colorType == PngColorType.INDEXED)
            && (bitDepth == 1 || bitDepth == 2 || bitDepth == 4)) {
            return;
        }
        throw new IllegalArgumentException("Invalid bit depth " + bitDepth + " for color type " + colorType);
    }

    private static void validatePalette(PngColorType colorType, int bitDepth, byte[] palette, byte[] transparency) {
        if (colorType != PngColorType.INDEXED) {
            if (palette != null || transparency != null) {
                throw new IllegalArgumentException("palette or transparency not supported for non-indexed colors");
            }
            return;
        }

        if (palette == null) {
            throw new IllegalArgumentException("palette must not be null for colorType INDEXED");
        }
        if (palette.length == 0 || palette.length % 3 != 0) {
            throw new IllegalArgumentException("palette length must be a non-zero multiple of 3");
        }
        if (palette.length > (3 << bitDepth)) {
            throw new IllegalArgumentException("palette length must not be greater than " + (3 << bitDepth));
        }
        if (transparency != null && (transparency.length == 0 || transparency.length > palette.length / 3)) {
            throw new IllegalArgumentException("transparency length must be between 1 and palette length");
        }
    }

    private static void validateAnimation(int frameCount, int playCount) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException("frameCount must be greater than 0");
        }
        if (playCount < 0) {
            throw new IllegalArgumentException("playCount must be non-negative");
        }
    }

    /**
     * Creates a format for the given dimensions, color type, and bit depth.
     *
     * @param width     image width in pixels, must be greater than 0
     * @param height    image height in pixels, must be greater than 0
     * @param colorType the color model to use
     * @param bitDepth  bits per channel; valid values depend on {@code colorType}
     * @return a new {@code PngFormat}
     */
    public static PngFormat of(int width, int height, PngColorType colorType, int bitDepth) {
        return new PngFormat(width, height, colorType, bitDepth, null, null, 1, 0);
    }

    /**
     * Creates a format for a palette-based (indexed) image.
     *
     * @param width        image width in pixels, must be greater than 0
     * @param height       image height in pixels, must be greater than 0
     * @param bitDepth     bits per pixel; valid values are 1, 2, 4, or 8
     * @param palette      RGB palette entries packed as {@code [R, G, B, R, G, B, ...]}, must not be null
     * @param transparency per-entry alpha values for the palette, or {@code null} if fully opaque
     * @return a new {@code PngFormat} with {@link PngColorType#INDEXED}
     */
    public static PngFormat indexed(int width, int height, int bitDepth, byte[] palette, byte[] transparency) {
        return new PngFormat(width, height, PngColorType.INDEXED, bitDepth, palette, transparency, 1, 0);
    }

    /**
     * Returns the image width.
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the image height.
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the color model used by this format.
     *
     * @return the color type
     */
    public PngColorType getColorType() {
        return colorType;
    }

    /**
     * Returns the bit depth of this format.
     *
     * @return bits per channel, or bits per pixel for indexed images
     */
    public int getBitDepth() {
        return bitDepth;
    }

    /**
     * Returns the RGB palette for indexed images.
     *
     * @return palette bytes packed as {@code [R, G, B, R, G, B, ...]}, or {@code null} for non-indexed formats
     */
    public byte[] getPalette() {
        return palette;
    }

    /**
     * Returns the transparency data for indexed images.
     *
     * @return per-entry palette alpha values, or {@code null} if not present
     */
    public byte[] getTransparency() {
        return transparency;
    }

    /**
     * Returns the number of frames in the animation.
     *
     * @return frame count; 1 for a still image
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * Returns the number of times the animation loops.
     *
     * @return play count; 0 means loop indefinitely
     */
    public int getPlayCount() {
        return playCount;
    }

    /**
     * Returns the number of bytes needed to store one channel sample.
     *
     * @return bytes per channel
     */
    public int getBytesPerChannel() {
        return (bitDepth + 7) / 8;
    }

    /**
     * Returns the number of bytes needed to store one pixel.
     *
     * @return bytes per pixel
     */
    public int getBytesPerPixel() {
        return (colorType.samples() * bitDepth + 7) / 8;
    }

    /**
     * Returns the number of bytes needed to store one unfiltered row of pixels.
     *
     * @return bytes per row
     */
    public int getBytesPerRow() {
        return (width * colorType.samples() * bitDepth + 7) / 8;
    }

    /**
     * Returns the total number of bytes needed to store the full image.
     *
     * @return bytes per image ({@code getBytesPerRow() * height})
     */
    public int getBytesPerImage() {
        return getBytesPerRow() * height;
    }

    /**
     * Returns a copy of this format converted to an indexed color image with the given palette.
     *
     * @param bitDepth     bits per pixel; valid values are 1, 2, 4, or 8
     * @param palette      RGB palette entries packed as {@code [R, G, B, R, G, B, ...]}
     * @param transparency per-entry alpha values for the palette, or {@code null} if fully opaque
     * @return a new {@code PngFormat} with {@link PngColorType#INDEXED} and the given palette
     */
    public PngFormat withPalette(int bitDepth, byte[] palette, byte[] transparency) {
        return new PngFormat(width, height, PngColorType.INDEXED, bitDepth, palette, transparency, frameCount, playCount);
    }

    /**
     * Returns a copy of this format configured as an animation.
     *
     * @param frameCount number of frames, must be greater than 0
     * @param playCount  number of times to loop; 0 means loop indefinitely
     * @return a new {@code PngFormat} with the given animation parameters
     */
    public PngFormat withAnimation(int frameCount, int playCount) {
        return new PngFormat(width, height, colorType, bitDepth, palette, transparency, frameCount, playCount);
    }

    PngFormat withBitDepth(int bitDepth) {
        return new PngFormat(width, height, colorType, bitDepth, palette, transparency, frameCount, playCount);
    }

    PngFormat withColorType(PngColorType colorType) {
        return new PngFormat(width, height, colorType, bitDepth, palette, transparency, frameCount, playCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PngFormat)) {
            return false;
        }

        PngFormat other = (PngFormat) obj;
        return width == other.width
            && height == other.height
            && colorType == other.colorType
            && bitDepth == other.bitDepth
            && Arrays.equals(palette, other.palette)
            && Arrays.equals(transparency, other.transparency)
            && frameCount == other.frameCount
            && playCount == other.playCount;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Integer.hashCode(width);
        result = 31 * result + Integer.hashCode(height);
        result = 31 * result + Objects.hashCode(colorType);
        result = 31 * result + Integer.hashCode(bitDepth);
        result = 31 * result + Arrays.hashCode(palette);
        result = 31 * result + Arrays.hashCode(transparency);
        result = 31 * result + Integer.hashCode(frameCount);
        result = 31 * result + Integer.hashCode(playCount);
        return result;
    }

    @Override
    public String toString() {
        int paletteLength = palette != null ? palette.length : 0;
        int transparencyLength = transparency != null ? transparency.length : 0;

        return "PngFormat(" +
            "width=" + width + ", " +
            "height=" + height + ", " +
            "colorType=" + colorType + ", " +
            "bitDepth=" + bitDepth + ", " +
            "palette=[" + paletteLength + " entries], " +
            "transparency=[" + transparencyLength + " entries], " +
            "frameCount=" + frameCount + ", " +
            "playCount=" + playCount + ")";
    }
}
