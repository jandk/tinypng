package be.twofold.tinypng;

import java.util.*;

public final class PngFormat {
    private final int width;
    private final int height;
    private final PngColorType colorType;
    private final PngBitDepth bitDepth;
    private final boolean linear;

    public PngFormat(int width, int height, PngColorType colorType) {
        this(width, height, colorType, PngBitDepth.Eight);
    }

    public PngFormat(int width, int height, PngColorType colorType, PngBitDepth bitDepth) {
        this(width, height, colorType, bitDepth, false);
    }

    public PngFormat(int width, int height, PngColorType colorType, PngBitDepth bitDepth, boolean linear) {
        if (width <= 0) {
            throw new PngException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new PngException("height must be greater than 0");
        }
        Objects.requireNonNull(colorType, "colorType must not be null");
        Objects.requireNonNull(bitDepth, "bitDepth must not be null");
        checkDepthColor(bitDepth, colorType);

        this.width = width;
        this.height = height;
        this.colorType = colorType;
        this.bitDepth = bitDepth;
        this.linear = linear;
    }

    private void checkDepthColor(PngBitDepth bitDepth, PngColorType colorType) {
        if ((bitDepth == PngBitDepth.One || bitDepth == PngBitDepth.Two || bitDepth == PngBitDepth.Four)
            && (colorType == PngColorType.Rgb || colorType == PngColorType.GrayAlpha || colorType == PngColorType.RgbAlpha)
            || (bitDepth == PngBitDepth.Sixteen && colorType == PngColorType.Indexed)
        ) {
            throw new PngException("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
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

    public boolean linear() {
        return linear;
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
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PngFormat)) return false;

        PngFormat other = (PngFormat) obj;
        return width == other.width
            && height == other.height
            && colorType.equals(other.colorType)
            && bitDepth == other.bitDepth
            && linear == other.linear;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Integer.hashCode(width);
        result = 31 * result + Integer.hashCode(height);
        result = 31 * result + colorType.hashCode();
        result = 31 * result + bitDepth.hashCode();
        result = 31 * result + Boolean.hashCode(linear);
        return result;
    }

    @Override
    public String toString() {
        return "PngFormat(" +
            "width=" + width + ", " +
            "height=" + height + ", " +
            "colorType=" + colorType + ", " +
            "bitDepth=" + bitDepth + ", " +
            "linear=" + linear +
            ")";
    }
}
