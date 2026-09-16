package com.example.shopping.wishlist.service;

import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.wishlist.entity.WishlistItem;
import com.example.shopping.wishlist.repository.WishlistItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    @Test
    void add_createsItem_whenNotAlreadyFavorited() {
        when(wishlistItemRepository.existsByMemberIdAndProductId(1L, 10L)).thenReturn(false);
        when(productRepository.existsById(10L)).thenReturn(true);
        when(memberRepository.getReferenceById(1L)).thenReturn(new Member());

        wishlistService.add(1L, 10L);

        verify(wishlistItemRepository).save(any(WishlistItem.class));
    }

    @Test
    void add_isIdempotent_whenAlreadyFavorited() {
        when(wishlistItemRepository.existsByMemberIdAndProductId(1L, 10L)).thenReturn(true);

        wishlistService.add(1L, 10L);

        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void add_throws_whenProductNotFound() {
        when(wishlistItemRepository.existsByMemberIdAndProductId(1L, 10L)).thenReturn(false);
        when(productRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> wishlistService.add(1L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(wishlistItemRepository, never()).save(any());
    }

    @Test
    void remove_deletesItem_whenPresent() {
        WishlistItem item = new WishlistItem();
        item.setId(5L);
        when(wishlistItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.of(item));

        wishlistService.remove(1L, 10L);

        verify(wishlistItemRepository).delete(item);
    }

    @Test
    void remove_isIdempotent_whenNotPresent() {
        when(wishlistItemRepository.findByMemberIdAndProductId(1L, 10L)).thenReturn(Optional.empty());

        wishlistService.remove(1L, 10L);

        verify(wishlistItemRepository, never()).delete(any());
    }

    @Test
    void isFavorited_delegatesToRepository() {
        when(wishlistItemRepository.existsByMemberIdAndProductId(1L, 10L)).thenReturn(true);

        assertThat(wishlistService.isFavorited(1L, 10L)).isTrue();
    }
}
