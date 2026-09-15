package com.example.shopping.cart.service;

import com.example.shopping.cart.dto.request.AddCartItemRequest;
import com.example.shopping.cart.dto.request.UpdateCartItemRequest;
import com.example.shopping.cart.dto.response.CartItemResponse;
import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductSkuRepository productSkuRepository;
    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    private Product onSaleProduct;
    private ProductSku sku;

    @BeforeEach
    void setUp() {
        onSaleProduct = new Product();
        onSaleProduct.setId(10L);
        onSaleProduct.setName("測試商品");
        onSaleProduct.setStatus(ProductStatus.ON_SALE);

        sku = new ProductSku();
        sku.setId(1L);
        sku.setProduct(onSaleProduct);
        sku.setSpecName("均一規格");
        sku.setPrice(new BigDecimal("100.00"));
        sku.setStock(5);
    }

    @Test
    void addItem_createsNewCartItem_whenNotAlreadyInCart() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setSkuId(1L);
        request.setQuantity(2);

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(cartItemRepository.findByMemberIdAndProductSkuId(100L, 1L)).thenReturn(Optional.empty());
        when(memberRepository.getReferenceById(100L)).thenReturn(new Member());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemResponse response = cartService.addItem(100L, request);

        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void addItem_mergesQuantity_whenAlreadyInCart() {
        CartItem existing = new CartItem();
        existing.setId(5L);
        existing.setProductSku(sku);
        existing.setQuantity(1);

        AddCartItemRequest request = new AddCartItemRequest();
        request.setSkuId(1L);
        request.setQuantity(2);

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(cartItemRepository.findByMemberIdAndProductSkuId(100L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> inv.getArgument(0));

        CartItemResponse response = cartService.addItem(100L, request);

        assertThat(response.getQuantity()).isEqualTo(3);
        verify(memberRepository, never()).getReferenceById(any());
    }

    @Test
    void addItem_throws_whenProductOffShelf() {
        onSaleProduct.setStatus(ProductStatus.OFF_SHELF);
        AddCartItemRequest request = new AddCartItemRequest();
        request.setSkuId(1L);
        request.setQuantity(1);

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));

        assertThatThrownBy(() -> cartService.addItem(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已下架");
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_throws_whenExceedsStock() {
        AddCartItemRequest request = new AddCartItemRequest();
        request.setSkuId(1L);
        request.setQuantity(10);

        when(productSkuRepository.findById(1L)).thenReturn(Optional.of(sku));
        when(cartItemRepository.findByMemberIdAndProductSkuId(100L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(100L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("庫存不足");
    }

    @Test
    void updateQuantity_throws_whenExceedsStock() {
        CartItem existing = new CartItem();
        existing.setId(5L);
        existing.setProductSku(sku);
        existing.setQuantity(1);

        when(cartItemRepository.findByIdAndMemberId(5L, 100L)).thenReturn(Optional.of(existing));

        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(99);

        assertThatThrownBy(() -> cartService.updateQuantity(100L, 5L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("庫存不足");
    }

    @Test
    void updateQuantity_throws_whenItemNotOwnedByMember() {
        when(cartItemRepository.findByIdAndMemberId(5L, 100L)).thenReturn(Optional.empty());
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(1);

        assertThatThrownBy(() -> cartService.updateQuantity(100L, 5L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_deletesWhenOwned() {
        CartItem existing = new CartItem();
        existing.setId(5L);
        when(cartItemRepository.findByIdAndMemberId(5L, 100L)).thenReturn(Optional.of(existing));

        cartService.removeItem(100L, 5L);

        verify(cartItemRepository).delete(existing);
    }

    @Test
    void clearCart_deletesAllItemsForMember() {
        cartService.clearCart(100L);

        verify(cartItemRepository).deleteByMemberId(100L);
    }
}
