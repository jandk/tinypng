package be.twofold.tinypng;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.zip.*;

public final class PngWriter implements AutoCloseable {
    private static final byte[] Magic = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final OutputStream output;
    private final Info info;

    // Filtering
    private final byte[][] filtered;
    private byte[] curr;
    private byte[] prev;

    public PngWriter(OutputStream output, Info info) {
        this.output = Objects.requireNonNull(output, "output is null");
        this.info = Objects.requireNonNull(info, "info is null");
        this.filtered = new byte[5][info.bytesPerPixel() + info.bytesPerRow()];
        this.curr = new byte[info.bytesPerPixel() + info.bytesPerRow()];
        this.prev = new byte[info.bytesPerPixel() + info.bytesPerRow()];
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
        try (var ios = new IdatOutputStream()) {
            for (var y = 0; y < info.height(); y++) {
                var filterMethod = filter(data, y * info.bytesPerRow());
                ios.write(filterMethod);
                ios.write(filtered[filterMethod], info.bytesPerPixel(), info.bytesPerRow());
            }
        }
    }

    private void writeChunk(String type, byte[] data) throws IOException {
        writeChunk(type, data, data.length);
    }

    private void writeChunk(String type, byte[] data, int length) throws IOException {
        output.write(toBytes(length));
        output.write(type.getBytes());
        output.write(data, 0, length);
        output.write(toBytes(crc(type, data, length)));
    }

    private int crc(String type, byte[] data, int length) {
        var crc = new CRC32();
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
        int bpp = info.bytesPerPixel();
        int bpr = info.bytesPerRow();

        var nRow = filtered[0];
        var sRow = filtered[1];
        var uRow = filtered[2];
        var aRow = filtered[3];
        var pRow = filtered[4];

        System.arraycopy(row, offset, curr, bpp, bpr);
        for (var i = bpp; i < bpp + bpr; i++) {
            var x = Byte.toUnsignedInt(curr[i]);
            var a = Byte.toUnsignedInt(curr[i - bpp]);
            var b = Byte.toUnsignedInt(prev[i]);
            var c = Byte.toUnsignedInt(prev[i - bpp]);

            nRow[i] = (byte) (x);
            sRow[i] = (byte) (x - a);
            uRow[i] = (byte) (x - b);
            aRow[i] = (byte) (x - (a + b >>> 1));
            pRow[i] = (byte) (x - paeth(a, b, c));
        }

        var temp = prev;
        prev = curr;
        curr = temp;
        return findBest();
    }

    private int findBest() {
        var bestRow = 0;
        var bestSad = Integer.MAX_VALUE;
        for (var i = 0; i < 5; i++) {
            var sad = 0;
            for (var pixel : filtered[i]) {
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
        var p = a + b - c;
        var pa = Math.abs(p - a);
        var pb = Math.abs(p - b);
        var pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }

    public enum ColorType {
        Gray(0, 1),
        Rgb(2, 3),
        Palette(3, 1),
        GrayAlpha(4, 2),
        RgbAlpha(6, 4);

        final int code;
        final int channels;

        ColorType(int code, int channels) {
            this.code = code;
            this.channels = channels;
        }
    }

    public record Info(
        int width,
        int height,
        int bitDepth,
        ColorType colorType,
        boolean linear
    ) {
        public Info {
            if (width <= 0) {
                throw new IllegalArgumentException("width must be greater than 0");
            }
            if (height <= 0) {
                throw new IllegalArgumentException("height must be greater than 0");
            }
            if (bitDepth != 8 && bitDepth != 16) {
                throw new IllegalArgumentException("bitDepth must be 8 or 16");
            }
            Objects.requireNonNull(colorType, "colorType is null");
        }

        public int bytesPerPixel() {
            return colorType.channels * (bitDepth == 8 ? 1 : 2);
        }

        public int bytesPerRow() {
            return bytesPerPixel() * width;
        }

        public int bytesPerImage() {
            return bytesPerRow() * height;
        }
    }

    private final class IdatOutputStream extends OutputStream {
        private final Deflater deflater;
        private final byte[] buffer;
        private int length = 0;

        private IdatOutputStream() {
            this.deflater = new Deflater(Deflater.BEST_SPEED);
            this.buffer = new byte[32 * 1024];
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (!deflater.finished()) {
                deflater.setInput(b, off, len);
                while (!deflater.needsInput()) {
                    deflate();
                }
            }
        }

        @Override
        public void close() throws IOException {
            if (!deflater.finished()) {
                deflater.finish();
                while (!deflater.finished()) {
                    deflate();
                }
                writeChunk("IDAT", buffer, length);
            }
            deflater.end();
        }

        private void deflate() throws IOException {
            int len = deflater.deflate(buffer, length, buffer.length - length);
            if (len > 0) {
                length += len;
                if (length == buffer.length) {
                    writeChunk("IDAT", buffer);
                    length = 0;
                }
            }
        }
    }
}
