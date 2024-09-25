package be.twofold.tinypng;

import java.util.*;

public final class PngFormat {
    private final int width;
    private final int height;
    private final ColorType colorType;
    private final BitDepth bitDepth;
    private final PngPalette palette;

    private PngFormat(int width, int height, BitDepth bitDepth, ColorType colorType, PngPalette palette) {
        if (width <= 0) {
            throw new PngException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new PngException("height must be greater than 0");
        }
        Objects.requireNonNull(bitDepth, "bitDepth must not be null");
        Objects.requireNonNull(colorType, "colorType must not be null");
        if ((bitDepth == BitDepth.ONE || bitDepth == BitDepth.TWO || bitDepth == BitDepth.FOUR)
            && (colorType == ColorType.TRUECOLOR || colorType == ColorType.GRAYSCALE_ALPHA || colorType == ColorType.TRUECOLOR_ALPHA)
            || (bitDepth == BitDepth.SIXTEEN && colorType == ColorType.INDEXED)
        ) {
            throw new PngException("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
        if (colorType == ColorType.INDEXED && palette == null) {
            throw new PngException("palette must not be null for indexed color type");
        }

        this.width = width;
        this.height = height;
        this.bitDepth = bitDepth;
        this.colorType = colorType;
        this.palette = palette;
    }

    public static PngFormat of(int width, int height, BitDepth bitDepth, ColorType colorType) {
        return new PngFormat(width, height, bitDepth, colorType, null);
    }

    public static PngFormat indexed(int width, int height, BitDepth bitDepth, PngPalette palette) {
        return new PngFormat(width, height, BitDepth.EIGHT, ColorType.INDEXED, palette);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public ColorType colorType() {
        return colorType;
    }

    public BitDepth bitDepth() {
        return bitDepth;
    }

    public Optional<PngPalette> palette() {
        return Optional.ofNullable(palette);
    }

    public int bytesPerPixel() {
        return colorType.samples() * ((bitDepth.value() + 7) >> 3);
    }

    public int bytesPerRow() {
        int samples = width * colorType.samples();
        return ((samples * bitDepth.value()) + 7) >>> 3;
    }

    public int bytesPerImage() {
        return bytesPerRow() * height;
    }

    @Override
    public String toString() {
        return "PngFormat(" +
            "width=" + width + ", " +
            "height=" + height + ", " +
            "colorType=" + colorType + ", " +
            "bitDepth=" + bitDepth + ", " +
            "palette=" + palette +
            ")";
    }
}
