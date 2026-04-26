package be.twofold.tinypng;

import java.util.*;
import java.util.function.*;

final class Filter {

    private final int[] counts = new int[256];
    private final int bytesPerPixel;
    private final int bytesPerRow;
    private final PngFilter filter;
    private final byte[][] filtered;
    private final byte[] current;
    private final byte[] previous;

    Filter(PngFormat format, PngFilter filter) {
        this.bytesPerPixel = format.getBytesPerPixel();
        this.bytesPerRow = format.getBytesPerRow();
        this.filter = Objects.requireNonNull(filter);
        this.filtered = new byte[5][bytesPerPixel + bytesPerRow];
        this.current = filtered[0];
        this.previous = new byte[bytesPerPixel + bytesPerRow];
    }

    byte[] bestRow(PngFilter filter) {
        return filtered[filter.ordinal()];
    }

    PngFilter filter(byte[] row, int offset) {
        System.arraycopy(row, offset, current, bytesPerPixel, bytesPerRow);
        if (filter == PngFilter.NONE) {
            return PngFilter.NONE;
        }
        PngFilter result = applyFilter();
        System.arraycopy(current, bytesPerPixel, previous, bytesPerPixel, bytesPerRow);
        return result;
    }

    private PngFilter applyFilter() {
        switch (filter) {
            case NONE:
                return PngFilter.NONE;
            case SUB:
                return filterSub(bytesPerPixel, filtered[1]);
            case UP:
                return filterUp(bytesPerPixel, filtered[2]);
            case AVERAGE:
                return filterAverage(bytesPerPixel, filtered[3]);
            case PAETH:
                return filterPaeth(bytesPerPixel, filtered[4]);
            case ADAPTIVE:
                return applyAdaptive(this::sad);
            case MIN_ENTROPY:
                return applyAdaptive(this::entropy);
            default:
                throw new AssertionError();
        }
    }

    private PngFilter applyAdaptive(ToIntBiFunction<byte[], Integer> heuristic) {
        filterSub(bytesPerPixel, filtered[1]);
        filterUp(bytesPerPixel, filtered[2]);
        filterAverage(bytesPerPixel, filtered[3]);
        filterPaeth(bytesPerPixel, filtered[4]);

        return findBest(bytesPerPixel, heuristic);
    }

    private PngFilter filterSub(int bpp, byte[] filtered) {
        byte[] current = this.current;
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int a = Byte.toUnsignedInt(current[i - bpp]);
            filtered[i] = (byte) (x - a);
        }
        return PngFilter.SUB;
    }

    private PngFilter filterUp(int bpp, byte[] filtered) {
        byte[] current = this.current;
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int b = Byte.toUnsignedInt(previous[i]);

            filtered[i] = (byte) (x - b);
        }
        return PngFilter.UP;
    }

    private PngFilter filterAverage(int bpp, byte[] filtered) {
        byte[] current = this.current;
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int a = Byte.toUnsignedInt(current[i - bpp]);
            int b = Byte.toUnsignedInt(previous[i]);

            filtered[i] = (byte) (x - ((a + b) >> 1));
        }
        return PngFilter.AVERAGE;
    }

    private PngFilter filterPaeth(int bpp, byte[] filtered) {
        byte[] current = this.current;
        for (int i = bpp; i < current.length; i++) {
            int x = Byte.toUnsignedInt(current[i]);
            int a = Byte.toUnsignedInt(current[i - bpp]);
            int b = Byte.toUnsignedInt(previous[i]);
            int c = Byte.toUnsignedInt(previous[i - bpp]);

            filtered[i] = (byte) (x - paeth(a, b, c));
        }
        return PngFilter.PAETH;
    }

    private PngFilter findBest(int bpp, ToIntBiFunction<byte[], Integer> scorer) {
        int bestRow = 0;
        int bestResult = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            int result = scorer.applyAsInt(filtered[i], bpp);
            if (result < bestResult) {
                bestRow = i;
                bestResult = result;
            }
        }
        return PngFilter.of(bestRow);
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
