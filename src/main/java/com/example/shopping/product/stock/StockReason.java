package com.example.shopping.product.stock;

public enum StockReason {
    /** 新增商品 / 規格時的初始庫存 */
    INITIAL,
    /** 會員下單扣庫存 */
    ORDER,
    /** 訂單取消歸還 */
    ORDER_CANCEL,
    /** 退貨核准歸還 */
    RETURN,
    /** 後台手動調整 */
    MANUAL,
    /** CSV 匯入 */
    IMPORT,
    /** 編輯商品時修改規格庫存 */
    PRODUCT_EDIT
}
