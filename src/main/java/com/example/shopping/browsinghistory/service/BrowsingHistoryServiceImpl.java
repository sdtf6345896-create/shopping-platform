package com.example.shopping.browsinghistory.service;

import com.example.shopping.browsinghistory.dto.response.BrowsingHistoryItemResponse;
import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class BrowsingHistoryServiceImpl implements BrowsingHistoryService {

    private final BrowsingHistoryItemRepository browsingHistoryItemRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    public BrowsingHistoryServiceImpl(BrowsingHistoryItemRepository browsingHistoryItemRepository,
                                       MemberRepository memberRepository,
                                       ProductRepository productRepository) {
        this.browsingHistoryItemRepository = browsingHistoryItemRepository;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BrowsingHistoryItemResponse> list(Long memberId, Pageable pageable) {
        return browsingHistoryItemRepository.findAllByMemberIdWithDetails(memberId, pageable)
                .map(BrowsingHistoryItemResponse::from);
    }

    @Override
    public void recordView(Long memberId, Long productId) {
        BrowsingHistoryItem existing = browsingHistoryItemRepository
                .findByMemberIdAndProductId(memberId, productId)
                .orElse(null);
        if (existing != null) {
            existing.setViewedAt(LocalDateTime.now());
            return;
        }

        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("商品不存在");
        }
        BrowsingHistoryItem item = new BrowsingHistoryItem();
        item.setMember(memberRepository.getReferenceById(memberId));
        item.setProduct(productRepository.getReferenceById(productId));
        item.setViewedAt(LocalDateTime.now());
        browsingHistoryItemRepository.save(item);
    }

    @Override
    public void remove(Long memberId, Long productId) {
        browsingHistoryItemRepository.findByMemberIdAndProductId(memberId, productId)
                .ifPresent(browsingHistoryItemRepository::delete);
    }

    @Override
    public void clear(Long memberId) {
        browsingHistoryItemRepository.deleteByMemberId(memberId);
    }
}
