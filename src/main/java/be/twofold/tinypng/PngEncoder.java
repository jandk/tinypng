package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;

public final class PngEncoder implements Closeable {
    private static final byte[] EMPTY = new byte[0];

    private final PngFormat format;
    private final ChunkWriter writer;
    private boolean headerWritten;

    public PngEncoder(OutputStream output, PngFormat format) throws IOException {
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.writer = new ChunkWriter(output);
    }

    public PngWriter writeHeader() throws IOException {
        if (headerWritten) {
            throw new IllegalStateException("Header already written");
        }

        writeIHDR();
        writePLTE();
        writeTRNS();
        headerWritten = true;

        return new PngWriter(format, writer);
    }

    private void writeIHDR() throws IOException {
        byte[] chunk = ByteBuffer.allocate(13)
            .putInt(format.getWidth())
            .putInt(format.getHeight())
            .put((byte) format.getBitDepth())
            .put(format.getColorType().value())
            .put((byte) 0)
            .put((byte) 0)
            .put((byte) 0)
            .array();
        writer.writeChunk(ChunkWriter.IHDR, chunk);
    }

    private void writePLTE() throws IOException {
        byte[] palette = format.getPalette();
        if (palette != null) {
            writer.writeChunk(ChunkWriter.PLTE, palette);
        }
    }

    private void writeTRNS() throws IOException {
        byte[] transparency = format.getTransparency();
        if (transparency != null) {
            writer.writeChunk(ChunkWriter.tRNS, transparency);
        }
    }

    private void writeIEND() throws IOException {
        writer.writeChunk(ChunkWriter.IEND, EMPTY);
    }

    @Override
    public void close() throws IOException {
        if (headerWritten) {
            writeIEND();
        }
        writer.close();
    }
}
