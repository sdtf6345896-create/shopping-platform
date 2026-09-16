package com.example.shopping.wishlist.service;

import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.wishlist.dto.response.WishlistItemResponse;
import com.example.shopping.wishlist.entity.WishlistItem;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public WishlistServiceImpl(WishlistItemRepository wishlistItemRepository,
                                MemberRepository memberRepository,
                                ProductRepository productRepository) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WishlistItemResponse> list(Long memberId, Pageable pageable) {
        return wishlistItemRepository.findAllByMemberIdWithDetails(memberId, pageable)
                .map(WishlistItemResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorited(Long memberId, Long productId) {
        return wishlistItemRepository.existsByMemberIdAndProductId(memberId, productId);
    }

    @Override
    public void add(Long memberId, Long productId) {
        if (wishlistItemRepository.existsByMemberIdAndProductId(memberId, productId)) {
            return;
        }
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("商品不存在");
        }
        WishlistItem item = new WishlistItem();
        item.setMember(memberRepository.getReferenceById(memberId));
        item.setProduct(productRepository.getReferenceById(productId));
        wishlistItemRepository.save(item);
    }

    @Override
    public void remove(Long memberId, Long productId) {
        wishlistItemRepository.findByMemberIdAndProductId(memberId, productId)
                .ifPresent(wishlistItemRepository::delete);
    }
}
