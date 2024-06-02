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
            .isThrownBy(() -> PngFormat.of(0, 1, PngColorType.Gray, PngBitDepth.Eight))
            .withMessage("width must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(-1, 1, PngColorType.Gray, PngBitDepth.Eight))
            .withMessage("width must be greater than 0");
    }

    @Test
    void testThrowsOnZeroOrNegativeHeight() {
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, 0, PngColorType.Gray, PngBitDepth.Eight))
            .withMessage("height must be greater than 0");
        assertThatExceptionOfType(PngException.class)
            .isThrownBy(() -> PngFormat.of(1, -1, PngColorType.Gray, PngBitDepth.Eight))
            .withMessage("height must be greater than 0");
    }

    @Test
    void testThrowsOnNullColorType() {
        assertThatNullPointerException()
            .isThrownBy(() -> PngFormat.of(1, 1, null, PngBitDepth.Eight))
            .withMessage("colorType must not be null");
    }

    @Test
    void testThrowsOnNullBitDepth() {
        assertThatNullPointerException()
            .isThrownBy(() -> PngFormat.of(1, 1, PngColorType.Gray, null))
            .withMessage("bitDepth must not be null");
    }

    @ParameterizedTest
    @MethodSource("provideBitDepthAndColorTypeCombinations")
    void testThrowsOnInvalidBitDepthAndColourTypeCombination(PngBitDepth bitDepth, PngColorType colorType, boolean valid) {
        if (valid) {
            assertThatNoException()
                .isThrownBy(() -> createFormat(bitDepth, colorType));
        } else {
            assertThatExceptionOfType(PngException.class)
                .isThrownBy(() -> createFormat(bitDepth, colorType))
                .withMessage("Invalid bit depth " + bitDepth + " for color type " + colorType);
        }
    }

    private static PngFormat createFormat(PngBitDepth bitDepth, PngColorType colorType) {
        if (colorType == PngColorType.Indexed && bitDepth != PngBitDepth.Sixteen) {
            PngPalette palette = new PngPalette(List.of(new PngPalette.Color(0, 0, 0)));
            return PngFormat.indexed(1, 1, palette);
        } else {
            return PngFormat.of(1, 1, colorType, bitDepth);
        }
    }

    private static Stream<Arguments> provideBitDepthAndColorTypeCombinations() {
        return Stream.of(
            Arguments.of(PngBitDepth.One, PngColorType.Gray, true),
            Arguments.of(PngBitDepth.One, PngColorType.Rgb, false),
            Arguments.of(PngBitDepth.One, PngColorType.Indexed, true),
            Arguments.of(PngBitDepth.One, PngColorType.GrayAlpha, false),
            Arguments.of(PngBitDepth.One, PngColorType.RgbAlpha, false),
            Arguments.of(PngBitDepth.Two, PngColorType.Gray, true),
            Arguments.of(PngBitDepth.Two, PngColorType.Rgb, false),
            Arguments.of(PngBitDepth.Two, PngColorType.Indexed, true),
            Arguments.of(PngBitDepth.Two, PngColorType.GrayAlpha, false),
            Arguments.of(PngBitDepth.Two, PngColorType.RgbAlpha, false),
            Arguments.of(PngBitDepth.Four, PngColorType.Gray, true),
            Arguments.of(PngBitDepth.Four, PngColorType.Rgb, false),
            Arguments.of(PngBitDepth.Four, PngColorType.Indexed, true),
            Arguments.of(PngBitDepth.Four, PngColorType.GrayAlpha, false),
            Arguments.of(PngBitDepth.Four, PngColorType.RgbAlpha, false),
            Arguments.of(PngBitDepth.Eight, PngColorType.Gray, true),
            Arguments.of(PngBitDepth.Eight, PngColorType.Rgb, true),
            Arguments.of(PngBitDepth.Eight, PngColorType.Indexed, true),
            Arguments.of(PngBitDepth.Eight, PngColorType.GrayAlpha, true),
            Arguments.of(PngBitDepth.Eight, PngColorType.RgbAlpha, true),
            Arguments.of(PngBitDepth.Sixteen, PngColorType.Gray, true),
            Arguments.of(PngBitDepth.Sixteen, PngColorType.Rgb, true),
            Arguments.of(PngBitDepth.Sixteen, PngColorType.Indexed, false),
            Arguments.of(PngBitDepth.Sixteen, PngColorType.GrayAlpha, true),
            Arguments.of(PngBitDepth.Sixteen, PngColorType.RgbAlpha, true)
        );
    }
}
