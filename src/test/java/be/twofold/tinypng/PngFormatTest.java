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
            .isThrownBy(() -> PngFormat.of(0, 1, BitDepth.EIGHT, ColorType.GRAYSCALE))
            .withMessage("width must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(-1, 1, BitDepth.EIGHT, ColorType.GRAYSCALE))
            .withMessage("width must be greater than 0");
    }

    @Test
    void testThrowsOnZeroOrNegativeHeight() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, 0, BitDepth.EIGHT, ColorType.GRAYSCALE))
            .withMessage("height must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, -1, BitDepth.EIGHT, ColorType.GRAYSCALE))
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
            .isThrownBy(() -> PngFormat.of(1, 1, null, ColorType.GRAYSCALE))
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
            Arguments.of(BitDepth.ONE, ColorType.GRAYSCALE, true),
            Arguments.of(BitDepth.ONE, ColorType.TRUECOLOR, false),
            Arguments.of(BitDepth.ONE, ColorType.INDEXED, true),
            Arguments.of(BitDepth.ONE, ColorType.GRAYSCALE_ALPHA, false),
            Arguments.of(BitDepth.ONE, ColorType.TRUECOLOR_ALPHA, false),
            Arguments.of(BitDepth.TWO, ColorType.GRAYSCALE, true),
            Arguments.of(BitDepth.TWO, ColorType.TRUECOLOR, false),
            Arguments.of(BitDepth.TWO, ColorType.INDEXED, true),
            Arguments.of(BitDepth.TWO, ColorType.GRAYSCALE_ALPHA, false),
            Arguments.of(BitDepth.TWO, ColorType.TRUECOLOR_ALPHA, false),
            Arguments.of(BitDepth.FOUR, ColorType.GRAYSCALE, true),
            Arguments.of(BitDepth.FOUR, ColorType.TRUECOLOR, false),
            Arguments.of(BitDepth.FOUR, ColorType.INDEXED, true),
            Arguments.of(BitDepth.FOUR, ColorType.GRAYSCALE_ALPHA, false),
            Arguments.of(BitDepth.FOUR, ColorType.TRUECOLOR_ALPHA, false),
            Arguments.of(BitDepth.EIGHT, ColorType.GRAYSCALE, true),
            Arguments.of(BitDepth.EIGHT, ColorType.TRUECOLOR, true),
            Arguments.of(BitDepth.EIGHT, ColorType.INDEXED, true),
            Arguments.of(BitDepth.EIGHT, ColorType.GRAYSCALE_ALPHA, true),
            Arguments.of(BitDepth.EIGHT, ColorType.TRUECOLOR_ALPHA, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.GRAYSCALE, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.TRUECOLOR, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.INDEXED, false),
            Arguments.of(BitDepth.SIXTEEN, ColorType.GRAYSCALE_ALPHA, true),
            Arguments.of(BitDepth.SIXTEEN, ColorType.TRUECOLOR_ALPHA, true)
        );
    }
}
