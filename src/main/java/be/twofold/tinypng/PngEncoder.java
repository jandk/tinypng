package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * This might be a dumb idea, because Java already has a {@link javax.imageio.ImageIO} class.
 * But there's a bug in the PNG writer that causes it to use the wrong filter type.
 * <p>
 * So here we are. Good thing it's not that hard to write a PNG file.
 */
public final class PngEncoder implements AutoCloseable {
    private final PngFormat format;
    private final ChunkWriter writer;

    public PngEncoder(OutputStream output, PngFormat format) {
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.writer = new ChunkWriter(output);
    }

    public PngWriter writeHeader() {
        writeIHDR();
        writePLTE();

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
            writeIEND();
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
