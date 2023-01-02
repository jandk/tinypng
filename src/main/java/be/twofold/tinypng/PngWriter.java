package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;

public final class PngWriter implements AutoCloseable {
    private static final byte[] Magic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final OutputStream output;
    private final PngFormat format;
    private final PngFilter filter;

    public PngWriter(OutputStream output, PngFormat format) {
        this.output = Objects.requireNonNull(output, "output is null");
        this.format = Objects.requireNonNull(format, "format is null");
        this.filter = new PngFilter(format);
    }

    public void write(byte[] image) throws IOException {
        output.write(Magic);
        writeChunk(ChunkType.IHDR, createIHDR());
        writeIdat(image);
        writeChunk(ChunkType.IEND, new byte[0]);
    }

    private byte[] createIHDR() {
        return ByteBuffer.allocate(13)
            .putInt(format.getWidth())
            .putInt(format.getHeight())
            .put((byte) format.getBitDepth())
            .put((byte) format.getColorType().getCode())
            .put((byte) 0)
            .put((byte) 0)
            .put((byte) 0)
            .array();
    }

    private void writeIdat(byte[] data) throws IOException {
        try (OutputStream ios = new IdatOutputStream(this)) {
            for (int y = 0; y < format.getHeight(); y++) {
                int filterMethod = filter.filter(data, y * format.getBytesPerRow());
                ios.write(filterMethod);
                ios.write(filter.getFiltered(filterMethod), format.getBytesPerPixel(), format.getBytesPerRow());
            }
        }
    }

    void writeChunk(ChunkType type, byte[] data) throws IOException {
        new Chunk(type, data).write(output);
    }

    void writeChunk(ChunkType type, byte[] data, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        output.write(buffer.array(), buffer.position(), buffer.limit() - buffer.position());
        new Chunk(type, Arrays.copyOf(data, length)).write(output);
    }

    @Override
    public void close() {
        try {
            output.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
