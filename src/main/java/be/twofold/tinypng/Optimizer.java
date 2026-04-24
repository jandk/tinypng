package be.twofold.tinypng;

import java.util.*;

final class Optimizer {
    private static final byte[] GRAY_DEPTH = {1, 4, 4, 4, 4, 2, 4, 4, 4, 4, 2, 4, 4, 4, 4, 1};

    private PngFormat format;
    byte[] data;

    Optimizer(PngFormat format) {
        this.format = Objects.requireNonNull(format);
    }

    PngFormat format() {
        return format;
    }

    byte[] data() {
        return data;
    }

    void optimize(byte[] data) {
        this.data = Objects.requireNonNull(data);

        reduceBitDepth16To8();
        reduceToGray();
        reduceToOpaque();
        reduceToPalette();
        reduceBitDepth();
    }

    void reduceBitDepth16To8() {
        // Preconditions
        if (format.getBitDepth() != 16) {
            return;
        }

        // Check
        int length = format.getBytesPerImage();
        for (int i = 0; i < length; i += 2) {
            if (data[i] != data[i + 1]) {
                return;
            }
        }

        // Apply
        for (int i = 0, o = 0; i < length; i += 2, o++) {
            data[o] = data[i];
        }

        format = format.withBitDepth(8);
    }

    void reduceToOpaque() {
        // Preconditions
        if (format.getColorType() != ColorType.RGB_ALPHA && format.getColorType() != ColorType.GRAY_ALPHA) {
            return;
        }

        // Check
        int length = format.getBytesPerImage();
        int bytesPerPixel = format.getBytesPerPixel();
        int bytesPerChannel = format.getBytesPerChannel();

        for (int i = bytesPerPixel - bytesPerChannel; i < length; i += bytesPerPixel) {
            if (data[i] != (byte) 0xFF) {
                return;
            }
            if (bytesPerChannel == 2 && data[i + 1] != (byte) 0xFF) {
                return;
            }
        }

        // Apply
        int newBytesPerPixel = bytesPerPixel - bytesPerChannel;
        for (int i = 0, o = 0; i < length; i += bytesPerPixel, o += newBytesPerPixel) {
            for (int j = 0; j < newBytesPerPixel; j++) {
                data[o + j] = data[i + j];
            }
        }

        ColorType newColorType = format.getColorType() == ColorType.RGB_ALPHA
            ? ColorType.RGB
            : ColorType.GRAY;
        format = format.withColorType(newColorType);
    }


    void reduceToGray() {
        // Preconditions
        if (format.getColorType() != ColorType.RGB && format.getColorType() != ColorType.RGB_ALPHA) {
            return;
        }

        // Check and Apply
        boolean reduced = format.getBitDepth() == 8
            ? reduceToOpaque8()
            : reduceToOpaque16();

        if (reduced) {
            format = format.withColorType(format.getColorType() == ColorType.RGB
                ? ColorType.GRAY
                : ColorType.GRAY_ALPHA);
        }
    }

