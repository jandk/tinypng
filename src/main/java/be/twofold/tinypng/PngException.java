package be.twofold.tinypng;

public final class PngException extends RuntimeException {
    public PngException(String message) {
        super(message);
    }

    public PngException(String message, Throwable cause) {
        super(message, cause);
    }
}
