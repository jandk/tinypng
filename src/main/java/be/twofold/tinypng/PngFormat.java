package be.twofold.tinypng;

import java.util.*;

public final class PngFormat {
    private final int width;
    private final int height;
    private final PngColorType colorType;
    private final int bitDepth;
    private final boolean linear;

    public PngFormat(int width, int height, PngColorType colorType) {
        this(width, height, colorType, 8);
    }

    public PngFormat(int width, int height, PngColorType colorType, int bitDepth) {
        this(width, height, colorType, bitDepth, false);
    }

    public PngFormat(int width, int height, PngColorType colorType, int bitDepth, boolean linear) {
        Objects.requireNonNull(colorType, "colorType must not be null");
        if (width <= 0) {
            throw new IllegalArgumentException("width must be greater than 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("height must be greater than 0");
        }
        if (bitDepth != 8 && bitDepth != 16) {
            throw new IllegalArgumentException("bitDepth must be 8 or 16");
        }

        this.width = width;
        this.height = height;
        this.colorType = colorType;
        this.bitDepth = bitDepth;
        this.linear = linear;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public PngColorType getColorType() {
        return colorType;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public boolean isLinear() {
        return linear;
    }

    public int getBytesPerPixel() {
        return colorType.channels() * (bitDepth == 8 ? 1 : 2);
    }

    public int getBytesPerRow() {
        return getBytesPerPixel() * width;
    }

    public int getBytesPerImage() {
        return getBytesPerRow() * height;
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
        result = 31 * result + Integer.hashCode(bitDepth);
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
