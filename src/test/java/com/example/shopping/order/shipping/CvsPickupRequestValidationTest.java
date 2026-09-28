package com.example.shopping.order.shipping;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CvsPickupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<ConstraintViolation<CvsPickupRequest>> validate(String phone, String storeCode) {
        CvsPickupRequest request = new CvsPickupRequest();
        request.setBrand(CvsBrand.FAMILY_MART);
        request.setStoreName("台大店");
        request.setStoreCode(storeCode);
        request.setRecipientName("王小明");
        request.setRecipientPhone(phone);
        return validator.validate(request);
    }

    @Test
    void acceptsTaiwanMobileAndOptionalNumericStoreCode() {
        assertThat(validate("0912345678", "013579")).isEmpty();
        assertThat(validate("0912345678", null)).isEmpty();
        assertThat(validate("0912345678", "")).isEmpty();
    }

    @Test
    void rejectsLandlineOrMalformedPhoneAndNonNumericStoreCode() {
        assertThat(validate("0223456789", null)).isNotEmpty();
        assertThat(validate("091234567", null)).isNotEmpty();
        assertThat(validate("0912-345-678", null)).isNotEmpty();
        assertThat(validate("0912345678", "A123")).isNotEmpty();
    }

    @Test
    void describeStore_omitsBlankStoreCode() {
        CvsPickupRequest request = new CvsPickupRequest();
        request.setBrand(CvsBrand.FAMILY_MART);
        request.setStoreName(" 台大店 ");
        request.setStoreCode(" ");
        assertThat(request.describeStore()).isEqualTo("全家 台大店");
    }
}
