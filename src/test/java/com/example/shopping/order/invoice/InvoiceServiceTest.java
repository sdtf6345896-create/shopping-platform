package com.example.shopping.order.invoice;

import com.example.shopping.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoiceServiceTest {

    private final InvoiceService invoiceService = new InvoiceService();

    private static InvoiceRequest request(InvoiceType type) {
        InvoiceRequest request = new InvoiceRequest();
        request.setType(type);
        return request;
    }

    @Test
    void defaultsToMemberCarrier() {
        assertThat(invoiceService.resolve(null).getType()).isEqualTo(InvoiceType.MEMBER_CARRIER);
        assertThat(invoiceService.resolve(new InvoiceRequest()).getType()).isEqualTo(InvoiceType.MEMBER_CARRIER);
    }

    @Test
    void mobileBarcode_isUppercased_andOtherFieldsDropped() {
        InvoiceRequest request = request(InvoiceType.MOBILE_BARCODE);
        request.setCarrierCode(" /abc1234 ");
        request.setTaxId("04595257");

        InvoiceInfo info = invoiceService.resolve(request);

        assertThat(info.getCarrierCode()).isEqualTo("/ABC1234");
        assertThat(info.getTaxId()).isNull();
    }

    @Test
    void company_requiresValidTaxIdAndTitle() {
        InvoiceRequest request = request(InvoiceType.COMPANY);
        request.setTaxId("04595258");
        request.setCompanyTitle("範例股份有限公司");
        assertThatThrownBy(() -> invoiceService.resolve(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("統一編號");

        request.setTaxId("04595257");
        request.setCompanyTitle("  ");
        assertThatThrownBy(() -> invoiceService.resolve(request)).hasMessageContaining("抬頭");

        request.setCompanyTitle("範例股份有限公司");
        InvoiceInfo info = invoiceService.resolve(request);
        assertThat(info.getTaxId()).isEqualTo("04595257");
        assertThat(info.getCompanyTitle()).isEqualTo("範例股份有限公司");
    }

    @Test
    void donation_requiresValidCode() {
        InvoiceRequest request = request(InvoiceType.DONATION);
        request.setDonationCode("12");
        assertThatThrownBy(() -> invoiceService.resolve(request)).hasMessageContaining("愛心碼");

        request.setDonationCode("919");
        assertThat(invoiceService.resolve(request).getDonationCode()).isEqualTo("919");
    }
}
