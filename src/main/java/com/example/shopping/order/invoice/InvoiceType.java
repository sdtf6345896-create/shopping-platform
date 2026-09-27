package com.example.shopping.order.invoice;

/** 電子發票開立方式 */
public enum InvoiceType {
    /** 會員載具(發票存在本站帳號,中獎時通知) */
    MEMBER_CARRIER,
    /** 手機條碼載具 */
    MOBILE_BARCODE,
    /** 公司戶(三聯式,需統一編號與抬頭) */
    COMPANY,
    /** 捐贈(需愛心碼) */
    DONATION
}
