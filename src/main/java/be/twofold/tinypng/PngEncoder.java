package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Encodes PNG image data and writes it to an OutputStream.
 * This class is responsible for writing the PNG signature, header, optional palette data,
 * and closing the file with terminating data.
 * <p>
 * Instances of this class allow the creation of a PNG file by taking
 * a {@link PngFormat} configuration and handling the required chunks.
 * <p>
 * The usage involves writing the header first, followed by image data through a {@link PngWriter}.
 * The PNG stream is properly terminated upon calling {@link #close()}.
 * <p>
 * This class is not thread-safe and should be used in a single-threaded context.
 * <p>
 * Implements {@link AutoCloseable} to ensure proper resource management.
 */
public final class PngEncoder implements AutoCloseable {
    private final PngFormat format;
    private final ChunkWriter writer;
    private boolean headerWritten;

    public PngEncoder(OutputStream output, PngFormat format) {
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.writer = new ChunkWriter(output);
    }

    public PngWriter writeHeader() {
        if (headerWritten) {
            throw new IllegalStateException("Header already written");
        }

        writeIHDR();
        writePLTE();
        this.headerWritten = true;

        return new PngWriter(format, writer);
    }

    private void writeIHDR() {
        byte[] chunk = ByteBuffer.allocate(13)
            .putInt(format.width())
            .putInt(format.height())
            .put(format.bitDepth().value())
            .put(format.colorType().value())
            .put((byte) 0)
            .put((byte) 0)
            .put((byte) 0)
            .array();
        writer.writeChunk(ChunkType.IHDR, chunk);
    }

    @SuppressWarnings("PointlessArithmeticExpression")
    private void writePLTE() {
        if (format.palette().isEmpty()) {
            return;
        }

        var palette = format.palette().get();
        byte[] data = new byte[palette.size() * 3];
        for (int i = 0; i < palette.size(); i++) {
            PngPalette.Color color = palette.get(i);
            data[i * 3 + 0] = color.red();
            data[i * 3 + 1] = color.green();
            data[i * 3 + 2] = color.blue();
        }
        writer.writeChunk(ChunkType.PLTE, data);
    }

    private void writeIEND() {
        writer.writeChunk(ChunkType.IEND, new byte[0]);
    }

    @Override
    public void close() {
        try {
            if (headerWritten) {
                writeIEND();
            }
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
