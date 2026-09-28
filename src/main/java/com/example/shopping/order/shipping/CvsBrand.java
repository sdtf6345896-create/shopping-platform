package com.example.shopping.order.shipping;

/** 可取貨的超商 */
public enum CvsBrand {
    SEVEN_ELEVEN("7-ELEVEN"),
    FAMILY_MART("全家"),
    HI_LIFE("萊爾富"),
    OK_MART("OK 超商");

    private final String label;

    CvsBrand(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
