package be.twofold.tinypng;

public enum ChunkType {
    IHDR(0x49484452),
    PLTE(0x504c5445),
    IDAT(0x49444154),
    IEND(0x49454e44);

    private final int type;

    ChunkType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
