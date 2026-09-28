package com.example.shopping.common.csv;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvWriterTest {

    private static final String CRLF = "" + (char) 13 + (char) 10;

    @Test
    void write_prefixesBomAndUsesCrlf() {
        byte[] bytes = CsvWriter.write(List.of("a", "b"), List.of(List.of("1", "x,y")));

        assertThat(Arrays.copyOf(bytes, 3)).containsExactly(0xEF, 0xBB, 0xBF);
        assertThat(new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8))
                .isEqualTo("a,b" + CRLF + "1,\"x,y\"" + CRLF);
    }

    @Test
    void escape_quotesCommasQuotesAndNewlines() {
        assertThat(CsvWriter.escape("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvWriter.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvWriter.escape("line1\nline2")).isEqualTo("\"line1\nline2\"");
        assertThat(CsvWriter.escape(null)).isEmpty();
    }

    @Test
    void escape_neutralizesFormulaInjection_butKeepsNegativeNumbers() {
        assertThat(CsvWriter.escape("=HYPERLINK(\"http://evil\")")).startsWith("\"'=HYPERLINK");
        assertThat(CsvWriter.escape("@SUM(A1)")).isEqualTo("'@SUM(A1)");
        assertThat(CsvWriter.escape("-100.00")).isEqualTo("-100.00");
    }
}
