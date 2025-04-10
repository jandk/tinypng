package be.twofold.tinypng;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class PngEncoderTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "basi0g01", "basi0g02", "basi0g04", "basi0g08", "basi0g16", "basi2c08", "basi2c16", "basi3p01",
        "basi3p02", "basi3p04", "basi3p08", "basi4a08", "basi4a16", "basi6a08", "basi6a16", "basn0g01",
        "basn0g02", "basn0g04", "basn0g08", "basn0g16", "basn2c08", "basn2c16", "basn3p01", "basn3p02",
        "basn3p04", "basn3p08", "basn4a08", "basn4a16", "basn6a08", "basn6a16", "bgai4a08", "bgai4a16",
        "bgan6a08", "bgan6a16", "bgbn4a08", "bggn4a16", "bgwn6a08", "bgyn6a16", "ccwn2c08", "ccwn3p08",
        "cdfn2c08", "cdhn2c08", "cdsn2c08", "cdun2c08", "ch1n3p04", "ch2n3p08", "cm0n0g04", "cm7n0g04",
        "cm9n0g04", "cs3n2c16", "cs3n3p08", "cs5n2c08", "cs5n3p08", "cs8n2c08", "cs8n3p08", "ct0n0g04",
        "ct1n0g04", "cten0g04", "ctfn0g04", "ctgn0g04", "cthn0g04", "ctjn0g04", "ctzn0g04", "exif2c08",
        "f00n0g08", "f00n2c08", "f01n0g08", "f01n2c08", "f02n0g08", "f02n2c08", "f03n0g08", "f03n2c08",
        "f04n0g08", "f04n2c08", "f99n0g04", "g03n0g16", "g03n2c08", "g03n3p04", "g04n0g16", "g04n2c08",
        "g04n3p04", "g05n0g16", "g05n2c08", "g05n3p04", "g07n0g16", "g07n2c08", "g07n3p04", "g10n0g16",
        "g10n2c08", "g10n3p04", "g25n0g16", "g25n2c08", "g25n3p04", "oi1n0g16", "oi1n2c16", "oi2n0g16",
        "oi2n2c16", "oi4n0g16", "oi4n2c16", "oi9n0g16", "oi9n2c16", "pp0n2c16", "pp0n6a08", "ps1n0g08",
        "ps1n2c16", "ps2n0g08", "ps2n2c16", "s01i3p01", "s01n3p01", "s02i3p01", "s02n3p01", "s03i3p01",
        "s03n3p01", "s04i3p01", "s04n3p01", "s05i3p02", "s05n3p02", "s06i3p02", "s06n3p02", "s07i3p02",
        "s07n3p02", "s08i3p02", "s08n3p02", "s09i3p02", "s09n3p02", "s32i3p04", "s32n3p04", "s33i3p04",
        "s33n3p04", "s34i3p04", "s34n3p04", "s35i3p04", "s35n3p04", "s36i3p04", "s36n3p04", "s37i3p04",
        "s37n3p04", "s38i3p04", "s38n3p04", "s39i3p04", "s39n3p04", "s40i3p04", "s40n3p04", "tbbn0g04",
        "tbbn2c16", "tbbn3p08", "tbgn2c16", "tbgn3p08", "tbrn2c08", "tbwn0g16", "tbwn3p08", "tbyn3p08",
        "tm3n3p02", "tp0n0g08", "tp0n2c08", "tp0n3p08", "tp1n3p08", "z00n2c08", "z03n2c08", "z06n2c08",
        "z09n2c08"
    })
    void testRoundTrip(String filename) throws IOException {
        BufferedImage source = readImage("/png/" + filename + ".png");
        byte[] expected = decode(source);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PngFormat format = fromImage(source);
        Optimizer optimizer = new Optimizer(format);
        byte[] optimized = optimizer.optimize(expected);
        try (PngEncoder encoder = new PngEncoder(out, optimizer.format())) {
            encoder.writeHeader().writeImage(optimized);
        }

        byte[] encoded = out.toByteArray();
        byte[] actual = decode(ImageIO.read(new ByteArrayInputStream(encoded)));

        assertThat(actual).isEqualTo(expected);
    }

    private byte[] decode(BufferedImage image) {
        DataBuffer buffer = image.getData().getDataBuffer();
        if (buffer instanceof DataBufferByte) {
            byte[] bytes = ((DataBufferByte) buffer).getData();
            if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
                byte[] result = new byte[bytes.length];
                for (int i = 0; i < bytes.length; i += 3) {
                    result[i] = bytes[i + 2];
                    result[i + 1] = bytes[i + 1];
                    result[i + 2] = bytes[i];
                }
                return result;
            }
            if (image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
                byte[] result = new byte[bytes.length];
                for (int i = 0; i < bytes.length; i += 4) {
                    result[i] = bytes[i + 3];
                    result[i + 1] = bytes[i + 2];
                    result[i + 2] = bytes[i + 1];
                    result[i + 3] = bytes[i];
                }
                return result;
            }
            return bytes;
        }
        if (buffer instanceof DataBufferUShort) {
            short[] data = ((DataBufferUShort) buffer).getData();
            byte[] result = new byte[data.length * 2];
            ByteBuffer.wrap(result).asShortBuffer().put(data);
            return result;
        }
        throw new IllegalArgumentException("Unsupported data buffer: " + buffer.getClass());
    }

    private BufferedImage readImage(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(name)) {
            return ImageIO.read(in);
        }
    }

    private PngFormat fromImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        switch (image.getType()) {
            case BufferedImage.TYPE_CUSTOM:
                if (image.getColorModel().getNumComponents() == 2 && image.getColorModel().getPixelSize() == 16) {
                    return PngFormat.of(width, height, BitDepth.EIGHT, ColorType.GRAY_ALPHA);
                }
                if (image.getColorModel().getNumComponents() == 2 && image.getColorModel().getPixelSize() == 32) {
                    return PngFormat.of(width, height, BitDepth.SIXTEEN, ColorType.GRAY_ALPHA);
                }
                if (image.getColorModel().getNumComponents() == 3 && image.getColorModel().getPixelSize() == 48) {
                    return PngFormat.of(width, height, BitDepth.SIXTEEN, ColorType.RGB);
                }
                if (image.getColorModel().getNumComponents() == 4 && image.getColorModel().getPixelSize() == 64) {
                    return PngFormat.of(width, height, BitDepth.SIXTEEN, ColorType.RGB_ALPHA);
                }
                throw new IllegalArgumentException("Unsupported custom image type: " + image.getColorModel());
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_4BYTE_ABGR:
                return PngFormat.of(width, height, BitDepth.EIGHT, ColorType.RGB_ALPHA);
            case BufferedImage.TYPE_3BYTE_BGR:
                return PngFormat.of(width, height, BitDepth.EIGHT, ColorType.RGB);
            case BufferedImage.TYPE_BYTE_GRAY:
                return PngFormat.of(width, height, BitDepth.EIGHT, ColorType.GRAY);
            case BufferedImage.TYPE_USHORT_GRAY:
                return PngFormat.of(width, height, BitDepth.SIXTEEN, ColorType.GRAY);
            case BufferedImage.TYPE_BYTE_BINARY:
                return PngFormat.of(width, height, fromDepth(image.getColorModel().getPixelSize()), ColorType.GRAY);
            case BufferedImage.TYPE_BYTE_INDEXED:
                return PngFormat.indexed(width, height, BitDepth.EIGHT, paletteFrom(image.getColorModel()));
            default:
                throw new IllegalArgumentException("Unsupported image type: " + image.getType());
        }
    }

    private PngPalette paletteFrom(ColorModel cm) {
        IndexColorModel icm = (IndexColorModel) cm;
        byte[] reds = new byte[icm.getMapSize()];
        byte[] greens = new byte[icm.getMapSize()];
        byte[] blues = new byte[icm.getMapSize()];

        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);

        List<PngPalette.Color> colors = new ArrayList<>();
        for (int i = 0; i < icm.getMapSize(); i++) {
            colors.add(new PngPalette.Color(
                Byte.toUnsignedInt(reds[i]),
                Byte.toUnsignedInt(greens[i]),
                Byte.toUnsignedInt(blues[i])
            ));
        }
        return new PngPalette(colors);
    }

    private static BitDepth fromDepth(int pixelSize) {
        switch (pixelSize) {
            case 1:
                return BitDepth.ONE;
            case 2:
                return BitDepth.TWO;
            case 4:
                return BitDepth.FOUR;
            case 8:
                return BitDepth.EIGHT;
            default:
                throw new IllegalArgumentException("Unsupported indexed image depth: " + pixelSize);
        }
    }
}
