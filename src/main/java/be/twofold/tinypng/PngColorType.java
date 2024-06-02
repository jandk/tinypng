package be.twofold.tinypng;

public enum PngColorType {
    Gray(0, 1),
    Rgb(2, 3),
    Indexed(3, 1),
    GrayAlpha(4, 2),
    RgbAlpha(6, 4);

    private final int value;
    private final int samples;

    PngColorType(int value, int samples) {
        this.value = value;
        this.samples = samples;
    }

    public int code() {
        return value;
    }

    public int samples() {
        return samples;
    }
}
