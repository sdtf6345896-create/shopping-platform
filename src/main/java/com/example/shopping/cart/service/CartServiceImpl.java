package com.example.shopping.cart.service;

import com.example.shopping.cart.dto.request.AddCartItemRequest;
import com.example.shopping.cart.dto.request.UpdateCartItemRequest;
import com.example.shopping.cart.dto.response.CartItemResponse;
import com.example.shopping.cart.dto.response.CartSummaryResponse;
import com.example.shopping.cart.entity.CartItem;
import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.entity.ProductSku;
import com.example.shopping.product.repository.ProductSkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository productSkuRepository;
    private final MemberRepository memberRepository;

    public CartServiceImpl(CartItemRepository cartItemRepository,
                            ProductSkuRepository productSkuRepository,
                            MemberRepository memberRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productSkuRepository = productSkuRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CartSummaryResponse getCart(Long memberId) {
        var items = cartItemRepository.findAllByMemberIdWithDetails(memberId).stream()
                .map(CartItemResponse::from)
                .toList();
        return CartSummaryResponse.from(items);
    }

    @Override
    public CartItemResponse addItem(Long memberId, AddCartItemRequest request) {
        ProductSku sku = productSkuRepository.findById(request.getSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("商品規格不存在"));

        if (sku.getProduct().getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException("商品已下架,無法加入購物車");
        }

        CartItem cartItem = cartItemRepository.findByMemberIdAndProductSkuId(memberId, sku.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setMember(memberRepository.getReferenceById(memberId));
                    newItem.setProductSku(sku);
                    newItem.setQuantity(0);
                    return newItem;
                });

        int newQuantity = cartItem.getQuantity() + request.getQuantity();
        ensureStockAvailable(sku, newQuantity);
        cartItem.setQuantity(newQuantity);

        return CartItemResponse.from(cartItemRepository.save(cartItem));
    }

    @Override
    public CartItemResponse updateQuantity(Long memberId, Long itemId, UpdateCartItemRequest request) {
        CartItem cartItem = findOwnedOrThrow(memberId, itemId);
        ensureStockAvailable(cartItem.getProductSku(), request.getQuantity());
        cartItem.setQuantity(request.getQuantity());
        return CartItemResponse.from(cartItem);
    }

    @Override
    public void removeItem(Long memberId, Long itemId) {
        CartItem cartItem = findOwnedOrThrow(memberId, itemId);
        cartItemRepository.delete(cartItem);
    }

    @Override
    public void clearCart(Long memberId) {
        cartItemRepository.deleteByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSubtotal(Long memberId, List<Long> cartItemIds) {
        List<CartItem> items = resolveItems(memberId, cartItemIds);
        if (items.isEmpty()) {
            throw new BusinessException("購物車是空的");
        }
        return items.stream()
                .map(item -> item.getProductSku().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CartItem> resolveItems(Long memberId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return cartItemRepository.findAllByMemberIdWithDetails(memberId);
        }
        return cartItemIds.stream()
                .map(id -> cartItemRepository.findByIdAndMemberId(id, memberId)
                        .orElseThrow(() -> new ResourceNotFoundException("購物車項目不存在:" + id)))
                .toList();
    }

    private void ensureStockAvailable(ProductSku sku, int requestedQuantity) {
        if (requestedQuantity > sku.getStock()) {
            throw new BusinessException("庫存不足,尚有 " + sku.getStock() + " 件");
        }
    }

    private CartItem findOwnedOrThrow(Long memberId, Long itemId) {
        return cartItemRepository.findByIdAndMemberId(itemId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("購物車項目不存在"));
    }
}
