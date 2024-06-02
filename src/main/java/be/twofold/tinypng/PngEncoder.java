package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

/**
 * This might be a dumb idea, because Java already has a {@link javax.imageio.ImageIO} class.
 * But there's a bug in the PNG writer that causes it to use the wrong filter type.
 * <p>
 * So here we are. Good thing it's not that hard to write a PNG file.
 */
public final class PngEncoder implements AutoCloseable {
    private static final byte[] Magic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final int IHDR = 0x49484452;
    private static final int PLTE = 0x504c5445;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454e44;

    private final OutputStream output;
    private final PngFormat format;
    private final PngFilter filter;

    // IDAT
    private final Deflater deflater = new Deflater(Deflater.BEST_SPEED);
    private final byte[] idatBuffer = new byte[32 * 1024];
    private int idatLength = 0;

    public PngEncoder(OutputStream output, PngFormat format) {
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.format = Objects.requireNonNull(format, "format must not be null");
        this.filter = new PngFilter(format);
        try {
            output.write(Magic);
            writeIHDR();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeImage(byte[] image) {
        if (image.length != format.bytesPerImage()) {
            throw new IllegalArgumentException("image has wrong size, expected " + format.bytesPerImage() + " but was " + image.length);
        }
        for (int y = 0; y < format.height(); y++) {
            writeRow(image, y * format.bytesPerRow());
        }
    }

    private void writeRow(byte[] image, int offset) {
        if (offset + format.bytesPerRow() > image.length) {
            throw new IllegalArgumentException("image has wrong size, expected at least " + (offset + format.bytesPerRow()) + " but was " + image.length);
        }
        int filterMethod = filter.filter(image, offset);
        deflate(new byte[]{(byte) filterMethod}, 0, 1);
        deflate(filter.bestRow(filterMethod), format.bytesPerPixel(), format.bytesPerRow());
    }

    // region Chunk writing

    private void writeIHDR() {
        byte[] chunk = ByteBuffer.allocate(13)
            .putInt(format.width())
            .putInt(format.height())
            .put((byte) format.bitDepth().value())
            .put((byte) format.colorType().code())
            .put((byte) 0)
            .put((byte) 0)
            .put((byte) 0)
            .array();
        writeChunk(IHDR, chunk);
    }

    private void writeIDAT() {
        writeChunk(IDAT, idatBuffer, idatLength);
        idatLength = 0;
    }

    private void writeIEND() {
        writeChunk(IEND, new byte[0]);
    }

    private void writeChunk(int type, byte[] data) {
        writeChunk(type, data, data.length);
    }

    private void writeChunk(int type, byte[] data, int length) {
        byte[] rawType = toBytes(type);
        CRC32 crc32 = new CRC32();
        crc32.update(rawType, 0, rawType.length);
        crc32.update(data, 0, length);

        try {
            output.write(toBytes(length));
            output.write(rawType);
            output.write(data, 0, length);
            output.write(toBytes((int) crc32.getValue()));
        } catch (IOException e) {
            throw new PngException("Failed to write chunk", e);
        }
    }

    private static byte[] toBytes(int value) {
        return new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    // endregion

    // region Deflate

    private void deflate(byte[] array, int offset, int length) {
        deflater.setInput(array, offset, length);
        while (!deflater.needsInput()) {
            deflate();
        }
    }

    private void deflate() {
        int len = deflater.deflate(idatBuffer, idatLength, idatBuffer.length - idatLength);
        if (len > 0) {
            idatLength += len;
            if (idatLength == idatBuffer.length) {
                writeIDAT();
            }
        }
    }

    // endregion

    @Override
    public void close() {
        try {
            deflater.finish();
            while (!deflater.finished()) {
                deflate();
            }
            deflater.end();

            writeIDAT();
            writeIEND();
            output.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}