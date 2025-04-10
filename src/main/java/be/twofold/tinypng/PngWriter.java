package be.twofold.tinypng;

import java.util.*;

/**
 * After writing the header, this class will allow you to write images.
 * <p>
 * For now, only a single image is supported
 */
public final class PngWriter {
    private final PngFormat format;
    private final PngFilter filter;
    private IDATWriter writer;

    PngWriter(PngFormat format, ChunkWriter writer) {
        this.format = Objects.requireNonNull(format);
        this.filter = new PngFilter(format);
        this.writer = new IDATWriter(writer);
    }

    /**
     * Writes a single image to the output
     *
     * @param image The image to write
     */
    public void writeImage(byte[] image) {
        if (writer == null) {
            throw new IllegalStateException("Image has already been written");
        }
        if (image.length != format.bytesPerImage()) {
            throw new IllegalArgumentException("image has wrong size, expected " + format.bytesPerImage() + " but was " + image.length);
        }

        for (int row = 0; row < format.height(); row++) {
            int filterMethod = filter.filter(image, row * format.bytesPerRow());
            writer.write((byte) filterMethod);
            writer.write(filter.bestRow(filterMethod), format.bytesPerPixel(), format.bytesPerRow());
        }

        writer.close();
        writer = null;
    }
}
