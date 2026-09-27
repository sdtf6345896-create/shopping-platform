package com.example.shopping.order.dto.response;

import com.example.shopping.order.invoice.InvoiceInfo;
import com.example.shopping.order.invoice.InvoiceType;

public record InvoiceResponse(InvoiceType type, String carrierCode, String taxId, String companyTitle,
                              String donationCode) {

    public static InvoiceResponse from(InvoiceInfo info) {
        InvoiceInfo safe = info == null ? new InvoiceInfo() : info;
        return new InvoiceResponse(safe.getType(), safe.getCarrierCode(), safe.getTaxId(), safe.getCompanyTitle(),
                safe.getDonationCode());
    }
}
