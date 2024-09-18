package be.twofold.tinypng;

import java.nio.charset.*;

enum ChunkType {
    IHDR,
    PLTE,
    IDAT,
    IEND;

    public byte[] bytes() {
        return name().getBytes(StandardCharsets.US_ASCII);
    }
}
