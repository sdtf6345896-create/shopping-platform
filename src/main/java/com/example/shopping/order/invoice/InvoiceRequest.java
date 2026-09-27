package com.example.shopping.order.invoice;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** 結帳時的發票選項;格式在 InvoiceService 依開立方式驗證 */
@Getter
@Setter
public class InvoiceRequest {

    /** 不填視為會員載具 */
    private InvoiceType type;

    /** 手機條碼(type = MOBILE_BARCODE) */
    private String carrierCode;

    /** 統一編號(type = COMPANY) */
    private String taxId;

    /** 公司抬頭(type = COMPANY) */
    @Size(max = 60, message = "發票抬頭最多 60 字")
    private String companyTitle;

    /** 愛心碼(type = DONATION) */
    private String donationCode;
}
