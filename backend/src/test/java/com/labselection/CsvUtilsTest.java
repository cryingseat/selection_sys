package com.labselection;

import com.labselection.util.CsvUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CsvUtilsTest {

    @Test
    void shouldParseAndIgnoreInvalidFragments() {
        assertEquals(List.of(1L, 3L, 5L), CsvUtils.parseIds("1, 3, x, 5"));
        assertEquals(List.of(), CsvUtils.parseIds(""));
    }

    @Test
    void shouldJoinDistinctIds() {
        assertEquals("1,2,3", CsvUtils.joinIds(List.of(1L, 2L, 3L, 2L)));
        assertNull(CsvUtils.joinIds(List.of()));
    }
}
