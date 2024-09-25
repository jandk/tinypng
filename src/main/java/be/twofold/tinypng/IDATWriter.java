package be.twofold.tinypng;

import java.util.*;
import java.util.zip.*;

final class IDATWriter implements AutoCloseable {
    private final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
    private final byte[] buffer = new byte[32 * 1024];
    private final byte[] single = new byte[1];
    private int length = 0;

    private final ChunkWriter chunkWriter;

    IDATWriter(ChunkWriter chunkWriter) {
        this.chunkWriter = Objects.requireNonNull(chunkWriter);
    }

    void write(byte value) {
        single[0] = value;
        write(single, 0, 1);
    }

    void write(byte[] bytes, int offset, int length) {
        deflater.setInput(bytes, offset, length);
        while (!deflater.needsInput()) {
            deflate();
        }
    }

    private void deflate() {
        int len = deflater.deflate(buffer, length, buffer.length - length);
        if (len > 0) {
            length += len;
            if (length == buffer.length) {
                writeIDAT();
            }
        }
    }

    private void writeIDAT() {
        chunkWriter.writeChunk(ChunkType.IDAT, buffer, length);
        length = 0;
    }

    @Override
    public void close() {
        deflater.finish();
        while (!deflater.finished()) {
            deflate();
        }
        deflater.end();

        writeIDAT();
    }
}
