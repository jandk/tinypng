package be.twofold.tinypng;

import java.util.*;

final class Optimizer {
    private PngFormat format;
    private byte[] data;
    private int length;

    Optimizer(PngFormat format) {
        this.format = Objects.requireNonNull(format);
    }

    public PngFormat format() {
        return format;
    }

    byte[] optimize(byte[] data) {
        this.data = data;
        this.length = data.length;

        reduceBitDepth16To8();
        reduceRgbToGrayscale();
        optimizeAlpha();

        return data;
    }

    private void reduceBitDepth16To8() {
        if (format.getBitDepth() != 16) {
            return;
        }

        // Check all high bytes and low bytes are equal
        for (int i = 0; i < length; i += 2) {
            if (data[i] != data[i + 1]) {
                return;
            }
        }

        // Remove every other byte
        for (int i = 0; i < length / 2; i++) {
            data[i] = data[i * 2];
        }

        format = format.withBitDepth(8);
        length /= 2;
    }

    private void reduceRgbToGrayscale() {
        if (format.getColorType() != ColorType.RGB && format.getColorType() != ColorType.RGB_ALPHA) {
            return;
        }

        boolean reduced = format.getBitDepth() == 8
            ? reduceRgbToGrayscale8()
            : reduceRgbToGrayscale16();

        if (reduced) {
            if (format.getColorType() == ColorType.RGB) {
                format = format.withColorType(ColorType.GRAY);
                length /= 3;
            } else {
                format = format.withColorType(ColorType.GRAY_ALPHA);
                length /= 2;
            }
        }
    }

    private boolean reduceRgbToGrayscale8() {
        int bpp = format.getBytesPerPixel();
        for (int i = 0; i < length; i += bpp) {
            if (data[i] != data[i + 1] || data[i] != data[i + 2]) {
                return false;
            }
        }

        if (format.getColorType() == ColorType.RGB) {
            for (int i = 0, o = 0; i < length; i += 3, o++) {
                data[o] = data[i];
            }
        } else {
            for (int i = 0, o = 0; i < length; i += 4, o += 2) {
                data[o/**/] = data[i/**/];
                data[o + 1] = data[i + 3];
            }
        }
        return true;
    }

    private boolean reduceRgbToGrayscale16() {
        int bpp = format.getBytesPerPixel();
        for (int i = 0; i < length; i += bpp) {
            if (data[i/**/] != data[i + 2] ||
                data[i + 1] != data[i + 3] ||
                data[i/**/] != data[i + 4] ||
                data[i + 1] != data[i + 5]) {
                return false;
            }
        }

        if (format.getColorType() == ColorType.RGB) {
            for (int i = 0, o = 0; i < length; i += 6, o += 2) {
                data[o/**/] = data[i/**/];
                data[o + 1] = data[i + 1];
            }
        } else {
            for (int i = 0, o = 0; i < length; i += 8, o += 4) {
                data[o/**/] = data[i/**/];
                data[o + 1] = data[i + 1];
                data[o + 2] = data[i + 6];
                data[o + 3] = data[i + 7];
            }
        }
        return true;
    }

    private void optimizeAlpha() {
        if (format.getColorType() != ColorType.RGB_ALPHA && format.getColorType() != ColorType.GRAY_ALPHA) {
            return;
        }

        int bpp = format.getBytesPerPixel();
        int bpc = format.getBytesPerChannel();

        for (int i = bpp - bpc; i < length; i += bpp) {
            if (data[i] != -1 || (bpc == 2 && data[i + 1] != -1)) {
                return;
            }
        }

        int newBpp = bpp - bpc;
        for (int i = 0, o = 0; i < length; i += bpp, o += newBpp) {
            for (int j = 0; j < newBpp; j++) {
                data[o + j] = data[i + j];
            }
        }

        format = format.withColorType(format.getColorType() == ColorType.RGB_ALPHA ? ColorType.RGB : ColorType.GRAY);
        length = length * newBpp / bpp;
    }

    private byte get(int index) {
        return data[index];
    }
}
