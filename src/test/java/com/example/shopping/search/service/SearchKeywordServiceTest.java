package com.example.shopping.search.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchKeywordServiceTest {

    @Test
    void normalize_trimsCollapsesSpacesAndLowercases() {
        assertThat(SearchKeywordService.normalize("  Nike   Air  ")).isEqualTo("nike air");
        assertThat(SearchKeywordService.normalize("Nike\t\tAir")).isEqualTo("nike air");
    }

    @Test
    void normalize_rejectsBlankAndOverlongKeywords() {
        assertThat(SearchKeywordService.normalize("   ")).isNull();
        assertThat(SearchKeywordService.normalize(null)).isNull();
        assertThat(SearchKeywordService.normalize("a".repeat(31))).isNull();
        assertThat(SearchKeywordService.normalize("a".repeat(30))).hasSize(30);
    }
}
