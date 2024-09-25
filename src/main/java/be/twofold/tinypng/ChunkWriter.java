package be.twofold.tinypng;

import java.io.*;
import java.util.*;
import java.util.zip.*;

final class ChunkWriter implements AutoCloseable {
    private static final byte[] Magic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final CRC32 crc = new CRC32();
    private final OutputStream output;

    ChunkWriter(OutputStream output) {
        this.output = Objects.requireNonNull(output);
        try {
            output.write(Magic);
        } catch (IOException e) {
            throw new PngException("Failed to write magic", e);
        }
    }

    void writeChunk(ChunkType type, byte[] data) {
        writeChunk(type, data, data.length);
    }

    void writeChunk(ChunkType type, byte[] data, int length) {
        crc.reset();
        crc.update(type.bytes(), 0, 4);
        crc.update(data, 0, length);

        try {
            output.write(toBytesBE(length));
            output.write(type.bytes());
            output.write(data, 0, length);
            output.write(toBytesBE((int) crc.getValue()));
        } catch (IOException e) {
            throw new PngException("Failed to write chunk", e);
        }
    }

    private byte[] toBytesBE(int value) {
        return new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
