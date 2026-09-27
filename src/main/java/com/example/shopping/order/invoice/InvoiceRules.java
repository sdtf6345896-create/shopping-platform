package com.example.shopping.order.invoice;

import java.util.regex.Pattern;

/**
 * 台灣電子發票欄位格式驗證(純函式,前後端規則一致)。
 */
public final class InvoiceRules {

    /** 手機條碼:斜線開頭 + 7 碼(數字、大寫英文、. + -) */
    private static final Pattern MOBILE_BARCODE = Pattern.compile("^/[0-9A-Z.+\\-]{7}$");
    /** 捐贈碼(愛心碼):3~7 位數字 */
    private static final Pattern DONATION_CODE = Pattern.compile("^\\d{3,7}$");
    private static final Pattern EIGHT_DIGITS = Pattern.compile("^\\d{8}$");
    private static final int[] TAX_ID_WEIGHTS = {1, 2, 1, 2, 1, 2, 4, 1};

    private InvoiceRules() {
    }

    public static boolean isValidMobileBarcode(String value) {
        return value != null && MOBILE_BARCODE.matcher(value).matches();
    }

    public static boolean isValidDonationCode(String value) {
        return value != null && DONATION_CODE.matcher(value).matches();
    }

    /**
     * 營利事業統一編號檢查碼。各位數乘上權數 1,2,1,2,1,2,4,1 後把乘積的十位數與個位數相加再加總,
     * 依財政部 2023 年起的規則,總和能被 5 整除即有效(可相容舊的「被 10 整除」編號)。
     * 第 7 位是 7 時乘積 28 → 2+8 = 10,可視為 1 或 0,兩者任一符合即可。
     */
    public static boolean isValidTaxId(String value) {
        if (value == null || !EIGHT_DIGITS.matcher(value).matches()) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            int product = (value.charAt(i) - '0') * TAX_ID_WEIGHTS[i];
            sum += product / 10 + product % 10;
        }
        if (sum % 5 == 0) {
            return true;
        }
        // 第 7 位為 7:10 取 1 時上面已算進去;改取 0 等於總和少 1
        return value.charAt(6) == '7' && (sum - 1) % 5 == 0;
    }
}
