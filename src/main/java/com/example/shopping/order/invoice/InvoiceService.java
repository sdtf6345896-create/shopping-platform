package com.example.shopping.order.invoice;

import com.example.shopping.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** 依開立方式驗證並整理發票欄位,多餘的欄位一律不存 */
@Component
public class InvoiceService {

    public InvoiceInfo resolve(InvoiceRequest request) {
        InvoiceInfo info = new InvoiceInfo();
        if (request == null || request.getType() == null || request.getType() == InvoiceType.MEMBER_CARRIER) {
            return info;
        }
        info.setType(request.getType());
        switch (request.getType()) {
            case MOBILE_BARCODE -> {
                String code = trim(request.getCarrierCode());
                code = code == null ? null : code.toUpperCase(Locale.ROOT);
                if (!InvoiceRules.isValidMobileBarcode(code)) {
                    throw new BusinessException("手機條碼格式不正確(斜線開頭加 7 碼英數字)");
                }
                info.setCarrierCode(code);
            }
            case COMPANY -> {
                String taxId = trim(request.getTaxId());
                if (!InvoiceRules.isValidTaxId(taxId)) {
                    throw new BusinessException("統一編號不正確");
                }
                String title = trim(request.getCompanyTitle());
                if (title == null) {
                    throw new BusinessException("請填寫發票抬頭");
                }
                info.setTaxId(taxId);
                info.setCompanyTitle(title);
            }
            case DONATION -> {
                String code = trim(request.getDonationCode());
                if (!InvoiceRules.isValidDonationCode(code)) {
                    throw new BusinessException("愛心碼格式不正確(3 到 7 位數字)");
                }
                info.setDonationCode(code);
            }
            default -> {
            }
        }
        return info;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
