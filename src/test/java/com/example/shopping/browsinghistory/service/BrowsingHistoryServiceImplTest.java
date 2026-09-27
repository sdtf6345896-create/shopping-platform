package com.example.shopping.browsinghistory.service;

import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrowsingHistoryServiceImplTest {

    @Mock
    private BrowsingHistoryItemRepository browsingHistoryItemRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BrowsingHistoryServiceImpl browsingHistoryService;

    @Test
    void recordView_createsItem_whenNotViewedBefore() {
        when(browsingHistoryItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(productRepository.existsById(10L)).thenReturn(true);
        when(memberRepository.getReferenceById(1L)).thenReturn(new Member());

        browsingHistoryService.recordView(1L, 10L);

        verify(browsingHistoryItemRepository).save(any(BrowsingHistoryItem.class));
    }

    @Test
    void recordView_throws_whenProductNotFound() {
        when(browsingHistoryItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(productRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> browsingHistoryService.recordView(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(browsingHistoryItemRepository, never()).save(any());
    }

    @Test
    void recordView_updatesViewedAt_whenAlreadyViewed() {
        BrowsingHistoryItem existing = new BrowsingHistoryItem();
        existing.setViewedAt(LocalDateTime.now().minusDays(1));
        when(browsingHistoryItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.of(existing));

        LocalDateTime before = existing.getViewedAt();
        browsingHistoryService.recordView(1L, 10L);

        assertThat(existing.getViewedAt()).isAfter(before);
        verify(browsingHistoryItemRepository, never()).save(any());
    }

    @Test
    void remove_deletesItem_whenPresent() {
        BrowsingHistoryItem item = new BrowsingHistoryItem();
        item.setId(5L);
        when(browsingHistoryItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.of(item));

        browsingHistoryService.remove(1L, 10L);

        verify(browsingHistoryItemRepository).delete(item);
    }

    @Test
    void remove_isIdempotent_whenNotPresent() {
        when(browsingHistoryItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.empty());

        browsingHistoryService.remove(1L, 10L);

        verify(browsingHistoryItemRepository, never()).delete(any());
    }

    @Test
    void clear_deletesAllItemsForMember() {
        browsingHistoryService.clear(1L);

        verify(browsingHistoryItemRepository).deleteByMemberId(1L);
    }
}
