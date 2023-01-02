package be.twofold.tinypng;

import java.util.*;

final class PngFilter {
    private final PngFormat format;
    private final byte[][] filtered;
    private byte[] curr;
    private byte[] prev;

    PngFilter(PngFormat format) {
        this.format = Objects.requireNonNull(format, "format is null");

        int length = format.getBytesPerPixel() + format.getBytesPerRow();
        this.filtered = new byte[5][length];
        this.curr = new byte[length];
        this.prev = new byte[length];
    }

    byte[] getFiltered(int filterMethod) {
        return filtered[filterMethod];
    }

    int filter(byte[] row, int offset) {
        int bpp = format.getBytesPerPixel();
        int bpr = format.getBytesPerRow();

        byte[] nRow = filtered[0];
        byte[] sRow = filtered[1];
        byte[] uRow = filtered[2];
        byte[] aRow = filtered[3];
        byte[] pRow = filtered[4];

        System.arraycopy(row, offset, curr, bpp, bpr);
        for (int i = bpp; i < bpp + bpr; i++) {
            int x = Byte.toUnsignedInt(curr[i]);
            int a = Byte.toUnsignedInt(curr[i - bpp]);
            int b = Byte.toUnsignedInt(prev[i]);
            int c = Byte.toUnsignedInt(prev[i - bpp]);

            nRow[i] = (byte) x;
            sRow[i] = (byte) (x - a);
            uRow[i] = (byte) (x - b);
            aRow[i] = (byte) (x - (a + b >>> 1));
            pRow[i] = (byte) (x - paeth(a, b, c));
        }

        byte[] temp = prev;
        prev = curr;
        curr = temp;
        return findBest();
    }

    private int findBest() {
        int bestRow = 0;
        int bestSad = Integer.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            int sad = 0;
            for (byte pixel : filtered[i]) {
                sad += Math.abs(pixel);
            }
            if (sad < bestSad) {
                bestRow = i;
                bestSad = sad;
            }
        }
        return bestRow;
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
