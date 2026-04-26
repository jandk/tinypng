package be.twofold.tinypng;

import nl.jqno.equalsverifier.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;

class PngFormatTest {

    @Test
    void testEqualsAndHashCode() {
        EqualsVerifier
            .forClass(PngFormat.class)
            .verify();
    }

    @Test
    void testOf() {
        PngFormat format = PngFormat.of(10, 20, PngColorType.RGB, 8);

        assertThat(format.getWidth()).isEqualTo(10);
        assertThat(format.getHeight()).isEqualTo(20);
        assertThat(format.getColorType()).isEqualTo(PngColorType.RGB);
        assertThat(format.getBitDepth()).isEqualTo(8);
        assertThat(format.getPalette()).isNull();
        assertThat(format.getTransparency()).isNull();
        assertThat(format.getFrameCount()).isEqualTo(1);
        assertThat(format.getPlayCount()).isEqualTo(0);
    }

    @Test
    void testIndexed() {
        byte[] palette = makeArray(0x00, 0x00, 0x00, 0xFF, 0xFF, 0xFF);
        byte[] transparency = makeArray(0x80);

        PngFormat format = PngFormat.indexed(4, 8, 8, palette, transparency);

        assertThat(format.getWidth()).isEqualTo(4);
        assertThat(format.getHeight()).isEqualTo(8);
        assertThat(format.getColorType()).isEqualTo(PngColorType.INDEXED);
        assertThat(format.getBitDepth()).isEqualTo(8);
        assertThat(format.getPalette()).isEqualTo(palette);
        assertThat(format.getTransparency()).isEqualTo(transparency);
    }

    @Test
    void testIndexedNullTransparency() {
        byte[] palette = makeArray(0x00, 0x00, 0x00);
        assertThatNoException()
            .isThrownBy(() -> PngFormat.indexed(1, 1, 8, palette, null));
    }

    // region validation


