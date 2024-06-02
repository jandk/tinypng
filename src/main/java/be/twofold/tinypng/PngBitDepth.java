package be.twofold.tinypng;

public enum PngBitDepth {
    One(1),
    Two(2),
    Four(4),
    Eight(8),
    Sixteen(16);

    private final int value;

    PngBitDepth(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
