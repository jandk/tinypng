package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public final class PngOutputStream implements AutoCloseable {

    private static final byte[] Magic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final int IHDR = 0x49484452;
    private static final int PLTE = 0x504c5445;
    private static final int IDAT = 0x49444154;
    private static final int IEND = 0x49454e44;

    private final OutputStream output;
    private final PngFormat format;

    // Filtering
    private final byte[][] filtered;
    private byte[] previous;

    // IDAT
    private final Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
    private final byte[] idatBuffer = new byte[32 * 1024];
    private int idatLength = 0;

    public PngOutputStream(OutputStream output, PngFormat format) {
        this.output = Objects.requireNonNull(output, "output is null");
        this.format = Objects.requireNonNull(format, "format is null");
        this.filtered = new byte[5][format.getBytesPerPixel() + format.getBytesPerRow()];
        this.previous = new byte[format.getBytesPerPixel() + format.getBytesPerRow()];

        try {
            output.write(Magic);
            writeIHDR();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writeImage(byte[] image) throws IOException {
        if (image.length != format.getBytesPerImage()) {
            throw new IllegalArgumentException("image has wrong size, expected " + format.getBytesPerImage() + " but was " + image.length);
        }
        for (int y = 0; y < format.getHeight(); y++) {
            writeRow(image, y * format.getBytesPerRow());
        }
    }

    public void writeRow(byte[] image, int offset) throws IOException {
        if (offset + format.getBytesPerRow() > image.length) {
            throw new IllegalArgumentException("image has wrong size, expected at least " + (offset + format.getBytesPerRow()) + " but was " + image.length);
        }
        int filterMethod = filter(image, offset);
        deflate(new byte[]{(byte) filterMethod}, 0, 1);
        deflate(filtered[filterMethod], format.getBytesPerPixel(), format.getBytesPerRow());
    }

    //
    // Filtering
    //

    private int filter(byte[] row, int offset) {
        int bpp = format.getBytesPerPixel();
        int bpr = format.getBytesPerRow();

        byte[] curr = filtered[0];
        byte[] sRow = filtered[1];
        byte[] uRow = filtered[2];
        byte[] aRow = filtered[3];
        byte[] pRow = filtered[4];

        System.arraycopy(row, offset, curr, bpp, bpr);
        for (int i = bpp; i < bpp + bpr; i++) {
            int x = Byte.toUnsignedInt(curr[i]);
            int a = Byte.toUnsignedInt(curr[i - bpp]);
            int b = Byte.toUnsignedInt(previous[i]);
            int c = Byte.toUnsignedInt(previous[i - bpp]);

            sRow[i] = (byte) (x - a);
            uRow[i] = (byte) (x - b);
            aRow[i] = (byte) (x - (a + b >> 1));
            pRow[i] = (byte) (x - paeth(a, b, c));
        }

        int best = findBest();
        byte[] temp = previous;
        previous = filtered[0];
        filtered[0] = temp;
        return best;
    }

    private int findBest() {
        int bestRow = 0;
        int bestSad = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            int sad = 0;
            for (byte pixel : filtered[i]) {
                sad += Math.abs(pixel);
            }
            if (sad < bestSad) {
                bestRow = i;
                bestSad = sad;
            }
        }
        return bestRow;
    }

    private static int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }

    //
    // Chunk writing
    //

    private void writeIHDR() throws IOException {
        byte[] chunk = ByteBuffer.allocate(13)
            .putInt(format.getWidth())
            .putInt(format.getHeight())
            .put((byte) format.getBitDepth())
            .put((byte) format.getColorType().getCode())
            .put((byte) 0)
            .put((byte) 0)
            .put((byte) 0)
            .array();
        writeChunk(IHDR, chunk);
    }

    private void writeIDAT() throws IOException {
        writeChunk(IDAT, idatBuffer, idatLength);
        idatLength = 0;
    }

    private void writeIEND() throws IOException {
        writeChunk(IEND, new byte[0]);
    }

    private void writeChunk(int type, byte[] data) throws IOException {
        writeChunk(type, data, data.length);
    }

    private void writeChunk(int type, byte[] data, int length) throws IOException {
        byte[] rawType = toBytes(type);
        CRC32 crc32 = new CRC32();
        crc32.update(rawType);
        crc32.update(data, 0, length);

        output.write(toBytes(length));
        output.write(rawType);
        output.write(data, 0, length);
        output.write(toBytes((int) crc32.getValue()));
    }

    private static byte[] toBytes(int value) {
        return new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    //
    // Deflate
    //

    private void deflate(byte[] array, int offset, int length) throws IOException {
        deflater.setInput(array, offset, length);
        while (!deflater.needsInput()) {
            deflate();
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
