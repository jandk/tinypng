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
    private final ColorType colorType;
    private final int bitDepth;
    private final byte[] palette;
    private final byte[] transparency;

    private PngFormat(int width, int height, ColorType colorType, int bitDepth, byte[] palette, byte[] transparency) {
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than 0");
        }
        Objects.requireNonNull(colorType, "colorType must not be null");
        validate(colorType, bitDepth);
        if (colorType == ColorType.INDEXED) {
            if (palette == null) {
                throw new IllegalArgumentException("palette must not be null for colorType indexed");
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
        } else {
            if (palette != null || transparency != null) {
                throw new IllegalArgumentException("palette or transparency not supported for non-indexed colors");
            }
        }

        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.colorType = colorType;
        this.palette = palette != null ? palette.clone() : null;
        this.transparency = transparency != null ? transparency.clone() : null;
    }

    private static void validate(ColorType colorType, int bitDepth) {
        if (bitDepth == 8) {
            return;
        }
        if (colorType != ColorType.INDEXED && bitDepth == 16) {
            return;
        }
        if ((colorType == ColorType.GRAY || colorType == ColorType.INDEXED)
            && (bitDepth == 1 || bitDepth == 2 || bitDepth == 4)) {
            return;
        }
        throw new IllegalArgumentException("Invalid bit depth " + bitDepth + " for color type " + colorType);
    }

    /**
     * Creates a format for non-indexed color types.
     *
     * @param width     image width in pixels, must be positive
     * @param height    image height in pixels, must be positive
     * @param colorType the color model; must not be {@link ColorType#INDEXED}
     * @param bitDepth  bits per sample: 1, 2, 4, 8, or 16 (not all combinations are valid)
     * @return The newly created format.
     * @throws IllegalArgumentException if any parameter is invalid or the colorType/bitDepth combination is not allowed by the PNG spec
     * @throws NullPointerException     if {@code colorType} is null
     */
    public static PngFormat of(int width, int height, ColorType colorType, int bitDepth) {
        if (colorType == ColorType.INDEXED) {
            throw new IllegalArgumentException("use PngFormat.indexed() for indexed images");
        }
        return new PngFormat(width, height, colorType, bitDepth, null, null);
    }

    /**
     * Creates a format for palette-based (indexed) images.
     *
     * @param width        image width in pixels, must be positive
     * @param height       image height in pixels, must be positive
     * @param bitDepth     bits per palette index: 1, 2, 4, or 8
     * @param palette      flat RGB palette as consecutive R, G, B bytes
     * @param transparency transparency values for the palette entries
     * @return The newly created format.
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static PngFormat indexed(int width, int height, int bitDepth, byte[] palette, byte[] transparency) {
        return new PngFormat(width, height, ColorType.INDEXED, bitDepth, palette, transparency);
    }

    /**
     * Returns the image width in pixels.
     *
     * @return image width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the image height in pixels.
     *
     * @return image height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the color type.
     *
     * @return the color type
     */
    public ColorType getColorType() {
        return colorType;
    }

    /**
     * Returns the bit depth (bits per sample or palette index).
     *
     * @return bits per sample or palette index
     */
    public int getBitDepth() {
        return bitDepth;
    }

    /**
     * Returns the number of bytes in one encoded row, including sub-byte packing.
     *
     * @return bytes per encoded row
     */
    public int getBytesPerRow() {
        return (width * colorType.samples() * bitDepth + 7) / 8;
    }

    /**
     * Returns the total number of bytes for the full image ({@code getBytesPerRow() * height}).
     *
     * @return total bytes for the full image
     */
    public int getBytesPerImage() {
        return getBytesPerRow() * height;
    }

    byte[] getPalette() {
        return palette;
    }

    byte[] getTransparency() {
        return transparency;
    }

    int getBytesPerPixel() {
        return colorType.samples() * getBytesPerChannel();
    }

    int getBytesPerChannel() {
        return (bitDepth + 7) / 8;
    }

    PngFormat withColorType(ColorType colorType) {
        return new PngFormat(width, height, colorType, bitDepth, colorType == ColorType.INDEXED ? palette : null, null);
    }

    PngFormat withBitDepth(int bitDepth) {
        return new PngFormat(width, height, colorType, bitDepth, palette, transparency);
    }

    PngFormat withPalette(byte[] palette) {
        return new PngFormat(width, height, ColorType.INDEXED, 8, palette, null);
    }

    PngFormat withTransparency(byte[] transparency) {
        return new PngFormat(width, height, colorType, bitDepth, palette, transparency);
    }

    @Override
    public String toString() {
        String palette = this.palette != null
            ? "[" + this.palette.length / 3 + " entries]"
            : "null";

        return "PngFormat(" +
            "width=" + width + ", " +
            "height=" + height + ", " +
            "colorType=" + colorType + ", " +
            "bitDepth=" + bitDepth + ", " +
            "palette=" + palette + ", " +
            "transparency=" + (this.transparency != null ? "[" + this.transparency.length + " bytes]" : "null") +
            ")";
    }
}
