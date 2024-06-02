package be.twofold.tinypng;

import java.util.*;

public final class PngFormat {
    private final int width;
    private final int height;
    private final PngColorType colorType;
    private final PngBitDepth bitDepth;
    private final PngPalette palette;

    private PngFormat(int width, int height, PngColorType colorType, PngBitDepth bitDepth, PngPalette palette) {
        Objects.requireNonNull(bitDepth, "bitDepth must not be null");
        Objects.requireNonNull(colorType, "colorType must not be null");
        if (width <= 0) {
            throw new PngException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new PngException("height must be greater than 0");
        }
        if ((bitDepth == PngBitDepth.One || bitDepth == PngBitDepth.Two || bitDepth == PngBitDepth.Four)
            && (colorType == PngColorType.Rgb || colorType == PngColorType.GrayAlpha || colorType == PngColorType.RgbAlpha)
            || (bitDepth == PngBitDepth.Sixteen && colorType == PngColorType.Indexed)
        ) {
            throw new PngException("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
        if (colorType == PngColorType.Indexed && palette == null) {
            throw new PngException("palette must not be null for indexed color type");
        }

        this.width = width;
        this.height = height;
        this.colorType = colorType;
        this.bitDepth = bitDepth;
        this.palette = palette;
    }

    public static PngFormat of(int width, int height, PngColorType colorType, PngBitDepth bitDepth) {
        return new PngFormat(width, height, colorType, bitDepth, null);
    }

    public static PngFormat indexed(int width, int height, PngPalette palette) {
        return new PngFormat(width, height, PngColorType.Indexed, PngBitDepth.Eight, palette);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public PngColorType colorType() {
        return colorType;
    }

    public PngBitDepth bitDepth() {
        return bitDepth;
    }

    public PngPalette palette() {
        return palette;
    }

    public int bytesPerPixel() {
        return colorType.samples() * ((bitDepth.value() + 7) >>> 3);
    }

    public int bytesPerRow() {
        int samples = width * colorType.samples();
        switch (bitDepth) {
            case Eight:
                return samples;
            case Sixteen:
                return samples * 2;
            default:
                return (samples * bitDepth.value()) + 7 >>> 3;
        }
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
