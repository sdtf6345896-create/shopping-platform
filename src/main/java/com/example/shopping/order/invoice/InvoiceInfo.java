package com.example.shopping.order.invoice;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 訂單上的發票資訊(只會有對應開立方式的欄位有值) */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class InvoiceInfo {

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private InvoiceType type = InvoiceType.MEMBER_CARRIER;

    @Column(name = "invoice_carrier", length = 8)
    private String carrierCode;

    @Column(name = "invoice_tax_id", length = 8)
    private String taxId;

    @Column(name = "invoice_title", length = 60)
    private String companyTitle;

    @Column(name = "invoice_donation_code", length = 7)
    private String donationCode;
}
