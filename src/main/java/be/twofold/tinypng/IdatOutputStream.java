package be.twofold.tinypng;

import java.io.*;
import java.util.zip.*;

final class IdatOutputStream extends OutputStream {
    private final PngWriter pngWriter;
    private final Deflater deflater;
    private final byte[] buffer;
    private int length = 0;

    IdatOutputStream(PngWriter pngWriter) {
        this.pngWriter = pngWriter;
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
            pngWriter.writeChunk(ChunkType.IDAT, buffer, length);
        }
        deflater.end();
    }

    private void deflate() throws IOException {
        int len = deflater.deflate(buffer, length, buffer.length - length);
        if (len > 0) {
            length += len;
            if (length == buffer.length) {
                pngWriter.writeChunk(ChunkType.IDAT, buffer);
                length = 0;
            }
        }
    }
}
