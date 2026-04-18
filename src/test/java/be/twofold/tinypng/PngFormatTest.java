package be.twofold.tinypng;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;

class PngFormatTest {

    @Test
    void testThrowsOnZeroOrNegativeWidth() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(0, 1, ColorType.GRAY, 8))
            .withMessage("width must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(-1, 1, ColorType.GRAY, 8))
            .withMessage("width must be greater than 0");
    }

    @Test
    void testThrowsOnZeroOrNegativeHeight() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, 0, ColorType.GRAY, 8))
            .withMessage("height must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, -1, ColorType.GRAY, 8))
            .withMessage("height must be greater than 0");
    }

    @Test
    void testThrowsOnNullColorType() {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, null, 8))
            .withMessage("colorType must not be null");
    }

    @Test
    void testThrowsOnInvalidBitDepth() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, 1, ColorType.GRAY, 3))
            .withMessage("Invalid bit depth: 3");
    }

    @ParameterizedTest
    @MethodSource("provideBitDepthAndColorTypeCombinations")
    void testThrowsOnInvalidBitDepthAndColourTypeCombination(int bitDepth, ColorType colorType, boolean valid) {
        if (valid) {
            assertThatNoException()
                .isThrownBy(() -> createFormat(colorType, bitDepth));
        } else {
            assertThatExceptionOfType(PngException.class)
                .isThrownBy(() -> createFormat(colorType, bitDepth))
                .withMessage("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
    }

    private static PngFormat createFormat(ColorType colorType, int bitDepth) {
        if (colorType == ColorType.INDEXED && bitDepth != 16) {
            return PngFormat.indexed(1, 1, bitDepth, new byte[]{0, 0, 0});
        } else {
            return PngFormat.of(1, 1, colorType, bitDepth);
        }
    }

    private static Stream<Arguments> provideBitDepthAndColorTypeCombinations() {
        return Stream.of(
            Arguments.of(ColorType.GRAY, 1, true),
            Arguments.of(ColorType.RGB, 1, false),
            Arguments.of(ColorType.INDEXED, 1, true),
            Arguments.of(ColorType.GRAY_ALPHA, 1, false),
            Arguments.of(ColorType.RGB_ALPHA, 1, false),
            Arguments.of(ColorType.GRAY, 2, true),
            Arguments.of(ColorType.RGB, 2, false),
            Arguments.of(ColorType.INDEXED, 2, true),
            Arguments.of(ColorType.GRAY_ALPHA, 2, false),
            Arguments.of(ColorType.RGB_ALPHA, 2, false),
            Arguments.of(ColorType.GRAY, 4, true),
            Arguments.of(ColorType.RGB, 4, false),
            Arguments.of(ColorType.INDEXED, 4, true),
            Arguments.of(ColorType.GRAY_ALPHA, 4, false),
            Arguments.of(ColorType.RGB_ALPHA, 4, false),
            Arguments.of(ColorType.GRAY, 8, true),
            Arguments.of(ColorType.RGB, 8, true),
            Arguments.of(ColorType.INDEXED, 8, true),
            Arguments.of(ColorType.GRAY_ALPHA, 8, true),
            Arguments.of(ColorType.RGB_ALPHA, 8, true),
            Arguments.of(ColorType.GRAY, 16, true),
            Arguments.of(ColorType.RGB, 16, true),
            Arguments.of(ColorType.INDEXED, 16, false),
            Arguments.of(ColorType.GRAY_ALPHA, 16, true),
            Arguments.of(ColorType.RGB_ALPHA, 16, true)
        );
    }
}
