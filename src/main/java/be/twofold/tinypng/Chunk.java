package be.twofold.tinypng;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public final class Chunk {
    public static final int IHDR = 0x49484452;
    public static final int PLTE = 0x504c5445;
    public static final int IDAT = 0x49444154;
    public static final int IEND = 0x49454e44;

    private final ChunkType type;
    private final byte[] data;
    private int crc;

    public Chunk(ChunkType type, byte[] data) {
        this.type = Objects.requireNonNull(type, "type is null");
        this.data = Objects.requireNonNull(data, "data is null");
    }

    public ChunkType getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }

    public int getCrc() {
        if (crc == 0) {
            CRC32 crc32 = new CRC32();
            crc32.update(toBytes(type.getType()));
            crc32.update(data);
            crc = (int) crc32.getValue();
        }
        return crc;
    }

    public void write(OutputStream out) throws IOException {
        out.write(toBytes(data.length));
        out.write(toBytes(type.getType()));
        out.write(data);
        out.write(toBytes(getCrc()));
    }

    private static byte[] toBytes(int value) {
        return new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Chunk)) return false;

        Chunk other = (Chunk) obj;
        return type.equals(other.type) && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + type.hashCode();
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        return "Chunk(" +
            "type=" + type + ", " +
            "length=" + data.length +
            ")";
    }
}
