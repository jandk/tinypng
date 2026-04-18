package be.twofold.tinypng;

import java.io.*;
import java.util.*;
import java.util.zip.*;

final class ChunkWriter implements AutoCloseable {
    private static final byte[] MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    static final int IHDR = 'I' << 24 | 'H' << 16 | 'D' << 8 | 'R';
    static final int PLTE = 'P' << 24 | 'L' << 16 | 'T' << 8 | 'E';
    static final int IDAT = 'I' << 24 | 'D' << 16 | 'A' << 8 | 'T';
    static final int IEND = 'I' << 24 | 'E' << 16 | 'N' << 8 | 'D';

    private final CRC32 crc = new CRC32();
    private final OutputStream output;

    ChunkWriter(OutputStream output) {
        this.output = Objects.requireNonNull(output);
        try {
            output.write(MAGIC);
        } catch (IOException e) {
            throw new PngException("Failed to write magic", e);
        }
    }

    void writeChunk(int type, byte[] data) {
        writeChunk(type, data, data.length);
    }

    void writeChunk(int type, byte[] data, int length) {
        crc.reset();
        crc.update(type >> 24);
        crc.update(type >> 16);
        crc.update(type >> 8);
        crc.update(type);
        crc.update(data, 0, length);

        try {
            writeBytesBE(length);
            writeBytesBE(type);
            output.write(data, 0, length);
            writeBytesBE((int) crc.getValue());
        } catch (IOException e) {
            throw new PngException("Failed to write chunk", e);
        }
    }

    private void writeBytesBE(int value) throws IOException {
        output.write(value >> 24);
        output.write(value >> 16);
        output.write(value >> 8);
        output.write(value);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
