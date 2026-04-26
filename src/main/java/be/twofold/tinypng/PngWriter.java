package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;

public final class PngWriter implements Closeable {
    private static final byte[] EMPTY = new byte[0];

    private final PngFormat format;
    private final PngConfig config;
    private final ChunkWriter writer;
    private boolean finished;

    public PngWriter(OutputStream output, PngFormat format) throws IOException {
        this(output, format, PngConfig.of());
    }

    public PngWriter(OutputStream output, PngFormat format, PngConfig config) throws IOException {
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.writer = new ChunkWriter(output);
    }

    /**
     * Encodes a single image, optimizing the format first if enabled in the config.
     *
     * @param image raw pixel data matching the format passed to the constructor
     */
    public void writeImage(byte[] image) throws IOException {
        if (image.length != format.getBytesPerImage()) {
            throw new IllegalArgumentException("image has wrong size, expected " + format.getBytesPerImage() + " but was " + image.length);
        }
        if (finished) {
            throw new IllegalStateException("Writer is already finished.");
        }

        PngFormat format = this.format;
        if (config.isOptimize()) {
            Optimizer optimizer = new Optimizer(format);
            optimizer.optimize(image);
            format = optimizer.format();
        }

        writeHeader(format);
        writeImageData(image, format);
        finished = true;
    }

    private void writeHeader(PngFormat format) throws IOException {
        writeIHDR(format);
        writePLTE(format);
        writeTRNS(format);
    }

    private void writeIHDR(PngFormat format) throws IOException {
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

    private void writePLTE(PngFormat format) throws IOException {
        byte[] palette = format.getPalette();
        if (palette != null) {
            writer.writeChunk(ChunkWriter.PLTE, palette);
        }
    }

    private void writeTRNS(PngFormat format) throws IOException {
        byte[] transparency = format.getTransparency();
        if (transparency != null) {
            writer.writeChunk(ChunkWriter.tRNS, transparency);
        }
    }

    private void writeImageData(byte[] image, PngFormat format) throws IOException {
        PngFilter filterMethod = format.getColorType() == PngColorType.INDEXED
            ? PngFilter.NONE
            : config.getFilterMethod();

        Filter filter = new Filter(format, filterMethod);
        try (IDATWriter idatWriter = new IDATWriter(writer, config.getCompressionLevel())) {
            for (int row = 0; row < format.getHeight(); row++) {
                PngFilter applied = filter.filter(image, row * format.getBytesPerRow());
                idatWriter.write((byte) applied.ordinal());
                idatWriter.write(filter.bestRow(applied), format.getBytesPerPixel(), format.getBytesPerRow());
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (finished) {
            writer.writeChunk(ChunkWriter.IEND, EMPTY);
        }
        writer.close();
    }
}
