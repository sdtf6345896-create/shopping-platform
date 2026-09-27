package com.example.shopping.order.invoice;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceRulesTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "04595257",   // 財政部範例(總和 40,新舊規則皆有效)
            "22099131",   // 台積電
            "10000004",   // 總和 5:只有 2023 年新規則(被 5 整除)才有效
            "10000070"    // 第 7 位為 7,總和 11:須把 28 → 10 視為 0(總和 10)才有效
    })
    void validTaxIds(String taxId) {
        assertThat(InvoiceRules.isValidTaxId(taxId)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"04595258", "12345678", "10000072", "1234567", "abcdefgh", "045952570", ""})
    void invalidTaxIds(String taxId) {
        assertThat(InvoiceRules.isValidTaxId(taxId)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ABC1234", "/A.B+C-1", "/1234567"})
    void validMobileBarcodes(String code) {
        assertThat(InvoiceRules.isValidMobileBarcode(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12345", "/abc1234", "/ABC123", "/ABC12345", "/ABC 123"})
    void invalidMobileBarcodes(String code) {
        assertThat(InvoiceRules.isValidMobileBarcode(code)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"919", "8585", "1234567"})
    void validDonationCodes(String code) {
        assertThat(InvoiceRules.isValidDonationCode(code)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "12345678", "12a4"})
    void invalidDonationCodes(String code) {
        assertThat(InvoiceRules.isValidDonationCode(code)).isFalse();
    }
}
