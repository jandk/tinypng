package be.twofold.tinypng;

import java.util.zip.*;

/**
 * Encoding parameters for a {@link PngEncoder}.
 *
 * <p>These control how the image is compressed, not what the image contains.
 * Create an instance with {@link #of} and chain {@code with*} methods for
 * non-default settings.
 */
public final class PngConfig {
    private final int compressionLevel;
    private final PngFilter filterMethod = PngFilter.ADAPTIVE;

    private PngConfig(
        int compressionLevel,
        PngFilter filterMethod
    ) {
        // Deflater accepts 0–9 or DEFAULT_COMPRESSION (-1)
        if (compressionLevel != Deflater.DEFAULT_COMPRESSION
            && (compressionLevel < 0 || compressionLevel > 9)) {
            throw new IllegalArgumentException(
                "compressionLevel must be between 0 and 9, or Deflater.DEFAULT_COMPRESSION");
        }
        this.compressionLevel = compressionLevel;
    }

    public static PngConfig of() {
        return new PngConfig(
            Deflater.DEFAULT_COMPRESSION,
            PngFilter.ADAPTIVE
        );
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public PngFilter getFilterMethod() {
        return filterMethod;
    }

    public PngConfig withCompressionLevel(int compressionLevel) {
        return new PngConfig(compressionLevel, filterMethod);
    }

    public PngConfig withFilterMethod(PngFilter filterMethod) {
        return new PngConfig(compressionLevel, filterMethod);
    }
}
