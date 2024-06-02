package be.twofold.tinypng;

import java.util.*;

public final class PngPalette extends AbstractList<PngPalette.Color> {
    private final List<Color> colors;

    public PngPalette(List<Color> colors) {
        if (colors.size() > 256) {
            throw new PngException("Palette can only contain up to 256 colors");
        }
        this.colors = List.copyOf(colors);
    }

    @Override
    public Color get(int index) {
        return colors.get(index);
    }

    @Override
    public int size() {
        return colors.size();
    }

    public static final class Color {
        private final byte red;
        private final byte green;
        private final byte blue;

        public Color(int red, int green, int blue) {
            this.red = toByteExact(red);
            this.green = toByteExact(green);
            this.blue = toByteExact(blue);
        }

        private byte toByteExact(int i) {
            if (i < 0 || i > 255) {
                throw new PngException("Color value out of range: " + i + " (0-255)");
            }
            return (byte) i;
        }

        public byte red() {
            return red;
        }

        public byte green() {
            return green;
        }

        public byte blue() {
            return blue;
        }
    }
}
