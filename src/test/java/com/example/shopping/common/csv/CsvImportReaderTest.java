package com.example.shopping.common.csv;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvImportReaderTest {

    @Test
    void splitCells_handlesQuotedCommasEscapedQuotesAndWhitespace() {
        assertThat(CsvImportReader.splitCells("A001, 黑貓宅急便 ,\"台北市, 信義區\",\"他說\"\"好\"\"\","))
                .containsExactly("A001", "黑貓宅急便", "台北市, 信義區", "他說\"好\"", "");
        assertThat(CsvImportReader.splitCells("\"BOTTLE-WHT\", \"25\"")).containsExactly("BOTTLE-WHT", "25");
    }

    @Test
    void splitCells_keepsQuotesInsideUnquotedCell() {
        assertThat(CsvImportReader.splitCells("12\"吋,x")).containsExactly("12\"吋", "x");
    }
}