    @Test
    void testThrowsOnZeroOrNegativeWidth() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(0, 1, PngColorType.GRAY, 8))
            .withMessage("width must be greater than 0");
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(-1, 1, PngColorType.GRAY, 8))
            .withMessage("width must be greater than 0");
    }

    @Test
    void testThrowsOnZeroOrNegativeHeight() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(1, 0, PngColorType.GRAY, 8))
            .withMessage("height must be greater than 0");
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(1, -1, PngColorType.GRAY, 8))
            .withMessage("height must be greater than 0");
    }

    // endregion

    // region validateColorAndDepth

    @Test
    void testThrowsOnNullColorType() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, null, 8))
            .withMessage("colorType must not be null");
    }

    @Test
    void testThrowsOnInvalidBitDepth() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, PngColorType.GRAY, 3))
            .withMessage("Invalid bit depth 3 for color type GRAY");
    }

    @ParameterizedTest
    @MethodSource("provideBitDepthAndColorTypeCombinations")
    void testThrowsOnInvalidBitDepthAndColourTypeCombination(PngColorType colorType, int bitDepth, boolean valid) {
        if (valid) {
            assertThatNoException()
                .isThrownBy(() -> createFormat(colorType, bitDepth));
        } else {
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> createFormat(colorType, bitDepth))
                .withMessage("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
    }

    private static Stream<Arguments> provideBitDepthAndColorTypeCombinations() {
        return Stream.of(
            Arguments.of(PngColorType.GRAY, 1, true),
            Arguments.of(PngColorType.RGB, 1, false),
            Arguments.of(PngColorType.INDEXED, 1, true),
            Arguments.of(PngColorType.GRAY_ALPHA, 1, false),
            Arguments.of(PngColorType.RGB_ALPHA, 1, false),
            Arguments.of(PngColorType.GRAY, 2, true),
            Arguments.of(PngColorType.RGB, 2, false),
            Arguments.of(PngColorType.INDEXED, 2, true),
            Arguments.of(PngColorType.GRAY_ALPHA, 2, false),
            Arguments.of(PngColorType.RGB_ALPHA, 2, false),
            Arguments.of(PngColorType.GRAY, 4, true),
            Arguments.of(PngColorType.RGB, 4, false),
            Arguments.of(PngColorType.INDEXED, 4, true),
            Arguments.of(PngColorType.GRAY_ALPHA, 4, false),
            Arguments.of(PngColorType.RGB_ALPHA, 4, false),
            Arguments.of(PngColorType.GRAY, 8, true),
            Arguments.of(PngColorType.RGB, 8, true),
            Arguments.of(PngColorType.INDEXED, 8, true),
            Arguments.of(PngColorType.GRAY_ALPHA, 8, true),
            Arguments.of(PngColorType.RGB_ALPHA, 8, true),
            Arguments.of(PngColorType.GRAY, 16, true),
            Arguments.of(PngColorType.RGB, 16, true),
            Arguments.of(PngColorType.INDEXED, 16, false),
            Arguments.of(PngColorType.GRAY_ALPHA, 16, true),
            Arguments.of(PngColorType.RGB_ALPHA, 16, true)
        );
    }

    // endregion

    // region validatePalette

    @Test
    void testWithPaletteChangesColorTypeToIndexed() {
        byte[] palette = makeArray(0x00, 0x00, 0x00, 0xFF, 0x00, 0x00);
        PngFormat format = PngFormat.of(4, 4, PngColorType.GRAY, 8).withPalette(8, palette, null);

        assertThat(format.getColorType()).isEqualTo(PngColorType.INDEXED);
        assertThat(format.getPalette()).isEqualTo(palette);
        assertThat(format.getWidth()).isEqualTo(4);
        assertThat(format.getHeight()).isEqualTo(4);
    }

    @Test
    void testThrowsOnNullPaletteForIndexed() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.indexed(1, 1, 8, null, null))
            .withMessage("palette must not be null for colorType INDEXED");
    }

    @Test
    void testThrowsOnEmptyPalette() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.indexed(1, 1, 8, new byte[0], null))
            .withMessage("palette length must be a non-zero multiple of 3");
    }

    @Test
    void testThrowsOnPaletteLengthNotMultipleOfThree() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.indexed(1, 1, 8, new byte[]{0, 0}, null))
            .withMessage("palette length must be a non-zero multiple of 3");
    }

    @Test
    void testThrowsOnPaletteTooLargeForBitDepth() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.indexed(1, 1, 1, new byte[9], null))
            .withMessage("palette length must not be greater than 6");
    }

    @Test
    void testThrowsOnEmptyTransparency() {
        byte[] palette = makeArray(0, 0, 0);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.indexed(1, 1, 8, palette, new byte[0]))
            .withMessage("transparency length must be between 1 and palette length");
    }

    @Test
    void testThrowsOnTransparencyLongerThanPalette() {
        byte[] palette = makeArray(0, 0, 0);
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.indexed(1, 1, 8, palette, new byte[]{0, 0})) // 2 entries
            .withMessage("transparency length must be between 1 and palette length");
    }

    // endregion

    // region validateAnimation

    @Test
    void testWithAnimationReturnsExpectedFields() {
        PngFormat format = PngFormat.of(1, 1, PngColorType.GRAY, 8).withAnimation(5, 3);

        assertThat(format.getFrameCount()).isEqualTo(5);
        assertThat(format.getPlayCount()).isEqualTo(3);
    }

    @Test
    void testWithAnimationPlayCountZeroMeansInfiniteLoop() {
        PngFormat format = PngFormat.of(1, 1, PngColorType.GRAY, 8).withAnimation(2, 0);

        assertThat(format.getPlayCount()).isEqualTo(0);
    }

    @Test
    void testThrowsOnZeroFrameCount() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, PngColorType.GRAY, 8).withAnimation(0, 0))
            .withMessage("frameCount must be greater than 0");
    }

    @Test
    void testThrowsOnNegativeFrameCount() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, PngColorType.GRAY, 8).withAnimation(-1, 0))
            .withMessage("frameCount must be greater than 0");
    }

    @Test
    void testThrowsOnNegativePlayCount() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, PngColorType.GRAY, 8).withAnimation(2, -1))
            .withMessage("playCount must be non-negative");
    }

    // endregion

    // region Custom getters

    @Test
    void testGetBytesPerChannelReturnsCorrectValues() {
        assertThat(PngFormat.of(1, 1, PngColorType.GRAY, 1).getBytesPerChannel()).isEqualTo(1);
        assertThat(PngFormat.of(1, 1, PngColorType.GRAY, 8).getBytesPerChannel()).isEqualTo(1);
        assertThat(PngFormat.of(1, 1, PngColorType.GRAY, 16).getBytesPerChannel()).isEqualTo(2);
    }

    @Test
    void testGetBytesPerPixelReturnsCorrectValues() {
        assertThat(PngFormat.of(1, 1, PngColorType.GRAY, 8).getBytesPerPixel()).isEqualTo(1);
        assertThat(PngFormat.of(1, 1, PngColorType.RGB, 8).getBytesPerPixel()).isEqualTo(3);
        assertThat(PngFormat.of(1, 1, PngColorType.GRAY_ALPHA, 8).getBytesPerPixel()).isEqualTo(2);
        assertThat(PngFormat.of(1, 1, PngColorType.RGB_ALPHA, 8).getBytesPerPixel()).isEqualTo(4);
        assertThat(PngFormat.of(1, 1, PngColorType.RGB, 16).getBytesPerPixel()).isEqualTo(6);
    }

    @Test
    void testGetBytesPerRowReturnsCorrectValues() {
        assertThat(PngFormat.of(10, 1, PngColorType.RGB, 8).getBytesPerRow()).isEqualTo(30);
        assertThat(PngFormat.of(8, 1, PngColorType.GRAY, 1).getBytesPerRow()).isEqualTo(1);
        assertThat(PngFormat.of(9, 1, PngColorType.GRAY, 1).getBytesPerRow()).isEqualTo(2);
    }

    @Test
    void testGetBytesPerImageReturnsRowTimesHeight() {
        PngFormat format = PngFormat.of(10, 20, PngColorType.RGB, 8);

        assertThat(format.getBytesPerImage()).isEqualTo(600);
    }

    // endregion

    private static void createFormat(PngColorType colorType, int bitDepth) {
        if (colorType == PngColorType.INDEXED) {
            PngFormat.indexed(1, 1, bitDepth, new byte[]{0, 0, 0}, null);
        } else {
            PngFormat.of(1, 1, colorType, bitDepth);
        }
    }

    private static byte[] makeArray(int... ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }
}
