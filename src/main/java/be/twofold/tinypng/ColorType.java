package be.twofold.tinypng;

public enum ColorType {
    Gray(0, 1),
    Rgb(2, 3),
    // Palette(3, 1), // Not supported for now
    GrayAlpha(4, 2),
    RgbAlpha(6, 4);

    private final int code;
    private final int channels;

    ColorType(int code, int channels) {
        this.code = code;
        this.channels = channels;
    }

    public int getCode() {
        return code;
    }

    public int getChannels() {
        return channels;
    }
}
