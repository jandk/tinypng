package be.twofold.tinypng;

import java.util.*;

final class Optimizer {
    private PngFormat format;

    Optimizer(PngFormat format) {
        this.format = Objects.requireNonNull(format);
    }

    public PngFormat format() {
        return format;
    }

    byte[] optimize(byte[] data) {
        // Reduce 16 bits to 8 bits
        var reduced = reduceBitDepth16To8(data);
        if (reduced.isPresent()) {
            format = format.withBitDepth(8);
            data = reduced.get();
        }

        reduced = removeOpaqueAlpha(data);
        if (reduced.isPresent()) {
            format = format.withColorType(format.colorType() == ColorType.RGB_ALPHA ? ColorType.RGB : ColorType.GRAY);
            data = reduced.get();
        }

        return data;
    }

    private Optional<byte[]> reduceBitDepth16To8(byte[] data) {
        if (format.bitDepth() != 16) {
            return Optional.empty();
        }

        for (int i = 0; i < data.length; i += 2) {
            if (data[i] != data[i + 1]) {
                return Optional.empty();
            }
        }

        byte[] reduced = new byte[data.length / 2];
        for (int i = 0, o = 0; i < data.length; i += 2, o++) {
            reduced[o] = data[i];
        }

        return Optional.of(reduced);
    }

    private Optional<byte[]> removeOpaqueAlpha(byte[] data) {
        // Implies a bit depth of 8 or 16
        if (format.colorType() != ColorType.GRAY_ALPHA && format.colorType() != ColorType.RGB_ALPHA) {
            return Optional.empty();
        }

        int bytesPerPixel = format.bytesPerPixel();
        int bytesPerChannel = format.bytesPerChannel();
        int colorSize = bytesPerPixel - bytesPerChannel;
        byte[] reduced = new byte[data.length / bytesPerPixel * colorSize];
        for (int i = 0, o = 0; i < data.length; i += bytesPerPixel) {
            for (int j = 0; j < bytesPerChannel; j++) {
                if (data[i + colorSize + j] != (byte) 0xFF) {
                    return Optional.empty();
                }
            }
            for (int j = 0; j < colorSize; j++) {
                reduced[o++] = data[i + j];
            }
        }
        return Optional.of(reduced);
    }

}
