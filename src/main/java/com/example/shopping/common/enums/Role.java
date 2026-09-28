package com.example.shopping.common.enums;

public enum Role {
    MEMBER,
    /** 後台最高權限 */
    ADMIN,
    /** 後台客服人員:處理訂單、退貨、問答、評價、留言,商品與會員只能查看 */
    STAFF;

    /** 後台帳號(管理員或客服) */
    public boolean isBackOffice() {
        return this == ADMIN || this == STAFF;
    }
}
