package be.twofold.tinypng;

import nl.jqno.equalsverifier.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class PngFormatTest {

    @Test
    void testEqualsAndHashCode() {
        EqualsVerifier
            .forClass(PngFormat.class)
            .suppress(Warning.NULL_FIELDS)
            .verify();
    }

    @Test
    void testThrowsOnZeroOrNegativeWidth() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(0, 1, PngColorType.Gray))
            .withMessage("width must be greater than 0");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(-1, 1, PngColorType.Gray))
            .withMessage("width must be greater than 0");
    }

    @Test
    void testThrowsOnZeroOrNegativeHeight() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(1, 0, PngColorType.Gray))
            .withMessage("height must be greater than 0");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(1, -1, PngColorType.Gray))
            .withMessage("height must be greater than 0");
    }

    @Test
    void testThrowsOnNullColorType() {
        assertThatNullPointerException()
            .isThrownBy(() -> new PngFormat(1, 1, null))
            .withMessage("colorType must not be null");
    }

    @Test
    void testThrowsOnBitDepthNot8Or16() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, PngBitDepth.One))
            .withMessage("bitDepth must be 8 or 16");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, PngBitDepth.Two))
            .withMessage("bitDepth must be 8 or 16");
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, PngBitDepth.Four))
            .withMessage("bitDepth must be 8 or 16");
        assertThatNoException()
            .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, PngBitDepth.Eight));
        assertThatNoException()
            .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, PngBitDepth.Sixteen));
    }
}
