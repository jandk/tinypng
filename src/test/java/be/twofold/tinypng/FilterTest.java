package be.twofold.tinypng;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class FilterTest {
    private static final PngFormat FORMAT = PngFormat.of(3, 1, PngColorType.GRAY, 8);
    private static final int BPP = FORMAT.getBytesPerPixel();
    private static final int BPR = FORMAT.getBytesPerRow();

    @Test
    void testFirstRowNoneFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.NONE);
        filter.filter(new byte[]{10, 20, 30}, 0);

        assertThat(rowBytes(filter, PngFilter.NONE)).isEqualTo(new byte[]{10, 20, 30});
    }

    @Test
    void testFirstRowSubFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.SUB);
        filter.filter(new byte[]{10, 20, 30}, 0);

        assertThat(rowBytes(filter, PngFilter.SUB)).isEqualTo(new byte[]{10, 10, 10});
    }

    @Test
    void testFirstRowUpFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.UP);
        filter.filter(new byte[]{10, 20, 30}, 0);

        assertThat(rowBytes(filter, PngFilter.UP)).isEqualTo(new byte[]{10, 20, 30});
    }

    @Test
    void testFirstRowAverageFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.AVERAGE);
        filter.filter(new byte[]{10, 20, 30}, 0);

        assertThat(rowBytes(filter, PngFilter.AVERAGE)).isEqualTo(new byte[]{10, 15, 20});
    }

    @Test
    void testFirstRowPaethFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.PAETH);
        filter.filter(new byte[]{10, 20, 30}, 0);

        assertThat(rowBytes(filter, PngFilter.PAETH)).isEqualTo(new byte[]{10, 10, 10});
    }

    @Test
    void testSecondRowUpFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.UP);
        filter.filter(new byte[]{10, 20, 30}, 0);
        filter.filter(new byte[]{40, 50, 60}, 0);

        assertThat(rowBytes(filter, PngFilter.UP)).isEqualTo(new byte[]{30, 30, 30});
    }

    @Test
    void testSecondRowAverageFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.AVERAGE);
        filter.filter(new byte[]{10, 20, 30}, 0);
        filter.filter(new byte[]{40, 50, 60}, 0);

        assertThat(rowBytes(filter, PngFilter.AVERAGE)).isEqualTo(new byte[]{35, 20, 20});
    }

    @Test
    void testSecondRowPaethFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.PAETH);
        filter.filter(new byte[]{10, 20, 30}, 0);
        filter.filter(new byte[]{40, 50, 60}, 0);

        assertThat(rowBytes(filter, PngFilter.PAETH)).isEqualTo(new byte[]{30, 10, 10});
    }

    @Test
    void testFilterWithOffset() {
        Filter filter = new Filter(FORMAT, PngFilter.NONE);
        filter.filter(new byte[]{99, 10, 20, 30, 99}, 1);

        assertThat(rowBytes(filter, PngFilter.NONE)).isEqualTo(new byte[]{10, 20, 30});
    }

    @Test
    void testAdaptivePicksBestFilter() {
        Filter filter = new Filter(FORMAT, PngFilter.ADAPTIVE);
        PngFilter applied = filter.filter(new byte[]{100, 100, 100}, 0);

        assertThat(applied).isEqualTo(PngFilter.SUB);
        assertThat(rowBytes(filter, applied)).isEqualTo(new byte[]{100, 0, 0});
    }

    private static byte[] rowBytes(Filter filter, PngFilter f) {
        return Arrays.copyOfRange(filter.bestRow(f), BPP, BPP + BPR);
    }
}
