package be.twofold.tinypng;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.*;
import java.util.stream.*;

import static org.assertj.core.api.Assertions.*;

class PngFormatTest {

    @Test
    void testThrowsOnZeroOrNegativeWidth() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(0, 1, BitDepth.EIGHT, ColorType.GRAY))
            .withMessage("width must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(-1, 1, BitDepth.EIGHT, ColorType.GRAY))
            .withMessage("width must be greater than 0");
    }

    @Test
    void testThrowsOnZeroOrNegativeHeight() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, 0, BitDepth.EIGHT, ColorType.GRAY))
            .withMessage("height must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, -1, BitDepth.EIGHT, ColorType.GRAY))
            .withMessage("height must be greater than 0");
    }

    @Test
    void testThrowsOnNullColorType() {
        assertThatNullPointerException()
            .isThrownBy(() -> PngFormat.of(1, 1, BitDepth.EIGHT, null))
            .withMessage("colorType must not be null");
    }

    @Test
    void testThrowsOnNullBitDepth() {
        assertThatNullPointerException()
            .isThrownBy(() -> PngFormat.of(1, 1, null, ColorType.GRAY))
            .withMessage("bitDepth must not be null");
    }

    @ParameterizedTest
    @MethodSource("provideBitDepthAndColorTypeCombinations")
    void testThrowsOnInvalidBitDepthAndColourTypeCombination(BitDepth bitDepth, ColorType colorType, boolean valid) {
        if (valid) {
            assertThatNoException()
                .isThrownBy(() -> createFormat(bitDepth, colorType));
        } else {
            assertThatExceptionOfType(PngException.class)
                .isThrownBy(() -> createFormat(bitDepth, colorType))
                .withMessage("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
    }

    private static PngFormat createFormat(BitDepth bitDepth, ColorType colorType) {
        if (colorType == ColorType.INDEXED && bitDepth != BitDepth.SIXTEEN) {
            PngPalette palette = new PngPalette(List.of(new PngPalette.Color(0, 0, 0)));
            return PngFormat.indexed(1, 1, BitDepth.EIGHT, palette);
        } else {
            return PngFormat.of(1, 1, bitDepth, colorType);
        }
    }

    private static Stream<Arguments> provideBitDepthAndColorTypeCombinations() {
        return Stream.of(
            Arguments.of(BitDepth.ONE, ColorType.GRAY, true),
            Arguments.of(BitDepth.ONE, ColorType.RGB, false),
            Arguments.of(BitDepth.ONE, ColorType.INDEXED, true),
            Arguments.of(BitDepth.ONE, ColorType.GRAY_ALPHA, false),
            Arguments.of(BitDepth.ONE, ColorType.RGB_ALPHA, false),
            Arguments.of(BitDepth.TWO, ColorType.GRAY, true),
            Arguments.of(BitDepth.TWO, ColorType.RGB, false),
            Arguments.of(BitDepth.TWO, ColorType.INDEXED, true),
            Arguments.of(BitDepth.TWO, ColorType.GRAY_ALPHA, false),
            Arguments.of(BitDepth.TWO, ColorType.RGB_ALPHA, false),
            Arguments.of(BitDepth.FOUR, ColorType.GRAY, true),
            Arguments.of(BitDepth.FOUR, ColorType.RGB, false),
            Arguments.of(BitDepth.FOUR, ColorType.INDEXED, true),
            Arguments.of(BitDepth.FOUR, ColorType.GRAY_ALPHA, false),
            Arguments.of(BitDepth.FOUR, ColorType.RGB_ALPHA, false),
            Arguments.of(BitDepth.EIGHT, ColorType.GRAY, true),
            Arguments.of(BitDepth.EIGHT, ColorType.RGB, true),
            Arguments.of(BitDepth.EIGHT, ColorType.INDEXED, true),
            Arguments.of(BitDepth.EIGHT, ColorType.GRAY_ALPHA, true),
            Arguments.of(BitDepth.EIGHT, ColorType.RGB_ALPHA, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.GRAY, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.RGB, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.INDEXED, false),
            Arguments.of(BitDepth.SIXTEEN, ColorType.GRAY_ALPHA, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.RGB_ALPHA, true)
        );
    }
}
