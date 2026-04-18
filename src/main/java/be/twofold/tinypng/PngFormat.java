package be.twofold.tinypng;

import java.util.*;

public final class PngFormat {
    private final int width;
    private final int height;
    private final int bitDepth;
    private final ColorType colorType;
    private final byte[] palette;

    private PngFormat(int width, int height, ColorType colorType, int bitDepth, byte[] palette) {
        if (width <= 0) {
            throw new PngException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new PngException("height must be greater than 0");
        }
        Objects.requireNonNull(colorType, "colorType must not be null");
        if (bitDepth != 1 && bitDepth != 2 && bitDepth != 4 && bitDepth != 8 && bitDepth != 16) {
            throw new PngException("Invalid bit depth: " + bitDepth);
        }
        if ((bitDepth == 1 || bitDepth == 2 || bitDepth == 4)
            && (colorType == ColorType.RGB || colorType == ColorType.GRAY_ALPHA || colorType == ColorType.RGB_ALPHA)
            || (bitDepth == 16 && colorType == ColorType.INDEXED)) {
            throw new PngException("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
        if (colorType == ColorType.INDEXED) {
            if (palette == null) {
                throw new PngException("palette must not be null for colorType indexed");
            }
            if (palette.length == 0 || palette.length % 3 != 0) {
                throw new PngException("palette length must be a non-zero multiple of 3");
            }
            if (palette.length > 256 * 3) {
                throw new PngException("palette cannot contain more than 256 colors");
            }
        }

        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.colorType = colorType;
        this.palette = palette != null ? palette.clone() : null;
    }

    public static PngFormat of(int width, int height, ColorType colorType, int bitDepth) {
        return new PngFormat(width, height, colorType, bitDepth, null);
    }

    public static PngFormat indexed(int width, int height, int bitDepth, byte[] palette) {
        return new PngFormat(width, height, ColorType.INDEXED, bitDepth, palette);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int bitDepth() {
        return bitDepth;
    }

    public ColorType colorType() {
        return colorType;
    }

    public Optional<byte[]> palette() {
        return Optional.ofNullable(palette).map(byte[]::clone);
    }

    public int bytesPerChannel() {
        return (bitDepth + 7) / 8;
    }

    public int bytesPerPixel() {
        return colorType.samples() * bytesPerChannel();
    }

    public int bytesPerRow() {
        return (width * colorType.samples() * bitDepth + 7) / 8;
    }

    public int bytesPerImage() {
        return bytesPerRow() * height;
    }

    PngFormat withBitDepth(int bitDepth) {
        return new PngFormat(width, height, colorType, bitDepth, palette);
    }

    PngFormat withColorType(ColorType colorType) {
        return new PngFormat(width, height, colorType, bitDepth, palette);
    }

    @Override
    public String toString() {
        return "PngFormat(" +
            "width=" + width + ", " +
            "height=" + height + ", " +
            "colorType=" + colorType + ", " +
            "bitDepth=" + bitDepth + ", " +
            "palette=" + Arrays.toString(palette) +
            ")";
    }
}
