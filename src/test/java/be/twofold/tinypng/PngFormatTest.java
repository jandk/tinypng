package be.twofold.tinypng;

import nl.jqno.equalsverifier.*;
import org.junit.jupiter.api.*;

import java.util.*;

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
        List<Integer> exceptions = Arrays.asList(8, 16);
        for (int i = 0; i < 100; i++) {
            int bitDepth = i;
            if (exceptions.contains(bitDepth)) {
                assertThatNoException()
                    .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, bitDepth));
            } else {
                assertThatIllegalArgumentException()
                    .isThrownBy(() -> new PngFormat(1, 1, PngColorType.Gray, bitDepth))
                    .withMessage("bitDepth must be 8 or 16");
            }
        }
    }

}
