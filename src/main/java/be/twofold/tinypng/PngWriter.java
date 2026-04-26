package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public final class PngWriter implements Closeable {
    private static final byte[] MAGIC = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] EMPTY = new byte[0];

    private static final int IHDR = 'I' << 24 | 'H' << 16 | 'D' << 8 | 'R';
    private static final int PLTE = 'P' << 24 | 'L' << 16 | 'T' << 8 | 'E';
    private static final int tRNS = 't' << 24 | 'R' << 16 | 'N' << 8 | 'S';
    private static final int IDAT = 'I' << 24 | 'D' << 16 | 'A' << 8 | 'T';
    private static final int IEND = 'I' << 24 | 'E' << 16 | 'N' << 8 | 'D';

    private final PngFormat format;
    private final PngConfig config;
    private boolean finished;

    // IDAT
    private final byte[] idatBuffer = new byte[32 * 1024];
    private final byte[] singleByte = new byte[1];
    private final Deflater deflater;
    private int idatLength = 0;

    // Chunks
    private final CRC32 crc = new CRC32();
    private final OutputStream output;

    public PngWriter(OutputStream output, PngFormat format) throws IOException {
        this(output, format, PngConfig.of());
    }

    public PngWriter(OutputStream output, PngFormat format, PngConfig config) throws IOException {
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.deflater = new Deflater(config.getCompressionLevel());
        output.write(MAGIC);
    }

    /**
     * Encodes a single image, optimizing the format first if enabled in the config.
     *
     * @param image raw pixel data matching the format passed to the constructor
     */
    public void writeImage(byte[] image) throws IOException {
        if (finished) {
            throw new IllegalStateException("Writer is already finished.");
        }
        if (image.length != format.getBytesPerImage()) {
            throw new IllegalArgumentException("image has wrong size, expected " + format.getBytesPerImage() + " but was " + image.length);
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
        writeChunk(IHDR, chunk);
    }

    private void writePLTE(PngFormat format) throws IOException {
        byte[] palette = format.getPalette();
        if (palette != null) {
            writeChunk(PLTE, palette);
        }
    }

    private void writeTRNS(PngFormat format) throws IOException {
        byte[] transparency = format.getTransparency();
        if (transparency != null) {
            writeChunk(tRNS, transparency);
        }
    }

    private void writeImageData(byte[] image, PngFormat format) throws IOException {
        PngFilter filterMethod = format.getColorType() == PngColorType.INDEXED
            ? PngFilter.NONE
            : config.getFilterMethod();

        Filter filter = new Filter(format, filterMethod);
        for (int row = 0; row < format.getHeight(); row++) {
            PngFilter applied = filter.filter(image, row * format.getBytesPerRow());
            idatWrite((byte) applied.ordinal());
            idatWrite(filter.bestRow(applied), format.getBytesPerPixel(), format.getBytesPerRow());
        }
        idatFinish();
    }

    // region IDAT

    private void idatWrite(byte value) throws IOException {
        singleByte[0] = value;
        idatWrite(singleByte, 0, 1);
    }

    private void idatWrite(byte[] bytes, int offset, int length) throws IOException {
        deflater.setInput(bytes, offset, length);
        while (!deflater.needsInput()) {
            deflate();
        }
    }

    private void idatFinish() throws IOException {
        try {
            deflater.finish();
            while (!deflater.finished()) {
                deflate();
            }
            writeIDAT();
        } finally {
            deflater.end();
        }
    }

    private void deflate() throws IOException {
        int len = deflater.deflate(idatBuffer, idatLength, idatBuffer.length - idatLength);
        if (len > 0) {
            idatLength += len;
            if (idatLength == idatBuffer.length) {
                writeIDAT();
            }
        }
    }

    private void writeIDAT() throws IOException {
        if (idatLength == 0) {
            return;
        }
        writeChunk(IDAT, idatBuffer, idatLength);
        idatLength = 0;
    }

    // endregion

    // region Chunks

    private void writeChunk(int type, byte[] data) throws IOException {
        writeChunk(type, data, data.length);
    }

    private void writeChunk(int type, byte[] data, int length) throws IOException {
        crc.reset();
        crc.update(type >> 24);
        crc.update(type >> 16);
        crc.update(type >> 8);
        crc.update(type);
        crc.update(data, 0, length);

        writeBytesBE(length);
        writeBytesBE(type);
        output.write(data, 0, length);
        writeBytesBE((int) crc.getValue());
    }

    private void writeBytesBE(int value) throws IOException {
        output.write(value >> 24);
        output.write(value >> 16);
        output.write(value >> 8);
        output.write(value);
    }

    // endregion

    @Override
    public void close() throws IOException {
        if (finished) {
            writeChunk(IEND, EMPTY);
        }
        output.close();
    }
}
