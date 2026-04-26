package be.twofold.tinypng;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class OptimizerTest {

    @Nested
    class Reduce16To8 {
        @Test
        void skipsWhenBitDepthIsNot16() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x12, 0x34);

            opt.reduceBitDepth16To8();

            assertThat(opt.format()).isEqualTo(format);
            assertThat(data(opt)).containsExactly(0x12, 0x34);
        }

        @Test
        void skipsWhenAnyPairDiffers() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.GRAY, 16);
            Optimizer opt = withData(format,
                0x12, 0x34, 0x56, 0x56);

            opt.reduceBitDepth16To8();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void reducesWhenAllPairsMatch() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.GRAY, 16);
            Optimizer opt = withData(format,
                0x12, 0x12, 0x34, 0x34);

            opt.reduceBitDepth16To8();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.GRAY);
            assertThat(opt.format().getBitDepth()).isEqualTo(8);
            assertThat(data(opt)).containsExactly(0x12, 0x34);
        }
    }

    @Nested
    class ReduceToGray {
        @Test
        void skipsWhenNotRgb() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.GRAY_ALPHA, 8);
            Optimizer opt = withData(format,
                0x80, 0xFF, 0x90, 0xFF);

            opt.reduceToGray();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void skipsWhenChannelsDiffer() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB, 8);
            Optimizer opt = withData(format,
                0x10, 0x20, 0x10, 0x30, 0x30, 0x30);

            opt.reduceToGray();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void reducesRgbToGrayWhenChannelsEqual() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB, 8);
            Optimizer opt = withData(format,
                0x80, 0x80, 0x80, 0x90, 0x90, 0x90);

            opt.reduceToGray();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.GRAY);
            assertThat(data(opt)).containsExactly(0x80, 0x90);
        }

        @Test
        void reducesRgbaToGrayAlphaWhenChannelsEqual() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB_ALPHA, 8);
            Optimizer opt = withData(format,
                0x80, 0x80, 0x80, 0x7F, 0x90, 0x90, 0x90, 0xFF);

            opt.reduceToGray();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.GRAY_ALPHA);
            assertThat(data(opt)).containsExactly(0x80, 0x7F, 0x90, 0xFF);
        }

        @Test
        void reducesRgb16ToGray16WhenChannelsEqual() {
            PngFormat format = PngFormat.of(1, 1, PngColorType.RGB, 16);
            Optimizer opt = withData(format,
                0x12, 0x34, 0x12, 0x34, 0x12, 0x34);

            opt.reduceToGray();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.GRAY);
            assertThat(opt.format().getBitDepth()).isEqualTo(16);
            assertThat(data(opt)).containsExactly(0x12, 0x34);
        }
    }

    @Nested
    class ReduceToOpaque {
        @Test
        void skipsWhenNotAlpha() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB, 8);
            Optimizer opt = withData(format,
                0x10, 0x20, 0x30, 0x40, 0x50, 0x60);

            opt.reduceToOpaque();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void skipsWhenAnyAlphaBelowFull() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB_ALPHA, 8);
            Optimizer opt = withData(format,
                0x10, 0x20, 0x30, 0x80, 0x40, 0x50, 0x60, 0xFF);

            opt.reduceToOpaque();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void reducesRgbAlphaToRgbWhenAllOpaque() {
            Optimizer opt = withData(PngFormat.of(2, 1, PngColorType.RGB_ALPHA, 8),
                0x10, 0x20, 0x30, 0xFF, 0x40, 0x50, 0x60, 0xFF);

            opt.reduceToOpaque();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.RGB);
            assertThat(data(opt)).containsExactly(0x10, 0x20, 0x30, 0x40, 0x50, 0x60);
        }

        @Test
        void reducesGrayAlphaToGrayWhenAllOpaque() {
            Optimizer opt = withData(PngFormat.of(2, 1, PngColorType.GRAY_ALPHA, 8),
                0x80, 0xFF, 0x90, 0xFF);

            opt.reduceToOpaque();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.GRAY);
            assertThat(data(opt)).containsExactly(0x80, 0x90);
        }

        @Test
        void reducesRgbAlpha16ToRgb16WhenAllOpaque() {
            Optimizer opt = withData(PngFormat.of(1, 1, PngColorType.RGB_ALPHA, 16),
                0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0xFF, 0xFF);

            opt.reduceToOpaque();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.RGB);
            assertThat(opt.format().getBitDepth()).isEqualTo(16);
            assertThat(data(opt)).containsExactly(0x10, 0x20, 0x30, 0x40, 0x50, 0x60);
        }
    }

    @Nested
    class ReduceToPalette {
        @Test
        void skipsWhenBitDepthNot8() {
            PngFormat format = PngFormat.of(1, 1, PngColorType.RGB, 16);
            Optimizer opt = withData(format,
                0x10, 0x10, 0x20, 0x20, 0x30, 0x30);

            opt.reduceToPalette();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void skipsWhenAlreadyIndexed() {
            PngFormat format = PngFormat.indexed(2, 1, 8, palette(2), null);
            Optimizer opt = withData(format,
                0x00, 0x01);

            opt.reduceToPalette();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void skipsWhenNotRgbOrRgbAlpha() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x80, 0x90);

            opt.reduceToPalette();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void skipsWhenTooManyColors() {
            int[] data = new int[257 * 3];
            for (int i = 0; i < 257; i++) {
                data[i * 3] = i & 0xFF;
                data[i * 3 + 1] = i >> 8;
            }

            PngFormat format = PngFormat.of(257, 1, PngColorType.RGB, 8);
            Optimizer opt = withData(format, data);

            opt.reduceToPalette();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void reducesRgbToIndexed() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB, 8);
            Optimizer opt = withData(format,
                0x10, 0x20, 0x30, 0x40, 0x50, 0x60);

            opt.reduceToPalette();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.INDEXED);
            assertThat(opt.format().getPalette()).containsExactly(0x10, 0x20, 0x30, 0x40, 0x50, 0x60);
            assertThat(opt.format().getTransparency()).isNull();
            assertThat(data(opt)).containsExactly(0, 1);
        }

        @Test
        void reducesRgbaToIndexedAndSortsTransparentColorsFirst() {
            Optimizer opt = withData(PngFormat.of(2, 1, PngColorType.RGB_ALPHA, 8),
                0x10, 0x20, 0x30, 0xFF, 0x40, 0x50, 0x60, 0x80);

            opt.reduceToPalette();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.INDEXED);
            assertThat(opt.format().getPalette()).containsExactly(0x40, 0x50, 0x60, 0x10, 0x20, 0x30);
            assertThat(opt.format().getTransparency()).containsExactly(0x80);
            assertThat(data(opt)).containsExactly(1, 0);
        }

        @Test
        void noTransparencyWhenAllPixelsOpaque() {
            Optimizer opt = withData(PngFormat.of(2, 1, PngColorType.RGB_ALPHA, 8),
                0x10, 0x20, 0x30, 0xFF, 0x40, 0x50, 0x60, 0xFF);

            opt.reduceToPalette();

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.INDEXED);
            assertThat(opt.format().getTransparency()).isNull();
        }
    }

    @Nested
    class ReduceBitDepth {
        @Test
        void skipsWhenNotGrayOrIndexed() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB, 8);
            Optimizer opt = withData(format,
                0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF);

            opt.reduceBitDepth();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void skipsGray8WhenValuesNotDivisible() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x00, 0x01);

            opt.reduceBitDepth();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void reducesIndexed8To1BitWhenTwoColors() {
            PngFormat format = PngFormat.indexed(3, 1, 8, palette(2), null);
            Optimizer opt = withData(format,
                0x00, 0x01, 0x01);

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(1);
            assertThat(data(opt)).containsExactly(0x60);
        }

        @Test
        void reducesIndexed8To2BitWhenFourColors() {
            PngFormat format = PngFormat.indexed(3, 1, 8, palette(4), null);
            Optimizer opt = withData(format,
                0x00, 0x01, 0x02);

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(2);
            assertThat(data(opt)).containsExactly(0x18);
        }

        @Test
        void reducesIndexed8To4BitWhenFiveColors() {
            PngFormat format = PngFormat.indexed(3, 1, 8, palette(5), null);
            Optimizer opt = withData(format,
                0x00, 0x01, 0x04);

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(4);
            assertThat(data(opt)).containsExactly(0x01, 0x40);
        }

        @Test
        void staysIndexed8WhenPaletteHas17Colors() {
            PngFormat format = PngFormat.indexed(4, 1, 8, palette(17), null);
            Optimizer opt = withData(format,
                0x00, 0x01, 0x02, 0x03);

            opt.reduceBitDepth();

            assertThat(opt.format()).isEqualTo(format);
        }

        @Test
        void reducesGray8To1Bit() {
            PngFormat format = PngFormat.of(3, 1, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x00, 0xFF, 0x00);

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(1);
            assertThat(data(opt)).containsExactly(0x40);
        }

        @Test
        void reducesGray8To2Bit() {
            PngFormat format = PngFormat.of(3, 1, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x00, 0x55, 0xAA);

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(2);
            assertThat(data(opt)).containsExactly(0x18);
        }

        @Test
        void reducesGray8To4Bit() {
            PngFormat format = PngFormat.of(3, 1, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x11, 0x22, 0x33);

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(4);
            assertThat(data(opt)).containsExactly(0x12, 0x30);
        }

        @Test
        void packsEachRowIndependently() {
            PngFormat format = PngFormat.of(3, 2, PngColorType.GRAY, 8);
            Optimizer opt = withData(format,
                0x11, 0x22, 0x33,
                0x22, 0x33, 0x44
            );

            opt.reduceBitDepth();

            assertThat(opt.format().getBitDepth()).isEqualTo(4);
            assertThat(data(opt)).containsExactly(0x12, 0x30, 0x23, 0x40);
        }
    }

    @Nested
    class Optimize {
        @Test
        void fullChainReducesRgbAlpha16GrayOpaqueToGray8() {
            PngFormat format = PngFormat.of(1, 1, PngColorType.RGB_ALPHA, 16);
            Optimizer opt = withOptimize(format,
                0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0xFF, 0xFF);

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.GRAY);
            assertThat(opt.format().getBitDepth()).isEqualTo(8);
            assertThat(data(opt)).containsExactly(0x80);
        }

        @Test
        void reducesRgbWithFewColorsToIndexed() {
            PngFormat format = PngFormat.of(2, 1, PngColorType.RGB, 8);
            Optimizer opt = withOptimize(format,
                0x10, 0x20, 0x30, 0x40, 0x50, 0x60);

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.INDEXED);
            assertThat(opt.format().getBitDepth()).isEqualTo(1);
            assertThat(opt.format().getPalette()).containsExactly(0x10, 0x20, 0x30, 0x40, 0x50, 0x60);
            assertThat(opt.format().getTransparency()).isNull();
            assertThat(data(opt)).containsExactly(0x40);
        }

        @Test
        void reducesRgbaWithTransparencyToIndexed() {
            Optimizer opt = withOptimize(PngFormat.of(2, 1, PngColorType.RGB_ALPHA, 8),
                0x10, 0x20, 0x30, 0xFF, 0x40, 0x50, 0x60, 0x80);

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.INDEXED);
            assertThat(opt.format().getTransparency()).containsExactly(0x80);
        }

        @Test
        void preservesRgbWhenTooManyColorsForPalette() {
            int[] data = new int[257 * 3];
            for (int i = 0; i < 257; i++) {
                data[i * 3] = i & 0xFF;
                data[i * 3 + 1] = i >> 8;
            }

            PngFormat format = PngFormat.of(257, 1, PngColorType.RGB, 8);
            Optimizer opt = withOptimize(format, data);

            assertThat(opt.format().getColorType()).isEqualTo(PngColorType.RGB);
            assertThat(opt.format().getBitDepth()).isEqualTo(8);
        }
    }

    // Helpers

    private Optimizer withData(PngFormat format, int... data) {
        Optimizer opt = new Optimizer(format);
        opt.data = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            opt.data[i] = (byte) data[i];
        }
        return opt;
    }

    private Optimizer withOptimize(PngFormat format, int... data) {
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        Optimizer opt = new Optimizer(format);
        opt.optimize(bytes);
        return opt;
    }

    private byte[] data(Optimizer opt) {
        return Arrays.copyOf(opt.data, opt.format().getBytesPerImage());
    }

    private byte[] palette(int colorCount) {
        return new byte[colorCount * 3];
    }

}
