package com.example.shopping.cart.dto.response;

import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long skuId;
    private Long productId;
    private String productName;
    private String specName;
    private String mainImage;
    private BigDecimal price;
    private int quantity;
    private BigDecimal subtotal;
    private int stock;
    private ProductStatus productStatus;

    public static CartItemResponse from(CartItem cartItem) {
        ProductSku sku = cartItem.getProductSku();
        Product product = sku.getProduct();
        BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));

        return new CartItemResponse(
                cartItem.getId(),
                sku.getId(),
                product.getId(),
                product.getName(),
                sku.getSpecName(),
                product.getMainImage(),
                sku.getPrice(),
                cartItem.getQuantity(),
                subtotal,
                sku.getStock(),
                product.getStatus());
    }
}