    private boolean reduceToOpaque8() {
        // Check
        int length = format.getBytesPerImage();
        int bytesPerPixel = format.getBytesPerPixel();
        for (int i = 0; i < length; i += bytesPerPixel) {
            if (data[i] != data[i + 1] || data[i] != data[i + 2]) {
                return false;
            }
        }

        // Apply
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

    private boolean reduceToOpaque16() {
        // Check
        int length = format.getBytesPerImage();
        int bytesPerPixel = format.getBytesPerPixel();
        for (int i = 0; i < length; i += bytesPerPixel) {
            if (data[i/**/] != data[i + 2] ||
                data[i + 1] != data[i + 3] ||
                data[i/**/] != data[i + 4] ||
                data[i + 1] != data[i + 5]) {
                return false;
            }
        }

        // Apply
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

    void reduceToPalette() {
        // Preconditions
        if (format.getBitDepth() != 8 || format.getPalette() != null) {
            return;
        }
        if (format.getColorType() != ColorType.RGB && format.getColorType() != ColorType.RGB_ALPHA) {
            return;
        }

        // Check
        int length = format.getBytesPerImage();
        int bytesPerPixel = format.getBytesPerPixel();
        boolean hasAlpha = bytesPerPixel == 4;

        // Collect unique colors
        Map<Integer, Integer> uniqueColors = new LinkedHashMap<>();
        for (int i = 0; i < length; i += bytesPerPixel) {
            int pixel = getPixel(i, hasAlpha);
            uniqueColors.putIfAbsent(pixel, uniqueColors.size());
            if (uniqueColors.size() > 256) {
                return;
            }
        }

        // Sort colors to minimize tRNS
        if (hasAlpha) {
            var sorted = new ArrayList<>(uniqueColors.keySet());
            sorted.sort(Integer::compareUnsigned);

            uniqueColors.clear();
            for (int i = 0; i < sorted.size(); i++) {
                uniqueColors.put(sorted.get(i), i);
            }
        }

        // Build palette
        int colorCount = uniqueColors.size();
        byte[] palette = new byte[colorCount * 3];

        int index = 0;
        int lastTransparent = -1;
        for (int color : uniqueColors.keySet()) {
            palette[index * 3/**/] = (byte) (color);
            palette[index * 3 + 1] = (byte) (color >>> 8);
            palette[index * 3 + 2] = (byte) (color >>> 16);
            if (hasAlpha && (color >>> 24) != 0xFF) {
                lastTransparent = index;
            }
            index++;
        }

        // Build transparency
        byte[] transparency = null;
        if (lastTransparent >= 0) {
            transparency = new byte[lastTransparent + 1];
            index = 0;
            for (int color : uniqueColors.keySet()) {
                if (index > lastTransparent) {
                    break;
                }
                transparency[index] = (byte) (color >>> 24);
                index++;
            }
        }

        // Apply
        for (int i = 0, o = 0; i < length; i += bytesPerPixel, o++) {
            int pixel = getPixel(i, hasAlpha);
            data[o] = uniqueColors.get(pixel).byteValue();
        }
        format = format
            .withPalette(palette)
            .withTransparency(transparency);
    }

    void reduceBitDepth() {
        // Preconditions
        if ((format.getColorType() != ColorType.INDEXED) &&
            (format.getColorType() != ColorType.GRAY || format.getBitDepth() != 8)) {
            return;
        }

        int bitDepth;
        if (format.getColorType() == ColorType.INDEXED) {
            int colorCount = format.getPalette().length / 3;
            if (colorCount <= 2) {
                bitDepth = 1;
            } else if (colorCount <= 4) {
                bitDepth = 2;
            } else if (colorCount <= 16) {
                bitDepth = 4;
            } else {
                return;
            }
        } else {
            bitDepth = 1;
            int length = format.getBytesPerImage();

            for (int i = 0; i < length; i++) {
                int pixel = data[i];
                if (((pixel >> 4) & 0xF) != (pixel & 0xF)) {
                    return;
                }
                bitDepth = Math.max(bitDepth, GRAY_DEPTH[pixel & 0xF]);
            }
        }
        pack(bitDepth);
        format = format.withBitDepth(bitDepth);
    }

    private void pack(int bitDepth) {
        int width = format.getWidth();
        int height = format.getHeight();
        int chunkSize = 8 / bitDepth;
        int mask = (1 << bitDepth) - 1;

        for (int row = 0, i = 0, o = 0; row < height; row++) {
            for (int x = 0; x < width; x += chunkSize) {
                int value = 0;
                for (int p = 0, s = 8 - bitDepth; p < Math.min(chunkSize, width - x); p++, s -= bitDepth) {
                    value |= (data[i++] & mask) << s;
                }
                data[o++] = (byte) value;
            }
        }
    }

    private int getPixel(int offset, boolean hasAlpha) {
        int r = Byte.toUnsignedInt(data[offset/**/]);
        int g = Byte.toUnsignedInt(data[offset + 1]);
        int b = Byte.toUnsignedInt(data[offset + 2]);
        int a = hasAlpha ? Byte.toUnsignedInt(data[offset + 3]) : 0xFF;
        return a << 24 | b << 16 | g << 8 | r;
    }
}
