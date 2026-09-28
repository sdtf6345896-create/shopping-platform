package com.example.shopping.order.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 超商取貨資訊。沒有串接超商電子地圖(需付費簽約),由會員自行填寫門市;
 * 取件人姓名需與證件相符,手機用來收到貨簡訊。
 */
@Getter
@Setter
public class CvsPickupRequest {

    @NotNull(message = "請選擇取貨超商")
    private CvsBrand brand;

    @NotBlank(message = "請填寫取貨門市")
    @Size(max = 30, message = "門市名稱最多 30 字")
    private String storeName;

    /** 門市店號,選填 */
    @Pattern(regexp = "^\\d{0,8}$", message = "店號只能是數字(最多 8 碼)")
    private String storeCode;

    @NotBlank(message = "請填寫取件人姓名")
    @Size(max = 50, message = "取件人姓名最多 50 字")
    private String recipientName;

    @NotBlank(message = "請填寫取件人手機")
    @Pattern(regexp = "^09\\d{8}$", message = "取件人手機格式應為 09 開頭的 10 碼數字")
    private String recipientPhone;

    /** 寫進訂單收件地址的門市描述,例如「7-ELEVEN 信義門市(店號 123456)」 */
    public String describeStore() {
        String code = storeCode == null || storeCode.isBlank() ? "" : "(店號 " + storeCode.trim() + ")";
        return brand.getLabel() + " " + storeName.trim() + code;
    }
}
