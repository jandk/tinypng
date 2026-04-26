package be.twofold.tinypng;

import java.util.*;
import java.util.function.*;

final class Filter {

    private final int[] counts = new int[256];
    private final byte[][] filtered;
    private final byte[] previous;
    private final int bytesPerPixel;
    private final int bytesPerRow;

    Filter(PngFormat format) {
        this.bytesPerPixel = format.getBytesPerPixel();
        this.bytesPerRow = format.getBytesPerRow();
        this.filtered = new byte[5][bytesPerPixel + bytesPerRow];
        this.previous = new byte[bytesPerPixel + bytesPerRow];
    }

    byte[] bestRow(int method) {
        return filtered[method];
    }

    PngFilter filter(byte[] row, int offset, PngFilter filter) {
        switch (filter) {
            case ADAPTIVE:
                return adaptiveFilter(row, offset, this::sad);
            case MIN_ENTROPY:
                return adaptiveFilter(row, offset, this::entropy);
            default:
                throw new AssertionError();
        }
    }

    private int adaptiveFilter(byte[] row, int offset, ToIntBiFunction<byte[], Integer> function) {
        byte[] current = filtered[0];

        System.arraycopy(row, offset, current, bytesPerPixel, bytesPerRow);
        filterSub(current, bytesPerPixel, filtered[1]);
        filterUp(current, bytesPerPixel, filtered[2]);
        filterAverage(current, bytesPerPixel, filtered[3]);
        filterPaeth(current, bytesPerPixel, filtered[4]);

        int best = findBest(bytesPerPixel, function);
        System.arraycopy(current, bytesPerPixel, previous, bytesPerPixel, bytesPerRow);
        return best;
    }

    private void filterSub(byte[] current, int bpp, byte[] filtered) {
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int a = Byte.toUnsignedInt(current[i - bpp]);
            filtered[i] = (byte) (x - a);
        }
    }

    private void filterUp(byte[] current, int bpp, byte[] filtered) {
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int b = Byte.toUnsignedInt(previous[i]);

            filtered[i] = (byte) (x - b);
        }
    }

    private void filterAverage(byte[] current, int bpp, byte[] filtered) {
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int a = Byte.toUnsignedInt(current[i - bpp]);
            int b = Byte.toUnsignedInt(previous[i]);

            filtered[i] = (byte) (x - ((a + b) >> 1));
        }
    }

    private void filterPaeth(byte[] current, int bpp, byte[] filtered) {
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int a = Byte.toUnsignedInt(current[i - bpp]);
            int b = Byte.toUnsignedInt(previous[i]);
            int c = Byte.toUnsignedInt(previous[i - bpp]);

            filtered[i] = (byte) (x - paeth(a, b, c));
        }
    }

    private int findBest(int bpp, ToIntBiFunction<byte[], Integer> function) {
        int bestRow = 0;
        int bestResult = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            int result = function.applyAsInt(filtered[i], bpp);
            if (result < bestResult) {
                bestRow = i;
                bestResult = result;
            }
        }
        return bestRow;
    }

    private int sad(byte[] bytes, int offset) {
        int sad = 0;
        for (int i = offset; i < bytes.length; i++) {
            sad += Math.abs(bytes[i]);
        }
        return sad;
    }

    private int entropy(byte[] bytes, int offset) {
        int[] counts = this.counts;
        Arrays.fill(counts, 0);

        for (int i = offset; i < bytes.length; i++) {
            counts[Byte.toUnsignedInt(bytes[i])]++;
        }

        int result = ilog2i(bytes.length - offset);
        for (int count : counts) {
            if (count > 0) {
                result -= ilog2i(count);
            }
        }
        return result;
    }

    private int ilog2i(int i) {
        int log2 = 31 - Integer.numberOfLeadingZeros(i);
        return i * log2 + ((i - (1 << log2)) * 2);
    }

    private int paeth(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }
}
