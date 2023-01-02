package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public final class PngWriter implements AutoCloseable {
    private static final byte[] Magic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final OutputStream output;
    private final PngFormat info;

    // Filtering
    private final byte[][] filtered;
    private byte[] curr;
    private byte[] prev;

    public PngWriter(OutputStream output, PngFormat info) {
        this.output = Objects.requireNonNull(output, "output is null");
        this.info = Objects.requireNonNull(info, "info is null");
        this.filtered = new byte[5][info.getBytesPerPixel() + info.getBytesPerRow()];
        this.curr = new byte[info.getBytesPerPixel() + info.getBytesPerRow()];
        this.prev = new byte[info.getBytesPerPixel() + info.getBytesPerRow()];
    }

    public void write(byte[] image) throws IOException {
        output.write(Magic);
        writeChunk("IHDR", createIHDR());
        writeChunk("gAMA", createGAMA(info.linear() ? 1.0f : 2.2f));
        if (!info.linear()) {
            writeChunk("sRGB", new byte[]{0});
        }
        writeIdat(image);
        writeChunk("IEND", new byte[0]);
    }

    private byte[] createIHDR() {
        return ByteBuffer.allocate(13)
            .putInt(info.width())
            .putInt(info.height())
            .put((byte) info.bitDepth())
            .put((byte) info.colorType().code)
            .put((byte) 0)
            .put((byte) 0)
            .put((byte) 0)
            .array();
    }

    private byte[] createGAMA(float gamma) {
        return toBytes(Math.round((1.0f / gamma) * 100_000));
    }

    private void writeIdat(byte[] data) throws IOException {
        try (OutputStream ios = new IdatOutputStream(this)) {
            for (int y = 0; y < info.height(); y++) {
                int filterMethod = filter(data, y * info.getBytesPerRow());
                ios.write(filterMethod);
                ios.write(filtered[filterMethod], info.getBytesPerPixel(), info.getBytesPerRow());
            }
        }
    }

    void writeChunk(String type, byte[] data) throws IOException {
        writeChunk(type, data, data.length);
    }

    void writeChunk(String type, byte[] data, int length) throws IOException {
        output.write(toBytes(length));
        output.write(type.getBytes());
        output.write(data, 0, length);
        output.write(toBytes(crc(type, data, length)));
    }

    private int crc(String type, byte[] data, int length) {
        CRC32 crc = new CRC32();
        crc.update(type.getBytes());
        crc.update(data, 0, length);
        return (int) crc.getValue();
    }

    private byte[] toBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }

    @Override
    public void close() {
        try {
            output.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    //
    // Filtering
    //

    private int filter(byte[] row, int offset) {
        int bpp = info.getBytesPerPixel();
        int bpr = info.getBytesPerRow();

        byte[] nRow = filtered[0];
        byte[] sRow = filtered[1];
        byte[] uRow = filtered[2];
        byte[] aRow = filtered[3];
        byte[] pRow = filtered[4];

        System.arraycopy(row, offset, curr, bpp, bpr);
        for (int i = bpp; i < bpp + bpr; i++) {
            int x = Byte.toUnsignedInt(curr[i]);
            int a = Byte.toUnsignedInt(curr[i - bpp]);
            int b = Byte.toUnsignedInt(prev[i]);
            int c = Byte.toUnsignedInt(prev[i - bpp]);

            nRow[i] = (byte) (x);
            sRow[i] = (byte) (x - a);
            uRow[i] = (byte) (x - b);
            aRow[i] = (byte) (x - (a + b >>> 1));
            pRow[i] = (byte) (x - paeth(a, b, c));
        }

        byte[] temp = prev;
        prev = curr;
        curr = temp;
        return findBest();
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

    private int paeth(int a, int b, int c) {
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

}
