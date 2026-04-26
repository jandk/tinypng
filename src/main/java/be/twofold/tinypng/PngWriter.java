package be.twofold.tinypng;

import java.io.*;
import java.util.*;

/**
 * After writing the header, this class will allow you to write images.
 * <p>
 * For now, only a single image is supported
 */
public final class PngWriter implements Closeable {
    private final PngFormat format;
    private final Filter filter;
    private IDATWriter writer;

    PngWriter(PngFormat format, ChunkWriter writer, PngFilter filterStrategy) {
        this.format = Objects.requireNonNull(format);
        this.filter = new Filter(format, filterStrategy);
        this.writer = new IDATWriter(writer);
    }

    /**
     * Writes a single image to the output
     *
     * @param image The image to write
     */
    public void writeImage(byte[] image) throws IOException {
        if (writer == null) {
            throw new IllegalStateException("Image has already been written");
        }
        if (image.length != format.getBytesPerImage()) {
            throw new IllegalArgumentException("image has wrong size, expected " + format.getBytesPerImage() + " but was " + image.length);
        }

        for (int row = 0; row < format.getHeight(); row++) {
            PngFilter filterMethod = filter.filter(image, row * format.getBytesPerRow());
            writer.write((byte) filterMethod.ordinal());
            writer.write(filter.bestRow(filterMethod), format.getBytesPerPixel(), format.getBytesPerRow());
        }

        writer.close();
        writer = null;
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
            writer = null;
        }
    }
}
