package be.twofold.tinypng;

import java.util.*;
import java.util.zip.*;

/**
 * Encoding parameters for a {@link PngWriter}.
 *
 * <p>These control how the image is compressed, not what the image contains.
 * Create an instance with {@link #of} and chain {@code with*} methods for
 * non-default settings.
 */
public final class PngConfig {
    private final boolean optimize;
    private final PngFilter filterMethod;
    private final int compressionLevel;

    private PngConfig(boolean optimize, PngFilter filterMethod, int compressionLevel) {
        if ((compressionLevel < 0 || compressionLevel > 9) && compressionLevel != Deflater.DEFAULT_COMPRESSION) {
            throw new IllegalArgumentException("compressionLevel must be between 0 and 9, or Deflater.DEFAULT_COMPRESSION");
        }
        this.optimize = optimize;
        this.filterMethod = Objects.requireNonNull(filterMethod);
        this.compressionLevel = compressionLevel;
    }

    public static PngConfig of() {
        return new PngConfig(true, PngFilter.ADAPTIVE, Deflater.DEFAULT_COMPRESSION);
    }

    public boolean isOptimize() {
        return optimize;
    }

    public PngFilter getFilterMethod() {
        return filterMethod;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public PngConfig withOptimize(boolean optimize) {
        return new PngConfig(optimize, filterMethod, compressionLevel);
    }

    public PngConfig withFilterMethod(PngFilter filterMethod) {
        return new PngConfig(optimize, filterMethod, compressionLevel);
    }

    public PngConfig withCompressionLevel(int compressionLevel) {
        return new PngConfig(optimize, filterMethod, compressionLevel);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PngConfig)) {
            return false;
        }

        PngConfig other = (PngConfig) obj;
        return optimize == other.optimize
            && filterMethod == other.filterMethod
            && compressionLevel == other.compressionLevel;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Boolean.hashCode(optimize);
        result = 31 * result + filterMethod.hashCode();
        result = 31 * result + Integer.hashCode(compressionLevel);
        return result;
    }

    @Override
    public String toString() {
        return "PngConfig(" +
            "optimize=" + optimize + ", " +
            "filterMethod=" + filterMethod + ", " +
            "compressionLevel=" + compressionLevel + ")";
    }
}
